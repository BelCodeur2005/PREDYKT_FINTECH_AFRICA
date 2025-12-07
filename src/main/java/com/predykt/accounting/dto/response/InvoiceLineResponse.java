package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de réponse pour une ligne de facture client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLineResponse {

    private Long id;
    private Integer lineNumber;
    private String productCode;
    private String description;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
    private BigDecimal subtotal;
    private BigDecimal totalHt;
    private BigDecimal vatRate;
    private BigDecimal vatAmount;
    private BigDecimal totalTtc;
    private String accountNumber;
}
