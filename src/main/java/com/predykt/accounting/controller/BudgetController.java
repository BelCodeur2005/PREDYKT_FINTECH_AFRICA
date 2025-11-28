package com.predykt.accounting.controller;

import com.predykt.accounting.domain.entity.Budget;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/companies/{companyId}/budgets")
@RequiredArgsConstructor
@Tag(name = "Budgets", description = "Gestion des budgets previsionnels")
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    @Operation(summary = "Creer un budget",
               description = "Cree un nouveau budget previsionnel pour une periode")
    public ResponseEntity<ApiResponse<Budget>> createBudget(
            @PathVariable Long companyId,
            @RequestBody Budget budget) {

        // Associer l'entreprise
        budget.getCompany().setId(companyId);

        Budget created = budgetService.createBudget(budget);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(created));
    }

    @GetMapping
    @Operation(summary = "Lister les budgets",
               description = "Recupere tous les budgets d'une entreprise")
    public ResponseEntity<ApiResponse<List<Budget>>> getCompanyBudgets(
            @PathVariable Long companyId) {

        List<Budget> budgets = budgetService.getCompanyBudgets(companyId);
        return ResponseEntity.ok(ApiResponse.success(budgets));
    }

    @GetMapping("/{budgetId}")
    @Operation(summary = "Recuperer un budget",
               description = "Recupere un budget par son ID")
    public ResponseEntity<ApiResponse<Budget>> getBudget(
            @PathVariable Long companyId,
            @PathVariable Long budgetId) {

        Budget budget = budgetService.getBudgetById(budgetId);
        return ResponseEntity.ok(ApiResponse.success(budget));
    }

    @PutMapping("/{budgetId}")
    @Operation(summary = "Mettre a jour un budget",
               description = "Met a jour un budget existant")
    public ResponseEntity<ApiResponse<Budget>> updateBudget(
            @PathVariable Long companyId,
            @PathVariable Long budgetId,
            @RequestBody Budget budget) {

        Budget updated = budgetService.updateBudget(budgetId, budget);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/{budgetId}")
    @Operation(summary = "Supprimer un budget",
               description = "Supprime un budget")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(
            @PathVariable Long companyId,
            @PathVariable Long budgetId) {

        budgetService.deleteBudget(budgetId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/fiscal-year/{fiscalYear}")
    @Operation(summary = "Budgets par annee fiscale",
               description = "Recupere les budgets pour une annee fiscale")
    public ResponseEntity<ApiResponse<List<Budget>>> getBudgetsByYear(
            @PathVariable Long companyId,
            @PathVariable String fiscalYear) {

        List<Budget> budgets = budgetService.getBudgetsByFiscalYear(companyId, fiscalYear);
        return ResponseEntity.ok(ApiResponse.success(budgets));
    }

    @PutMapping("/{budgetId}/actual-amount")
    @Operation(summary = "Mettre a jour montant reel",
               description = "Met a jour le montant reel et recalcule la variance")
    public ResponseEntity<ApiResponse<Budget>> updateActualAmount(
            @PathVariable Long companyId,
            @PathVariable Long budgetId,
            @RequestParam BigDecimal actualAmount) {

        Budget updated = budgetService.updateActualAmount(budgetId, actualAmount);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PostMapping("/update-actuals/{fiscalYear}")
    @Operation(summary = "Calculer montants reels",
               description = "Calcule les montants reels depuis le grand livre")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateActualsFromLedger(
            @PathVariable Long companyId,
            @PathVariable String fiscalYear) {

        int updatedCount = budgetService.updateActualAmountsFromLedger(companyId, fiscalYear);
        Map<String, Object> result = Map.of(
            "updatedBudgets", updatedCount,
            "fiscalYear", fiscalYear
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/over-budget")
    @Operation(summary = "Budgets depasses",
               description = "Recupere les budgets depasses")
    public ResponseEntity<ApiResponse<List<Budget>>> getOverBudgets(
            @PathVariable Long companyId) {

        List<Budget> overBudgets = budgetService.getOverBudgets(companyId);
        return ResponseEntity.ok(ApiResponse.success(overBudgets));
    }

    @GetMapping("/high-variance")
    @Operation(summary = "Budgets avec variance elevee",
               description = "Recupere les budgets avec variance significative")
    public ResponseEntity<ApiResponse<List<Budget>>> getHighVarianceBudgets(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "10.0") BigDecimal thresholdPct) {

        List<Budget> budgets = budgetService.getBudgetsWithHighVariance(companyId, thresholdPct);
        return ResponseEntity.ok(ApiResponse.success(budgets));
    }

    @PutMapping("/{budgetId}/lock")
    @Operation(summary = "Verrouiller un budget",
               description = "Verrouille un budget pour empecher les modifications")
    public ResponseEntity<ApiResponse<Budget>> lockBudget(
            @PathVariable Long companyId,
            @PathVariable Long budgetId) {

        Budget locked = budgetService.lockBudget(budgetId);
        return ResponseEntity.ok(ApiResponse.success(locked));
    }

    @PutMapping("/{budgetId}/unlock")
    @Operation(summary = "Deverrouiller un budget",
               description = "Deverrouille un budget")
    public ResponseEntity<ApiResponse<Budget>> unlockBudget(
            @PathVariable Long companyId,
            @PathVariable Long budgetId) {

        Budget unlocked = budgetService.unlockBudget(budgetId);
        return ResponseEntity.ok(ApiResponse.success(unlocked));
    }

    @GetMapping("/statistics/{fiscalYear}")
    @Operation(summary = "Statistiques budgetaires",
               description = "Genere les statistiques budgetaires pour une annee")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBudgetStatistics(
            @PathVariable Long companyId,
            @PathVariable String fiscalYear) {

        Map<String, Object> stats = budgetService.getBudgetStatistics(companyId, fiscalYear);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/compare")
    @Operation(summary = "Comparer budget vs reel",
               description = "Compare les budgets vs reels pour une periode")
    public ResponseEntity<ApiResponse<Map<String, Object>>> compareBudgetVsActual(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Map<String, Object> comparison = budgetService.compareBudgetVsActual(companyId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(comparison));
    }
}
