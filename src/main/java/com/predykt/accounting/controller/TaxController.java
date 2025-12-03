package com.predykt.accounting.controller;

import com.predykt.accounting.domain.entity.RecoverabilityRule;
import com.predykt.accounting.domain.entity.TaxCalculation;
import com.predykt.accounting.domain.entity.TaxConfiguration;
import com.predykt.accounting.domain.enums.TaxType;
import com.predykt.accounting.domain.enums.VATRecoverableCategory;
import com.predykt.accounting.dto.response.*;
import com.predykt.accounting.mapper.TaxMapper;
import com.predykt.accounting.repository.RecoverabilityRuleRepository;
import com.predykt.accounting.service.TaxService;
import com.predykt.accounting.service.VATDeclarationService;
import com.predykt.accounting.service.VATRecoverabilityRuleEngine;
import com.predykt.accounting.service.VATRecoverabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller pour la gestion fiscale complète
 * Gère les 5 taxes essentielles: TVA, Acompte IS, AIR, IRPP Loyer, CNPS
 */
@RestController
@RequestMapping("/companies/{companyId}/taxes")
@RequiredArgsConstructor
@Tag(name = "Taxes", description = "Gestion fiscale camerounaise (TVA, IS, AIR, IRPP, CNPS)")
public class TaxController {

    private final TaxService taxService;
    private final TaxMapper taxMapper;
    private final VATDeclarationService vatDeclarationService;
    private final VATRecoverabilityService vatRecoverabilityService;
    private final VATRecoverabilityRuleEngine ruleEngine;
    private final RecoverabilityRuleRepository ruleRepository;

