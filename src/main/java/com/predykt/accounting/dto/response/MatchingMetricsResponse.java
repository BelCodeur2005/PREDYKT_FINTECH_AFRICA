package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO pour les métriques globales du système de matching
 *
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchingMetricsResponse {

    /**
     * Période d'analyse
     */
    private LocalDate startDate;
    private LocalDate endDate;

    /**
     * Métriques globales
     */
    private GlobalMetrics globalMetrics;

    /**
     * Répartition par niveau de confiance
     */
    private List<ConfidenceLevelMetric> confidenceLevelBreakdown;

    /**
     * Taux d'application par type de suggestion
     */
    private List<SuggestionTypeMetric> suggestionTypeMetrics;

    /**
     * Top raisons de rejet
     */
    private List<RejectionReasonMetric> topRejectionReasons;

    /**
     * Performance par volume de transactions
     */
    private List<VolumePerformanceMetric> volumePerformance;

    /**
     * Évolution dans le temps (si période > 1 mois)
     */
    private List<TimeSeriesMetric> timeSeriesData;

    /**
     * Recommandations d'amélioration
     */
    private List<String> recommendations;

    /**
     * Métriques globales
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GlobalMetrics {
        private long totalAnalyses;
        private long totalTransactionsAnalyzed;
        private long totalSuggestionsGenerated;
        private long totalSuggestionsApplied;
        private long totalSuggestionsRejected;
        private long totalSuggestionsPending;

        private BigDecimal overallPrecisionRate;      // % de suggestions appliquées
        private BigDecimal averageConfidenceScore;     // Score moyen de confiance
        private Double averageAnalysisTimeSeconds;     // Temps moyen d'analyse
        private Double medianAnalysisTimeSeconds;      // Temps médian
        private Double p95AnalysisTimeSeconds;         // Percentile 95

        private BigDecimal monthOverMonthChange;       // Variation vs mois précédent
    }

    /**
     * Métrique par niveau de confiance
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfidenceLevelMetric {
        private String confidenceLevel;    // EXCELLENT, GOOD, FAIR, LOW
        private String scoreRange;         // "95-100%", "80-94%", etc.
        private long count;
        private long applied;
        private long rejected;
        private BigDecimal applicationRate;
        private BigDecimal percentage;     // % du total
    }

    /**
     * Métrique par type de suggestion
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuggestionTypeMetric {
        private String suggestionType;           // Type d'opération en suspens
        private String displayName;              // Nom affiché
        private long totalGenerated;
        private long totalApplied;
        private long totalRejected;
        private BigDecimal applicationRate;
        private BigDecimal averageConfidence;
        private String trend;                    // UP, DOWN, STABLE
    }

    /**
     * Métrique de raison de rejet
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectionReasonMetric {
        private String reason;
        private long count;
        private BigDecimal percentage;
        private String suggestedAction;          // Action recommandée
        private String priority;                 // HIGH, MEDIUM, LOW
    }

    /**
     * Métrique de performance par volume
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolumePerformanceMetric {
        private String volumeRange;              // "< 50", "50-100", etc.
        private long analysesCount;
        private Double averageTimeSeconds;
        private Double maxTimeSeconds;
        private Double p95TimeSeconds;
        private BigDecimal averagePrecision;
        private String status;                   // OK, WARNING, CRITICAL
    }

    /**
     * Métrique temporelle (évolution)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesMetric {
        private LocalDate date;
        private long suggestionsGenerated;
        private long suggestionsApplied;
        private BigDecimal precisionRate;
        private Double averageTimeSeconds;
    }
}
