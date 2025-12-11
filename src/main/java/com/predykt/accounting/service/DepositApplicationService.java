package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.*;
import com.predykt.accounting.dto.request.JournalEntryLineRequest;
import com.predykt.accounting.dto.request.JournalEntryRequest;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.exception.ValidationException;
import com.predykt.accounting.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service métier pour la gestion des imputations partielles d'acomptes (Phase 2).
 *
 * Ce service permet de:
 * - Appliquer partiellement un acompte sur une facture
 * - Gérer plusieurs imputations d'un même acompte sur différentes factures
 * - Annuler des imputations partielles
 * - Recalculer les montants disponibles
 * - Obtenir des statistiques sur les imputations
 *
 * Conformité OHADA:
 * - Chaque imputation partielle génère une écriture comptable distincte
 * - DÉBIT 4191 (Avances) + DÉBIT 4431 (TVA) / CRÉDIT 411 (Clients)
 * - Traçabilité complète de toutes les imputations
 *
 * @author PREDYKT Accounting Team
 * @version 2.0 (Phase 2 - Imputation Partielle)
 * @since 2025-12-11
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DepositApplicationService {

    private final DepositApplicationRepository applicationRepository;
    private final DepositRepository depositRepository;
    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;
    private final GeneralLedgerService generalLedgerService;

    // =====================================================================
    // Imputation partielle
    // =====================================================================

    /**
     * Applique partiellement un acompte sur une facture.
     *
     * @param companyId ID de l'entreprise
     * @param depositId ID de l'acompte
     * @param invoiceId ID de la facture
     * @param amountToApply Montant à imputer (TTC)
     * @param appliedBy Utilisateur effectuant l'imputation
     * @param notes Notes optionnelles
     * @return L'imputation créée
     * @throws ResourceNotFoundException si l'acompte ou la facture n'existe pas
     * @throws ValidationException si les validations métier échouent
     */
    public DepositApplication applyPartially(Long companyId, Long depositId, Long invoiceId,
                                            BigDecimal amountToApply, String appliedBy, String notes) {
        log.info("🔄 Imputation partielle: {} XAF de l'acompte {} sur facture {} par {}",
            amountToApply, depositId, invoiceId, appliedBy);

        // 1. Récupérer les entités
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise", "id", companyId));

        Deposit deposit = depositRepository.findById(depositId)
            .orElseThrow(() -> new ResourceNotFoundException("Acompte", "id", depositId));

        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Facture", "id", invoiceId));

        // 2. Validations métier
        validatePartialApplication(deposit, invoice, amountToApply, company);

        // 3. Calculer les montants proportionnels
        BigDecimal amountHt = calculateProportionalHt(amountToApply, deposit.getVatRate());
        BigDecimal vatAmount = amountToApply.subtract(amountHt);

        // 4. Créer l'imputation
        DepositApplication application = DepositApplication.builder()
            .deposit(deposit)
            .invoice(invoice)
            .company(company)
            .amountHt(amountHt)
            .vatRate(deposit.getVatRate())
            .vatAmount(vatAmount)
            .amountTtc(amountToApply)
            .appliedAt(LocalDateTime.now())
            .appliedBy(appliedBy)
            .notes(notes)
            .build();

        // Valider l'imputation
        application.validate();

        // 5. Sauvegarder l'imputation
        DepositApplication savedApplication = applicationRepository.save(application);

        // 6. Mettre à jour l'acompte
        deposit.addApplication(savedApplication);
        depositRepository.save(deposit);

        // 7. Mettre à jour la facture
        updateInvoiceAmounts(invoice, amountToApply);
        invoiceRepository.save(invoice);

        // 8. Générer l'écriture comptable OHADA
        Long journalEntryId = createPartialApplicationJournalEntry(savedApplication, deposit, invoice);
        savedApplication.setJournalEntryId(journalEntryId);
        applicationRepository.save(savedApplication);

        log.info("✅ Imputation partielle créée: {} XAF imputés (restant sur acompte: {} XAF)",
            amountToApply, deposit.getAvailableAmount());

        return savedApplication;
    }

    /**
     * Annule une imputation partielle.
     *
     * @param companyId ID de l'entreprise
     * @param applicationId ID de l'imputation à annuler
     * @throws ResourceNotFoundException si l'imputation n'existe pas
     */
    public void cancelApplication(Long companyId, Long applicationId) {
        log.warn("⚠️ Annulation imputation partielle ID {}", applicationId);

        DepositApplication application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Imputation", "id", applicationId));

        // Vérifier l'entreprise
        if (!application.getCompany().getId().equals(companyId)) {
            throw new ValidationException("Cette imputation n'appartient pas à l'entreprise spécifiée");
        }

        Deposit deposit = application.getDeposit();
        Invoice invoice = application.getInvoice();
        BigDecimal amountTtc = application.getAmountTtc();

        // 1. Retirer l'imputation de l'acompte
        deposit.removeApplication(application);
        depositRepository.save(deposit);

        // 2. Mettre à jour la facture
        updateInvoiceAmounts(invoice, amountTtc.negate());
        invoiceRepository.save(invoice);

        // 3. Supprimer l'imputation
        applicationRepository.delete(application);

        // 4. TODO: Générer une écriture comptable d'annulation (contrepassation)

        log.info("✅ Imputation partielle annulée: {} XAF restitués à l'acompte {}",
            amountTtc, deposit.getDepositNumber());
    }

    // =====================================================================
    // Validations
    // =====================================================================

    /**
     * Valide les conditions pour une imputation partielle.
     */
    private void validatePartialApplication(Deposit deposit, Invoice invoice,
                                           BigDecimal amountToApply, Company company) {
        // Vérifier que le montant à imputer est positif
        if (amountToApply.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Le montant à imputer doit être strictement positif");
        }

        // Vérifier que l'acompte a suffisamment de montant disponible
        BigDecimal availableAmount = deposit.getAvailableAmount();
        if (amountToApply.compareTo(availableAmount) > 0) {
            throw new ValidationException(String.format(
                "Montant à imputer (%s XAF) dépasse le montant disponible de l'acompte (%s XAF)",
                amountToApply, availableAmount
            ));
        }

        // Vérifier que la facture a un montant restant dû suffisant
        BigDecimal invoiceDue = invoice.getAmountDue();
        if (amountToApply.compareTo(invoiceDue) > 0) {
            throw new ValidationException(String.format(
                "Montant à imputer (%s XAF) dépasse le montant restant dû sur la facture (%s XAF)",
                amountToApply, invoiceDue
            ));
        }

        // Vérifier que l'acompte et la facture appartiennent à la même entreprise
        if (!deposit.getCompany().getId().equals(company.getId())) {
            throw new ValidationException("L'acompte n'appartient pas à l'entreprise spécifiée");
        }

        if (!invoice.getCompany().getId().equals(company.getId())) {
            throw new ValidationException("La facture n'appartient pas à l'entreprise spécifiée");
        }

        // Vérifier que le client correspond
        if (deposit.getCustomer() != null && invoice.getCustomer() != null) {
            if (!deposit.getCustomer().getId().equals(invoice.getCustomer().getId())) {
                throw new ValidationException(String.format(
                    "L'acompte appartient au client %s mais la facture au client %s",
                    deposit.getCustomer().getName(),
                    invoice.getCustomer().getName()
                ));
            }
        }
    }

    /**
     * Calcule le montant HT proportionnel à partir d'un montant TTC et d'un taux de TVA.
     *
     * @param amountTtc Montant TTC
     * @param vatRate Taux de TVA (en %)
     * @return Montant HT
     */
    private BigDecimal calculateProportionalHt(BigDecimal amountTtc, BigDecimal vatRate) {
        // Formule: HT = TTC / (1 + taux/100)
        BigDecimal divisor = BigDecimal.ONE.add(vatRate.divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP));
        return amountTtc.divide(divisor, 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Met à jour les montants de la facture après imputation/annulation.
     *
     * @param invoice La facture
     * @param amount Montant à ajouter (positif pour imputation, négatif pour annulation)
     */
    private void updateInvoiceAmounts(Invoice invoice, BigDecimal amount) {
        BigDecimal currentPaid = invoice.getAmountPaid() != null ? invoice.getAmountPaid() : BigDecimal.ZERO;
        BigDecimal newPaid = currentPaid.add(amount);

        invoice.setAmountPaid(newPaid);

        BigDecimal totalTtc = invoice.getTotalTtc();
        BigDecimal newDue = totalTtc.subtract(newPaid);
        invoice.setAmountDue(newDue);

        // Mettre à jour le statut de la facture
        if (newDue.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.markAsPaid();
        } else if (newPaid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.markAsPartiallyPaid();
        } else {
            invoice.markAsUnpaid();
        }
    }

    // =====================================================================
    // Écriture comptable OHADA
    // =====================================================================

    /**
     * Génère l'écriture comptable pour une imputation partielle.
     *
     * DÉBIT  4191 Clients - Avances         (HT)
     * DÉBIT  4431 TVA collectée              (TVA)
     *     CRÉDIT 411  Clients                    (TTC)
     *
     * @param application L'imputation partielle
     * @param deposit L'acompte source
     * @param invoice La facture destination
     * @return ID de l'écriture comptable créée
     */
    private Long createPartialApplicationJournalEntry(DepositApplication application,
                                                      Deposit deposit,
                                                      Invoice invoice) {
        List<JournalEntryLineRequest> lines = new ArrayList<>();

        // DÉBIT: Compte 4191 - Clients - Avances et acomptes reçus (HT)
        lines.add(JournalEntryLineRequest.builder()
            .accountNumber("4191")
            .description(String.format("Imputation partielle %s sur facture %s",
                deposit.getDepositNumber(), invoice.getInvoiceNumber()))
            .debit(application.getAmountHt())
            .credit(BigDecimal.ZERO)
            .build());

        // DÉBIT: Compte 4431 - TVA collectée
        lines.add(JournalEntryLineRequest.builder()
            .accountNumber("4431")
            .description(String.format("TVA sur imputation partielle %s",
                deposit.getDepositNumber()))
            .debit(application.getVatAmount())
            .credit(BigDecimal.ZERO)
            .build());

        // CRÉDIT: Compte 411 - Clients
        lines.add(JournalEntryLineRequest.builder()
            .accountNumber("411")
            .description(String.format("Imputation partielle acompte %s",
                deposit.getDepositNumber()))
            .debit(BigDecimal.ZERO)
            .credit(application.getAmountTtc())
            .build());

        // Créer l'écriture comptable
        JournalEntryRequest journalEntry = JournalEntryRequest.builder()
            .entryDate(LocalDate.now())
            .reference(String.format("IMP-PART-%s-%s-%d",
                deposit.getDepositNumber(),
                invoice.getInvoiceNumber(),
                application.getId()))
            .journalCode("OD")
            .lines(lines)
            .build();

        try {
            GeneralLedger entry = generalLedgerService.createEntry(
                deposit.getCompany().getId(),
                journalEntry
            );
            return entry.getId();
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de l'écriture comptable d'imputation partielle", e);
            throw new ValidationException("Échec de la création de l'écriture comptable: " + e.getMessage());
        }
    }

    // =====================================================================
    // Consultations
    // =====================================================================

    /**
     * Récupère toutes les imputations d'un acompte.
     *
     * @param companyId ID de l'entreprise
     * @param depositId ID de l'acompte
     * @return Liste des imputations
     */
    @Transactional(readOnly = true)
    public List<DepositApplication> getApplicationsByDeposit(Long companyId, Long depositId) {
        Deposit deposit = depositRepository.findById(depositId)
            .orElseThrow(() -> new ResourceNotFoundException("Acompte", "id", depositId));

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise", "id", companyId));

        return applicationRepository.findByCompanyAndDeposit(company, deposit);
    }

    /**
     * Récupère toutes les imputations sur une facture.
     *
     * @param companyId ID de l'entreprise
     * @param invoiceId ID de la facture
     * @return Liste des imputations
     */
    @Transactional(readOnly = true)
    public List<DepositApplication> getApplicationsByInvoice(Long companyId, Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Facture", "id", invoiceId));

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise", "id", companyId));

        return applicationRepository.findByCompanyAndInvoice(company, invoice);
    }

    /**
     * Récupère une imputation par son ID.
     *
     * @param companyId ID de l'entreprise
     * @param applicationId ID de l'imputation
     * @return L'imputation
     */
    @Transactional(readOnly = true)
    public DepositApplication getApplicationById(Long companyId, Long applicationId) {
        DepositApplication application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Imputation", "id", applicationId));

        if (!application.getCompany().getId().equals(companyId)) {
            throw new ValidationException("Cette imputation n'appartient pas à l'entreprise spécifiée");
        }

        return application;
    }

    /**
     * Récupère toutes les imputations d'une entreprise (avec pagination).
     *
     * @param companyId ID de l'entreprise
     * @param pageable Pagination
     * @return Page d'imputations
     */
    @Transactional(readOnly = true)
    public Page<DepositApplication> getAllApplications(Long companyId, Pageable pageable) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise", "id", companyId));

        return applicationRepository.findByCompany(company, pageable);
    }

    /**
     * Récupère les imputations récentes d'une entreprise.
     *
     * @param companyId ID de l'entreprise
     * @param pageable Pagination (limite)
     * @return Page des imputations récentes
     */
    @Transactional(readOnly = true)
    public Page<DepositApplication> getRecentApplications(Long companyId, Pageable pageable) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise", "id", companyId));

        return applicationRepository.findByCompanyOrderByAppliedAtDesc(company, pageable);
    }

    // =====================================================================
    // Statistiques
    // =====================================================================

    /**
     * Calcule le montant total imputé pour un acompte.
     *
     * @param depositId ID de l'acompte
     * @return Montant total imputé
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalAppliedForDeposit(Long depositId) {
        Deposit deposit = depositRepository.findById(depositId)
            .orElseThrow(() -> new ResourceNotFoundException("Acompte", "id", depositId));

        return applicationRepository.sumAmountByDeposit(deposit);
    }

    /**
     * Calcule le montant total des acomptes imputés sur une facture.
     *
     * @param invoiceId ID de la facture
     * @return Montant total des acomptes
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalDepositsForInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Facture", "id", invoiceId));

        return applicationRepository.sumAmountByInvoice(invoice);
    }

    /**
     * Compte le nombre d'imputations pour un acompte.
     *
     * @param depositId ID de l'acompte
     * @return Nombre d'imputations
     */
    @Transactional(readOnly = true)
    public long getApplicationCountForDeposit(Long depositId) {
        Deposit deposit = depositRepository.findById(depositId)
            .orElseThrow(() -> new ResourceNotFoundException("Acompte", "id", depositId));

        return applicationRepository.countByDeposit(deposit);
    }
}