    @GetMapping("/summary")
    @Operation(summary = "Résumé fiscal mensuel",
               description = "Calcule le résumé de toutes les taxes pour un mois donné")
    public ResponseEntity<ApiResponse<TaxSummaryResponse>> getMonthlySummary(
            @PathVariable Long companyId,
            @RequestParam int year,
            @RequestParam int month) {

        Map<TaxType, BigDecimal> summary = taxService.getMonthlySummary(companyId, year, month);

        // Construire la réponse structurée
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        TaxSummaryResponse response = TaxSummaryResponse.builder()
            .startDate(startDate)
            .endDate(endDate)
            .fiscalPeriod(String.format("%04d-%02d", year, month))
            .vatAmount(summary.getOrDefault(TaxType.VAT, BigDecimal.ZERO))
            .isAdvanceAmount(summary.getOrDefault(TaxType.IS_ADVANCE, BigDecimal.ZERO))
            .airWithNiuAmount(summary.getOrDefault(TaxType.AIR_WITH_NIU, BigDecimal.ZERO))
            .airWithoutNiuAmount(summary.getOrDefault(TaxType.AIR_WITHOUT_NIU, BigDecimal.ZERO))
            .irppRentAmount(summary.getOrDefault(TaxType.IRPP_RENT, BigDecimal.ZERO))
            .cnpsAmount(summary.getOrDefault(TaxType.CNPS, BigDecimal.ZERO))
            .build();

        // Calculer totaux
        BigDecimal airTotal = response.getAirWithNiuAmount().add(response.getAirWithoutNiuAmount());
        response.setAirAmount(airTotal);

        BigDecimal total = response.getVatAmount()
            .add(response.getIsAdvanceAmount())
            .add(airTotal)
            .add(response.getIrppRentAmount())
            .add(response.getCnpsAmount());
        response.setTotalTaxes(total);

        // Calculer pénalités AIR
        BigDecimal penaltyCost = response.getAirWithoutNiuAmount()
            .multiply(new BigDecimal("3.3"))
            .divide(new BigDecimal("5.5"), 2, java.math.RoundingMode.HALF_UP);
        response.setAirPenaltyCost(penaltyCost);

        // Compter les alertes
        Long alertCount = taxService.getActiveAlerts(companyId).stream().count();
        response.setAlertCount(alertCount.intValue());

        // Breakdown détaillé
        Map<String, BigDecimal> breakdown = new HashMap<>();
        summary.forEach((type, amount) -> breakdown.put(type.getDisplayName(), amount));
        response.setTaxBreakdown(breakdown);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/alerts")
    @Operation(summary = "Alertes fiscales actives",
               description = "Récupère toutes les alertes fiscales (NIU manquant, anomalies)")
    public ResponseEntity<ApiResponse<List<TaxAlertResponse>>> getActiveAlerts(
            @PathVariable Long companyId) {

        List<TaxCalculation> alerts = taxService.getActiveAlerts(companyId);
        List<TaxAlertResponse> response = alerts.stream()
            .map(taxMapper::toAlertResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/configurations")
    @Operation(summary = "Configurations fiscales",
               description = "Récupère toutes les configurations fiscales de l'entreprise")
    public ResponseEntity<ApiResponse<List<TaxConfigurationResponse>>> getConfigurations(
            @PathVariable Long companyId) {

        List<TaxConfiguration> configs = taxService.getTaxConfigurations(companyId);
        List<TaxConfigurationResponse> response = configs.stream()
            .map(taxMapper::toConfigurationResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/history")
    @Operation(summary = "Historique des calculs fiscaux",
               description = "Récupère l'historique des calculs pour une période")
    public ResponseEntity<ApiResponse<List<TaxCalculation>>> getCalculationHistory(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<TaxCalculation> history = taxService.getCalculationHistory(companyId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @PutMapping("/configurations/{taxType}/toggle")
    @Operation(summary = "Activer/Désactiver une taxe",
               description = "Active ou désactive une taxe pour l'entreprise")
    public ResponseEntity<ApiResponse<String>> toggleTax(
            @PathVariable Long companyId,
            @PathVariable TaxType taxType,
            @RequestParam boolean active) {

        taxService.toggleTax(companyId, taxType, active);

        String message = String.format("Taxe %s %s avec succès",
            taxType.getDisplayName(),
            active ? "activée" : "désactivée");

        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @PutMapping("/configurations/{taxType}/rate")
    @Operation(summary = "Modifier le taux d'une taxe",
               description = "Met à jour le taux d'une taxe pour l'entreprise")
    public ResponseEntity<ApiResponse<String>> updateTaxRate(
            @PathVariable Long companyId,
            @PathVariable TaxType taxType,
            @RequestParam BigDecimal newRate) {

        taxService.updateTaxRate(companyId, taxType, newRate);

        String message = String.format("Taux de %s modifié à %s%%",
            taxType.getDisplayName(), newRate);

        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @GetMapping("/suppliers-without-niu/count")
    @Operation(summary = "Compte fournisseurs sans NIU",
               description = "Retourne le nombre de fournisseurs sans NIU (alertes potentielles)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> countSuppliersWithoutNiu(
            @PathVariable Long companyId) {

        Long count = taxService.countSuppliersWithoutNiu(companyId);

        Map<String, Object> result = Map.of(
            "count", count,
            "message", count > 0
                ? String.format("⚠️ %d fournisseur(s) sans NIU - Risque de pénalités AIR", count)
                : "✅ Tous les fournisseurs ont un NIU"
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard fiscal",
               description = "Vue d'ensemble de la situation fiscale de l'entreprise")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTaxDashboard(
            @PathVariable Long companyId) {

        // Résumé du mois en cours
        YearMonth currentMonth = YearMonth.now();
        Map<TaxType, BigDecimal> currentSummary = taxService.getMonthlySummary(
            companyId, currentMonth.getYear(), currentMonth.getMonthValue()
        );

        // Alertes actives
        Long alertCount = taxService.getActiveAlerts(companyId).stream().count();

        // Fournisseurs sans NIU
        Long suppliersWithoutNiu = taxService.countSuppliersWithoutNiu(companyId);

        // Configurations actives
        List<TaxConfiguration> configs = taxService.getTaxConfigurations(companyId);
        long activeConfigsCount = configs.stream().filter(TaxConfiguration::getIsActive).count();

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("currentPeriod", currentMonth.toString());
        dashboard.put("totalTaxes", currentSummary.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        dashboard.put("alertCount", alertCount);
        dashboard.put("suppliersWithoutNiu", suppliersWithoutNiu);
        dashboard.put("activeTaxConfigurations", activeConfigsCount);
        dashboard.put("taxBreakdown", currentSummary);

        // Statut général
        String status = alertCount == 0 && suppliersWithoutNiu == 0
            ? "COMPLIANT"
            : "NEEDS_ATTENTION";
        dashboard.put("complianceStatus", status);

        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    @PostMapping("/initialize")
    @Operation(summary = "Initialiser configurations fiscales",
               description = "Crée les configurations par défaut pour une entreprise (admin)")
    public ResponseEntity<ApiResponse<String>> initializeTaxConfigurations(
            @PathVariable Long companyId) {

        // Note: Cette méthode devrait être protégée par un rôle ADMIN
        // Pour le MVP, on la laisse accessible

        // Cette méthode est normalement appelée automatiquement lors de la création d'une entreprise
        // Mais elle peut être utilisée pour réinitialiser les configurations

        return ResponseEntity.ok(ApiResponse.success(
            "Utilisez POST /companies pour créer une entreprise avec configurations fiscales automatiques"
        ));
    }

    // ============================================
    // ENDPOINTS DÉCLARATION DE TVA (CA3)
    // ============================================

    @PostMapping("/vat-declarations/generate")
    @Operation(summary = "Générer déclaration TVA mensuelle (CA3)",
               description = "Génère automatiquement la déclaration de TVA pour un mois donné à partir du grand livre")
    public ResponseEntity<ApiResponse<com.predykt.accounting.domain.entity.VATDeclaration>> generateVATDeclaration(
            @PathVariable Long companyId,
            @RequestParam int year,
            @RequestParam int month) {

        com.predykt.accounting.domain.entity.VATDeclaration declaration =
            vatDeclarationService.generateMonthlyDeclaration(companyId, year, month);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(declaration));
    }

    @GetMapping("/vat-declarations")
    @Operation(summary = "Liste des déclarations de TVA",
               description = "Récupère toutes les déclarations de TVA de l'entreprise")
    public ResponseEntity<ApiResponse<List<com.predykt.accounting.domain.entity.VATDeclaration>>> getAllVATDeclarations(
            @PathVariable Long companyId) {

        List<com.predykt.accounting.domain.entity.VATDeclaration> declarations =
            vatDeclarationService.getAllDeclarations(companyId);

        return ResponseEntity.ok(ApiResponse.success(declarations));
    }

    @GetMapping("/vat-declarations/{declarationId}")
    @Operation(summary = "Détails d'une déclaration de TVA",
               description = "Récupère les détails d'une déclaration par ID")
    public ResponseEntity<ApiResponse<com.predykt.accounting.domain.entity.VATDeclaration>> getVATDeclaration(
            @PathVariable Long companyId,
            @PathVariable Long declarationId) {

        com.predykt.accounting.domain.entity.VATDeclaration declaration =
            vatDeclarationService.getDeclaration(declarationId);

        return ResponseEntity.ok(ApiResponse.success(declaration));
    }

    @GetMapping("/vat-declarations/status/{status}")
    @Operation(summary = "Déclarations par statut",
               description = "Récupère les déclarations par statut (DRAFT, VALIDATED, SUBMITTED, PAID)")
    public ResponseEntity<ApiResponse<List<com.predykt.accounting.domain.entity.VATDeclaration>>> getVATDeclarationsByStatus(
            @PathVariable Long companyId,
            @PathVariable String status) {

        List<com.predykt.accounting.domain.entity.VATDeclaration> declarations =
            vatDeclarationService.getDeclarationsByStatus(companyId, status);

        return ResponseEntity.ok(ApiResponse.success(declarations));
    }

    @PutMapping("/vat-declarations/{declarationId}/validate")
    @Operation(summary = "Valider une déclaration de TVA",
               description = "Valide une déclaration en brouillon (recalcule les totaux et change le statut)")
    public ResponseEntity<ApiResponse<com.predykt.accounting.domain.entity.VATDeclaration>> validateVATDeclaration(
            @PathVariable Long companyId,
            @PathVariable Long declarationId) {

        com.predykt.accounting.domain.entity.VATDeclaration declaration =
            vatDeclarationService.validateDeclaration(declarationId);

        return ResponseEntity.ok(ApiResponse.success(declaration));
    }

    @PutMapping("/vat-declarations/{declarationId}/submit")
    @Operation(summary = "Soumettre une déclaration de TVA",
               description = "Soumet une déclaration validée à l'administration fiscale")
    public ResponseEntity<ApiResponse<com.predykt.accounting.domain.entity.VATDeclaration>> submitVATDeclaration(
            @PathVariable Long companyId,
            @PathVariable Long declarationId,
            @RequestParam String referenceNumber) {

        com.predykt.accounting.domain.entity.VATDeclaration declaration =
            vatDeclarationService.submitDeclaration(declarationId, referenceNumber);

        return ResponseEntity.ok(ApiResponse.success(declaration));
    }

    @PutMapping("/vat-declarations/{declarationId}/mark-paid")
    @Operation(summary = "Marquer comme payée",
               description = "Marque une déclaration comme payée")
    public ResponseEntity<ApiResponse<com.predykt.accounting.domain.entity.VATDeclaration>> markVATDeclarationAsPaid(
            @PathVariable Long companyId,
            @PathVariable Long declarationId) {

        com.predykt.accounting.domain.entity.VATDeclaration declaration =
            vatDeclarationService.markDeclarationAsPaid(declarationId);

        return ResponseEntity.ok(ApiResponse.success(declaration));
    }

    @DeleteMapping("/vat-declarations/{declarationId}")
    @Operation(summary = "Supprimer une déclaration de TVA",
               description = "Supprime une déclaration en brouillon (uniquement si DRAFT)")
    public ResponseEntity<ApiResponse<String>> deleteVATDeclaration(
            @PathVariable Long companyId,
            @PathVariable Long declarationId) {

        vatDeclarationService.deleteDeclaration(declarationId);

        return ResponseEntity.ok(ApiResponse.success(
            "Déclaration supprimée avec succès"
        ));
    }

    @GetMapping("/vat-declarations/{declarationId}/report")
    @Operation(summary = "Rapport de déclaration TVA",
               description = "Génère un rapport textuel formaté de la déclaration")
    public ResponseEntity<String> generateVATDeclarationReport(
            @PathVariable Long companyId,
            @PathVariable Long declarationId) {

        String report = vatDeclarationService.generateDeclarationReport(declarationId);

        return ResponseEntity.ok()
            .header("Content-Type", "text/plain; charset=UTF-8")
            .body(report);
    }

    // ============================================
    // ENDPOINTS TVA RÉCUPÉRABLE / NON RÉCUPÉRABLE
    // ============================================

    @GetMapping("/vat-recoverability/transactions")
    @Operation(summary = "Transactions de TVA avec récupérabilité",
               description = "Récupère toutes les transactions de TVA pour une période avec leur catégorie de récupérabilité")
    public ResponseEntity<ApiResponse<List<com.predykt.accounting.domain.entity.VATTransaction>>> getVATTransactions(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<com.predykt.accounting.domain.entity.VATTransaction> transactions =
            vatRecoverabilityService.getTransactionsByPeriod(companyId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/vat-recoverability/non-recoverable")
    @Operation(summary = "Transactions avec TVA non récupérable",
               description = "Récupère les transactions ayant de la TVA non récupérable (véhicules tourisme, carburant VP, etc.)")
    public ResponseEntity<ApiResponse<List<com.predykt.accounting.domain.entity.VATTransaction>>> getNonRecoverableTransactions(
            @PathVariable Long companyId) {

        List<com.predykt.accounting.domain.entity.VATTransaction> transactions =
            vatRecoverabilityService.getNonRecoverableTransactions(companyId);

        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/vat-recoverability/statistics")
    @Operation(summary = "Statistiques TVA non récupérable",
               description = "Statistiques détaillées de la TVA non récupérable par catégorie (véhicules, carburant, etc.)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNonRecoverableVATStatistics(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Map<String, Object> statistics =
            vatRecoverabilityService.getNonRecoverableVATStatistics(companyId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    @PutMapping("/vat-recoverability/transactions/{transactionId}/category")
    @Operation(summary = "Modifier la catégorie de récupérabilité",
               description = "Modifie la catégorie de récupérabilité d'une transaction (correction manuelle)")
    public ResponseEntity<ApiResponse<com.predykt.accounting.domain.entity.VATTransaction>> updateRecoverableCategory(
            @PathVariable Long companyId,
            @PathVariable Long transactionId,
            @RequestParam com.predykt.accounting.domain.enums.VATRecoverableCategory category,
            @RequestParam(required = false) String justification) {

        com.predykt.accounting.domain.entity.VATTransaction transaction =
            vatRecoverabilityService.updateRecoverableCategory(transactionId, category, justification);

        return ResponseEntity.ok(ApiResponse.success(transaction));
    }

    @GetMapping("/vat-recoverability/alerts/count")
    @Operation(summary = "Nombre d'alertes TVA récupérabilité",
               description = "Compte le nombre de transactions avec alertes de récupérabilité")
    public ResponseEntity<ApiResponse<Map<String, Object>>> countRecoverabilityAlerts(
            @PathVariable Long companyId) {

        Long count = vatRecoverabilityService.countTransactionsWithAlerts(companyId);

        Map<String, Object> result = Map.of(
            "alertCount", count,
            "message", count > 0
                ? String.format("⚠️ %d transaction(s) avec TVA non/partiellement récupérable", count)
                : "✅ Toutes les transactions ont une TVA 100% récupérable"
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ============================================
    // ENDPOINTS ADMINISTRATION DES RÈGLES DE DÉTECTION
    // ============================================

    @GetMapping("/vat-recoverability/rules")
    @Operation(summary = "Liste des règles de détection",
               description = "Récupère toutes les règles de détection de récupérabilité TVA (actives et inactives)")
    public ResponseEntity<ApiResponse<List<RecoverabilityRule>>> getAllRules(
            @PathVariable Long companyId) {

        List<RecoverabilityRule> rules = ruleRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(rules));
    }

    @GetMapping("/vat-recoverability/rules/active")
    @Operation(summary = "Règles actives par priorité",
               description = "Récupère toutes les règles actives triées par priorité croissante")
    public ResponseEntity<ApiResponse<List<RecoverabilityRule>>> getActiveRules(
            @PathVariable Long companyId) {

        List<RecoverabilityRule> rules = ruleRepository.findByIsActiveTrueOrderByPriorityAsc();
        return ResponseEntity.ok(ApiResponse.success(rules));
    }

    @GetMapping("/vat-recoverability/rules/category/{category}")
    @Operation(summary = "Règles par catégorie",
               description = "Récupère les règles actives pour une catégorie spécifique")
    public ResponseEntity<ApiResponse<List<RecoverabilityRule>>> getRulesByCategory(
            @PathVariable Long companyId,
            @PathVariable VATRecoverableCategory category) {

        List<RecoverabilityRule> rules = ruleRepository.findByIsActiveTrueAndCategoryOrderByPriorityAsc(category);
        return ResponseEntity.ok(ApiResponse.success(rules));
    }

    @GetMapping("/vat-recoverability/rules/{ruleId}")
    @Operation(summary = "Détails d'une règle",
               description = "Récupère les détails d'une règle par ID")
    public ResponseEntity<ApiResponse<RecoverabilityRule>> getRule(
            @PathVariable Long companyId,
            @PathVariable Long ruleId) {

        RecoverabilityRule rule = ruleRepository.findById(ruleId)
            .orElseThrow(() -> new com.predykt.accounting.exception.ResourceNotFoundException("Règle non trouvée"));
        return ResponseEntity.ok(ApiResponse.success(rule));
    }

    @PostMapping("/vat-recoverability/rules")
    @Operation(summary = "Créer une nouvelle règle",
               description = "Crée une nouvelle règle de détection de récupérabilité TVA")
    public ResponseEntity<ApiResponse<RecoverabilityRule>> createRule(
            @PathVariable Long companyId,
            @Valid @RequestBody RecoverabilityRule rule) {

        RecoverabilityRule saved = ruleRepository.save(rule);
        ruleEngine.invalidateCache();

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(saved));
    }

    @PutMapping("/vat-recoverability/rules/{ruleId}")
    @Operation(summary = "Modifier une règle",
               description = "Met à jour une règle de détection existante")
    public ResponseEntity<ApiResponse<RecoverabilityRule>> updateRule(
            @PathVariable Long companyId,
            @PathVariable Long ruleId,
            @Valid @RequestBody RecoverabilityRule updatedRule) {

        RecoverabilityRule rule = ruleRepository.findById(ruleId)
            .orElseThrow(() -> new com.predykt.accounting.exception.ResourceNotFoundException("Règle non trouvée"));

        // Mettre à jour les champs
        rule.setName(updatedRule.getName());
        rule.setDescription(updatedRule.getDescription());
        rule.setPriority(updatedRule.getPriority());
        rule.setConfidenceScore(updatedRule.getConfidenceScore());
        rule.setAccountPattern(updatedRule.getAccountPattern());
        rule.setDescriptionPattern(updatedRule.getDescriptionPattern());
        rule.setRequiredKeywords(updatedRule.getRequiredKeywords());
        rule.setExcludedKeywords(updatedRule.getExcludedKeywords());
        rule.setCategory(updatedRule.getCategory());
        rule.setReason(updatedRule.getReason());
        rule.setLegalReference(updatedRule.getLegalReference());
        rule.setIsActive(updatedRule.getIsActive());
        rule.setRuleType(updatedRule.getRuleType());

        RecoverabilityRule saved = ruleRepository.save(rule);
        ruleEngine.invalidateCache();

        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @PutMapping("/vat-recoverability/rules/{ruleId}/toggle")
    @Operation(summary = "Activer/Désactiver une règle",
               description = "Active ou désactive une règle de détection")
    public ResponseEntity<ApiResponse<RecoverabilityRule>> toggleRule(
            @PathVariable Long companyId,
            @PathVariable Long ruleId,
            @RequestParam boolean active) {

        RecoverabilityRule rule = ruleRepository.findById(ruleId)
            .orElseThrow(() -> new com.predykt.accounting.exception.ResourceNotFoundException("Règle non trouvée"));

        rule.setIsActive(active);
        RecoverabilityRule saved = ruleRepository.save(rule);
        ruleEngine.invalidateCache();

        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @DeleteMapping("/vat-recoverability/rules/{ruleId}")
    @Operation(summary = "Supprimer une règle",
               description = "Supprime une règle de détection (désactivation recommandée à la place)")
    public ResponseEntity<ApiResponse<String>> deleteRule(
            @PathVariable Long companyId,
            @PathVariable Long ruleId) {

        ruleRepository.deleteById(ruleId);
        ruleEngine.invalidateCache();

        return ResponseEntity.ok(ApiResponse.success("Règle supprimée avec succès"));
    }

    @GetMapping("/vat-recoverability/rules/statistics")
    @Operation(summary = "Statistiques des règles",
               description = "Récupère les statistiques de performance du moteur de règles")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRuleEngineStatistics(
            @PathVariable Long companyId) {

        Map<String, Object> stats = vatRecoverabilityService.getRuleEngineStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/vat-recoverability/rules/needing-review")
    @Operation(summary = "Règles nécessitant une révision",
               description = "Récupère les règles avec faible précision ou trop de corrections")
    public ResponseEntity<ApiResponse<List<RecoverabilityRule>>> getRulesNeedingReview(
            @PathVariable Long companyId) {

        List<RecoverabilityRule> rules = ruleRepository.findRulesNeedingReview();
        return ResponseEntity.ok(ApiResponse.success(rules));
    }

    @GetMapping("/vat-recoverability/rules/top-performing")
    @Operation(summary = "Règles les plus performantes",
               description = "Récupère les règles avec le meilleur taux de précision")
    public ResponseEntity<ApiResponse<List<RecoverabilityRule>>> getTopPerformingRules(
            @PathVariable Long companyId) {

        List<RecoverabilityRule> rules = ruleRepository.findTopPerformingRules();
        return ResponseEntity.ok(ApiResponse.success(rules));
    }

    @PostMapping("/vat-recoverability/rules/cache/invalidate")
    @Operation(summary = "Invalider le cache des règles",
               description = "Force le rechargement des règles depuis la base de données")
    public ResponseEntity<ApiResponse<String>> invalidateRuleCache(
            @PathVariable Long companyId) {

        vatRecoverabilityService.invalidateRuleCache();
        return ResponseEntity.ok(ApiResponse.success("Cache invalidé avec succès"));
    }

    @PostMapping("/vat-recoverability/detect")
    @Operation(summary = "Tester la détection de catégorie",
               description = "Teste la détection de catégorie pour un compte et une description donnés")
    public ResponseEntity<ApiResponse<VATRecoverabilityRuleEngine.DetectionResult>> testDetection(
            @PathVariable Long companyId,
            @RequestParam String accountNumber,
            @RequestParam String description) {

        VATRecoverabilityRuleEngine.DetectionResult result =
            vatRecoverabilityService.detectRecoverableCategoryWithDetails(accountNumber, description);

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
