package com.predykt.accounting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour créer/modifier une ligne de facture client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLineRequest {

    @NotBlank(message = "La description est obligatoire")
    private String description;

    private String productCode;

    @NotNull(message = "La quantité est obligatoire")
    @Positive(message = "La quantité doit être positive")
    private BigDecimal quantity;

    private String unit;  // kg, m², unité, heure...

    @NotNull(message = "Le prix unitaire est obligatoire")
    @Positive(message = "Le prix unitaire doit être positif")
    private BigDecimal unitPrice;

    private BigDecimal discountPercentage;

    private BigDecimal vatRate;  // Si non fourni, 19.25% par défaut

    private String accountNumber;  // Compte de produit (701, 706...)
}
