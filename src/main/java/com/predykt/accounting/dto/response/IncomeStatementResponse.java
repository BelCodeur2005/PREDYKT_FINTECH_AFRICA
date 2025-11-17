// IncomeStatementResponse.java
package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeStatementResponse {
    private LocalDate startDate;
    private LocalDate endDate;
    private String currency;
    
    // PRODUITS
    private BigDecimal salesRevenue;           // Ventes (701)
    private BigDecimal serviceRevenue;         // Prestations (706)
    private BigDecimal otherOperatingIncome;   // Autres produits
    private BigDecimal financialIncome;        // Produits financiers
    private BigDecimal totalRevenue;
    
    // CHARGES
    private BigDecimal purchasesCost;          // Achats (601)
    private BigDecimal personnelCost;          // Charges personnel (66)
    private BigDecimal operatingExpenses;      // Autres charges exploitation
    private BigDecimal financialExpenses;      // Charges financières
    private BigDecimal taxesAndDuties;         // Impôts et taxes
    private BigDecimal totalExpenses;
    
    // RÉSULTATS
    private BigDecimal grossProfit;            // Marge brute
    private BigDecimal operatingIncome;        // Résultat d'exploitation
    private BigDecimal netIncome;              // Résultat net
    
    // RATIOS
    private BigDecimal grossMarginPercentage;
    private BigDecimal netMarginPercentage;
}