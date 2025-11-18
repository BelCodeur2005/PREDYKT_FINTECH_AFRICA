// ============================================
// FinancialRatiosResponse.java
// ============================================
package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialRatiosResponse {
    private String fiscalYear;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    
    // Rentabilité
    private BigDecimal grossMarginPct;
    private BigDecimal netMarginPct;
    private BigDecimal roaPct;
    private BigDecimal roePct;
    
    // Liquidité
    private BigDecimal currentRatio;
    private BigDecimal quickRatio;
    private BigDecimal cashRatio;
    
    // Solvabilité
    private BigDecimal debtRatioPct;
    private BigDecimal debtToEquity;
    
    // Activité
    private Integer dsoDays;
    private Integer dioDays;
    private Integer dpoDays;
    private Integer cashConversionCycle;
    
    // Données brutes
    private BigDecimal totalRevenue;
    private BigDecimal netIncome;
    private BigDecimal totalAssets;
    
    private LocalDateTime calculatedAt;
}