package com.predykt.accounting.domain.entity;

import com.predykt.accounting.domain.enums.PendingItemType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Opération en suspens dans un rapprochement bancaire
 * Détaille chaque écart entre le relevé bancaire et le livre
 */
@Entity
@Table(name = "bank_reconciliation_items", indexes = {
    @Index(name = "idx_recon_item_reconciliation", columnList = "reconciliation_id"),
    @Index(name = "idx_recon_item_type", columnList = "item_type"),
    @Index(name = "idx_recon_item_date", columnList = "transaction_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankReconciliationItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Le rapprochement parent est obligatoire")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_id", nullable = false)
    private BankReconciliation bankReconciliation;

    @NotNull(message = "Le type d'opération est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 50)
    private PendingItemType itemType;

    @NotNull(message = "La date de transaction est obligatoire")
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @NotNull(message = "Le montant est obligatoire")
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Référence à la transaction bancaire d'origine si applicable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_transaction_id")
    private BankTransaction bankTransaction;

    // Référence à l'écriture comptable si applicable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_entry_id")
    private GeneralLedger glEntry;

    // Référence externe (ex: numéro de chèque)
    @Column(name = "reference", length = 100)
    private String reference;

    // Tiers concerné (client, fournisseur)
    @Column(name = "third_party", length = 200)
    private String thirdParty;

    // Indique si cet item a été résolu/régularisé
    @Column(name = "is_resolved")
    @Builder.Default
    private Boolean isResolved = false;

    @Column(name = "resolved_date")
    private LocalDate resolvedDate;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    /**
     * Retourne true si cet item affecte le solde bancaire
     */
    public boolean affectsBankBalance() {
        return itemType.affectsBankBalance();
    }

    /**
     * Retourne true si cet item affecte le solde livre
     */
    public boolean affectsBookBalance() {
        return itemType.affectsBookBalance();
    }

    /**
     * Retourne le montant signé selon le type d'opération
     */
    public BigDecimal getSignedAmount() {
        return itemType.isAddition() ? amount : amount.negate();
    }

    /**
     * Marque cet item comme résolu
     */
    public void resolve(String notes) {
        this.isResolved = true;
        this.resolvedDate = LocalDate.now();
        this.resolutionNotes = notes;
    }
}
