// BalanceSheetResponse.java
package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceSheetResponse {
    private LocalDate asOfDate;
    private String currency;
    
    // ACTIF
    private BigDecimal fixedAssets;        // Actif immobilisé
    private BigDecimal currentAssets;      // Actif circulant
    private BigDecimal cash;               // Trésorerie
    private BigDecimal totalAssets;
    
    // PASSIF
    private BigDecimal equity;             // Capitaux propres
    private BigDecimal longTermLiabilities; // Dettes long terme
    private BigDecimal currentLiabilities;  // Dettes court terme
    private BigDecimal totalLiabilities;
    
    private List<AccountBalanceDetail> assetDetails;
    private List<AccountBalanceDetail> liabilityDetails;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountBalanceDetail {
        private String accountNumber;
        private String accountName;
        private BigDecimal balance;
    }
}