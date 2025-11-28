package com.predykt.accounting.controller;

import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/companies/{companyId}/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Tableaux de bord et KPIs")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Dashboard complet",
               description = "Recupere le dashboard complet avec tous les KPIs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(
            @PathVariable Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        if (asOfDate == null) {
            asOfDate = LocalDate.now();
        }

        Map<String, Object> dashboard = dashboardService.getCompanyDashboard(companyId, asOfDate);
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    @GetMapping("/period-summary")
    @Operation(summary = "Resume d'une periode",
               description = "Genere un resume financier pour une periode donnee")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPeriodSummary(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Map<String, Object> summary = dashboardService.getPeriodSummary(companyId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/compare-periods")
    @Operation(summary = "Comparer deux periodes",
               description = "Compare les performances de deux periodes")
    public ResponseEntity<ApiResponse<Map<String, Object>>> comparePeriods(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate period1Start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate period1End,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate period2Start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate period2End) {

        Map<String, Object> comparison = dashboardService.comparePeriods(
            companyId, period1Start, period1End, period2Start, period2End
        );
        return ResponseEntity.ok(ApiResponse.success(comparison));
    }
}
