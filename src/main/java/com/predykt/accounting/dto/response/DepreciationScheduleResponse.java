package com.predykt.accounting.dto.response;

import com.predykt.accounting.domain.enums.AssetCategory;
import com.predykt.accounting.domain.enums.DepreciationMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Réponse contenant le tableau d'amortissements complet d'un exercice
 * Conforme OHADA et fiscalité camerounaise
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepreciationScheduleResponse {

    // === EN-TÊTE ===
    private Long companyId;
    private String companyName;
    private Integer fiscalYear;
    private LocalDate fiscalYearStart;
    private LocalDate fiscalYearEnd;

    // === LISTE DES IMMOBILISATIONS ===
    private List<DepreciationItem> items;

    // === TOTAUX PAR CATÉGORIE ===
    private List<CategorySummary> categorySummaries;

    // === RÉSUMÉ GLOBAL ===
    private DepreciationSummary summary;

    // === MOUVEMENTS DE L'EXERCICE ===
    private List<AssetMovement> acquisitions;
    private List<AssetMovement> disposals;

    // === ANALYSE ===
    private DepreciationAnalysis analysis;

    /**
     * Détail d'une immobilisation dans le tableau d'amortissements
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepreciationItem {
        // Identification
        private Long id;
        private String assetNumber;
        private String assetName;
        private AssetCategory category;
        private String categoryName;
        private String accountNumber;

        // Acquisition
        private LocalDate acquisitionDate;
        private BigDecimal acquisitionCost;
        private BigDecimal installationCost;
        private BigDecimal totalCost; // Valeur brute

        // Amortissement
        private DepreciationMethod depreciationMethod;
        private String depreciationMethodName;
        private Integer usefulLifeYears;
        private BigDecimal depreciationRate; // En %
        private BigDecimal residualValue;

        // Calculs de l'exercice
        private BigDecimal depreciableAmount; // Base amortissable
        private BigDecimal previousAccumulatedDepreciation; // Amortissements N-1
        private BigDecimal currentYearDepreciation; // Dotation exercice N
        private BigDecimal accumulatedDepreciation; // Amortissements cumulés N
        private BigDecimal netBookValue; // VNC (Valeur Nette Comptable)

        // Prorata temporis (pour acquisitions en cours d'année)
        private Boolean isProrata;
        private Integer monthsInService; // Nombre de mois en service durant l'exercice

        // Cession
        private LocalDate disposalDate;
        private BigDecimal disposalAmount;
        private BigDecimal disposalGainLoss; // Plus-value ou moins-value

        // Statut
        private Boolean isFullyDepreciated;
        private Boolean isDisposed;
    }

    /**
     * Résumé par catégorie d'immobilisation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private AssetCategory category;
        private String categoryName;
        private String accountPrefix;

        private Integer assetCount;
        private BigDecimal totalAcquisitionCost;
        private BigDecimal totalPreviousDepreciation;
        private BigDecimal totalCurrentDepreciation;
        private BigDecimal totalAccumulatedDepreciation;
        private BigDecimal totalNetBookValue;
    }

    /**
     * Résumé global du tableau d'amortissements
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepreciationSummary {
        // Totaux généraux
        private Integer totalAssetCount;
        private Integer activeAssetCount;
        private Integer disposedAssetCount;
        private Integer fullyDepreciatedCount;

        // Valeurs financières
        private BigDecimal totalGrossValue; // Valeur brute totale
        private BigDecimal totalPreviousDepreciation; // Amortissements N-1
        private BigDecimal totalCurrentDepreciation; // Dotation exercice N
        private BigDecimal totalAccumulatedDepreciation; // Amortissements cumulés N
        private BigDecimal totalNetBookValue; // VNC totale

        // Mouvements
        private Integer acquisitionsCount;
        private BigDecimal acquisitionsValue;
        private Integer disposalsCount;
        private BigDecimal disposalsValue;
        private BigDecimal disposalsGainLoss; // Plus-values nettes

        // Répartition par méthode
        private Map<String, BigDecimal> depreciationByMethod; // LINEAR, DECLINING_BALANCE, etc.
    }

    /**
     * Mouvement d'immobilisation (acquisition ou cession)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetMovement {
        private Long assetId;
        private String assetNumber;
        private String assetName;
        private AssetCategory category;
        private LocalDate movementDate;
        private BigDecimal amount;
        private String description;

        // Spécifique aux cessions
        private BigDecimal accumulatedDepreciation;
        private BigDecimal netBookValue;
        private BigDecimal gainLoss; // Plus-value ou moins-value
    }

    /**
     * Analyse et recommandations
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepreciationAnalysis {
        // Alertes
        private List<String> alerts;

        // Recommandations
        private List<String> recommendations;

        // Indicateurs clés
        private BigDecimal averageAge; // Âge moyen du parc (années)
        private BigDecimal depreciationRate; // Taux d'amortissement moyen
        private BigDecimal renewalRate; // Taux de renouvellement (acquisitions/total)

        // Immobilisations critiques
        private List<DepreciationItem> fullyDepreciatedAssets;
        private List<DepreciationItem> oldAssets; // > durée de vie utile

        // Prévisions
        private BigDecimal nextYearDepreciation; // Dotation prévisionnelle N+1
        private List<DepreciationItem> upcomingFullyDepreciated; // Seront totalement amorties N+1
    }
}
