package com.predykt.accounting.controller;

import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.BalanceSheetResponse;
import com.predykt.accounting.dto.response.IncomeStatementResponse;
import com.predykt.accounting.service.FinancialReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/companies/{companyId}/reports")
@RequiredArgsConstructor
@Tag(name = "Rapports Financiers", description = "Génération des états financiers")
public class FinancialReportController {
    
    private final FinancialReportService reportService;
    
    @GetMapping("/balance-sheet")
    @Operation(summary = "Générer le Bilan",
               description = "Génère le bilan (Balance Sheet) à une date donnée")
    public ResponseEntity<ApiResponse<BalanceSheetResponse>> getBalanceSheet(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now()}") 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        
        BalanceSheetResponse balanceSheet = reportService.generateBalanceSheet(companyId, asOfDate);
        
        return ResponseEntity.ok(ApiResponse.success(balanceSheet));
    }
    
    @GetMapping("/income-statement")
    @Operation(summary = "Générer le Compte de Résultat",
               description = "Génère le compte de résultat (Income Statement) pour une période")
    public ResponseEntity<ApiResponse<IncomeStatementResponse>> getIncomeStatement(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        IncomeStatementResponse incomeStatement = 
            reportService.generateIncomeStatement(companyId, startDate, endDate);
        
        return ResponseEntity.ok(ApiResponse.success(incomeStatement));
    }
}