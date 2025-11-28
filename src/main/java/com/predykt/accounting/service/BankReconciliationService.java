package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.BankReconciliation;
import com.predykt.accounting.domain.entity.BankReconciliationItem;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.enums.ReconciliationStatus;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.repository.BankReconciliationRepository;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.repository.GeneralLedgerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service de gestion des rapprochements bancaires conforme OHADA
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BankReconciliationService {

    private final BankReconciliationRepository reconciliationRepository;
    private final CompanyRepository companyRepository;
    private final GeneralLedgerRepository generalLedgerRepository;
    private final ChartOfAccountsService chartOfAccountsService;

    /**
     * Crée un nouveau rapprochement bancaire
     */
    public BankReconciliation createReconciliation(BankReconciliation reconciliation) {
        log.info("Création d'un rapprochement bancaire pour l'entreprise {} - Compte: {}, Date: {}",
            reconciliation.getCompany().getId(),
            reconciliation.getBankAccountNumber(),
            reconciliation.getReconciliationDate());

        // Vérifier que l'entreprise existe
        Company company = companyRepository.findById(reconciliation.getCompany().getId())
            .orElseThrow(() -> new EntityNotFoundException(
                "Entreprise non trouvée avec l'ID: " + reconciliation.getCompany().getId()));

        reconciliation.setCompany(company);

        // Vérifier qu'il n'existe pas déjà un rapprochement pour cette date et ce compte
        if (reconciliationRepository.existsByCompanyAndBankAccountNumberAndReconciliationDate(
            company, reconciliation.getBankAccountNumber(), reconciliation.getReconciliationDate())) {
            throw new IllegalStateException(
                "Un rapprochement existe déjà pour ce compte à cette date");
        }

        // Calculer le solde comptable automatiquement si non fourni
        if (reconciliation.getBookBalance() == null) {
            BigDecimal bookBalance = calculateBookBalance(
                company,
                reconciliation.getGlAccountNumber(),
                reconciliation.getPeriodEnd()
            );
            reconciliation.setBookBalance(bookBalance);
        }

        // Calculer les soldes rectifiés et l'écart
        reconciliation.calculateDifference();

        return reconciliationRepository.save(reconciliation);
    }

    /**
     * Calcule le solde comptable d'un compte bancaire (52X) à une date donnée
     */
    private BigDecimal calculateBookBalance(Company company, String accountNumber, LocalDate asOfDate) {
        if (accountNumber == null || accountNumber.isEmpty()) {
            // Par défaut, chercher le compte 521 (Banques, comptes courants)
            accountNumber = "521";
        }

        // Utiliser une date très ancienne comme début pour obtenir tout l'historique jusqu'à asOfDate
        LocalDate startDate = LocalDate.of(1900, 1, 1);

        return generalLedgerRepository.findByCompanyAndAccountAndEntryDateBetween(
            company,
            chartOfAccountsService.getAccountByNumber(company.getId(), accountNumber),
            startDate,
            asOfDate
        ).stream()
            .map(entry -> entry.getDebitAmount().subtract(entry.getCreditAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Met à jour un rapprochement existant
     */
    public BankReconciliation updateReconciliation(Long reconciliationId, BankReconciliation updatedData) {
        log.info("Mise à jour du rapprochement {}", reconciliationId);

        BankReconciliation existing = getReconciliationById(reconciliationId);

        // Vérifier que le rapprochement peut être modifié
        if (!existing.getStatus().canEdit()) {
            throw new IllegalStateException(
                "Le rapprochement ne peut pas être modifié dans le statut: " + existing.getStatus());
        }

        // Mettre à jour les champs
        existing.setBankStatementBalance(updatedData.getBankStatementBalance());
        existing.setBookBalance(updatedData.getBookBalance());
        existing.setNotes(updatedData.getNotes());

        // Recalculer
        existing.calculateDifference();

        return reconciliationRepository.save(existing);
    }

    /**
     * Ajoute une opération en suspens au rapprochement
     */
    public BankReconciliation addPendingItem(Long reconciliationId, BankReconciliationItem item) {
        log.info("Ajout d'une opération en suspens au rapprochement {}", reconciliationId);

        BankReconciliation reconciliation = getReconciliationById(reconciliationId);

        if (!reconciliation.getStatus().canEdit()) {
            throw new IllegalStateException(
                "Le rapprochement ne peut pas être modifié dans le statut: " + reconciliation.getStatus());
        }

        reconciliation.addPendingItem(item);

        return reconciliationRepository.save(reconciliation);
    }

    /**
     * Supprime une opération en suspens
     */
    public BankReconciliation removePendingItem(Long reconciliationId, Long itemId) {
        log.info("Suppression de l'opération {} du rapprochement {}", itemId, reconciliationId);

        BankReconciliation reconciliation = getReconciliationById(reconciliationId);

        if (!reconciliation.getStatus().canEdit()) {
            throw new IllegalStateException(
                "Le rapprochement ne peut pas être modifié dans le statut: " + reconciliation.getStatus());
        }

        reconciliation.getPendingItems().removeIf(item -> item.getId().equals(itemId));
        reconciliation.recalculateTotalsFromItems();

        return reconciliationRepository.save(reconciliation);
    }

    /**
     * Soumet le rapprochement pour révision
     */
    public BankReconciliation submitForReview(Long reconciliationId, String preparedBy) {
        log.info("Soumission du rapprochement {} pour révision par {}", reconciliationId, preparedBy);

        BankReconciliation reconciliation = getReconciliationById(reconciliationId);
        reconciliation.submitForReview(preparedBy);

        return reconciliationRepository.save(reconciliation);
    }

    /**
     * Approuve le rapprochement
     */
    public BankReconciliation approveReconciliation(Long reconciliationId, String approvedBy) {
        log.info("Approbation du rapprochement {} par {}", reconciliationId, approvedBy);

        BankReconciliation reconciliation = getReconciliationById(reconciliationId);
        reconciliation.approve(approvedBy);

        return reconciliationRepository.save(reconciliation);
    }

    /**
     * Rejette le rapprochement
     */
    public BankReconciliation rejectReconciliation(Long reconciliationId, String rejectedBy, String reason) {
        log.info("Rejet du rapprochement {} par {} - Raison: {}", reconciliationId, rejectedBy, reason);

        BankReconciliation reconciliation = getReconciliationById(reconciliationId);
        reconciliation.reject(rejectedBy, reason);

        return reconciliationRepository.save(reconciliation);
    }

    /**
     * Récupère un rapprochement par ID
     */
    @Transactional(readOnly = true)
    public BankReconciliation getReconciliationById(Long reconciliationId) {
        return reconciliationRepository.findById(reconciliationId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Rapprochement non trouvé avec l'ID: " + reconciliationId));
    }

    /**
     * Liste les rapprochements d'une entreprise
     */
    @Transactional(readOnly = true)
    public List<BankReconciliation> getCompanyReconciliations(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Entreprise non trouvée avec l'ID: " + companyId));

        return reconciliationRepository.findByCompanyOrderByReconciliationDateDesc(company);
    }

    /**
     * Liste les rapprochements par compte bancaire
     */
    @Transactional(readOnly = true)
    public List<BankReconciliation> getReconciliationsByBankAccount(
        Long companyId,
        String bankAccountNumber
    ) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Entreprise non trouvée avec l'ID: " + companyId));

        return reconciliationRepository.findByCompanyAndBankAccountNumberOrderByReconciliationDateDesc(
            company, bankAccountNumber);
    }

    /**
     * Liste les rapprochements non équilibrés
     */
    @Transactional(readOnly = true)
    public List<BankReconciliation> getUnbalancedReconciliations(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Entreprise non trouvée avec l'ID: " + companyId));

        return reconciliationRepository.findUnbalancedReconciliations(company);
    }

    /**
     * Liste les rapprochements en attente de validation
     */
    @Transactional(readOnly = true)
    public List<BankReconciliation> getPendingReconciliations(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Entreprise non trouvée avec l'ID: " + companyId));

        return reconciliationRepository.findByCompanyAndStatusIn(
            company,
            List.of(ReconciliationStatus.PENDING_REVIEW, ReconciliationStatus.REVIEWED)
        );
    }

    /**
     * Génère des statistiques de rapprochement
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getReconciliationStatistics(Long companyId, LocalDate startDate, LocalDate endDate) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Entreprise non trouvée avec l'ID: " + companyId));

        Map<String, Object> stats = new HashMap<>();

        long totalReconciliations = reconciliationRepository.countByCompanyAndPeriod(
            company, startDate, endDate);
        long approvedReconciliations = reconciliationRepository.countApprovedByCompanyAndPeriod(
            company, startDate, endDate);

        stats.put("totalReconciliations", totalReconciliations);
        stats.put("approvedReconciliations", approvedReconciliations);
        stats.put("approvalRate", totalReconciliations > 0 ?
            (approvedReconciliations * 100.0 / totalReconciliations) : 0.0);

        // Compter par statut
        Map<String, Long> byStatus = new HashMap<>();
        for (ReconciliationStatus status : ReconciliationStatus.values()) {
            byStatus.put(status.name(), reconciliationRepository.countByCompanyAndStatus(company, status));
        }
        stats.put("byStatus", byStatus);

        // Rapprochements non équilibrés
        stats.put("unbalanced", reconciliationRepository.findUnbalancedReconciliations(company).size());

        return stats;
    }

    /**
     * Supprime un rapprochement (seulement si en brouillon ou rejeté)
     */
    public void deleteReconciliation(Long reconciliationId) {
        log.info("Suppression du rapprochement {}", reconciliationId);

        BankReconciliation reconciliation = getReconciliationById(reconciliationId);

        if (!reconciliation.getStatus().canEdit()) {
            throw new IllegalStateException(
                "Le rapprochement ne peut pas être supprimé dans le statut: " + reconciliation.getStatus());
        }

        reconciliationRepository.delete(reconciliation);
    }
}
