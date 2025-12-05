package com.predykt.accounting.domain.entity;

import com.predykt.accounting.domain.enums.AssetCategory;
import com.predykt.accounting.domain.enums.DepreciationMethod;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Entité représentant une immobilisation (Fixed Asset) conforme OHADA
 * Gestion du patrimoine immobilisé de l'entreprise
 *
 * Classes de comptes OHADA:
 * - Classe 21: Immobilisations incorporelles
 * - Classe 22: Terrains
 * - Classe 23: Bâtiments, installations techniques et agencements
 * - Classe 24: Matériel
 * - Classe 26: Immobilisations financières
 * - Classe 28: Amortissements des immobilisations
 */
@Entity
@Table(name = "fixed_assets", indexes = {
    @Index(name = "idx_fixed_assets_company", columnList = "company_id"),
    @Index(name = "idx_fixed_assets_active", columnList = "company_id, is_active"),
    @Index(name = "idx_fixed_assets_category", columnList = "company_id, category"),
    @Index(name = "idx_fixed_assets_account", columnList = "account_number"),
    @Index(name = "idx_fixed_assets_acquisition_date", columnList = "acquisition_date")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_fixed_asset_number", columnNames = {"company_id", "asset_number"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FixedAsset extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false, foreignKey = @ForeignKey(name = "fk_fixed_asset_company"))
    @NotNull(message = "L'entreprise est obligatoire")
    private Company company;

    @NotBlank(message = "Le numéro d'immobilisation est obligatoire")
    @Column(name = "asset_number", nullable = false, length = 50)
    private String assetNumber; // Ex: IMM-2024-001

    @NotBlank(message = "Le nom de l'immobilisation est obligatoire")
    @Column(name = "asset_name", nullable = false, length = 255)
    private String assetName; // Ex: Véhicule Renault Duster

    @Column(columnDefinition = "TEXT")
    private String description; // Description détaillée

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @NotNull(message = "La catégorie est obligatoire")
    private AssetCategory category;

    @NotBlank(message = "Le numéro de compte OHADA est obligatoire")
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber; // Ex: 2410 (Matériel et outillage)

    @Column(name = "supplier_name", length = 200)
    private String supplierName; // Fournisseur

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber; // Numéro facture d'achat

    // === ACQUISITION ===

    @NotNull(message = "La date d'acquisition est obligatoire")
    @Column(name = "acquisition_date", nullable = false)
    private LocalDate acquisitionDate;

    @NotNull(message = "Le coût d'acquisition est obligatoire")
    @DecimalMin(value = "0.01", message = "Le coût d'acquisition doit être positif")
    @Column(name = "acquisition_cost", nullable = false, precision = 20, scale = 2)
    private BigDecimal acquisitionCost; // Valeur brute

    @Column(name = "acquisition_vat", precision = 20, scale = 2)
    @Builder.Default
    private BigDecimal acquisitionVat = BigDecimal.ZERO;

    @Column(name = "installation_cost", precision = 20, scale = 2)
    @Builder.Default
    private BigDecimal installationCost = BigDecimal.ZERO;

    @Column(name = "total_cost", precision = 20, scale = 2)
    private BigDecimal totalCost; // Calculé automatiquement

    // === AMORTISSEMENT ===

    @Enumerated(EnumType.STRING)
    @Column(name = "depreciation_method", nullable = false, length = 30)
    @NotNull(message = "La méthode d'amortissement est obligatoire")
    @Builder.Default
    private DepreciationMethod depreciationMethod = DepreciationMethod.LINEAR;

    @NotNull(message = "La durée de vie utile est obligatoire")
    @Positive(message = "La durée de vie doit être positive")
    @Column(name = "useful_life_years", nullable = false)
    private Integer usefulLifeYears;

    @Column(name = "depreciation_rate", precision = 10, scale = 4)
    private BigDecimal depreciationRate; // Calculé automatiquement

    @Column(name = "residual_value", precision = 20, scale = 2)
    @Builder.Default
    private BigDecimal residualValue = BigDecimal.ZERO;

    @Column(name = "depreciation_start_date")
    private LocalDate depreciationStartDate;

    // === CESSION / SORTIE ===

    @Column(name = "disposal_date")
    private LocalDate disposalDate;

    @Column(name = "disposal_amount", precision = 20, scale = 2)
    private BigDecimal disposalAmount;

    @Column(name = "disposal_reason", length = 500)
    private String disposalReason;

    // === LOCALISATION ===

    @Column(length = 200)
    private String location;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "responsible_person", length = 200)
    private String responsiblePerson;

    // === STATUTS ===

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_fully_depreciated", nullable = false)
    @Builder.Default
    private Boolean isFullyDepreciated = false;

    // === INFORMATIONS COMPLÉMENTAIRES ===

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber; // Immatriculation (véhicules)

    @Column(columnDefinition = "TEXT")
    private String notes;

    // === HOOKS ===

    @PrePersist
    @PreUpdate
    private void calculateValues() {
        // Calcul du coût total
        if (acquisitionCost != null) {
            BigDecimal total = acquisitionCost;
            if (installationCost != null) {
                total = total.add(installationCost);
            }
            this.totalCost = total;
        }

        // Calcul du taux d'amortissement
        if (depreciationRate == null && usefulLifeYears != null && usefulLifeYears > 0) {
            if (depreciationMethod == DepreciationMethod.LINEAR) {
                this.depreciationRate = BigDecimal.valueOf(100.0)
                    .divide(BigDecimal.valueOf(usefulLifeYears), 4, RoundingMode.HALF_UP);
            } else if (depreciationMethod == DepreciationMethod.DECLINING_BALANCE) {
                BigDecimal coefficient = depreciationMethod.getDecliningBalanceCoefficient(usefulLifeYears);
                BigDecimal linearRate = BigDecimal.valueOf(100.0)
                    .divide(BigDecimal.valueOf(usefulLifeYears), 4, RoundingMode.HALF_UP);
                this.depreciationRate = linearRate.multiply(coefficient);
            }
        }

        // Date de début d'amortissement par défaut
        if (depreciationStartDate == null && acquisitionDate != null) {
            this.depreciationStartDate = acquisitionDate;
        }
    }

    // === MÉTHODES MÉTIER ===

    /**
     * Calculer la base amortissable
     */
    public BigDecimal getDepreciableAmount() {
        BigDecimal base = totalCost != null ? totalCost : acquisitionCost;
        return base.subtract(residualValue != null ? residualValue : BigDecimal.ZERO);
    }

    /**
     * Vérifier si l'immobilisation est encore en service
     */
    public boolean isInService() {
        return isActive && disposalDate == null;
    }

    /**
     * Vérifier si l'immobilisation peut être amortie
     */
    public boolean isDepreciable() {
        return isInService() && !isFullyDepreciated && category.getIsDepreciable();
    }

    /**
     * Vérifier si l'immobilisation est cédée
     */
    public boolean isDisposed() {
        return disposalDate != null;
    }
}
