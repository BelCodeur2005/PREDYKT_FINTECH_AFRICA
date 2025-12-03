package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * DTO de réponse pour le résumé fiscal mensuel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxSummaryResponse {

    private LocalDate startDate;
    private LocalDate endDate;
    private String fiscalPeriod;  // "2024-11"

    // Taxes par type
    private BigDecimal vatAmount;           // TVA 19,25%
    private BigDecimal isAdvanceAmount;     // Acompte IS 2,2%
    private BigDecimal airAmount;           // AIR total
    private BigDecimal irppRentAmount;      // IRPP Loyer 15%
    private BigDecimal cnpsAmount;          // CNPS ~20%

    // Totaux
    private BigDecimal totalTaxes;
    private BigDecimal totalAlerts;

    // Détails AIR
    private BigDecimal airWithNiuAmount;    // AIR 2,2%
    private BigDecimal airWithoutNiuAmount; // AIR 5,5% (pénalité)
    private BigDecimal airPenaltyCost;      // Surcoût dû aux pénalités

    // Statuts
    private Integer alertCount;
    private Map<String, BigDecimal> taxBreakdown;  // Détail par type de taxe
}
