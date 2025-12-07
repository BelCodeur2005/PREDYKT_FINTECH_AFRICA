package com.predykt.accounting.dto.response;

import com.predykt.accounting.domain.enums.PaymentMethod;
import com.predykt.accounting.domain.enums.PaymentStatus;
import com.predykt.accounting.domain.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour un paiement
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private Long companyId;
    private String paymentNumber;
    private PaymentType paymentType;

    // Facture liée
    private Long invoiceId;
    private String invoiceNumber;
    private Long billId;
    private String billNumber;

    // Client/Fournisseur
    private Long customerId;
    private String customerName;
    private Long supplierId;
    private String supplierName;

    // Montant
    private LocalDate paymentDate;
    private BigDecimal amount;

    // Moyen de paiement
    private PaymentMethod paymentMethod;
    private String chequeNumber;
    private String mobileMoneyNumber;
    private String transactionReference;

    // Statut
    private PaymentStatus status;
    private Boolean isReconciled;
    private LocalDate reconciliationDate;
    private String reconciledBy;

    private String description;
    private String notes;

    private LocalDateTime createdAt;
    private String createdBy;
}
