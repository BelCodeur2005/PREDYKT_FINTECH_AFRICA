package com.predykt.accounting.controller;

import com.predykt.accounting.dto.response.AgingReportResponse;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.service.AgingReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Contrôleur REST pour les balances âgées (clients et fournisseurs)
 */
@RestController
@RequestMapping("/companies/{companyId}/reports")
@RequiredArgsConstructor
@Tag(name = "Balances Âgées", description = "Rapports de vieillissement des créances et dettes")
public class AgingReportController {

    private final AgingReportService agingReportService;

    @GetMapping("/customers-aging")
    @Operation(summary = "Balance âgée des clients",
               description = "Génère la balance âgée des clients avec analyse par tranches d'âge (0-30j, 30-60j, 60-90j, >90j)")
    public ResponseEntity<ApiResponse<AgingReportResponse>> getCustomersAgingReport(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now()}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        AgingReportResponse report = agingReportService.generateCustomersAgingReport(companyId, asOfDate);

        String message = String.format("Balance âgée clients générée: %d client(s) - %s FCFA total",
            report.getSummary().getTotalItems(),
            report.getSummary().getGrandTotal().setScale(0, java.math.RoundingMode.HALF_UP));

        return ResponseEntity.ok(ApiResponse.success(report, message));
    }

    @GetMapping("/suppliers-aging")
    @Operation(summary = "Balance âgée des fournisseurs",
               description = "Génère la balance âgée des fournisseurs avec analyse par tranches d'âge (0-30j, 30-60j, 60-90j, >90j)")
    public ResponseEntity<ApiResponse<AgingReportResponse>> getSuppliersAgingReport(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now()}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        AgingReportResponse report = agingReportService.generateSuppliersAgingReport(companyId, asOfDate);

        String message = String.format("Balance âgée fournisseurs générée: %d fournisseur(s) - %s FCFA total",
            report.getSummary().getTotalItems(),
            report.getSummary().getGrandTotal().setScale(0, java.math.RoundingMode.HALF_UP));

        return ResponseEntity.ok(ApiResponse.success(report, message));
    }
}
