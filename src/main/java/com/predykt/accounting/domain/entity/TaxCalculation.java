package com.predykt.accounting.domain.entity;

import com.predykt.accounting.domain.enums.TaxType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Historique des calculs fiscaux automatiques
 * Trace tous les calculs de taxes avec alertes éventuelles
 */
@Entity
@Table(name = "tax_calculations", indexes = {
    @Index(name = "idx_tax_calc_company", columnList = "company_id"),
    @Index(name = "idx_tax_calc_date", columnList = "calculation_date"),
    @Index(name = "idx_tax_calc_type", columnList = "tax_type"),
    @Index(name = "idx_tax_calc_status", columnList = "status"),
    @Index(name = "idx_tax_calc_alert", columnList = "has_alert")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /**
     * Transaction du grand livre associée
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private GeneralLedger transaction;

    /**
     * Facture client associée (pour TVA collectée)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    /**
     * Facture fournisseur associée (pour TVA déductible, AIR, IRPP)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id")
    private Bill bill;

    /**
     * Fournisseur associé (pour AIR et IRPP Loyer)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @NotNull(message = "Le type de taxe est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false, length = 50)
    private TaxType taxType;

    @NotNull(message = "La date de calcul est obligatoire")
    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;

    /**
     * Montant de base (HT) sur lequel la taxe est calculée
     */
    @NotNull(message = "Le montant de base est obligatoire")
    @Column(name = "base_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal baseAmount;

    /**
     * Taux de taxe appliqué (%)
     */
    @NotNull(message = "Le taux de taxe est obligatoire")
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    /**
     * Montant de la taxe calculée
     */
    @NotNull(message = "Le montant de la taxe est obligatoire")
    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount;

    /**
     * Compte OHADA de la taxe
     */
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    /**
     * Statut du calcul:
     * - CALCULATED: Calculé mais pas encore posté en comptabilité
     * - POSTED: Écriture comptable créée
     * - PAID: Taxe payée à l'État
     */
    @Column(length = 20)
    @Builder.Default
    private String status = "CALCULATED";

    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Indicateur d'alerte (ex: fournisseur sans NIU)
     */
    @Column(name = "has_alert")
    @Builder.Default
    private Boolean hasAlert = false;

    /**
     * Message d'alerte détaillé
     */
    @Column(name = "alert_message", columnDefinition = "TEXT")
    private String alertMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Marque le calcul comme posté en comptabilité
     */
    public void markAsPosted(GeneralLedger transaction) {
        this.transaction = transaction;
        this.status = "POSTED";
    }

    /**
     * Marque le calcul comme payé
     */
    public void markAsPaid() {
        this.status = "PAID";
    }

    /**
     * Ajoute une alerte au calcul
     */
    public void addAlert(String message) {
        this.hasAlert = true;
        this.alertMessage = message;
    }

    /**
     * Vérifie si ce calcul concerne l'AIR
     */
    public boolean isAIR() {
        return taxType == TaxType.AIR_WITH_NIU || taxType == TaxType.AIR_WITHOUT_NIU;
    }

    /**
     * Vérifie si ce calcul a un taux majoré (pénalité)
     */
    public boolean hasPenaltyRate() {
        return taxType == TaxType.AIR_WITHOUT_NIU;
    }

    /**
     * Calcule le surcoût dû à la pénalité (pour AIR sans NIU)
     */
    public BigDecimal calculatePenaltyCost() {
        if (!hasPenaltyRate()) {
            return BigDecimal.ZERO;
        }

        // Différence entre 5,5% et 2,2%
        BigDecimal normalRate = new BigDecimal("2.2");
        BigDecimal penaltyDifference = taxRate.subtract(normalRate);

        return baseAmount
            .multiply(penaltyDifference)
            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Retourne une description lisible du calcul
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(taxType.getDisplayName());
        desc.append(" - ").append(taxRate).append("%");
        desc.append(" sur ").append(baseAmount).append(" XAF");

        if (supplier != null) {
            desc.append(" (").append(supplier.getName()).append(")");
        }

        if (hasAlert) {
            desc.append(" ⚠️");
        }

        return desc.toString();
    }
}
