package com.predykt.accounting.dto;

import com.predykt.accounting.domain.enums.InvoiceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO pour CabinetInvoice (MODE CABINET)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CabinetInvoiceDTO {

    private Long id;

    @NotNull(message = "L'ID du cabinet est obligatoire")
    private Long cabinetId;

    private String cabinetName;

    @Size(max = 50, message = "Le numéro de facture ne peut pas dépasser 50 caractères")
    private String invoiceNumber;

    @NotNull(message = "La date de facture est obligatoire")
    private LocalDate invoiceDate;

    @NotNull(message = "La date d'échéance est obligatoire")
    private LocalDate dueDate;

    @NotNull(message = "Le montant HT est obligatoire")
    @Positive(message = "Le montant HT doit être positif")
    private BigDecimal amountHt;

    private BigDecimal vatAmount;

    private BigDecimal amountTtc;

    private InvoiceStatus status;

    private LocalDateTime paidAt;

    @Size(max = 50, message = "Le mode de paiement ne peut pas dépasser 50 caractères")
    private String paymentMethod;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    @Size(max = 5000, message = "La description ne peut pas dépasser 5000 caractères")
    private String description;

    @Size(max = 5000, message = "Les notes ne peuvent pas dépasser 5000 caractères")
    private String notes;

    private Boolean isOverdue;

    private Long daysUntilDue;

    private Long daysOverdue;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
