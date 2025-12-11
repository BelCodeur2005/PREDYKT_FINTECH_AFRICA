package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO pour la réponse contenant les informations d'un acompte (Deposit).
 *
 * Retourné par les endpoints:
 * - GET /deposits/{id}
 * - GET /deposits
 * - POST /deposits
 * - PUT /deposits/{id}
 *
 * @author PREDYKT System Optimizer
 * @since Phase 3 - Conformité OHADA Avancée
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositResponse {

    // Identification
    private Long id;
    private String depositNumber;  // Ex: RA-2025-000001
    private LocalDate depositDate;

    // Relations
    private Long companyId;
    private String companyName;

    private Long customerId;
    private String customerName;

    private Long invoiceId;
    private String invoiceNumber;

    private Long paymentId;
    private String paymentNumber;

    // Montants OHADA
    private BigDecimal amountHt;
    private BigDecimal vatRate;
    private BigDecimal vatAmount;
    private BigDecimal amountTtc;

    // État
    private Boolean isApplied;
    private LocalDateTime appliedAt;
    private String appliedBy;

    // Documentation
    private String description;
    private String customerOrderReference;
    private String depositReceiptUrl;
    private String notes;

    // Audit trail
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // Informations calculées
    private BigDecimal availableAmount;  // Montant disponible pour imputation
    private Boolean canBeApplied;  // Peut être imputé ?
}
