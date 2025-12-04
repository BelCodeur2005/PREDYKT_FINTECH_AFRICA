package com.predykt.accounting.service;

import com.predykt.accounting.config.TenantContextHolder;
import com.predykt.accounting.domain.entity.*;
import com.predykt.accounting.domain.enums.VATAccountType;
import com.predykt.accounting.domain.enums.VATRecoverableCategory;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.repository.VATRecoveryCalculationRepository;
import com.predykt.accounting.repository.VATTransactionRepository;
import com.predykt.accounting.repository.GeneralLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service de gestion de la récupérabilité de la TVA
 * Implémente les règles fiscales camerounaises sur la TVA non récupérable
 *
 * VERSION 2.0 avec support du PRORATA de TVA (CGI Art. 133)
 * Calcul en 2 ÉTAPES :
 * 1. Récupérabilité PAR NATURE (VP, VU, représentation, etc.)
 * 2. Application du PRORATA (activités mixtes taxables/exonérées)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VATRecoverabilityService {

    private final VATTransactionRepository vatTransactionRepository;
    private final CompanyRepository companyRepository;
    private final VATRecoverabilityRuleEngine ruleEngine;
    private final VATProratService prorataService;
    private final VATRecoveryCalculationRepository calculationRepository;
    private final GeneralLedgerRepository generalLedgerRepository;

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

    // ============================================
    // NOUVELLES MÉTHODES V2.0 : PRORATA DE TVA
    // ============================================

    /**
     * Calcule la TVA récupérable avec PRORATA (VERSION COMPLÈTE)
     *
     * Calcul en 2 étapes (conforme CGI Cameroun) :
     * 1. Récupérabilité PAR NATURE (VP, VU, représentation, etc.)
     * 2. Application du PRORATA (activités mixtes taxables/exonérées)
     *
     * @param companyId ID de l'entreprise
     * @param accountNumber Numéro de compte OHADA
     * @param description Description de la dépense
     * @param htAmount Montant HT
     * @param vatAmount Montant TVA
     * @param vatRate Taux TVA (19.25% au Cameroun)
     * @param fiscalYear Année fiscale
     * @param generalLedgerId ID de l'écriture (optionnel)
     * @return Résultat du calcul avec détails complets
     */
    @Transactional
    public VATRecoveryResult calculateRecoverableVATWithProrata(
            Long companyId,
            String accountNumber,
            String description,
            BigDecimal htAmount,
            BigDecimal vatAmount,
            BigDecimal vatRate,
            Integer fiscalYear,
            Long generalLedgerId) {

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        log.debug("🔍 Calcul TVA récupérable (avec prorata) pour {} - Compte: {} - TVA: {} FCFA",
            company.getName(), accountNumber, vatAmount);

        // ======== ÉTAPE 1: Détection par NATURE ========

        var context = TenantContextHolder.getContext();

        VATRecoverabilityRuleEngine.DetectionResult detection = ruleEngine.detectCategory(
            companyId,
            context != null ? context.getTenantId() : null,
            context != null ? context.getCabinetId() : null,
            accountNumber,
            description
        );

        VATRecoverableCategory category = detection.getCategory();
        BigDecimal recoveryByNatureRate = getRecoveryRateByCategory(category);
        BigDecimal recoverableByNature = vatAmount.multiply(recoveryByNatureRate)
            .setScale(2, RoundingMode.HALF_UP);

        log.debug("  ➤ ÉTAPE 1 (Nature): {} → {}% récupérable = {} FCFA",
            category.getDisplayName(),
            recoveryByNatureRate.multiply(new BigDecimal("100")),
            recoverableByNature);

        // ======== ÉTAPE 2: Application du PRORATA ========

        Optional<VATProrata> prorataOpt = prorataService.getActiveProrata(companyId, fiscalYear);

        BigDecimal prorataRate = null;
        BigDecimal recoverableWithProrata;
        VATProrata prorata = null;

        if (prorataOpt.isPresent()) {
            prorata = prorataOpt.get();
            prorataRate = prorata.getProrataRate();

            recoverableWithProrata = recoverableByNature.multiply(prorataRate)
                .setScale(0, RoundingMode.HALF_UP);

            log.debug("  ➤ ÉTAPE 2 (Prorata): {}% × {} FCFA = {} FCFA récupérable",
                prorata.getProrataPercentage(),
                recoverableByNature,
                recoverableWithProrata);
        } else {
            // Pas de prorata → 100% activités taxables
            recoverableWithProrata = recoverableByNature;
            log.debug("  ➤ ÉTAPE 2 (Prorata): Aucun prorata → 100% activités taxables");
        }

        // ======== RÉSULTAT FINAL ========

        BigDecimal finalRecoverable = recoverableWithProrata;
        BigDecimal nonRecoverable = vatAmount.subtract(finalRecoverable);

        log.info("✅ TVA calculée : {} FCFA → {} FCFA récupérable ({}) + {} FCFA non récupérable",
            vatAmount, finalRecoverable, category.getDisplayName(), nonRecoverable);

        // ======== ENREGISTREMENT AVEC TRAÇABILITÉ ========

        GeneralLedger generalLedger = null;
        if (generalLedgerId != null) {
            generalLedger = generalLedgerRepository.findById(generalLedgerId).orElse(null);
        }

        VATRecoveryCalculation calculation = VATRecoveryCalculation.builder()
            .company(company)
            .generalLedger(generalLedger)
            .accountNumber(accountNumber)
            .description(description)
            .htAmount(htAmount)
            .vatAmount(vatAmount)
            .vatRate(vatRate)
            .recoveryCategory(category)
            .recoveryByNatureRate(recoveryByNatureRate)
            .recoverableByNature(recoverableByNature)
            .prorata(prorata)
            .prorataRate(prorataRate)
            .recoverableWithProrata(recoverableWithProrata)
            .recoverableVat(finalRecoverable)
            .nonRecoverableVat(nonRecoverable)
            .appliedRule(detection.getAppliedRule())
            .detectionConfidence(detection.getConfidence())
            .detectionReason(detection.getReason())
            .calculationDate(LocalDateTime.now())
            .fiscalYear(fiscalYear)
            .build();

        calculation.calculate(); // Recalcul pour vérifier cohérence
        VATRecoveryCalculation saved = calculationRepository.save(calculation);

        return VATRecoveryResult.builder()
            .calculationId(saved.getId())
            .totalVAT(vatAmount)
            .recoveryCategory(category)
            .recoveryByNatureRate(recoveryByNatureRate)
            .recoverableByNature(recoverableByNature)
            .prorataRate(prorataRate)
            .prorataPercentage(prorataRate != null ?
                prorataRate.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) : null)
            .recoverableWithProrata(recoverableWithProrata)
            .recoverableVAT(finalRecoverable)
            .nonRecoverableVAT(nonRecoverable)
            .appliedRule(detection.getAppliedRule() != null ? detection.getAppliedRule().getName() : null)
            .detectionConfidence(detection.getConfidence())
            .detectionReason(detection.getReason())
            .hasProrataImpact(recoverableByNature.compareTo(recoverableWithProrata) != 0)
            .build();
    }

    /**
     * Calcul simplifié avec prorata (compatibilité)
     */
    @Transactional
    public VATRecoveryResult calculateRecoverableVATWithProrata(
            Long companyId,
            String accountNumber,
            String description,
            BigDecimal vatAmount,
            Integer fiscalYear) {

        // Déduire HT et taux standard (19.25%)
        BigDecimal vatRate = new BigDecimal("19.25");
        BigDecimal htAmount = vatAmount.divide(
            vatRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP),
            2,
            RoundingMode.HALF_UP
        );

        return calculateRecoverableVATWithProrata(
            companyId, accountNumber, description,
            htAmount, vatAmount, vatRate, fiscalYear, null
        );
    }

    /**
     * Récupère un calcul par ID
     */
    @Transactional(readOnly = true)
    public VATRecoveryCalculation getCalculation(Long calculationId) {
        return calculationRepository.findById(calculationId)
            .orElseThrow(() -> new ResourceNotFoundException("Calcul non trouvé"));
    }

    /**
     * Récupère tous les calculs pour une entreprise/année
     */
    @Transactional(readOnly = true)
    public List<VATRecoveryCalculation> getCalculationsByCompanyAndYear(Long companyId, Integer fiscalYear) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        return calculationRepository.findByCompanyAndFiscalYearOrderByCalculationDateDesc(company, fiscalYear);
    }

    /**
     * Calcule les statistiques de récupération avec prorata
     */
    @Transactional(readOnly = true)
    public VATRecoveryStatistics getRecoveryStatistics(Long companyId, Integer fiscalYear) {
        BigDecimal totalRecoverable = calculationRepository.sumRecoverableVatByCompanyAndYear(companyId, fiscalYear);
        BigDecimal totalNonRecoverable = calculationRepository.sumNonRecoverableVatByCompanyAndYear(companyId, fiscalYear);
        BigDecimal totalVAT = totalRecoverable.add(totalNonRecoverable);

        long totalCalculations = calculationRepository.countByCompanyAndFiscalYear(
            companyRepository.findById(companyId).orElseThrow(),
            fiscalYear
        );

        BigDecimal averageRecoveryRate = calculationRepository.calculateAverageRecoveryRate(companyId, fiscalYear);

        return VATRecoveryStatistics.builder()
            .totalVAT(totalVAT != null ? totalVAT : BigDecimal.ZERO)
            .totalRecoverable(totalRecoverable != null ? totalRecoverable : BigDecimal.ZERO)
            .totalNonRecoverable(totalNonRecoverable != null ? totalNonRecoverable : BigDecimal.ZERO)
            .averageRecoveryRate(averageRecoveryRate != null ? averageRecoveryRate : BigDecimal.ZERO)
            .totalCalculations(totalCalculations)
            .build();
    }

    /**
     * Convertit une catégorie en taux de récupération
     */
    private BigDecimal getRecoveryRateByCategory(VATRecoverableCategory category) {
        return switch (category) {
            case FULLY_RECOVERABLE -> BigDecimal.ONE;
            case RECOVERABLE_80_PERCENT -> new BigDecimal("0.80");
            case NON_RECOVERABLE_TOURISM_VEHICLE,
                 NON_RECOVERABLE_FUEL_VP,
                 NON_RECOVERABLE_REPRESENTATION,
                 NON_RECOVERABLE_LUXURY,
                 NON_RECOVERABLE_PERSONAL -> BigDecimal.ZERO;
        };
    }

    /**
     * Résultat du calcul de récupération (avec prorata)
     */
    @lombok.Data
    @lombok.Builder
    public static class VATRecoveryResult {
        private Long calculationId;
        private BigDecimal totalVAT;

        // ÉTAPE 1: Par nature
        private VATRecoverableCategory recoveryCategory;
        private BigDecimal recoveryByNatureRate;
        private BigDecimal recoverableByNature;

        // ÉTAPE 2: Avec prorata
        private BigDecimal prorataRate;
        private BigDecimal prorataPercentage;
        private BigDecimal recoverableWithProrata;

        // RÉSULTAT FINAL
        private BigDecimal recoverableVAT;
        private BigDecimal nonRecoverableVAT;

        // Métadonnées
        private String appliedRule;
        private Integer detectionConfidence;
        private String detectionReason;
        private Boolean hasProrataImpact;
    }

    /**
     * Statistiques de récupération
     */
    @lombok.Data
    @lombok.Builder
    public static class VATRecoveryStatistics {
        private BigDecimal totalVAT;
        private BigDecimal totalRecoverable;
        private BigDecimal totalNonRecoverable;
        private BigDecimal averageRecoveryRate;
        private Long totalCalculations;
    }
}
