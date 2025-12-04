package com.predykt.accounting.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entité représentant une immobilisation (Fixed Asset)
 * Gestion du patrimoine immobilisé de l'entreprise
 */
@Entity
@Table(name = "fixed_assets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "asset_number", nullable = false, length = 50)
    private String assetNumber;

    @Column(name = "asset_name", nullable = false)
    private String assetName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Catégorie d'immobilisation
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private AssetCategory category;

    /**
     * Numéro de compte OHADA (21x, 22x, 23x, 24x, 25x)
     */
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    // === ACQUISITION ===

    @Column(name = "acquisition_date", nullable = false)
    private LocalDate acquisitionDate;

    @Column(name = "acquisition_cost", nullable = false, precision = 20, scale = 2)
    private BigDecimal acquisitionCost;

    @Column(name = "supplier_name", length = 255)
    private String supplierName;

    // === AMORTISSEMENT ===

    /**
     * Méthode d'amortissement
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "depreciation_method", nullable = false, length = 30)
    private DepreciationMethod depreciationMethod;

    /**
     * Durée d'utilité en années
     */
    @Column(name = "useful_life_years", nullable = false)
    private Integer usefulLifeYears;

    /**
     * Valeur résiduelle (valeur de récupération estimée)
     */
    @Column(name = "residual_value", precision = 20, scale = 2)
    private BigDecimal residualValue;

    /**
     * Taux d'amortissement annuel (calculé automatiquement)
     */
    @Column(name = "depreciation_rate", precision = 5, scale = 4)
    private BigDecimal depreciationRate;

    // === CESSION/MISE AU REBUT ===

    @Column(name = "disposal_date")
    private LocalDate disposalDate;

    @Column(name = "disposal_amount", precision = 20, scale = 2)
    private BigDecimal disposalAmount;

    @Column(name = "disposal_reason", length = 255)
    private String disposalReason;

    // === LOCALISATION ET RESPONSABLE ===

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "responsible_person", length = 255)
    private String responsiblePerson;

    @Column(name = "department", length = 100)
    private String department;

    // === STATUT ===

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "is_fully_depreciated", nullable = false)
    private Boolean isFullyDepreciated;

    // === AUDIT ===

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // === HOOKS ===

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
        if (isFullyDepreciated == null) isFullyDepreciated = false;
        if (residualValue == null) residualValue = BigDecimal.ZERO;
        calculateDepreciationRate();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // === MÉTHODES MÉTIER ===

    /**
     * Calculer le taux d'amortissement annuel
     */
    public void calculateDepreciationRate() {
        if (usefulLifeYears != null && usefulLifeYears > 0) {
            if (depreciationMethod == DepreciationMethod.LINEAR) {
                // Linéaire: 1 / durée
                this.depreciationRate = BigDecimal.ONE
                    .divide(new BigDecimal(usefulLifeYears), 4, java.math.RoundingMode.HALF_UP);
            } else if (depreciationMethod == DepreciationMethod.DECLINING_BALANCE) {
                // Dégressif: taux = (1 / durée) × coefficient (généralement 2 ou 2.5)
                BigDecimal coefficient = new BigDecimal("2.0");
                this.depreciationRate = BigDecimal.ONE
                    .divide(new BigDecimal(usefulLifeYears), 4, java.math.RoundingMode.HALF_UP)
                    .multiply(coefficient);
            }
        }
    }

    /**
     * Calculer la base amortissable
     */
    public BigDecimal getDepreciableAmount() {
        return acquisitionCost.subtract(residualValue);
    }

    /**
     * Vérifier si l'immobilisation est cédée
     */
    public boolean isDisposed() {
        return disposalDate != null;
    }

    // === ENUMS ===

    /**
     * Catégories d'immobilisations selon OHADA
     */
    public enum AssetCategory {
        INTANGIBLE("Immobilisations incorporelles", "21"),
        LAND("Terrains", "22"),
        BUILDING("Bâtiments", "23"),
        EQUIPMENT("Matériel et outillage", "24"),
        VEHICLE("Matériel de transport", "245"),
        FURNITURE("Mobilier et matériel de bureau", "2441"),
        IT_EQUIPMENT("Matériel informatique", "2443"),
        FINANCIAL("Immobilisations financières", "26");

        private final String displayName;
        private final String accountPrefix;

        AssetCategory(String displayName, String accountPrefix) {
            this.displayName = displayName;
            this.accountPrefix = accountPrefix;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getAccountPrefix() {
            return accountPrefix;
        }
    }

    /**
     * Méthodes d'amortissement
     */
    public enum DepreciationMethod {
        LINEAR("Linéaire", "Amortissement constant sur toute la durée"),
        DECLINING_BALANCE("Dégressif", "Amortissement décroissant (coefficient 2x)");

        private final String displayName;
        private final String description;

        DepreciationMethod(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }
}
