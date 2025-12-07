package com.predykt.accounting.dto.response;

import com.predykt.accounting.domain.enums.InvoiceStatus;
import com.predykt.accounting.domain.enums.InvoiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO de réponse pour une facture client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private Long id;
    private Long companyId;
    private String companyName;

    // Client
    private Long customerId;
    private String customerName;
    private String customerNiu;
    private Boolean customerHasNiu;

    // Numérotation
    private String invoiceNumber;
    private InvoiceType invoiceType;

    // Dates
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate paymentDate;

    // Montants
    private BigDecimal totalHt;
    private BigDecimal vatAmount;
    private BigDecimal totalTtc;
    private BigDecimal amountPaid;
    private BigDecimal amountDue;

    // Statut
    private InvoiceStatus status;
    private Boolean isReconciled;
    private LocalDate reconciliationDate;

    // Références
    private String referenceNumber;
    private String description;
    private String notes;
    private String paymentTerms;

    // TVA
    private Boolean isVatExempt;
    private String vatExemptionReason;

    // Lignes
    @Builder.Default
    private List<InvoiceLineResponse> lines = new ArrayList<>();

    // Métriques calculées
    private Integer daysOverdue;
    private Boolean isOverdue;
    private String agingCategory;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
