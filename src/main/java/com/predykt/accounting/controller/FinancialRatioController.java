// ============================================
// FinancialRatioController.java
// ============================================
package com.predykt.accounting.controller;

import com.predykt.accounting.domain.entity.FinancialRatio;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.FinancialRatiosResponse;
import com.predykt.accounting.service.FinancialRatioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/companies/{companyId}/ratios")
@RequiredArgsConstructor
@Tag(name = "Ratios Financiers", description = "Calcul et consultation des ratios financiers")
public class FinancialRatioController {
    
    private final FinancialRatioService ratioService;
    
    @PostMapping("/calculate")
    @Operation(summary = "Calculer les ratios financiers",
               description = "Calcule tous les ratios financiers pour une période donnée")
    public ResponseEntity<ApiResponse<FinancialRatio>> calculateRatios(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        FinancialRatio ratios = ratioService.calculateAndSaveRatios(companyId, startDate, endDate);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(ratios, "Ratios calculés avec succès"));
    }
    
    @GetMapping("/year/{fiscalYear}")
    @Operation(summary = "Obtenir les ratios d'une année",
               description = "Récupère les ratios financiers d'une année fiscale spécifique")
    public ResponseEntity<ApiResponse<FinancialRatio>> getRatiosByYear(
            @PathVariable Long companyId,
            @PathVariable String fiscalYear) {
        
        FinancialRatio ratios = ratioService.getRatiosByYear(companyId, fiscalYear);
        
        return ResponseEntity.ok(ApiResponse.success(ratios));
    }
    
    @GetMapping("/history")
    @Operation(summary = "Historique des ratios",
               description = "Récupère l'historique complet des ratios financiers")
    public ResponseEntity<ApiResponse<List<FinancialRatiosResponse>>> getRatiosHistory(
            @PathVariable Long companyId) {
        
        List<FinancialRatiosResponse> history = ratioService.getRatiosHistory(companyId);
        
        return ResponseEntity.ok(ApiResponse.success(history));
    }
    
    @GetMapping("/compare")
    @Operation(summary = "Comparer deux années",
               description = "Compare les ratios entre deux années fiscales")
    public ResponseEntity<ApiResponse<FinancialRatioService.RatioComparison>> compareRatios(
            @PathVariable Long companyId,
            @RequestParam String year1,
            @RequestParam String year2) {
        
        var comparison = ratioService.compareRatios(companyId, year1, year2);
        
        return ResponseEntity.ok(ApiResponse.success(comparison));
    }
}