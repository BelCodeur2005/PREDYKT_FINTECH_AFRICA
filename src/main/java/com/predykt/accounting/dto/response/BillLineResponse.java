package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de réponse pour une ligne de facture fournisseur
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillLineResponse {

    private Long id;
    private Integer lineNumber;

    // Produit/Service
    private String productCode;
    private String description;

    // Quantités et prix
    private BigDecimal quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal discountPercentage;

    // Montants
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal totalHt;

    // TVA
    private BigDecimal vatRate;
    private BigDecimal vatAmount;
    private BigDecimal totalTtc;

    // Compte comptable
    private String accountNumber;
    private String accountName;
}
