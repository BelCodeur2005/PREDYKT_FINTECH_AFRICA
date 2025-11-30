package com.predykt.accounting.controller;

import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.MatchingMetricsResponse;
import com.predykt.accounting.dto.response.UserProductivityMetricsResponse;
import com.predykt.accounting.service.MatchingMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 📊 Contrôleur REST pour le Dashboard de Métriques du Matching Bancaire
 *
 * <p>Expose des endpoints pour analyser les performances du système de matching automatique :
 * <ul>
 *   <li>Taux de précision et de confiance</li>
 *   <li>Distribution par niveaux de confiance</li>
 *   <li>Raisons de rejet les plus fréquentes</li>
 *   <li>Performance par volume de transactions</li>
 *   <li>Évolution dans le temps</li>
 *   <li>Productivité des utilisateurs</li>
 * </ul>
 *
 * <p><strong>Cas d'usage :</strong>
 * <ol>
 *   <li><strong>Suivi de la qualité</strong> : Mesurer la précision du matching automatique</li>
 *   <li><strong>Amélioration continue</strong> : Identifier les points faibles (ex: types de transactions mal détectés)</li>
 *   <li><strong>Optimisation des paramètres</strong> : Ajuster les seuils de confiance si nécessaire</li>
 *   <li><strong>Management</strong> : Suivre la productivité et la qualité du travail des comptables</li>
 *   <li><strong>Audit</strong> : Traçabilité des décisions (acceptation/rejet) pour conformité</li>
 * </ol>
 *
 * <p><strong>Exemple d'utilisation :</strong>
 * <pre>
 * // Analyser les performances du dernier mois
 * GET /api/v1/companies/123/reconciliations/metrics/performance?startDate=2024-01-01&endDate=2024-01-31
 *
 * // Vérifier la productivité de l'équipe
 * GET /api/v1/companies/123/reconciliations/metrics/user-productivity?startDate=2024-01-01&endDate=2024-01-31
 *
 * // Identifier les raisons de rejet à améliorer
 * GET /api/v1/companies/123/reconciliations/metrics/rejection-reasons?startDate=2024-01-01&endDate=2024-01-31
 * </pre>
 *
 * @version 2.0
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/companies/{companyId}/reconciliations/metrics")
@RequiredArgsConstructor
@Tag(name = "📊 Dashboard de Métriques",
     description = "Analytics et KPIs du système de matching bancaire intelligent")
public class MatchingMetricsController {

    private final MatchingMetricsService metricsService;

