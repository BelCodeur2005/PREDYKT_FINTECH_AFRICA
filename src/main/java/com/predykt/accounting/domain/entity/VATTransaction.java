package com.predykt.accounting.domain.entity;

import com.predykt.accounting.domain.enums.VATAccountType;
import com.predykt.accounting.domain.enums.VATRecoverableCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Transaction de TVA avec gestion de la récupérabilité
 * Permet de gérer la TVA récupérable, partiellement récupérable et non récupérable
 *
 * CRUCIAL pour la conformité fiscale camerounaise :
 * - Véhicules de tourisme : TVA 0% récupérable
 * - Carburant VP : TVA 0% récupérable
 * - Carburant VU : TVA 80% récupérable
 * - Achats professionnels : TVA 100% récupérable
 */
@Entity
@Table(name = "vat_transactions", indexes = {
    @Index(name = "idx_vat_tx_company", columnList = "company_id"),
    @Index(name = "idx_vat_tx_date", columnList = "transaction_date"),
    @Index(name = "idx_vat_tx_category", columnList = "recoverable_category"),
    @Index(name = "idx_vat_tx_ledger", columnList = "ledger_entry_id")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VATTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /**
     * Lien vers l'écriture du grand livre
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ledger_entry_id")
    private GeneralLedger ledgerEntry;

    /**
     * Fournisseur (pour achats)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @NotNull(message = "La date de transaction est obligatoire")
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    /**
     * Type de compte TVA (4431, 4451, etc.)
     */
    @NotNull(message = "Le type de compte TVA est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(name = "vat_account_type", nullable = false, length = 50)
    private VATAccountType vatAccountType;

    /**
     * Type de transaction: SALE (vente) ou PURCHASE (achat)
     */
    @NotNull(message = "Le type de transaction est obligatoire")
    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType;  // SALE, PURCHASE

    /**
     * Montant hors taxes (base de calcul)
     */
    @NotNull(message = "Le montant HT est obligatoire")
    @Column(name = "amount_excluding_vat", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountExcludingVat;

    /**
     * Taux de TVA appliqué (généralement 19,25%)
     */
    @NotNull(message = "Le taux de TVA est obligatoire")
    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate;

    /**
     * Montant TOTAL de TVA (avant application du coefficient de récupérabilité)
     */
    @NotNull(message = "Le montant de TVA est obligatoire")
    @Column(name = "vat_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal vatAmount;

    /**
     * Catégorie de récupérabilité de la TVA
     * CRUCIAL pour déterminer si la TVA est déductible ou non
     */
    @NotNull(message = "La catégorie de récupérabilité est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(name = "recoverable_category", nullable = false, length = 50)
    @Builder.Default
    private VATRecoverableCategory recoverableCategory = VATRecoverableCategory.FULLY_RECOVERABLE;

    /**
     * Pourcentage de TVA récupérable (0%, 80%, 100%)
     */
    @NotNull(message = "Le pourcentage récupérable est obligatoire")
    @Column(name = "recoverable_percentage", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal recoverablePercentage = BigDecimal.valueOf(100.0);

    /**
     * Montant de TVA RÉCUPÉRABLE (déductible)
     * = vatAmount × recoverablePercentage / 100
     */
    @Column(name = "recoverable_vat_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal recoverableVatAmount = BigDecimal.ZERO;

    /**
     * Montant de TVA NON RÉCUPÉRABLE (à passer en charge)
     * = vatAmount - recoverableVatAmount
     */
    @Column(name = "non_recoverable_vat_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal nonRecoverableVatAmount = BigDecimal.ZERO;

    /**
     * Description de la transaction
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Justification pour la TVA non récupérable
     */
    @Column(name = "non_recoverable_justification", columnDefinition = "TEXT")
    private String nonRecoverableJustification;

    /**
     * Référence de la facture
     */
    @Column(name = "invoice_reference", length = 100)
    private String invoiceReference;

    /**
     * Indicateur d'alerte (ex: TVA non récupérable)
     */
    @Column(name = "has_alert")
    @Builder.Default
    private Boolean hasAlert = false;

    /**
     * Message d'alerte
     */
    @Column(name = "alert_message", columnDefinition = "TEXT")
    private String alertMessage;

    /**
     * Calcule le montant de TVA récupérable
     */
    public void calculateRecoverableAmounts() {
        if (vatAmount == null || recoverablePercentage == null) {
            this.recoverableVatAmount = BigDecimal.ZERO;
            this.nonRecoverableVatAmount = BigDecimal.ZERO;
            return;
        }

        // Montant récupérable = TVA × pourcentage
        this.recoverableVatAmount = vatAmount
            .multiply(recoverablePercentage)
            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

        // Montant non récupérable = TVA - récupérable
        this.nonRecoverableVatAmount = vatAmount.subtract(recoverableVatAmount);

        // Générer une alerte si TVA non récupérable
        if (nonRecoverableVatAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.hasAlert = true;
            this.alertMessage = String.format(
                "⚠️ TVA non récupérable: %s XAF (%s) - %s",
                nonRecoverableVatAmount,
                recoverableCategory.getDisplayName(),
                recoverableCategory.getDescription()
            );
        }
    }

    /**
     * Vérifie si la TVA est totalement récupérable
     */
    public boolean isFullyRecoverable() {
        return recoverableCategory != null && recoverableCategory.isFullyRecoverable();
    }

    /**
     * Vérifie si la TVA est partiellement récupérable
     */
    public boolean isPartiallyRecoverable() {
        return recoverableCategory != null && recoverableCategory.isPartiallyRecoverable();
    }

    /**
     * Vérifie si la TVA est non récupérable
     */
    public boolean isNonRecoverable() {
        return recoverableCategory != null && recoverableCategory.isNonRecoverable();
    }

    /**
     * Initialise les valeurs par défaut et calcule les montants
     */
    @PrePersist
    @PreUpdate
    private void initializeDefaults() {
        // Si pas de catégorie définie, considérer comme totalement récupérable
        if (recoverableCategory == null) {
            recoverableCategory = VATRecoverableCategory.FULLY_RECOVERABLE;
        }

        // Synchroniser le pourcentage avec la catégorie
        if (recoverablePercentage == null || recoverablePercentage.compareTo(BigDecimal.ZERO) == 0) {
            recoverablePercentage = BigDecimal.valueOf(
                recoverableCategory.getRecoverablePercentage()
            );
        }

        // Calculer les montants récupérables
        calculateRecoverableAmounts();
    }
}
