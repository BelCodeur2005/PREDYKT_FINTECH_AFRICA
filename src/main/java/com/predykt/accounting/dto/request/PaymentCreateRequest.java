package com.predykt.accounting.dto.request;

import com.predykt.accounting.domain.enums.PaymentMethod;
import com.predykt.accounting.domain.enums.PaymentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO pour créer un paiement
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateRequest {

    @NotNull(message = "Le type de paiement est obligatoire")
    private PaymentType paymentType;  // CUSTOMER_PAYMENT ou SUPPLIER_PAYMENT

    private Long invoiceId;  // Si paiement facture client

    private Long billId;  // Si paiement facture fournisseur

    @NotNull(message = "La date de paiement est obligatoire")
    private LocalDate paymentDate;

    @NotNull(message = "Le montant est obligatoire")
    @Positive(message = "Le montant doit être positif")
    private BigDecimal amount;

    @NotNull(message = "Le moyen de paiement est obligatoire")
    private PaymentMethod paymentMethod;

    // Détails selon le moyen
    private String chequeNumber;
    private String mobileMoneyNumber;
    private String transactionReference;

    private String description;
    private String notes;
}
