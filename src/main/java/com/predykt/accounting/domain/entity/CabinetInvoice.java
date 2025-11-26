package com.predykt.accounting.domain.entity;

import com.predykt.accounting.domain.enums.InvoiceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entité représentant une facture d'un cabinet comptable
 * Utilisée uniquement en MODE CABINET
 */
@Entity
@Table(name = "cabinet_invoices", indexes = {
    @Index(name = "idx_cabinet_invoices_cabinet", columnList = "cabinet_id"),
    @Index(name = "idx_cabinet_invoices_status", columnList = "status"),
    @Index(name = "idx_cabinet_invoices_date", columnList = "invoice_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CabinetInvoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cabinet_id", nullable = false)
    private Cabinet cabinet;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    // Montants
    @Column(name = "amount_ht", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountHt;

    @Column(name = "vat_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "amount_ttc", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountTtc;

    // Statut
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    // Description
    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Marque la facture comme payée
     */
    public void markAsPaid(String paymentMethod) {
        this.status = InvoiceStatus.PAID;
        this.paidAt = LocalDateTime.now();
        this.paymentMethod = paymentMethod;
    }

    /**
     * Annule la facture
     */
    public void cancel() {
        this.status = InvoiceStatus.CANCELLED;
    }

    /**
     * Vérifie si la facture est en retard
     */
    public boolean isOverdue() {
        return status == InvoiceStatus.PENDING &&
               dueDate != null &&
               LocalDate.now().isAfter(dueDate);
    }

    /**
     * Calcule le montant TTC à partir du HT et de la TVA
     */
    public void calculateTtc() {
        if (amountHt != null && vatAmount != null) {
            this.amountTtc = amountHt.add(vatAmount);
        }
    }

    /**
     * Calcule la TVA à partir du HT et d'un taux
     */
    public void calculateVat(BigDecimal vatRate) {
        if (amountHt != null && vatRate != null) {
            this.vatAmount = amountHt.multiply(vatRate).divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
            calculateTtc();
        }
    }
}
