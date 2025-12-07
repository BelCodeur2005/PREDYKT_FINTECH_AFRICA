package com.predykt.accounting.domain.entity;

import com.predykt.accounting.domain.enums.PaymentMethod;
import com.predykt.accounting.domain.enums.PaymentStatus;
import com.predykt.accounting.domain.enums.PaymentType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entité représentant un paiement (Payment)
 * Gère les paiements clients (encaissements) et fournisseurs (décaissements)
 * Système de lettrage automatique avec Invoice/Bill
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payments_company", columnList = "company_id"),
    @Index(name = "idx_payments_invoice", columnList = "invoice_id"),
    @Index(name = "idx_payments_bill", columnList = "bill_id"),
    @Index(name = "idx_payments_customer", columnList = "customer_id"),
    @Index(name = "idx_payments_supplier", columnList = "supplier_id"),
    @Index(name = "idx_payments_date", columnList = "payment_date"),
    @Index(name = "idx_payments_status", columnList = "status"),
    @Index(name = "idx_payments_reconciled", columnList = "is_reconciled")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_payment_number_company", columnNames = {"company_id", "payment_number"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @NotNull(message = "L'entreprise est obligatoire")
    private Company company;

    // ==================== Numérotation ====================

    @NotBlank(message = "Le numéro de paiement est obligatoire")
    @Column(name = "payment_number", nullable = false, length = 50)
    private String paymentNumber;  // PAY-2025-0001

    // ==================== Type de paiement ====================

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    @NotNull(message = "Le type de paiement est obligatoire")
    private PaymentType paymentType;

    // ==================== Lien avec facture ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;  // Si paiement d'une facture client

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id")
    private Bill bill;  // Si paiement d'une facture fournisseur

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;  // Client payeur

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;  // Fournisseur bénéficiaire

    // ==================== Dates et montants ====================

    @NotNull(message = "La date de paiement est obligatoire")
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @NotNull
    @Positive(message = "Le montant doit être positif")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    // ==================== Moyen de paiement ====================

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    @NotNull(message = "Le moyen de paiement est obligatoire")
    private PaymentMethod paymentMethod;

    // ==================== Détails selon le moyen de paiement ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;  // Si virement bancaire

    @Column(name = "cheque_number", length = 50)
    private String chequeNumber;  // Si chèque

    @Column(name = "mobile_money_number", length = 20)
    private String mobileMobilMoneyNumber;  // Si Mobile Money (MTN, Orange)

    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;  // Référence transaction

    // ==================== Statut ====================

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    // ==================== Lettrage ====================

    @Column(name = "is_reconciled", nullable = false)
    @Builder.Default
    private Boolean isReconciled = false;

    @Column(name = "reconciliation_date")
    private LocalDate reconciliationDate;

    @Column(name = "reconciled_by", length = 100)
    private String reconciledBy;

    // ==================== Lien comptable ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "general_ledger_id")
    private GeneralLedger generalLedger;  // Écriture comptable générée

    // ==================== Notes ====================

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // ==================== Méthodes métier ====================

    /**
     * Vérifie si le paiement est un encaissement client
     */
    public boolean isCustomerPayment() {
        return this.paymentType == PaymentType.CUSTOMER_PAYMENT;
    }

    /**
     * Vérifie si le paiement est un décaissement fournisseur
     */
    public boolean isSupplierPayment() {
        return this.paymentType == PaymentType.SUPPLIER_PAYMENT;
    }

    /**
     * Valide le paiement (change le statut à COMPLETED)
     */
    public void validate() {
        if (this.status == PaymentStatus.PENDING) {
            this.status = PaymentStatus.COMPLETED;
        }
    }

    /**
     * Annule le paiement
     */
    public void cancel() {
        if (this.status == PaymentStatus.PENDING || this.status == PaymentStatus.COMPLETED) {
            this.status = PaymentStatus.CANCELLED;
        }
    }

    /**
     * Marque le paiement comme rejeté (chèque sans provision, etc.)
     */
    public void markAsBounced() {
        this.status = PaymentStatus.BOUNCED;
    }

    /**
     * Lettrage du paiement avec sa facture
     */
    public void reconcile(String reconciledBy) {
        this.isReconciled = true;
        this.reconciliationDate = LocalDate.now();
        this.reconciledBy = reconciledBy;
    }

    /**
     * Annule le lettrage
     */
    public void unreconcile() {
        this.isReconciled = false;
        this.reconciliationDate = null;
        this.reconciledBy = null;
    }

    /**
     * Validation métier avant sauvegarde
     */
    @PrePersist
    @PreUpdate
    private void validatePayment() {
        // Vérifier qu'on a soit invoice SOIT bill (pas les deux)
        if (this.invoice != null && this.bill != null) {
            throw new IllegalStateException("Un paiement ne peut pas être lié à la fois à une facture client ET une facture fournisseur");
        }

        // Vérifier cohérence paymentType avec invoice/bill
        if (this.paymentType == PaymentType.CUSTOMER_PAYMENT && this.invoice == null) {
            throw new IllegalStateException("Un paiement client doit être lié à une facture client");
        }

        if (this.paymentType == PaymentType.SUPPLIER_PAYMENT && this.bill == null) {
            throw new IllegalStateException("Un paiement fournisseur doit être lié à une facture fournisseur");
        }

        // Remplir automatiquement customer ou supplier selon le type
        if (this.paymentType == PaymentType.CUSTOMER_PAYMENT && this.invoice != null && this.customer == null) {
            this.customer = this.invoice.getCustomer();
        }

        if (this.paymentType == PaymentType.SUPPLIER_PAYMENT && this.bill != null && this.supplier == null) {
            this.supplier = this.bill.getSupplier();
        }
    }
}
