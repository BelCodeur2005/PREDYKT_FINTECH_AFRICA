package com.predykt.accounting.domain.entity;

import com.predykt.accounting.domain.enums.BillStatus;
import com.predykt.accounting.domain.enums.BillType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité représentant une facture fournisseur (Bill)
 * Conforme OHADA et réglementation camerounaise (AIR, IRPP Loyer)
 *
 * Fonctionnalités:
 * - Facturation fournisseur avec lignes de détail
 * - Calcul automatique AIR (2.2% si NIU, 5.5% sinon)
 * - Calcul automatique IRPP Loyer 15% (si fournisseur = loueur)
 * - Gestion des échéances et retards de paiement
 * - Lettrage automatique avec paiements
 */
@Entity
@Table(name = "bills", indexes = {
    @Index(name = "idx_bills_company", columnList = "company_id"),
    @Index(name = "idx_bills_supplier", columnList = "supplier_id"),
    @Index(name = "idx_bills_status", columnList = "status"),
    @Index(name = "idx_bills_issue_date", columnList = "issue_date"),
    @Index(name = "idx_bills_due_date", columnList = "due_date"),
    @Index(name = "idx_bills_reconciled", columnList = "is_reconciled")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_bill_number_company", columnNames = {"company_id", "bill_number"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bill extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @NotNull(message = "L'entreprise est obligatoire")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    @NotNull(message = "Le fournisseur est obligatoire")
    private Supplier supplier;

    // ==================== Numérotation ====================

    @NotBlank(message = "Le numéro de facture est obligatoire")
    @Column(name = "bill_number", nullable = false, length = 50)
    private String billNumber;  // FA-2025-0001

    @Column(name = "supplier_invoice_number", length = 100)
    private String supplierInvoiceNumber;  // Numéro de facture du fournisseur

    @Enumerated(EnumType.STRING)
    @Column(name = "bill_type", nullable = false, length = 30)
    @Builder.Default
    private BillType billType = BillType.PURCHASE;

    // ==================== Dates ====================

    @NotNull(message = "La date d'émission est obligatoire")
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @NotNull(message = "La date d'échéance est obligatoire")
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    // ==================== Montants ====================

    @NotNull
    @Positive
    @Column(name = "total_ht", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalHt = BigDecimal.ZERO;

    @Column(name = "vat_deductible", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal vatDeductible = BigDecimal.ZERO;  // TVA déductible

    @Column(name = "air_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal airAmount = BigDecimal.ZERO;  // AIR (précompte) 2.2% ou 5.5%

    @Column(name = "irpp_rent_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal irppRentAmount = BigDecimal.ZERO;  // IRPP Loyer 15%

    @NotNull
    @Positive
    @Column(name = "total_ttc", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalTtc = BigDecimal.ZERO;

    // ==================== Paiement ====================

    @Column(name = "amount_paid", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "amount_due", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal amountDue = BigDecimal.ZERO;

    // ==================== Statut ====================

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BillStatus status = BillStatus.DRAFT;

    @Column(name = "is_reconciled", nullable = false)
    @Builder.Default
    private Boolean isReconciled = false;

    @Column(name = "reconciliation_date")
    private LocalDate reconciliationDate;

    // ==================== Références ====================

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String notes;  // Notes visibles (sur la facture fournisseur)

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;  // ✅ NOUVEAU: Notes comptables internes (NON visibles fournisseur)

    // ==================== NIU du fournisseur (copie au moment de la facture) ====================

    @Column(name = "supplier_niu", length = 50)
    private String supplierNiu;

    @Column(name = "supplier_has_niu", nullable = false)
    @Builder.Default
    private Boolean supplierHasNiu = false;

    @Column(name = "air_rate", precision = 5, scale = 2)
    private BigDecimal airRate;  // Taux AIR appliqué (2.2% ou 5.5%)

    // ==================== Relations ====================

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BillLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "bill")
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "general_ledger_id")
    private GeneralLedger generalLedger;

    // ==================== Méthodes métier ====================

    /**
     * Calcule le nombre de jours de retard
     */
    public int getDaysOverdue() {
        if (this.dueDate == null || this.status == BillStatus.PAID || this.status == BillStatus.CANCELLED) {
            return 0;
        }

        LocalDate now = LocalDate.now();
        if (now.isAfter(this.dueDate)) {
            return (int) java.time.temporal.ChronoUnit.DAYS.between(this.dueDate, now);
        }
        return 0;
    }

    /**
     * Vérifie si la facture est en retard
     */
    public boolean isOverdue() {
        return getDaysOverdue() > 0;
    }

    /**
     * Retourne la catégorie de balance âgée
     */
    public String getAgingCategory() {
        int days = getDaysOverdue();
        if (days == 0) return "Non échu";
        if (days <= 30) return "0-30 jours";
        if (days <= 60) return "30-60 jours";
        if (days <= 90) return "60-90 jours";
        return "+90 jours";
    }

    /**
     * Ajoute une ligne de facture
     */
    public void addLine(BillLine line) {
        lines.add(line);
        line.setBill(this);
    }

    /**
     * Supprime une ligne de facture
     */
    public void removeLine(BillLine line) {
        lines.remove(line);
        line.setBill(null);
    }

    /**
     * Recalcule les totaux à partir des lignes
     */
    public void calculateTotals() {
        this.totalHt = lines.stream()
            .map(BillLine::getTotalHt)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.vatDeductible = lines.stream()
            .map(BillLine::getVatAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalTtc = lines.stream()
            .map(BillLine::getTotalTtc)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculer AIR (si applicable)
        calculateAir();

        // Calculer IRPP Loyer (si fournisseur = loueur)
        calculateIrppRent();

        // Recalculer le montant dû
        this.amountDue = this.totalTtc.subtract(this.amountPaid);
    }

    /**
     * Calcule l'AIR (Acompte sur Impôt sur le Revenu)
     * - 2.2% si fournisseur a un NIU
     * - 5.5% si fournisseur n'a pas de NIU
     */
    private void calculateAir() {
        if (this.supplierHasNiu != null && this.totalHt != null) {
            if (this.supplierHasNiu) {
                this.airRate = new BigDecimal("2.2");
            } else {
                this.airRate = new BigDecimal("5.5");
            }
            this.airAmount = this.totalHt
                .multiply(this.airRate)
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        } else {
            this.airAmount = BigDecimal.ZERO;
        }
    }

    /**
     * Calcule l'IRPP Loyer (15% si fournisseur = loueur)
     */
    private void calculateIrppRent() {
        if (this.billType == BillType.RENT && this.totalHt != null) {
            this.irppRentAmount = this.totalHt
                .multiply(new BigDecimal("15"))
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        } else {
            this.irppRentAmount = BigDecimal.ZERO;
        }
    }

    /**
     * Enregistre un paiement
     */
    public void recordPayment(BigDecimal amount) {
        this.amountPaid = this.amountPaid.add(amount);
        this.amountDue = this.totalTtc.subtract(this.amountPaid);

        // Mettre à jour le statut
        if (this.amountPaid.compareTo(this.totalTtc) >= 0) {
            this.status = BillStatus.PAID;
            this.paymentDate = LocalDate.now();
        } else if (this.amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            this.status = BillStatus.PARTIAL_PAID;
        }
    }

    /**
     * Vérifie si la facture peut être modifiée
     */
    public boolean isEditable() {
        return this.status == BillStatus.DRAFT;
    }

    /**
     * Vérifie si la facture peut être annulée
     */
    public boolean isCancellable() {
        return this.status != BillStatus.PAID && this.amountPaid.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Copie le NIU du fournisseur au moment de la création de la facture
     */
    @PrePersist
    @PreUpdate
    private void copySupplierNiu() {
        if (this.supplier != null) {
            this.supplierNiu = this.supplier.getNiuNumber();
            this.supplierHasNiu = this.supplier.getHasNiu();
        }
    }
}
