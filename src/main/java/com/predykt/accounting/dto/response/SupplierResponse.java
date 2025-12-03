package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de réponse pour un fournisseur
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierResponse {

    private Long id;
    private String name;
    private String taxId;
    private String niuNumber;
    private Boolean hasNiu;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String country;
    private Boolean isActive;
    private String supplierType;
    private Integer paymentTerms;

    // Informations calculées
    private BigDecimal applicableAirRate;  // 2,2% ou 5,5%
    private Boolean requiresAlert;  // TRUE si NIU manquant
    private String alertMessage;
}
