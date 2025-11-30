package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO pour les métriques de productivité des utilisateurs
 *
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProductivityMetricsResponse {

    private LocalDate startDate;
    private LocalDate endDate;

    /**
     * Métriques par utilisateur
     */
    private List<UserMetric> userMetrics;

    /**
     * Métriques d'équipe (agrégées)
     */
    private TeamMetrics teamMetrics;

    /**
     * Métrique individuelle d'un utilisateur
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserMetric {
        private String userId;
        private String userName;
        private String userEmail;

        private long reconciliationsCompleted;
        private long transactionsProcessed;
        private long suggestionsApplied;
        private long suggestionsRejected;

        private BigDecimal applicationRate;           // % suggestions appliquées
        private Double averageTimePerReconciliation;  // Minutes
        private BigDecimal precisionScore;            // Score de précision

        private String performanceLevel;              // EXCELLENT, GOOD, AVERAGE, NEEDS_IMPROVEMENT
        private int ranking;                          // Classement dans l'équipe
        private BigDecimal productivityIndex;         // Index de productivité (0-100)
    }

    /**
     * Métriques d'équipe agrégées
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamMetrics {
        private long totalUsers;
        private long activeUsers;                     // Utilisateurs actifs dans la période

        private long totalReconciliations;
        private long totalTransactions;

        private BigDecimal averageApplicationRate;
        private Double averageTimePerReconciliation;
        private BigDecimal teamPrecisionScore;

        private String bestPerformer;                 // Nom du meilleur utilisateur
        private BigDecimal bestPerformerScore;
    }
}