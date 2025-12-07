package com.predykt.accounting.dto.response;

import com.predykt.accounting.domain.enums.BillStatus;
import com.predykt.accounting.domain.enums.BillType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO de réponse pour une facture fournisseur
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillResponse {

    private Long id;
    private Long companyId;
    private Long supplierId;
    private String supplierName;
    private String supplierNiu;

    // Numérotation
    private String billNumber;
    private String supplierInvoiceNumber;
    private BillType billType;

    // Dates
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate paymentDate;

    // Montants
    private BigDecimal totalHt;
    private BigDecimal vatDeductible;
    private BigDecimal airAmount;
    private BigDecimal irppRentAmount;
    private BigDecimal totalTtc;

    // Paiement
    private BigDecimal amountPaid;
    private BigDecimal amountDue;

    // Statut
    private BillStatus status;
    private Boolean isReconciled;
    private LocalDate reconciliationDate;

    // Références
    private String referenceNumber;
    private String description;
    private String notes;

    // Fiscalité
    private Boolean supplierHasNiu;
    private BigDecimal airRate;

    // Lignes de facture
    @Builder.Default
    private List<BillLineResponse> lines = new ArrayList<>();

    // Métadonnées
    private java.time.LocalDateTime createdAt;
    private String createdBy;
    private java.time.LocalDateTime updatedAt;
    private String updatedBy;

    // Informations calculées
    private Integer daysOverdue;  // Nombre de jours de retard
    private String agingCategory;  // Catégorie balance âgée

    // Lien comptable
    private Long generalLedgerId;  // ID de l'écriture comptable générée
}