    /**
     * 📈 Métriques globales de performance du matching
     *
     * <p>Retourne une vue d'ensemble complète de la performance du système sur une période donnée :
     * <ul>
     *   <li><strong>Métriques globales</strong> : Taux de précision, nombre d'analyses, temps moyen</li>
     *   <li><strong>Distribution par confiance</strong> : Répartition EXCELLENT / GOOD / FAIR / LOW</li>
     *   <li><strong>Performance par type</strong> : Taux d'application par type de suggestion</li>
     *   <li><strong>Top rejets</strong> : 10 raisons de rejet les plus fréquentes avec actions suggérées</li>
     *   <li><strong>Performance par volume</strong> : Impact du nombre de transactions sur la qualité</li>
     *   <li><strong>Évolution temporelle</strong> : Tendances sur la période (si > 1 mois)</li>
     *   <li><strong>Recommandations</strong> : Actions concrètes pour améliorer le système</li>
     * </ul>
     *
     * <p><strong>Cas d'usage :</strong>
     * <ul>
     *   <li>Dashboard principal pour le responsable comptable</li>
     *   <li>Réunion mensuelle de revue de performance</li>
     *   <li>Audit de la qualité du matching</li>
     *   <li>Décision d'activation de l'auto-approbation (si précision > 95%)</li>
     * </ul>
     *
     * <p><strong>Exemple de réponse :</strong>
     * <pre>
     * {
     *   "globalMetrics": {
     *     "totalAnalyses": 1247,
     *     "totalSuggestionsGenerated": 3894,
     *     "totalSuggestionsApplied": 3556,
     *     "overallPrecisionRate": 91.32,  // ✅ Excellent !
     *     "averageConfidenceScore": 87.45
     *   },
     *   "confidenceLevelBreakdown": [
     *     {"confidenceLevel": "EXCELLENT", "count": 2140, "applied": 2098, "applicationRate": 97.9},
     *     {"confidenceLevel": "GOOD", "count": 1234, "applied": 1123, "applicationRate": 91.0},
     *     ...
     *   ],
     *   "recommendations": [
     *     "✅ EXCELLENT PRECISION (91.32%) : You can enable auto-application for suggestions > 95% confidence",
     *     "⚠️ TYPE 'Crédit non identifié' has low application rate (43.6%) : Improve detection criteria"
     *   ]
     * }
     * </pre>
     *
     * @param companyId ID de l'entreprise (tenant isolation)
     * @param startDate Date de début de la période d'analyse (format: YYYY-MM-DD)
     * @param endDate Date de fin de la période d'analyse (format: YYYY-MM-DD)
     * @return Métriques complètes avec recommandations d'amélioration
     */
    @GetMapping("/performance")
    @Operation(
        summary = "📈 Métriques globales de performance",
        description = "Vue d'ensemble complète : taux de précision, distribution par confiance, " +
                      "raisons de rejet, évolution temporelle et recommandations d'amélioration. " +
                      "Utilisé pour le dashboard principal et les revues de performance mensuelle."
    )
    public ResponseEntity<ApiResponse<MatchingMetricsResponse>> getPerformanceMetrics(
            @PathVariable
            @Parameter(description = "ID de l'entreprise", example = "123")
            Long companyId,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Date de début de la période (YYYY-MM-DD)", example = "2024-01-01", required = true)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Date de fin de la période (YYYY-MM-DD)", example = "2024-01-31", required = true)
            LocalDate endDate) {

        log.info("📊 [METRICS] Calcul des métriques de performance - Company: {}, Période: {} -> {}",
            companyId, startDate, endDate);

        MatchingMetricsResponse metrics = metricsService.calculateMetrics(companyId, startDate, endDate);

        String message = String.format(
            "📊 Analyse complétée : %d analyses, %.1f%% de précision, %d recommandations",
            metrics.getGlobalMetrics().getTotalAnalyses(),
            metrics.getGlobalMetrics().getOverallPrecisionRate(),
            metrics.getRecommendations().size()
        );

        log.info("✅ [METRICS] Métriques calculées - Précision globale: {}%, Suggestions: {}",
            metrics.getGlobalMetrics().getOverallPrecisionRate(),
            metrics.getGlobalMetrics().getTotalSuggestionsGenerated());

        return ResponseEntity.ok(ApiResponse.success(metrics, message));
    }

    /**
     * 🎯 Taux d'application par niveau de confiance
     *
     * <p>Analyse détaillée de l'efficacité du système selon les niveaux de confiance :
     * <ul>
     *   <li><strong>EXCELLENT (95-100%)</strong> : Suggestions quasi-certaines</li>
     *   <li><strong>GOOD (80-94%)</strong> : Suggestions très probables</li>
     *   <li><strong>FAIR (70-79%)</strong> : Suggestions à vérifier</li>
     *   <li><strong>LOW (50-69%)</strong> : Suggestions douteuses</li>
     * </ul>
     *
     * <p><strong>Utilité :</strong> Valider les seuils de confiance configurés et ajuster
     * le paramètre <code>auto-approve-threshold</code> si nécessaire.
     *
     * <p><strong>Exemple :</strong> Si le niveau EXCELLENT a un taux d'application > 98%,
     * on peut activer l'auto-approbation pour ces suggestions.
     *
     * @param companyId ID de l'entreprise
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Distribution par niveau de confiance avec taux d'application
     */
    @GetMapping("/confidence-breakdown")
    @Operation(
        summary = "🎯 Taux d'application par niveau de confiance",
        description = "Analyse l'efficacité des suggestions selon leur niveau de confiance " +
                      "(EXCELLENT/GOOD/FAIR/LOW). Permet d'ajuster les seuils et d'activer " +
                      "l'auto-approbation si les résultats sont excellents."
    )
    public ResponseEntity<ApiResponse<MatchingMetricsResponse.ConfidenceLevelMetric[]>> getConfidenceBreakdown(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("🎯 [METRICS] Analyse par niveau de confiance - Company: {}", companyId);

        MatchingMetricsResponse metrics = metricsService.calculateMetrics(companyId, startDate, endDate);
        MatchingMetricsResponse.ConfidenceLevelMetric[] breakdown =
            metrics.getConfidenceLevelBreakdown().toArray(new MatchingMetricsResponse.ConfidenceLevelMetric[0]);

        return ResponseEntity.ok(ApiResponse.success(breakdown,
            String.format("🎯 Distribution calculée sur %d suggestions",
                metrics.getGlobalMetrics().getTotalSuggestionsGenerated())));
    }

    /**
     * ✗ Top 10 des raisons de rejet
     *
     * <p>Identifie les raisons les plus fréquentes de rejet des suggestions par les comptables.
     *
     * <p><strong>Utilité principale :</strong>
     * <ol>
     *   <li><strong>Amélioration continue</strong> : Identifier les faiblesses de l'algorithme</li>
     *   <li><strong>Priorités de développement</strong> : Corriger les erreurs récurrentes</li>
     *   <li><strong>Formation</strong> : Comprendre les difficultés des utilisateurs</li>
     * </ol>
     *
     * <p><strong>Exemple d'action :</strong>
     * <pre>
     * Si "Montant incorrect" est la raison #1 avec 234 occurrences :
     *   → Revoir la configuration <code>amount-tolerance</code>
     *   → Vérifier si les frais bancaires sont bien pris en compte
     *   → Ajuster le paramètre <code>amount-tolerance.large-amount-percent</code>
     * </pre>
     *
     * @param companyId ID de l'entreprise
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Top 10 des raisons de rejet avec actions suggérées et priorités
     */
    @GetMapping("/rejection-reasons")
    @Operation(
        summary = "✗ Top 10 des raisons de rejet",
        description = "Identifie les raisons les plus fréquentes de rejet des suggestions. " +
                      "Permet d'améliorer l'algorithme en corrigeant les erreurs récurrentes. " +
                      "Chaque raison inclut une action suggérée et un niveau de priorité."
    )
    public ResponseEntity<ApiResponse<MatchingMetricsResponse.RejectionReasonMetric[]>> getTopRejectionReasons(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10")
            @Parameter(description = "Nombre de raisons à retourner (1-50)", example = "10")
            int limit) {

        log.info("✗ [METRICS] Analyse des raisons de rejet - Company: {}, Limit: {}", companyId, limit);

        MatchingMetricsResponse metrics = metricsService.calculateMetrics(companyId, startDate, endDate);

        // Limiter au nombre demandé (max 50)
        int actualLimit = Math.min(limit, 50);
        MatchingMetricsResponse.RejectionReasonMetric[] topReasons = metrics.getTopRejectionReasons()
            .stream()
            .limit(actualLimit)
            .toArray(MatchingMetricsResponse.RejectionReasonMetric[]::new);

        long totalRejected = metrics.getGlobalMetrics().getTotalSuggestionsRejected();

        return ResponseEntity.ok(ApiResponse.success(topReasons,
            String.format("✗ Top %d raisons de rejet sur %d suggestions rejetées au total",
                topReasons.length, totalRejected)));
    }

    /**
     * 📊 Performance par volume de transactions
     *
     * <p>Analyse l'impact du nombre de transactions sur la qualité et les temps de traitement.
     *
     * <p><strong>Tranches analysées :</strong>
     * <ul>
     *   <li>&lt; 50 transactions : Petits rapprochements</li>
     *   <li>50-100 transactions : Volume moyen</li>
     *   <li>100-200 transactions : Volume élevé</li>
     *   <li>200-500 transactions : Très grand volume</li>
     *   <li>&gt; 500 transactions : Volume critique</li>
     * </ul>
     *
     * <p><strong>Métriques par tranche :</strong>
     * <ul>
     *   <li>Nombre d'analyses dans cette tranche</li>
     *   <li>Temps moyen, maximum, et percentile 95</li>
     *   <li>Précision moyenne (pour détecter une dégradation)</li>
     *   <li>Statut : OK / WARNING / CRITICAL</li>
     * </ul>
     *
     * <p><strong>Utilité :</strong> Détecter si les performances se dégradent au-delà d'un certain
     * volume, et activer le mode <code>high-performance-mode</code> si nécessaire.
     *
     * @param companyId ID de l'entreprise
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Performance par tranche de volume avec status (OK/WARNING/CRITICAL)
     */
    @GetMapping("/volume-performance")
    @Operation(
        summary = "📊 Performance par volume de transactions",
        description = "Analyse l'impact du nombre de transactions sur la qualité et le temps de traitement. " +
                      "Identifie les volumes problématiques nécessitant l'activation du mode haute performance."
    )
    public ResponseEntity<ApiResponse<MatchingMetricsResponse.VolumePerformanceMetric[]>> getVolumePerformance(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("📊 [METRICS] Analyse de performance par volume - Company: {}", companyId);

        MatchingMetricsResponse metrics = metricsService.calculateMetrics(companyId, startDate, endDate);
        MatchingMetricsResponse.VolumePerformanceMetric[] volumePerf =
            metrics.getVolumePerformance().toArray(new MatchingMetricsResponse.VolumePerformanceMetric[0]);

        // Détecter s'il y a des statuts WARNING ou CRITICAL
        long criticalCount = metrics.getVolumePerformance().stream()
            .filter(v -> "CRITICAL".equals(v.getStatus()))
            .count();
        long warningCount = metrics.getVolumePerformance().stream()
            .filter(v -> "WARNING".equals(v.getStatus()))
            .count();

        String message = String.format("📊 Analyse de %d tranches de volume", volumePerf.length);
        if (criticalCount > 0) {
            message += String.format(" - ⚠️ %d tranches en statut CRITICAL", criticalCount);
        } else if (warningCount > 0) {
            message += String.format(" - ⚠️ %d tranches en statut WARNING", warningCount);
        }

        return ResponseEntity.ok(ApiResponse.success(volumePerf, message));
    }

    /**
     * 📈 Évolution temporelle (Time Series)
     *
     * <p>Retourne l'évolution jour par jour (ou mois par mois) des métriques clés :
     * <ul>
     *   <li>Nombre de suggestions générées</li>
     *   <li>Nombre de suggestions appliquées</li>
     *   <li>Taux de précision</li>
     *   <li>Temps moyen d'analyse</li>
     * </ul>
     *
     * <p><strong>Utilité :</strong>
     * <ul>
     *   <li>Détecter des tendances (amélioration ou dégradation)</li>
     *   <li>Identifier des événements spécifiques (ex: chute de précision après un import massif)</li>
     *   <li>Valider l'impact d'un changement de configuration</li>
     *   <li>Alimenter des graphiques de tableau de bord</li>
     * </ul>
     *
     * <p><strong>Format de réponse :</strong>
     * <pre>
     * [
     *   {"date": "2024-01-01", "suggestionsGenerated": 45, "suggestionsApplied": 42, "precisionRate": 93.3},
     *   {"date": "2024-01-02", "suggestionsGenerated": 67, "suggestionsApplied": 61, "precisionRate": 91.0},
     *   ...
     * ]
     * </pre>
     *
     * @param companyId ID de l'entreprise
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Série temporelle des métriques (jour par jour ou mois par mois selon la période)
     */
    @GetMapping("/time-series")
    @Operation(
        summary = "📈 Évolution temporelle des métriques",
        description = "Série temporelle de l'évolution des performances (suggestions, précision, temps). " +
                      "Idéal pour alimenter des graphiques de dashboard et détecter des tendances."
    )
    public ResponseEntity<ApiResponse<MatchingMetricsResponse.TimeSeriesMetric[]>> getTimeSeries(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("📈 [METRICS] Analyse de série temporelle - Company: {}, Période: {} -> {}",
            companyId, startDate, endDate);

        MatchingMetricsResponse metrics = metricsService.calculateMetrics(companyId, startDate, endDate);
        MatchingMetricsResponse.TimeSeriesMetric[] timeSeries =
            metrics.getTimeSeriesData().toArray(new MatchingMetricsResponse.TimeSeriesMetric[0]);

        return ResponseEntity.ok(ApiResponse.success(timeSeries,
            String.format("📈 Série temporelle générée : %d points de données", timeSeries.length)));
    }

    /**
     * 👥 Productivité des utilisateurs
     *
     * <p>Analyse la productivité et la qualité du travail de chaque comptable :
     * <ul>
     *   <li><strong>Volume</strong> : Rapprochements complétés, transactions traitées</li>
     *   <li><strong>Qualité</strong> : Taux d'application des suggestions, score de précision</li>
     *   <li><strong>Efficacité</strong> : Temps moyen par rapprochement</li>
     *   <li><strong>Classement</strong> : Ranking dans l'équipe</li>
     *   <li><strong>Performance level</strong> : EXCELLENT / GOOD / AVERAGE / NEEDS_IMPROVEMENT</li>
     * </ul>
     *
     * <p><strong>Métriques d'équipe agrégées :</strong>
     * <ul>
     *   <li>Nombre d'utilisateurs actifs</li>
     *   <li>Moyennes d'équipe (taux d'application, temps moyen, etc.)</li>
     *   <li>Meilleur performer avec son score</li>
     * </ul>
     *
     * <p><strong>Cas d'usage :</strong>
     * <ol>
     *   <li><strong>Management</strong> : Revue de performance individuelle et d'équipe</li>
     *   <li><strong>Formation</strong> : Identifier les utilisateurs nécessitant un accompagnement</li>
     *   <li><strong>Recognition</strong> : Valoriser les meilleurs performers</li>
     *   <li><strong>Optimisation</strong> : Répartir la charge selon les compétences</li>
     * </ol>
     *
     * <p><strong>IMPORTANT - Confidentialité :</strong>
     * <ul>
     *   <li>⚠️ Endpoint réservé aux managers/admins (TODO: ajouter @PreAuthorize("hasRole('ADMIN')"))</li>
     *   <li>Les comptables ne doivent voir que leurs propres métriques</li>
     *   <li>Utiliser avec discrétion pour éviter une pression excessive</li>
     * </ul>
     *
     * @param companyId ID de l'entreprise
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Métriques individuelles et agrégées de productivité
     */
    @GetMapping("/user-productivity")
    @Operation(
        summary = "👥 Productivité des utilisateurs et de l'équipe",
        description = "Analyse détaillée de la productivité (volume, qualité, efficacité) de chaque comptable " +
                      "avec classement et niveau de performance. Inclut les métriques d'équipe agrégées. " +
                      "⚠️ RÉSERVÉ AUX MANAGERS/ADMINS."
    )
    // TODO: @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<UserProductivityMetricsResponse>> getUserProductivity(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("👥 [METRICS] Analyse de productivité utilisateurs - Company: {}", companyId);

        UserProductivityMetricsResponse productivity =
            metricsService.calculateUserProductivityMetrics(companyId, startDate, endDate);

        String message = String.format(
            "👥 Analyse de productivité : %d utilisateurs actifs sur %d total",
            productivity.getTeamMetrics().getActiveUsers(),
            productivity.getTeamMetrics().getTotalUsers()
        );

        if (productivity.getTeamMetrics().getBestPerformer() != null) {
            message += String.format(" - 🏆 Meilleur performer : %s (score: %.1f)",
                productivity.getTeamMetrics().getBestPerformer(),
                productivity.getTeamMetrics().getBestPerformerScore());
        }

        return ResponseEntity.ok(ApiResponse.success(productivity, message));
    }

    /**
     * 💡 Recommandations d'amélioration
     *
     * <p>Génère automatiquement des recommandations actionnables basées sur l'analyse des métriques :
     * <ul>
     *   <li>✅ Points forts à conserver</li>
     *   <li>⚠️ Points faibles à améliorer avec actions concrètes</li>
     *   <li>🔧 Paramètres de configuration à ajuster</li>
     *   <li>📚 Besoins de formation identifiés</li>
     * </ul>
     *
     * <p><strong>Exemples de recommandations :</strong>
     * <pre>
     * ✅ EXCELLENT PRECISION (91.32%) : You can enable auto-application for suggestions > 95% confidence
     * ⚠️ TYPE 'Crédit non identifié' has low application rate (43.6%) : Improve detection criteria
     * ⚠️ HIGH REJECTION RATE (> 15%) : Review configuration or provide user training
     * 🔧 Configure heuristics.virement-keywords to improve transfer detection
     * </pre>
     *
     * @param companyId ID de l'entreprise
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Liste de recommandations actionnables avec priorités
     */
    @GetMapping("/recommendations")
    @Operation(
        summary = "💡 Recommandations d'amélioration automatiques",
        description = "Génère des recommandations actionnables basées sur l'analyse des métriques : " +
                      "points forts, points faibles, paramètres à ajuster, besoins de formation."
    )
    public ResponseEntity<ApiResponse<String[]>> getRecommendations(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("💡 [METRICS] Génération de recommandations - Company: {}", companyId);

        MatchingMetricsResponse metrics = metricsService.calculateMetrics(companyId, startDate, endDate);
        String[] recommendations = metrics.getRecommendations().toArray(new String[0]);

        return ResponseEntity.ok(ApiResponse.success(recommendations,
            String.format("💡 %d recommandations générées", recommendations.length)));
    }

    /**
     * 📑 Résumé exécutif (Executive Summary)
     *
     * <p>Vue consolidée pour le management avec seulement les KPIs essentiels :
     * <ul>
     *   <li>Taux de précision global</li>
     *   <li>Nombre total d'analyses</li>
     *   <li>Temps moyen d'analyse</li>
     *   <li>Top 3 recommandations prioritaires</li>
     *   <li>Variation vs période précédente</li>
     * </ul>
     *
     * <p><strong>Utilité :</strong> Rapport mensuel pour la direction (1 page, essentiel uniquement).
     *
     * @param companyId ID de l'entreprise
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Résumé exécutif avec KPIs essentiels uniquement
     */
    @GetMapping("/executive-summary")
    @Operation(
        summary = "📑 Résumé exécutif (KPIs essentiels)",
        description = "Vue consolidée pour le management : taux de précision, volume, temps moyen, " +
                      "top 3 recommandations et variation vs période précédente. Idéal pour rapports mensuels."
    )
    public ResponseEntity<ApiResponse<ExecutiveSummary>> getExecutiveSummary(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("📑 [METRICS] Génération du résumé exécutif - Company: {}", companyId);

        MatchingMetricsResponse metrics = metricsService.calculateMetrics(companyId, startDate, endDate);

        ExecutiveSummary summary = ExecutiveSummary.builder()
            .period(startDate + " → " + endDate)
            .totalAnalyses(metrics.getGlobalMetrics().getTotalAnalyses())
            .overallPrecisionRate(metrics.getGlobalMetrics().getOverallPrecisionRate())
            .averageConfidenceScore(metrics.getGlobalMetrics().getAverageConfidenceScore())
            .averageAnalysisTimeSeconds(metrics.getGlobalMetrics().getAverageAnalysisTimeSeconds())
            .monthOverMonthChange(metrics.getGlobalMetrics().getMonthOverMonthChange())
            .topRecommendations(metrics.getRecommendations().stream().limit(3).toArray(String[]::new))
            .build();

        return ResponseEntity.ok(ApiResponse.success(summary, "📑 Résumé exécutif généré"));
    }

    /**
     * DTO pour le résumé exécutif
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ExecutiveSummary {
        private String period;
        private long totalAnalyses;
        private java.math.BigDecimal overallPrecisionRate;
        private java.math.BigDecimal averageConfidenceScore;
        private Double averageAnalysisTimeSeconds;
        private java.math.BigDecimal monthOverMonthChange;
        private String[] topRecommendations;
    }
}
