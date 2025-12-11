package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.*;
import com.predykt.accounting.dto.request.DepositApplyRequest;
import com.predykt.accounting.dto.request.DepositCreateRequest;
import com.predykt.accounting.dto.request.DepositUpdateRequest;
import com.predykt.accounting.dto.request.JournalEntryLineRequest;
import com.predykt.accounting.dto.request.JournalEntryRequest;
import com.predykt.accounting.dto.response.DepositResponse;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.exception.ValidationException;
import com.predykt.accounting.mapper.DepositMapper;
import com.predykt.accounting.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 🔵 SERVICE CRITIQUE: Gestion des Acomptes (OHADA Compte 4191)
 *
 * Ce service gère le cycle de vie complet des acomptes reçus des clients:
 * 1. Réception acompte → Génération reçu RA-YYYY-NNNNNN
 * 2. Écriture comptable: Débit 512 Banque / Crédit 4191 Avances + 4431 TVA
 * 3. Facturation finale → Imputation acompte
 * 4. Écriture comptable: Débit 4191 + 4431 TVA / Crédit 411 Clients
 *
 * Conformité OHADA:
 * - SYSCOHADA Articles 276-279: Compte 4191 obligatoire
 * - CGI Cameroun Article 128: TVA exigible sur encaissement
 * - Taux TVA: 19.25% (standard Cameroun)
 *
 * Fonctionnalités:
 * - Création avec génération automatique de numéro
 * - Imputation sur facture finale avec validation
 * - Annulation d'imputation (correction d'erreurs)
 * - Recherche avancée multi-critères
 * - Statistiques et reporting
 * - Génération automatique d'écritures comptables
 *
 * @author PREDYKT System Optimizer
 * @since Phase 3 - Conformité OHADA Avancée
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DepositService {

    private final DepositRepository depositRepository;
    private final CompanyRepository companyRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final GeneralLedgerService generalLedgerService;
    private final DepositMapper depositMapper;

    // Comptes OHADA pour acomptes
    private static final String ACCOUNT_4191_DEPOSITS = "4191";  // Clients - Avances et acomptes
    private static final String ACCOUNT_4431_VAT_COLLECTED = "4431";  // TVA collectée
    private static final String ACCOUNT_512_BANK = "512";  // Banque
    private static final String ACCOUNT_411_CUSTOMERS = "411";  // Clients

    // ==================== CRÉATION ====================

    /**
     * Crée un nouvel acompte avec génération automatique du numéro de reçu.
     *
     * Processus:
     * 1. Validation du montant HT et du taux de TVA
     * 2. Génération du numéro de reçu: RA-YYYY-NNNNNN
     * 3. Calcul automatique TVA et TTC (@PrePersist)
     * 4. Sauvegarde de l'acompte
     * 5. Génération écriture comptable: Débit 512 / Crédit 4191 + 4431
     *
     * @param companyId ID de la société
     * @param request Données de l'acompte
     * @return DepositResponse
     */
    public DepositResponse createDeposit(Long companyId, DepositCreateRequest request) {
        log.info("💰 Création acompte pour société {} - Montant: {} XAF HT", companyId, request.getAmountHt());

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Société non trouvée: " + companyId));

        // Créer l'entité Deposit
        Deposit deposit = depositMapper.toEntity(request);
        deposit.setCompany(company);

        // Résoudre le client si fourni
        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé: " + request.getCustomerId()));

            if (!customer.getCompany().getId().equals(companyId)) {
                throw new ValidationException("Le client n'appartient pas à cette société");
            }

            deposit.setCustomer(customer);
        }

        // Résoudre le paiement si fourni
        if (request.getPaymentId() != null) {
            Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Paiement non trouvé: " + request.getPaymentId()));

            if (!payment.getCompany().getId().equals(companyId)) {
                throw new ValidationException("Le paiement n'appartient pas à cette société");
            }

            deposit.setPayment(payment);
        }

        // Générer le numéro de reçu d'acompte
        String depositNumber = generateDepositNumber(company, request.getDepositDate());
        deposit.setDepositNumber(depositNumber);

        // Valider les montants
        deposit.validateAmounts();

        // Sauvegarder l'acompte (le @PrePersist calcule automatiquement vatAmount et amountTtc)
        Deposit savedDeposit = depositRepository.save(deposit);

        // Générer l'écriture comptable de réception d'acompte
        createDepositReceivedJournalEntry(savedDeposit);

        log.info("✅ Acompte {} créé avec succès: {} XAF TTC (TVA {}%)",
            savedDeposit.getDepositNumber(), savedDeposit.getAmountTtc(), savedDeposit.getVatRate());

        return depositMapper.toResponse(savedDeposit);
    }

    /**
     * Génère un numéro unique de reçu d'acompte.
     * Format: RA-YYYY-NNNNNN (ex: RA-2025-000001)
     *
     * Algorithme:
     * - Recherche du dernier acompte de l'année
     * - Incrémente le compteur ou démarre à 1
     * - Garantit l'unicité avec vérification en base
     */
    private String generateDepositNumber(Company company, LocalDate depositDate) {
        int year = depositDate.getYear();
        String prefix = String.format("RA-%d-", year);

        // Chercher le dernier numéro de l'année
        List<Deposit> depositsOfYear = depositRepository.findByCompanyAndDepositDateBetweenOrderByDepositDateDesc(
            company,
            LocalDate.of(year, 1, 1),
            LocalDate.of(year, 12, 31)
        );

        int nextNumber = 1;

        if (!depositsOfYear.isEmpty()) {
            // Extraire le numéro du dernier acompte
            String lastDepositNumber = depositsOfYear.get(0).getDepositNumber();
            try {
                String numberPart = lastDepositNumber.substring(prefix.length());
                nextNumber = Integer.parseInt(numberPart) + 1;
            } catch (Exception e) {
                log.warn("⚠️ Impossible de parser le numéro {}, démarre à 1", lastDepositNumber);
            }
        }

        String depositNumber;
        int attempts = 0;

        // Garantir l'unicité (en cas de concurrence)
        do {
            depositNumber = prefix + String.format("%06d", nextNumber + attempts);
            attempts++;

            if (attempts > 100) {
                throw new ValidationException("Impossible de générer un numéro d'acompte unique après 100 tentatives");
            }
        } while (depositRepository.existsByDepositNumberAndCompany(depositNumber, company));

        log.debug("🔢 Numéro d'acompte généré: {}", depositNumber);
        return depositNumber;
    }

    // ==================== LECTURE ====================

    /**
     * Récupère un acompte par ID.
     */
    public DepositResponse getDeposit(Long companyId, Long depositId) {
        Deposit deposit = findDepositByIdAndCompany(companyId, depositId);
        return depositMapper.toResponse(deposit);
    }

    /**
     * Récupère un acompte par numéro de reçu.
     */
    public DepositResponse getDepositByNumber(Long companyId, String depositNumber) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Société non trouvée: " + companyId));

        Deposit deposit = depositRepository.findByDepositNumberAndCompany(depositNumber, company)
            .orElseThrow(() -> new ResourceNotFoundException("Acompte non trouvé: " + depositNumber));

        return depositMapper.toResponse(deposit);
    }

    /**
     * Liste tous les acomptes d'une société avec pagination.
     */
    public Page<DepositResponse> getAllDeposits(Long companyId, Pageable pageable) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Société non trouvée: " + companyId));

        Page<Deposit> deposits = depositRepository.findByCompanyOrderByDepositDateDesc(company, pageable);
        return deposits.map(depositMapper::toResponse);
    }

    /**
     * Liste les acomptes disponibles (non imputés) pour un client.
     */
    public List<DepositResponse> getAvailableDepositsForCustomer(Long companyId, Long customerId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Société non trouvée: " + companyId));

        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé: " + customerId));

        if (!customer.getCompany().getId().equals(companyId)) {
            throw new ValidationException("Le client n'appartient pas à cette société");
        }

        List<Deposit> deposits = depositRepository
            .findByCompanyAndCustomerAndIsAppliedFalseOrderByDepositDateDesc(company, customer);

        return deposits.stream()
            .map(depositMapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Recherche avancée multi-critères avec pagination.
     */
    public Page<DepositResponse> searchDeposits(
        Long companyId,
        Long customerId,
        Boolean isApplied,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Pageable pageable
    ) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Société non trouvée: " + companyId));

        Customer customer = customerId != null
            ? customerRepository.findById(customerId).orElse(null)
            : null;

        Page<Deposit> deposits = depositRepository.searchDeposits(
            company, customer, isApplied, startDate, endDate, minAmount, maxAmount, pageable
        );

        return deposits.map(depositMapper::toResponse);
    }

    // ==================== MODIFICATION ====================

    /**
     * Met à jour un acompte existant.
     *
     * IMPORTANT: Les montants (amountHt, vatRate) ne peuvent PAS être modifiés.
     * Si modification des montants nécessaire: annuler l'acompte et en créer un nouveau.
     */
    public DepositResponse updateDeposit(Long companyId, Long depositId, DepositUpdateRequest request) {
        log.info("📝 Modification acompte {} pour société {}", depositId, companyId);

        Deposit deposit = findDepositByIdAndCompany(companyId, depositId);

        // Vérifier que l'acompte n'est pas imputé
        if (deposit.getIsApplied()) {
            throw new ValidationException(String.format(
                "Impossible de modifier l'acompte %s: il est déjà imputé sur la facture %s",
                deposit.getDepositNumber(),
                deposit.getInvoice() != null ? deposit.getInvoice().getInvoiceNumber() : "inconnue"
            ));
        }

        // Mettre à jour les champs modifiables
        depositMapper.updateEntityFromRequest(request, deposit);

        // Résoudre le nouveau client si changé
        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé: " + request.getCustomerId()));

            if (!customer.getCompany().getId().equals(companyId)) {
                throw new ValidationException("Le client n'appartient pas à cette société");
            }

            deposit.setCustomer(customer);
        }

        Deposit updatedDeposit = depositRepository.save(deposit);

        log.info("✅ Acompte {} modifié avec succès", updatedDeposit.getDepositNumber());

        return depositMapper.toResponse(updatedDeposit);
    }

    // ==================== IMPUTATION SUR FACTURE ====================

    /**
     * Impute un acompte sur une facture finale.
     *
     * Processus OHADA:
     * 1. Validation: acompte non imputé, client correspond, montant cohérent
     * 2. Imputation: deposit.applyToInvoice()
     * 3. Mise à jour facture: invoice.amountPaid += deposit.amountTtc
     * 4. Écriture comptable: Débit 4191 + 4431 TVA / Crédit 411 Clients
     */
    public DepositResponse applyDepositToInvoice(Long companyId, Long depositId, DepositApplyRequest request) {
        log.info("🔗 Imputation acompte {} sur facture {} (société {})",
            depositId, request.getInvoiceId(), companyId);

        Deposit deposit = findDepositByIdAndCompany(companyId, depositId);

        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
            .orElseThrow(() -> new ResourceNotFoundException("Facture non trouvée: " + request.getInvoiceId()));

        if (!invoice.getCompany().getId().equals(companyId)) {
            throw new ValidationException("La facture n'appartient pas à cette société");
        }

        // Imputer l'acompte (validation dans deposit.applyToInvoice())
        deposit.applyToInvoice(invoice, "SYSTEM");  // TODO: Récupérer utilisateur connecté

        depositRepository.save(deposit);

        // Mettre à jour le montant payé de la facture
        invoice.setAmountPaid(invoice.getAmountPaid().add(deposit.getAmountTtc()));
        invoice.setAmountDue(invoice.getTotalTtc().subtract(invoice.getAmountPaid()));

        // Vérifier si la facture est totalement payée
        if (invoice.getAmountDue().compareTo(BigDecimal.ZERO) <= 0) {
            invoice.markAsPaid();
        }

        invoiceRepository.save(invoice);

        // Générer l'écriture comptable d'imputation
        createDepositApplicationJournalEntry(deposit, invoice);

        log.info("✅ Acompte {} ({} XAF) imputé sur facture {}",
            deposit.getDepositNumber(), deposit.getAmountTtc(), invoice.getInvoiceNumber());

        return depositMapper.toResponse(deposit);
    }

    /**
     * Annule l'imputation d'un acompte (correction d'erreur).
     */
    public DepositResponse unapplyDeposit(Long companyId, Long depositId) {
        log.info("🔓 Annulation imputation acompte {} (société {})", depositId, companyId);

        Deposit deposit = findDepositByIdAndCompany(companyId, depositId);

        if (!deposit.getIsApplied()) {
            throw new ValidationException(String.format(
                "L'acompte %s n'est pas imputé", deposit.getDepositNumber()
            ));
        }

        Invoice invoice = deposit.getInvoice();

        if (invoice == null) {
            throw new ValidationException("L'acompte est marqué comme imputé mais n'a pas de facture associée");
        }

        // Annuler l'imputation
        deposit.unapply();
        depositRepository.save(deposit);

        // Mettre à jour le montant payé de la facture
        invoice.setAmountPaid(invoice.getAmountPaid().subtract(deposit.getAmountTtc()));
        invoice.setAmountDue(invoice.getTotalTtc().subtract(invoice.getAmountPaid()));

        // Re-marquer comme partiellement payée ou impayée
        if (invoice.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            invoice.markAsPartiallyPaid();
        } else {
            invoice.markAsUnpaid();
        }

        invoiceRepository.save(invoice);

        // TODO: Générer écriture comptable de contre-passation (si nécessaire)

        log.warn("⚠️ Imputation acompte {} annulée (facture {})",
            deposit.getDepositNumber(), invoice.getInvoiceNumber());

        return depositMapper.toResponse(deposit);
    }

    // ==================== ÉCRITURES COMPTABLES OHADA ====================

    /**
     * Génère l'écriture comptable de réception d'acompte.
     *
     * Écriture OHADA:
     * 512 Banque                         XXX (amountTtc)
     *     4191 Clients - Avances              XXX (amountHt)
     *     4431 TVA collectée                  XXX (vatAmount)
     *
     * Journal: BQ (Banque)
     */
    private void createDepositReceivedJournalEntry(Deposit deposit) {
        log.debug("📝 Génération écriture comptable réception acompte {}", deposit.getDepositNumber());

        List<JournalEntryLineRequest> lines = new ArrayList<>();

        // Débit: 512 Banque (encaissement)
        lines.add(JournalEntryLineRequest.builder()
            .accountNumber(ACCOUNT_512_BANK)
            .debitAmount(deposit.getAmountTtc())
            .creditAmount(BigDecimal.ZERO)
            .description(String.format("Réception acompte %s%s",
                deposit.getDepositNumber(),
                deposit.getCustomer() != null ? " - " + deposit.getCustomer().getName() : ""))
            .build());

        // Crédit: 4191 Clients - Avances et acomptes (dette envers client)
        lines.add(JournalEntryLineRequest.builder()
            .accountNumber(ACCOUNT_4191_DEPOSITS)
            .debitAmount(BigDecimal.ZERO)
            .creditAmount(deposit.getAmountHt())
            .description(String.format("Acompte reçu %s (HT)", deposit.getDepositNumber()))
            .build());

        // Crédit: 4431 TVA collectée (TVA exigible sur encaissement - CGI Cameroun Article 128)
        lines.add(JournalEntryLineRequest.builder()
            .accountNumber(ACCOUNT_4431_VAT_COLLECTED)
            .debitAmount(BigDecimal.ZERO)
            .creditAmount(deposit.getVatAmount())
            .description(String.format("TVA sur acompte %s (%.2f%%)",
                deposit.getDepositNumber(), deposit.getVatRate()))
            .build());

        JournalEntryRequest journalEntry = JournalEntryRequest.builder()
            .entryDate(deposit.getDepositDate())
            .reference(deposit.getDepositNumber())
            .journalCode("BQ")  // Journal Banque
            .lines(lines)
            .build();

        try {
            generalLedgerService.recordJournalEntry(deposit.getCompany().getId(), journalEntry);
            log.info("✅ Écriture comptable acompte {} enregistrée", deposit.getDepositNumber());
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'enregistrement de l'écriture comptable acompte {}: {}",
                deposit.getDepositNumber(), e.getMessage(), e);
            // Note: On ne lève pas l'exception pour ne pas bloquer la création de l'acompte
            // L'acompte est créé, l'écriture peut être re-générée manuellement si besoin
        }
    }

    /**
     * Génère l'écriture comptable d'imputation d'acompte sur facture.
     *
     * Écriture OHADA:
     * 4191 Clients - Avances             XXX (amountHt)
     * 4431 TVA collectée                 XXX (vatAmount)
     *     411 Clients                         XXX (amountTtc)
     *
     * Journal: OD (Opérations Diverses)
     */
    private void createDepositApplicationJournalEntry(Deposit deposit, Invoice invoice) {
        log.debug("📝 Génération écriture comptable imputation acompte {} sur facture {}",
            deposit.getDepositNumber(), invoice.getInvoiceNumber());

        List<JournalEntryLineRequest> lines = new ArrayList<>();

        // Débit: 4191 Clients - Avances (annulation de la dette)
        lines.add(JournalEntryLineRequest.builder()
            .accountNumber(ACCOUNT_4191_DEPOSITS)
            .debitAmount(deposit.getAmountHt())
            .creditAmount(BigDecimal.ZERO)
            .description(String.format("Imputation acompte %s sur facture %s (HT)",
                deposit.getDepositNumber(), invoice.getInvoiceNumber()))
            .build());

        // Débit: 4431 TVA collectée (annulation de la TVA déjà déclarée)
        lines.add(JournalEntryLineRequest.builder()
            .accountNumber(ACCOUNT_4431_VAT_COLLECTED)
            .debitAmount(deposit.getVatAmount())
            .creditAmount(BigDecimal.ZERO)
            .description(String.format("TVA acompte %s (imputation sur facture %s)",
                deposit.getDepositNumber(), invoice.getInvoiceNumber()))
            .build());

        // Crédit: 411 Clients (réduction de la créance client)
        lines.add(JournalEntryLineRequest.builder()
            .accountNumber(ACCOUNT_411_CUSTOMERS)
            .debitAmount(BigDecimal.ZERO)
            .creditAmount(deposit.getAmountTtc())
            .description(String.format("Imputation acompte %s sur facture %s",
                deposit.getDepositNumber(), invoice.getInvoiceNumber()))
            .build());

        JournalEntryRequest journalEntry = JournalEntryRequest.builder()
            .entryDate(deposit.getAppliedAt().toLocalDate())
            .reference(String.format("IMP-%s-%s", deposit.getDepositNumber(), invoice.getInvoiceNumber()))
            .journalCode("OD")  // Journal Opérations Diverses
            .lines(lines)
            .build();

        try {
            generalLedgerService.recordJournalEntry(deposit.getCompany().getId(), journalEntry);
            log.info("✅ Écriture comptable imputation acompte {} enregistrée", deposit.getDepositNumber());
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'enregistrement de l'écriture comptable imputation acompte {}: {}",
                deposit.getDepositNumber(), e.getMessage(), e);
        }
    }

    // ==================== STATISTIQUES ====================

    /**
     * Calcule le total des acomptes disponibles pour une société.
     */
    public BigDecimal getTotalAvailableDeposits(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Société non trouvée: " + companyId));

        return depositRepository.sumAvailableDepositsByCompany(company);
    }

    /**
     * Calcule le total des acomptes disponibles pour un client.
     */
    public BigDecimal getTotalAvailableDepositsForCustomer(Long companyId, Long customerId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Société non trouvée: " + companyId));

        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé: " + customerId));

        return depositRepository.sumAvailableDepositsByCustomer(company, customer);
    }

    // ==================== MÉTHODES PRIVÉES ====================

    /**
     * Recherche un acompte par ID et vérifie l'appartenance à la société (multi-tenant).
     */
    private Deposit findDepositByIdAndCompany(Long companyId, Long depositId) {
        Deposit deposit = depositRepository.findById(depositId)
            .orElseThrow(() -> new ResourceNotFoundException("Acompte non trouvé: " + depositId));

        if (!deposit.getCompany().getId().equals(companyId)) {
            throw new ValidationException("L'acompte n'appartient pas à cette société");
        }

        return deposit;
    }
}
