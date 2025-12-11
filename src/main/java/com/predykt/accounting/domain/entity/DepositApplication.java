package com.predykt.accounting.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Entité représentant une imputation (partielle ou totale) d'un acompte sur une facture.
 *
 * Cette entité permet de gérer les imputations partielles introduites en Phase 2.
 * Un acompte peut désormais être fractionné sur plusieurs factures.
 *
 * Exemple:
 * - Acompte de 300 000 XAF reçu
 * - Application 1: 100 000 XAF sur facture FV-001
 * - Application 2: 200 000 XAF sur facture FV-002
 *
 * Conformité OHADA:
 * - Chaque imputation génère une écriture comptable distincte
 * - DÉBIT 4191 + CRÉDIT 411 pour le montant imputé
 *
 * @author PREDYKT Accounting Team
 * @version 2.0 (Phase 2 - Imputation Partielle)
 * @since 2025-12-11
 */
@Entity
@Table(name = "deposit_applications",
       indexes = {
           @Index(name = "idx_deposit_applications_deposit", columnList = "deposit_id"),
           @Index(name = "idx_deposit_applications_invoice", columnList = "invoice_id"),
           @Index(name = "idx_deposit_applications_company", columnList = "company_id"),
           @Index(name = "idx_deposit_applications_applied_at", columnList = "applied_at"),
           @Index(name = "idx_deposit_applications_company_deposit", columnList = "company_id, deposit_id"),
           @Index(name = "idx_deposit_applications_company_invoice", columnList = "company_id, invoice_id")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"deposit", "invoice", "company"})
public class DepositApplication {

    // =====================================================================
    // Champs de base
    // =====================================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =====================================================================
    // Relations
    // =====================================================================

    /**
     * Acompte source de cette imputation
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_id", nullable = false)
    @NotNull(message = "L'acompte est obligatoire")
    private Deposit deposit;

    /**
     * Facture sur laquelle l'acompte est imputé
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @NotNull(message = "La facture est obligatoire")
    private Invoice invoice;

    /**
     * Entreprise (multi-tenant)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @NotNull(message = "L'entreprise est obligatoire")
    private Company company;

    // =====================================================================
    // Montants de l'imputation
    // =====================================================================

    /**
     * Montant HT de cette imputation partielle
     * Peut être inférieur au montant total de l'acompte si imputation partielle
     */
    @Column(name = "amount_ht", nullable = false, precision = 15, scale = 2)
    @NotNull(message = "Le montant HT est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant HT doit être positif")
    private BigDecimal amountHt;

    /**
     * Taux de TVA appliqué (en %)
     * Doit correspondre au taux TVA de l'acompte source
     */
    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    @NotNull(message = "Le taux de TVA est obligatoire")
    @Builder.Default
    private BigDecimal vatRate = new BigDecimal("19.25"); // Cameroun

    /**
     * Montant de TVA calculé automatiquement
     */
    @Column(name = "vat_amount", nullable = false, precision = 15, scale = 2)
    @NotNull(message = "Le montant de TVA est obligatoire")
    @Builder.Default
    private BigDecimal vatAmount = BigDecimal.ZERO;

    /**
     * Montant TTC de cette imputation (HT + TVA)
     */
    @Column(name = "amount_ttc", nullable = false, precision = 15, scale = 2)
    @NotNull(message = "Le montant TTC est obligatoire")
    @Builder.Default
    private BigDecimal amountTtc = BigDecimal.ZERO;

    // =====================================================================
    // Informations d'imputation
    // =====================================================================

    /**
     * Date/heure de l'imputation
     */
    @Column(name = "applied_at", nullable = false)
    @NotNull(message = "La date d'imputation est obligatoire")
    @Builder.Default
    private LocalDateTime appliedAt = LocalDateTime.now();

    /**
     * Utilisateur ayant effectué l'imputation
     */
    @Column(name = "applied_by", nullable = false)
    @NotNull(message = "L'utilisateur est obligatoire")
    private String appliedBy;

