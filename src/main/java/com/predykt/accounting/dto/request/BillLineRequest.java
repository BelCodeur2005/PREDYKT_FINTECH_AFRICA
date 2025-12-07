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
 * DTO pour une ligne de facture fournisseur
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillLineRequest {

    private String productCode;

    @NotBlank(message = "La description est obligatoire")
    private String description;

    @NotNull(message = "La quantité est obligatoire")
    @Positive(message = "La quantité doit être positive")
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Builder.Default
    private String unit = "Unité";  // kg, m², unité, heure, etc.

    @NotNull(message = "Le prix unitaire est obligatoire")
    @Positive(message = "Le prix unitaire doit être positif")
    private BigDecimal unitPrice;

    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal vatRate = new BigDecimal("19.25");  // Taux TVA Cameroun par défaut

    private String accountNumber;  // Compte d'achat (601, 602, 622, 625...)
}
