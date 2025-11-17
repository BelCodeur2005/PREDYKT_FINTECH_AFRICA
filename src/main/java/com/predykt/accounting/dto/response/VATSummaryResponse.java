// VATSummaryResponse.java
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
public class VATSummaryResponse {
    private LocalDate startDate;
    private LocalDate endDate;
    
    private BigDecimal vatCollected;     // TVA collectée (compte 4431)
    private BigDecimal vatDeductible;    // TVA déductible (compte 4451)
    private BigDecimal vatToPay;         // TVA à payer (ou crédit)
    
    private String status;  // "A_PAYER", "CREDIT"
}