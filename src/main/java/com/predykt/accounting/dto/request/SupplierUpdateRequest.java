package com.predykt.accounting.dto.request;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la mise à jour d'un fournisseur
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierUpdateRequest {

    private String name;
    private String taxId;
    private String niuNumber;

    @Email(message = "Email invalide")
    private String email;

    private String phone;
    private String address;
    private String city;
    private String country;
    private String supplierType;
    private Integer paymentTerms;
    private Boolean isActive;
}
