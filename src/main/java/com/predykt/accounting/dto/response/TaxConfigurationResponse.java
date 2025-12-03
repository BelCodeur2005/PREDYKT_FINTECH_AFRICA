package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour la configuration fiscale
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxConfigurationResponse {

    private Long id;
    private String taxType;
    private String taxName;
    private BigDecimal taxRate;
    private String accountNumber;
    private Boolean isActive;
    private Boolean isAutomatic;
    private Boolean applyToSales;
    private Boolean applyToPurchases;
    private String description;
    private String legalReference;
    private Integer dueDay;
}
