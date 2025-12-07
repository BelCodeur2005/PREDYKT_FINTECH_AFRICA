package com.predykt.accounting.domain.entity;

import com.predykt.accounting.domain.enums.InvoiceStatus;
import com.predykt.accounting.domain.enums.InvoiceType;
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
 * Entité représentant une facture client (Invoice)
 * Conforme OHADA et réglementation camerounaise (TVA 19.25%)
 *
 * Fonctionnalités:
 * - Facturation complète avec lignes de détail
 * - Gestion des échéances et retards de paiement
 * - Lettrage automatique avec paiements
 * - Balance âgée
 * - Exonérations TVA (Export, zones franches)
 */
@Entity
@Table(name = "invoices", indexes = {
    @Index(name = "idx_invoices_company", columnList = "company_id"),
    @Index(name = "idx_invoices_customer", columnList = "customer_id"),
    @Index(name = "idx_invoices_status", columnList = "status"),
    @Index(name = "idx_invoices_issue_date", columnList = "issue_date"),
    @Index(name = "idx_invoices_due_date", columnList = "due_date"),
    @Index(name = "idx_invoices_reconciled", columnList = "is_reconciled")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_invoice_number_company", columnNames = {"company_id", "invoice_number"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @NotNull(message = "L'entreprise est obligatoire")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @NotNull(message = "Le client est obligatoire")
    private Customer customer;

    // ==================== Numérotation ====================

    @NotBlank(message = "Le numéro de facture est obligatoire")
    @Column(name = "invoice_number", nullable = false, length = 50)
    private String invoiceNumber;  // FV-2025-0001

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, length = 30)
    @Builder.Default
    private InvoiceType invoiceType = InvoiceType.STANDARD;

    // ==================== Dates ====================

    @NotNull(message = "La date d'émission est obligatoire")
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @NotNull(message = "La date d'échéance est obligatoire")
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;  // Date de paiement effectif

    // ==================== Montants ====================

    @NotNull
    @Positive
    @Column(name = "total_ht", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalHt = BigDecimal.ZERO;  // Hors Taxes

    @NotNull
    @Column(name = "vat_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal vatAmount = BigDecimal.ZERO;  // Montant TVA 19.25%

    @NotNull
    @Positive
    @Column(name = "total_ttc", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalTtc = BigDecimal.ZERO;  // Toutes Taxes Comprises

    // ==================== Paiement ====================

    @Column(name = "amount_paid", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;  // Montant déjà payé

    @Column(name = "amount_due", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal amountDue = BigDecimal.ZERO;  // Montant restant dû

    // ==================== Statut ====================

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "is_reconciled", nullable = false)
    @Builder.Default
    private Boolean isReconciled = false;  // Lettrée ?

    @Column(name = "reconciliation_date")
    private LocalDate reconciliationDate;

    // ==================== Références ====================

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;  // Numéro de référence client (bon de commande, etc.)

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String notes;  // Notes visibles par le client (sur la facture)

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;  // ✅ NOUVEAU: Notes comptables internes (NON visibles client)

    @Column(name = "payment_terms", length = 255)
    private String paymentTerms;  // Conditions de paiement

    // ==================== TVA spécifique Cameroun ====================

    @Column(name = "is_vat_exempt", nullable = false)
    @Builder.Default
    private Boolean isVatExempt = false;  // Exonération TVA (Export, etc.)

    @Column(name = "vat_exemption_reason", length = 255)
    private String vatExemptionReason;  // Raison de l'exonération

    // ==================== NIU du client (copie au moment de la facture) ====================

    @Column(name = "customer_niu", length = 50)
    private String customerNiu;

    @Column(name = "customer_has_niu", nullable = false)
    @Builder.Default
    private Boolean customerHasNiu = false;

    // ==================== Relations ====================

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "invoice")
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "general_ledger_id")
    private GeneralLedger generalLedger;  // Écriture comptable générée

    // ==================== Méthodes métier ====================

    /**
     * Calcule le nombre de jours de retard
     */
    public int getDaysOverdue() {
        if (this.dueDate == null || this.status == InvoiceStatus.PAID || this.status == InvoiceStatus.CANCELLED) {
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
        return "+90 jours (créance douteuse)";
    }

    /**
     * Ajoute une ligne de facture
     */
    public void addLine(InvoiceLine line) {
        lines.add(line);
        line.setInvoice(this);
    }

    /**
     * Supprime une ligne de facture
     */
    public void removeLine(InvoiceLine line) {
        lines.remove(line);
        line.setInvoice(null);
    }

    /**
     * Recalcule les totaux à partir des lignes
     */
    public void calculateTotals() {
        this.totalHt = lines.stream()
            .map(InvoiceLine::getTotalHt)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.vatAmount = lines.stream()
            .map(InvoiceLine::getVatAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalTtc = lines.stream()
            .map(InvoiceLine::getTotalTtc)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Recalculer le montant dû
        this.amountDue = this.totalTtc.subtract(this.amountPaid);
    }

    /**
     * Enregistre un paiement
     */
    public void recordPayment(BigDecimal amount) {
        this.amountPaid = this.amountPaid.add(amount);
        this.amountDue = this.totalTtc.subtract(this.amountPaid);

        // Mettre à jour le statut
        if (this.amountPaid.compareTo(this.totalTtc) >= 0) {
            this.status = InvoiceStatus.PAID;
            this.paymentDate = LocalDate.now();
        } else if (this.amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            this.status = InvoiceStatus.PARTIAL_PAID;
        }
    }

    /**
     * Vérifie si la facture peut être modifiée
     */
    public boolean isEditable() {
        return this.status == InvoiceStatus.DRAFT;
    }

    /**
     * Vérifie si la facture peut être annulée
     */
    public boolean isCancellable() {
        return this.status != InvoiceStatus.PAID && this.amountPaid.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Copie le NIU du client au moment de la création de la facture
     */
    @PrePersist
    @PreUpdate
    private void copyCustomerNiu() {
        if (this.customer != null) {
            this.customerNiu = this.customer.getNiuNumber();
            this.customerHasNiu = this.customer.getHasNiu();
        }
    }
}
