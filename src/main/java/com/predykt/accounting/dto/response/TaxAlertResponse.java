package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO pour une alerte fiscale
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxAlertResponse {

    private Long id;
    private String taxType;
    private LocalDate calculationDate;
    private String supplierName;
    private String niuNumber;
    private BigDecimal baseAmount;
    private BigDecimal taxAmount;
    private BigDecimal penaltyCost;
    private String alertMessage;
    private LocalDateTime createdAt;

    // Métadonnées
    private String severity;  // "HIGH", "MEDIUM", "LOW"
    private String actionRequired;  // "Régulariser NIU fournisseur"
}
