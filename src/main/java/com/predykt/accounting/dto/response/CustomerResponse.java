package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {

    private Long id;
    private Long companyId;
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
    private String customerType;
    private Integer paymentTerms;
    private BigDecimal creditLimit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Numéro de compte auxiliaire OHADA (4111001, 4111002...)
     * Auto-généré lors de la création du client
     */
    private String auxiliaryAccountNumber;

    /**
     * Indicateur si le client est un client export (exonération TVA possible)
     */
    private Boolean isExportCustomer;

    /**
     * Indicateur si le client a un NIU valide
     */
    private Boolean hasValidNiu;
}
