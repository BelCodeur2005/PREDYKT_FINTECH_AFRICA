package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.*;
import com.predykt.accounting.domain.enums.VATAccountType;
import com.predykt.accounting.domain.enums.VATRecoverableCategory;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.repository.VATTransactionRepository;
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
 * Service de gestion de la récupérabilité de la TVA
 * Implémente les règles fiscales camerounaises sur la TVA non récupérable
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VATRecoverabilityService {

    private final VATTransactionRepository vatTransactionRepository;
    private final CompanyRepository companyRepository;
    private final VATRecoverabilityRuleEngine ruleEngine;

    /**
     * Enregistre une transaction de TVA avec sa catégorie de récupérabilité
     */
    @Transactional
    public VATTransaction recordVATTransaction(
            Company company,
            GeneralLedger ledgerEntry,
            Supplier supplier,
            LocalDate transactionDate,
            VATAccountType vatAccountType,
            String transactionType,
            BigDecimal amountExcludingVat,
            BigDecimal vatRate,
            BigDecimal vatAmount,
            VATRecoverableCategory recoverableCategory,
            String description,
            String invoiceReference
    ) {
        log.info("📝 Enregistrement transaction TVA - Type: {} - Catégorie: {} - Montant TVA: {} XAF",
            transactionType, recoverableCategory.getDisplayName(), vatAmount);

        VATTransaction transaction = VATTransaction.builder()
            .company(company)
            .ledgerEntry(ledgerEntry)
            .supplier(supplier)
            .transactionDate(transactionDate)
            .vatAccountType(vatAccountType)
            .transactionType(transactionType)
            .amountExcludingVat(amountExcludingVat)
            .vatRate(vatRate)
            .vatAmount(vatAmount)
            .recoverableCategory(recoverableCategory)
            .description(description)
            .invoiceReference(invoiceReference)
            .build();

        // Les montants récupérables/non récupérables sont calculés automatiquement par @PrePersist
        VATTransaction saved = vatTransactionRepository.save(transaction);

        if (saved.isNonRecoverable() || saved.isPartiallyRecoverable()) {
            log.warn("⚠️ TVA non/partiellement récupérable - Montant non récupérable: {} XAF - Raison: {}",
                saved.getNonRecoverableVatAmount(),
                saved.getRecoverableCategory().getDescription());
        }

        return saved;
    }

    /**
     * Détecte automatiquement la catégorie de récupérabilité selon le compte OHADA
     * Utilise le moteur de règles optimisé avec scoring et apprentissage
     *
     * @param accountNumber Numéro de compte OHADA
     * @param description Description de la transaction
     * @return Résultat de détection avec catégorie, confiance et règle appliquée
     */
    public VATRecoverabilityRuleEngine.DetectionResult detectRecoverableCategoryWithDetails(
            String accountNumber, String description) {

        return ruleEngine.detectCategory(
            accountNumber != null ? accountNumber : "",
            description != null ? description : ""
        );
    }

    /**
     * Détecte automatiquement la catégorie de récupérabilité (méthode simplifiée)
     * Règles fiscales camerounaises avec moteur de règles optimisé
     */
    public VATRecoverableCategory detectRecoverableCategory(String accountNumber, String description) {
        VATRecoverabilityRuleEngine.DetectionResult result = detectRecoverableCategoryWithDetails(
            accountNumber, description
        );

        log.debug("🔍 Détection catégorie - Compte: {} - Catégorie: {} - Confiance: {}% - Règle: {}",
            accountNumber,
            result.getCategory().getDisplayName(),
            result.getConfidence(),
            result.getAppliedRule() != null ? result.getAppliedRule().getName() : "Défaut");

        return result.getCategory();
    }

    /**
     * Calcule la TVA récupérable pour une période
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateRecoverableVAT(Long companyId, LocalDate startDate, LocalDate endDate, String accountType) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        return vatTransactionRepository.sumRecoverableVatByAccountType(
            company, startDate, endDate, accountType
        );
    }

    /**
     * Récupère les transactions avec TVA non récupérable
     */
    @Transactional(readOnly = true)
    public List<VATTransaction> getNonRecoverableTransactions(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        return vatTransactionRepository.findNonRecoverableTransactions(company);
    }

    /**
     * Récupère les statistiques de TVA non récupérable pour une période
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getNonRecoverableVATStatistics(Long companyId, LocalDate startDate, LocalDate endDate) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        List<Object[]> stats = vatTransactionRepository.getNonRecoverableVatStatistics(
            company, startDate, endDate
        );

        Map<String, Object> result = new HashMap<>();
        BigDecimal totalNonRecoverable = BigDecimal.ZERO;
        Map<String, Map<String, Object>> breakdown = new HashMap<>();

        for (Object[] row : stats) {
            VATRecoverableCategory category = (VATRecoverableCategory) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            Long count = (Long) row[2];

            totalNonRecoverable = totalNonRecoverable.add(amount);

            Map<String, Object> categoryData = new HashMap<>();
            categoryData.put("amount", amount);
            categoryData.put("transactionCount", count);
            categoryData.put("description", category.getDescription());
            categoryData.put("recoverablePercentage", category.getRecoverablePercentage());

            breakdown.put(category.getDisplayName(), categoryData);
        }

        result.put("totalNonRecoverableVAT", totalNonRecoverable);
        result.put("breakdown", breakdown);
        result.put("period", Map.of("start", startDate, "end", endDate));

        // Calculer le total récupérable pour comparaison
        BigDecimal totalRecoverable = vatTransactionRepository.sumRecoverableVatDeductible(
            company, startDate, endDate
        );
        result.put("totalRecoverableVAT", totalRecoverable != null ? totalRecoverable : BigDecimal.ZERO);

        // Calculer le taux de récupérabilité
        BigDecimal totalVAT = totalRecoverable.add(totalNonRecoverable);
        if (totalVAT.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal recoverabilityRate = totalRecoverable
                .multiply(BigDecimal.valueOf(100))
                .divide(totalVAT, 2, java.math.RoundingMode.HALF_UP);
            result.put("recoverabilityRate", recoverabilityRate);
        } else {
            result.put("recoverabilityRate", BigDecimal.valueOf(100));
        }

        log.info("📊 Statistiques TVA non récupérable - Période: {} à {} - Total non récupérable: {} XAF",
            startDate, endDate, totalNonRecoverable);

        return result;
    }

    /**
     * Récupère toutes les transactions pour une période
     */
    @Transactional(readOnly = true)
    public List<VATTransaction> getTransactionsByPeriod(Long companyId, LocalDate startDate, LocalDate endDate) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        return vatTransactionRepository.findByCompanyAndPeriod(company, startDate, endDate);
    }

    /**
     * Met à jour la catégorie de récupérabilité d'une transaction
     * Enregistre la correction pour apprentissage du moteur de règles
     */
    @Transactional
    public VATTransaction updateRecoverableCategory(Long transactionId, VATRecoverableCategory newCategory, String justification) {
        VATTransaction transaction = vatTransactionRepository.findById(transactionId)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction TVA non trouvée"));

        VATRecoverableCategory oldCategory = transaction.getRecoverableCategory();
        transaction.setRecoverableCategory(newCategory);
        transaction.setNonRecoverableJustification(justification);

        VATTransaction saved = vatTransactionRepository.save(transaction);

        // Enregistrer la correction pour l'apprentissage du moteur de règles
        if (!oldCategory.equals(newCategory)) {
            // Récupérer l'ID de la règle qui a été appliquée (si disponible)
            VATRecoverabilityRuleEngine.DetectionResult detectionResult = ruleEngine.detectCategory(
                transaction.getLedgerEntry() != null ? transaction.getLedgerEntry().getAccountNumber() : "",
                transaction.getDescription() != null ? transaction.getDescription() : ""
            );

            Long ruleId = detectionResult.getAppliedRule() != null
                ? detectionResult.getAppliedRule().getId()
                : null;

            ruleEngine.recordCorrection(transactionId, oldCategory, newCategory, ruleId);

            log.warn("⚠️ Correction enregistrée pour apprentissage - Transaction ID: {} - Ancien: {} - Nouveau: {} - Règle: {}",
                transactionId, oldCategory.getDisplayName(), newCategory.getDisplayName(),
                ruleId != null ? detectionResult.getAppliedRule().getName() : "Aucune");
        }

        log.info("🔄 Catégorie de récupérabilité modifiée - Transaction ID: {} - Ancien: {} - Nouveau: {}",
            transactionId, oldCategory.getDisplayName(), newCategory.getDisplayName());

        return saved;
    }

    /**
     * Récupère les statistiques du moteur de règles
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRuleEngineStatistics() {
        return ruleEngine.getStatistics();
    }

    /**
     * Invalide le cache du moteur de règles
     */
    public void invalidateRuleCache() {
        ruleEngine.invalidateCache();
        log.info("♻️ Cache du moteur de règles invalidé");
    }

    /**
     * Compte les transactions avec alertes
     */
    @Transactional(readOnly = true)
    public Long countTransactionsWithAlerts(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        return vatTransactionRepository.countTransactionsWithAlerts(company);
    }
}