    /**
     * ID de l'écriture comptable générée lors de l'imputation
     * Permet de retrouver l'écriture DÉBIT 4191 / CRÉDIT 411
     */
    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    /**
     * Notes et description de l'imputation
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // =====================================================================
    // Audit trail
    // =====================================================================

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    // =====================================================================
    // Lifecycle callbacks
    // =====================================================================

    /**
     * Calcule automatiquement les montants TVA et TTC avant insertion/mise à jour
     */
    @PrePersist
    @PreUpdate
    public void calculateAmounts() {
        if (this.amountHt != null && this.vatRate != null) {
            // Calcul TVA = HT * (taux / 100)
            this.vatAmount = this.amountHt
                .multiply(this.vatRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            // Calcul TTC = HT + TVA
            this.amountTtc = this.amountHt.add(this.vatAmount);
        }
    }

    /**
     * Initialise les dates d'audit avant insertion
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.appliedAt = LocalDateTime.now();
    }

    /**
     * Met à jour la date de modification avant mise à jour
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // =====================================================================
    // Méthodes métier
    // =====================================================================

    /**
     * Vérifie que les montants sont cohérents (HT + TVA = TTC)
     * Tolérance de 0.01 XAF pour les arrondis
     */
    public boolean isAmountValid() {
        if (this.amountHt == null || this.vatAmount == null || this.amountTtc == null) {
            return false;
        }

        BigDecimal expectedTtc = this.amountHt.add(this.vatAmount);
        BigDecimal difference = this.amountTtc.subtract(expectedTtc).abs();

        return difference.compareTo(new BigDecimal("0.01")) <= 0;
    }

    /**
     * Vérifie que cette imputation ne dépasse pas le montant disponible de l'acompte
     *
     * @return true si le montant est valide, false sinon
     */
    public boolean isWithinDepositLimit() {
        if (this.deposit == null || this.amountTtc == null) {
            return false;
        }

        BigDecimal availableAmount = this.deposit.getAvailableAmount();
        return this.amountTtc.compareTo(availableAmount) <= 0;
    }

    /**
     * Vérifie que cette imputation ne dépasse pas le montant restant dû sur la facture
     *
     * @return true si le montant est valide, false sinon
     */
    public boolean isWithinInvoiceLimit() {
        if (this.invoice == null || this.amountTtc == null) {
            return false;
        }

        BigDecimal remainingDue = this.invoice.getAmountDue();
        return this.amountTtc.compareTo(remainingDue) <= 0;
    }

    /**
     * Vérifie que le client de l'acompte correspond au client de la facture
     *
     * @return true si les clients correspondent, false sinon
     */
    public boolean hasMatchingCustomer() {
        if (this.deposit == null || this.invoice == null) {
            return false;
        }

        if (this.deposit.getCustomer() == null || this.invoice.getCustomer() == null) {
            return false;
        }

        return this.deposit.getCustomer().getId().equals(this.invoice.getCustomer().getId());
    }

    /**
     * Vérifie que le taux de TVA correspond à celui de l'acompte source
     *
     * @return true si les taux correspondent, false sinon
     */
    public boolean hasMatchingVatRate() {
        if (this.deposit == null || this.vatRate == null) {
            return false;
        }

        return this.vatRate.compareTo(this.deposit.getVatRate()) == 0;
    }

    /**
     * Valide complètement cette imputation avant persistance
     *
     * @throws IllegalStateException si l'imputation n'est pas valide
     */
    public void validate() {
        if (!isAmountValid()) {
            throw new IllegalStateException(
                String.format("Montants incohérents: HT=%s + TVA=%s != TTC=%s",
                    amountHt, vatAmount, amountTtc)
            );
        }

        if (!isWithinDepositLimit()) {
            throw new IllegalStateException(
                String.format("Montant d'imputation (%s XAF) dépasse le montant disponible de l'acompte (%s XAF)",
                    amountTtc, deposit.getAvailableAmount())
            );
        }

        if (!isWithinInvoiceLimit()) {
            throw new IllegalStateException(
                String.format("Montant d'imputation (%s XAF) dépasse le montant restant dû sur la facture (%s XAF)",
                    amountTtc, invoice.getAmountDue())
            );
        }

        if (!hasMatchingCustomer()) {
            throw new IllegalStateException(
                String.format("Le client de l'acompte (%s) ne correspond pas au client de la facture (%s)",
                    deposit.getCustomer().getName(), invoice.getCustomer().getName())
            );
        }

        if (!hasMatchingVatRate()) {
            throw new IllegalStateException(
                String.format("Le taux de TVA de l'imputation (%s%%) ne correspond pas à celui de l'acompte (%s%%)",
                    vatRate, deposit.getVatRate())
            );
        }
    }

    /**
     * Retourne une description lisible de cette imputation
     *
     * @return Description formatée
     */
    public String getDescription() {
        return String.format("Imputation de %s XAF de l'acompte %s sur la facture %s",
            amountTtc,
            deposit != null ? deposit.getDepositNumber() : "N/A",
            invoice != null ? invoice.getInvoiceNumber() : "N/A"
        );
    }

    /**
     * Retourne le pourcentage de l'acompte total que représente cette imputation
     *
     * @return Pourcentage (0-100)
     */
    public BigDecimal getPercentageOfDeposit() {
        if (this.deposit == null || this.amountTtc == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal depositTotal = this.deposit.getAmountTtc();
        if (depositTotal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return this.amountTtc
            .multiply(new BigDecimal("100"))
            .divide(depositTotal, 2, RoundingMode.HALF_UP);
    }

    /**
     * Retourne le pourcentage de la facture que représente cette imputation
     *
     * @return Pourcentage (0-100)
     */
    public BigDecimal getPercentageOfInvoice() {
        if (this.invoice == null || this.amountTtc == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal invoiceTotal = this.invoice.getTotalTtc();
        if (invoiceTotal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return this.amountTtc
            .multiply(new BigDecimal("100"))
            .divide(invoiceTotal, 2, RoundingMode.HALF_UP);
    }
}
