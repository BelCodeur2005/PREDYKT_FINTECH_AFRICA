package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.BankReconciliationSuggestion;
import com.predykt.accounting.domain.enums.SuggestionStatus;
import com.predykt.accounting.dto.response.MatchingMetricsResponse;
import com.predykt.accounting.dto.response.UserProductivityMetricsResponse;
import com.predykt.accounting.repository.BankReconciliationSuggestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service de calcul des métriques pour le Dashboard de Matching
 *
 * Calcule les statistiques de performance, précision, et productivité
 * du système de matching bancaire intelligent.
 *
 * @version 1.0
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class MatchingMetricsService {

    private final BankReconciliationSuggestionRepository suggestionRepository;

    /**
     * Calcule les métriques globales de matching pour une période donnée
     *
     * @param companyId ID de l'entreprise (null pour toutes)
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Métriques complètes
     */
    public MatchingMetricsResponse calculateMetrics(
            Long companyId,
            LocalDate startDate,
            LocalDate endDate) {

        log.info("📊 Calcul des métriques de matching pour la période {} - {}", startDate, endDate);

        // Récupérer toutes les suggestions de la période
        List<BankReconciliationSuggestion> suggestions = companyId != null ?
                suggestionRepository.findByCompanyAndDateRange(companyId, startDate, endDate) :
                suggestionRepository.findByDateRange(startDate, endDate);

        if (suggestions.isEmpty()) {
            log.warn("Aucune suggestion trouvée pour la période spécifiée");
            return buildEmptyMetrics(startDate, endDate);
        }

        // Calculer les métriques globales
        MatchingMetricsResponse.GlobalMetrics globalMetrics = calculateGlobalMetrics(suggestions);

        // Répartition par niveau de confiance
        List<MatchingMetricsResponse.ConfidenceLevelMetric> confidenceBreakdown =
                calculateConfidenceLevelBreakdown(suggestions);

        // Taux d'application par type
        List<MatchingMetricsResponse.SuggestionTypeMetric> typeMetrics =
                calculateSuggestionTypeMetrics(suggestions);

        // Top raisons de rejet
        List<MatchingMetricsResponse.RejectionReasonMetric> rejectionReasons =
                calculateTopRejectionReasons(suggestions);

        // Performance par volume (nécessite des données de rapprochement)
        List<MatchingMetricsResponse.VolumePerformanceMetric> volumePerformance =
                calculateVolumePerformance(suggestions);

        // Évolution dans le temps (si période > 1 mois)
        List<MatchingMetricsResponse.TimeSeriesMetric> timeSeries =
                calculateTimeSeries(suggestions, startDate, endDate);

        // Générer des recommandations
        List<String> recommendations = generateRecommendations(
                globalMetrics, typeMetrics, rejectionReasons);

        return MatchingMetricsResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .globalMetrics(globalMetrics)
                .confidenceLevelBreakdown(confidenceBreakdown)
                .suggestionTypeMetrics(typeMetrics)
                .topRejectionReasons(rejectionReasons)
                .volumePerformance(volumePerformance)
                .timeSeriesData(timeSeries)
                .recommendations(recommendations)
                .build();
    }

    /**
     * Calcule les métriques globales
     */
    private MatchingMetricsResponse.GlobalMetrics calculateGlobalMetrics(
            List<BankReconciliationSuggestion> suggestions) {

        long total = suggestions.size();
        long applied = suggestions.stream()
                .filter(s -> s.getStatus() == SuggestionStatus.APPLIED)
                .count();
        long rejected = suggestions.stream()
                .filter(s -> s.getStatus() == SuggestionStatus.REJECTED)
                .count();
        long pending = suggestions.stream()
                .filter(s -> s.getStatus() == SuggestionStatus.PENDING)
                .count();

        // Nombre de rapprochements distincts
        long totalAnalyses = suggestions.stream()
                .map(s -> s.getReconciliation().getId())
                .distinct()
                .count();

        // Nombre de transactions (somme des BT + GL)
        long totalTransactions = suggestions.stream()
                .mapToLong(s -> s.getBankTransactions().size() + s.getGlEntries().size())
                .sum();

        // Taux de précision
        BigDecimal precisionRate = total > 0 ?
                BigDecimal.valueOf(applied).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)) :
                BigDecimal.ZERO;

        // Score de confiance moyen
        BigDecimal avgConfidence = suggestions.stream()
                .map(BankReconciliationSuggestion::getConfidenceScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        return MatchingMetricsResponse.GlobalMetrics.builder()
                .totalAnalyses(totalAnalyses)
                .totalTransactionsAnalyzed(totalTransactions)
                .totalSuggestionsGenerated(total)
                .totalSuggestionsApplied(applied)
                .totalSuggestionsRejected(rejected)
                .totalSuggestionsPending(pending)
                .overallPrecisionRate(precisionRate)
                .averageConfidenceScore(avgConfidence)
                .averageAnalysisTimeSeconds(null) // TODO: Nécessite instrumentation
                .medianAnalysisTimeSeconds(null)
                .p95AnalysisTimeSeconds(null)
                .monthOverMonthChange(BigDecimal.ZERO) // TODO: Comparaison avec période précédente
                .build();
    }

    /**
     * Répartition par niveau de confiance
     */
    private List<MatchingMetricsResponse.ConfidenceLevelMetric> calculateConfidenceLevelBreakdown(
            List<BankReconciliationSuggestion> suggestions) {

        Map<String, List<BankReconciliationSuggestion>> byLevel = suggestions.stream()
                .collect(Collectors.groupingBy(BankReconciliationSuggestion::getConfidenceLevel));

        long total = suggestions.size();

        return byLevel.entrySet().stream()
                .map(entry -> {
                    String level = entry.getKey();
                    List<BankReconciliationSuggestion> levelSuggestions = entry.getValue();

                    long count = levelSuggestions.size();
                    long applied = levelSuggestions.stream()
                            .filter(s -> s.getStatus() == SuggestionStatus.APPLIED)
                            .count();
                    long rejected = levelSuggestions.stream()
                            .filter(s -> s.getStatus() == SuggestionStatus.REJECTED)
                            .count();

                    BigDecimal appRate = count > 0 ?
                            BigDecimal.valueOf(applied).divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100)) :
                            BigDecimal.ZERO;

                    BigDecimal percentage = total > 0 ?
                            BigDecimal.valueOf(count).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100)) :
                            BigDecimal.ZERO;

                    return MatchingMetricsResponse.ConfidenceLevelMetric.builder()
                            .confidenceLevel(level)
                            .scoreRange(getScoreRangeForLevel(level))
                            .count(count)
                            .applied(applied)
                            .rejected(rejected)
                            .applicationRate(appRate)
                            .percentage(percentage)
                            .build();
                })
                .sorted(Comparator.comparing(MatchingMetricsResponse.ConfidenceLevelMetric::getConfidenceLevel).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Métriques par type de suggestion
     */
    private List<MatchingMetricsResponse.SuggestionTypeMetric> calculateSuggestionTypeMetrics(
            List<BankReconciliationSuggestion> suggestions) {

        Map<String, List<BankReconciliationSuggestion>> byType = suggestions.stream()
                .collect(Collectors.groupingBy(s -> s.getSuggestedItemType().name()));

        return byType.entrySet().stream()
                .map(entry -> {
                    String type = entry.getKey();
                    List<BankReconciliationSuggestion> typeSuggestions = entry.getValue();

                    long generated = typeSuggestions.size();
                    long applied = typeSuggestions.stream()
                            .filter(s -> s.getStatus() == SuggestionStatus.APPLIED)
                            .count();
                    long rejected = typeSuggestions.stream()
                            .filter(s -> s.getStatus() == SuggestionStatus.REJECTED)
                            .count();

                    BigDecimal appRate = generated > 0 ?
                            BigDecimal.valueOf(applied).divide(BigDecimal.valueOf(generated), 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100)) :
                            BigDecimal.ZERO;

                    BigDecimal avgConfidence = typeSuggestions.stream()
                            .map(BankReconciliationSuggestion::getConfidenceScore)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(generated), 2, RoundingMode.HALF_UP);

                    return MatchingMetricsResponse.SuggestionTypeMetric.builder()
                            .suggestionType(type)
                            .displayName(getDisplayName(type))
                            .totalGenerated(generated)
                            .totalApplied(applied)
                            .totalRejected(rejected)
                            .applicationRate(appRate)
                            .averageConfidence(avgConfidence)
                            .trend("STABLE") // TODO: Comparaison avec période précédente
                            .build();
                })
                .sorted(Comparator.comparing(MatchingMetricsResponse.SuggestionTypeMetric::getTotalGenerated).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Top raisons de rejet
     */
    private List<MatchingMetricsResponse.RejectionReasonMetric> calculateTopRejectionReasons(
            List<BankReconciliationSuggestion> suggestions) {

        // Filtrer les suggestions rejetées avec raison
        List<BankReconciliationSuggestion> rejected = suggestions.stream()
                .filter(s -> s.getStatus() == SuggestionStatus.REJECTED)
                .filter(s -> s.getRejectionReason() != null && !s.getRejectionReason().isEmpty())
                .collect(Collectors.toList());

        if (rejected.isEmpty()) {
            return Collections.emptyList();
        }

        // Grouper par raison
        Map<String, Long> reasonCounts = rejected.stream()
                .collect(Collectors.groupingBy(
                        BankReconciliationSuggestion::getRejectionReason,
                        Collectors.counting()
                ));

        long totalRejected = rejected.size();

        return reasonCounts.entrySet().stream()
                .map(entry -> {
                    String reason = entry.getKey();
                    Long count = entry.getValue();

                    BigDecimal percentage = BigDecimal.valueOf(count)
                            .divide(BigDecimal.valueOf(totalRejected), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));

                    return MatchingMetricsResponse.RejectionReasonMetric.builder()
                            .reason(reason)
                            .count(count)
                            .percentage(percentage)
                            .suggestedAction(getSuggestedAction(reason))
                            .priority(getPriority(count, totalRejected))
                            .build();
                })
                .sorted(Comparator.comparing(MatchingMetricsResponse.RejectionReasonMetric::getCount).reversed())
                .limit(10) // Top 10
                .collect(Collectors.toList());
    }

    /**
     * Performance par volume de transactions
     */
    private List<MatchingMetricsResponse.VolumePerformanceMetric> calculateVolumePerformance(
            List<BankReconciliationSuggestion> suggestions) {

        // Grouper par rapprochement
        Map<Long, List<BankReconciliationSuggestion>> byReconciliation = suggestions.stream()
                .collect(Collectors.groupingBy(s -> s.getReconciliation().getId()));

        // Analyser chaque rapprochement
        Map<String, List<ReconciliationAnalysis>> byVolumeRange = byReconciliation.values().stream()
                .map(this::analyzeReconciliation)
                .collect(Collectors.groupingBy(ReconciliationAnalysis::getVolumeRange));

        return byVolumeRange.entrySet().stream()
                .map(entry -> {
                    String range = entry.getKey();
                    List<ReconciliationAnalysis> analyses = entry.getValue();

                    long count = analyses.size();
                    Double avgTime = analyses.stream()
                            .mapToDouble(ReconciliationAnalysis::getAnalysisTimeSeconds)
                            .average()
                            .orElse(0.0);
                    Double maxTime = analyses.stream()
                            .mapToDouble(ReconciliationAnalysis::getAnalysisTimeSeconds)
                            .max()
                            .orElse(0.0);
                    Double p95Time = calculatePercentile(
                            analyses.stream()
                                    .map(ReconciliationAnalysis::getAnalysisTimeSeconds)
                                    .collect(Collectors.toList()),
                            95
                    );
                    BigDecimal avgPrecision = analyses.stream()
                            .map(ReconciliationAnalysis::getPrecisionRate)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

                    String status = getPerformanceStatus(avgTime);

                    return MatchingMetricsResponse.VolumePerformanceMetric.builder()
                            .volumeRange(range)
                            .analysesCount(count)
                            .averageTimeSeconds(avgTime)
                            .maxTimeSeconds(maxTime)
                            .p95TimeSeconds(p95Time)
                            .averagePrecision(avgPrecision)
                            .status(status)
                            .build();
                })
                .sorted(Comparator.comparing(m -> getVolumeRangeOrder(m.getVolumeRange())))
                .collect(Collectors.toList());
    }

    /**
     * Évolution dans le temps (série temporelle)
     */
    private List<MatchingMetricsResponse.TimeSeriesMetric> calculateTimeSeries(
            List<BankReconciliationSuggestion> suggestions,
            LocalDate startDate,
            LocalDate endDate) {

        // Si période < 30 jours, pas de série temporelle
        if (startDate.plusDays(30).isAfter(endDate)) {
            return Collections.emptyList();
        }

        // Grouper par date de création
        Map<LocalDate, List<BankReconciliationSuggestion>> byDate = suggestions.stream()
                .collect(Collectors.groupingBy(s -> s.getCreatedAt().toLocalDate()));

        return byDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<BankReconciliationSuggestion> dateSuggestions = entry.getValue();

                    long generated = dateSuggestions.size();
                    long applied = dateSuggestions.stream()
                            .filter(s -> s.getStatus() == SuggestionStatus.APPLIED)
                            .count();

                    BigDecimal precision = generated > 0 ?
                            BigDecimal.valueOf(applied).divide(BigDecimal.valueOf(generated), 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100)) :
                            BigDecimal.ZERO;

                    return MatchingMetricsResponse.TimeSeriesMetric.builder()
                            .date(date)
                            .suggestionsGenerated(generated)
                            .suggestionsApplied(applied)
                            .precisionRate(precision)
                            .averageTimeSeconds(null) // TODO
                            .build();
                })
                .sorted(Comparator.comparing(MatchingMetricsResponse.TimeSeriesMetric::getDate))
                .collect(Collectors.toList());
    }

    /**
     * Génère des recommandations basées sur les métriques
     */
    private List<String> generateRecommendations(
            MatchingMetricsResponse.GlobalMetrics globalMetrics,
            List<MatchingMetricsResponse.SuggestionTypeMetric> typeMetrics,
            List<MatchingMetricsResponse.RejectionReasonMetric> rejectionReasons) {

        List<String> recommendations = new ArrayList<>();

        // Recommandation basée sur la précision globale
        if (globalMetrics.getOverallPrecisionRate().compareTo(new BigDecimal("80")) < 0) {
            recommendations.add("⚠️ PRÉCISION FAIBLE (" + globalMetrics.getOverallPrecisionRate() + "%) : " +
                    "Considérez ajuster les seuils de tolérance ou améliorer les mots-clés de détection");
        } else if (globalMetrics.getOverallPrecisionRate().compareTo(new BigDecimal("95")) >= 0) {
            recommendations.add("✅ EXCELLENTE PRÉCISION (" + globalMetrics.getOverallPrecisionRate() + "%) : " +
                    "Vous pouvez activer l'auto-application pour les suggestions > 95% de confiance");
        }

        // Recommandation basée sur les types avec faible taux
        typeMetrics.stream()
                .filter(t -> t.getApplicationRate().compareTo(new BigDecimal("50")) < 0)
                .filter(t -> t.getTotalGenerated() > 10) // Seulement si volume significatif
                .forEach(t -> recommendations.add(
                        "⚠️ TYPE \"" + t.getDisplayName() + "\" a un faible taux d'application (" +
                                t.getApplicationRate() + "%) : Améliorer les critères de détection ou baisser la confiance"
                ));

        // Recommandation basée sur les raisons de rejet fréquentes
        if (!rejectionReasons.isEmpty()) {
            MatchingMetricsResponse.RejectionReasonMetric topReason = rejectionReasons.get(0);
            if (topReason.getCount() > 20) {
                recommendations.add("🔍 RAISON DE REJET FRÉQUENTE : \"" + topReason.getReason() +
                        "\" (" + topReason.getCount() + " fois) → " + topReason.getSuggestedAction());
            }
        }

        if (recommendations.isEmpty()) {
            recommendations.add("✅ Système performant - Pas d'action corrective nécessaire");
        }

        return recommendations;
    }

    // ========== Méthodes auxiliaires ==========

    private MatchingMetricsResponse buildEmptyMetrics(LocalDate startDate, LocalDate endDate) {
        return MatchingMetricsResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .globalMetrics(MatchingMetricsResponse.GlobalMetrics.builder()
                        .totalAnalyses(0L)
                        .totalTransactionsAnalyzed(0L)
                        .totalSuggestionsGenerated(0L)
                        .overallPrecisionRate(BigDecimal.ZERO)
                        .build())
                .confidenceLevelBreakdown(Collections.emptyList())
                .suggestionTypeMetrics(Collections.emptyList())
                .topRejectionReasons(Collections.emptyList())
                .volumePerformance(Collections.emptyList())
                .timeSeriesData(Collections.emptyList())
                .recommendations(List.of("ℹ️ Aucune donnée disponible pour cette période"))
                .build();
    }

    private String getScoreRangeForLevel(String level) {
        switch (level) {
            case "EXCELLENT": return "95-100%";
            case "GOOD": return "80-94%";
            case "FAIR": return "60-79%";
            case "LOW": return "< 60%";
            default: return "N/A";
        }
    }

    private String getDisplayName(String type) {
        // Convertir le nom d'enum en nom affiché
        return type.replace("_", " ").toLowerCase()
                .replaceAll("\\b(\\w)", m -> m.toUpperCase());
    }

    private String getSuggestedAction(String reason) {
        if (reason.toLowerCase().contains("montant")) {
            return "Ajuster la tolérance de montant dans la configuration";
        } else if (reason.toLowerCase().contains("date")) {
            return "Augmenter la fenêtre temporelle acceptée";
        } else if (reason.toLowerCase().contains("description")) {
            return "Améliorer l'algorithme de similarité textuelle";
        } else if (reason.toLowerCase().contains("double") || reason.toLowerCase().contains("doublon")) {
            return "Implémenter une détection de doublons avant matching";
        }
        return "Analyser manuellement et ajuster la configuration";
    }

    private String getPriority(long count, long total) {
        double percentage = (double) count / total * 100;
        if (percentage > 20) return "HIGH";
        if (percentage > 10) return "MEDIUM";
        return "LOW";
    }

    private ReconciliationAnalysis analyzeReconciliation(List<BankReconciliationSuggestion> suggestions) {
        long txCount = suggestions.stream()
                .mapToLong(s -> s.getBankTransactions().size() + s.getGlEntries().size())
                .sum();

        long applied = suggestions.stream()
                .filter(s -> s.getStatus() == SuggestionStatus.APPLIED)
                .count();

        BigDecimal precision = suggestions.size() > 0 ?
                BigDecimal.valueOf(applied).divide(BigDecimal.valueOf(suggestions.size()), 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)) :
                BigDecimal.ZERO;

        // TODO: Récupérer le temps d'analyse réel (nécessite instrumentation)
        double analysisTime = estimateAnalysisTime(txCount);

        return new ReconciliationAnalysis(
                getVolumeRange(txCount),
                analysisTime,
                precision
        );
    }

    private String getVolumeRange(long txCount) {
        if (txCount < 50) return "< 50 tx";
        if (txCount < 100) return "50-100 tx";
        if (txCount < 200) return "100-200 tx";
        if (txCount < 500) return "200-500 tx";
        return "> 500 tx";
    }

    private int getVolumeRangeOrder(String range) {
        switch (range) {
            case "< 50 tx": return 1;
            case "50-100 tx": return 2;
            case "100-200 tx": return 3;
            case "200-500 tx": return 4;
            case "> 500 tx": return 5;
            default: return 99;
        }
    }

    private double estimateAnalysisTime(long txCount) {
        // Estimation basée sur la complexité O(n²) pour phase 1-2
        // et O(n log n) pour phase 2.5 optimisée
        return txCount * 0.02 + (txCount * txCount * 0.0001);
    }

    private String getPerformanceStatus(double avgTimeSeconds) {
        if (avgTimeSeconds < 10) return "OK";
        if (avgTimeSeconds < 30) return "WARNING";
        return "CRITICAL";
    }

    private Double calculatePercentile(List<Double> values, int percentile) {
        if (values.isEmpty()) return 0.0;

        List<Double> sorted = values.stream()
                .sorted()
                .collect(Collectors.toList());

        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, index));
    }

    // Classe auxiliaire pour l'analyse de rapprochement
    private static class ReconciliationAnalysis {
        private final String volumeRange;
        private final double analysisTimeSeconds;
        private final BigDecimal precisionRate;

        public ReconciliationAnalysis(String volumeRange, double analysisTimeSeconds, BigDecimal precisionRate) {
            this.volumeRange = volumeRange;
            this.analysisTimeSeconds = analysisTimeSeconds;
            this.precisionRate = precisionRate;
        }

        public String getVolumeRange() { return volumeRange; }
        public double getAnalysisTimeSeconds() { return analysisTimeSeconds; }
        public BigDecimal getPrecisionRate() { return precisionRate; }
    }
}