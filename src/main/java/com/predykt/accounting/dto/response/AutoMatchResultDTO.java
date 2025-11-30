package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Résultat du matching automatique pour un rapprochement bancaire
 * Contient toutes les suggestions trouvées par l'algorithme intelligent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoMatchResultDTO {

    /**
     * ID du rapprochement bancaire
     */
    private Long reconciliationId;

    /**
     * Date/heure de l'analyse
     */
    private LocalDateTime analyzedAt;

    /**
     * Statistiques globales
     */
    private MatchingStatistics statistics;

    /**
     * Liste de toutes les suggestions générées
     */
    @Builder.Default
    private List<MatchSuggestionDTO> suggestions = new ArrayList<>();

    /**
     * Transactions bancaires non réconciliées (sans correspondance trouvée)
     */
    @Builder.Default
    private List<UnmatchedTransactionDTO> unmatchedBankTransactions = new ArrayList<>();

    /**
     * Écritures comptables non réconciliées (sans correspondance trouvée)
     */
    @Builder.Default
    private List<UnmatchedTransactionDTO> unmatchedGLEntries = new ArrayList<>();

    /**
     * Messages d'information ou avertissements
     */
    @Builder.Default
    private List<String> messages = new ArrayList<>();

    /**
     * Indique si le matching automatique a pu équilibrer le rapprochement
     */
    private boolean isBalanced;

    /**
     * Écart résiduel après application de toutes les suggestions
     */
    private BigDecimal residualDifference;

    /**
     * Statistiques du matching
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchingStatistics {
        private int totalBankTransactions;
        private int totalGLEntries;
        private int exactMatches;
        private int probableMatches;
        private int possibleMatches;
        private int unmatchedBankTransactions;
        private int unmatchedGLEntries;
        private BigDecimal overallConfidenceScore;
        private int autoApprovedCount; // Suggestions avec confiance >= 95%
        private int manualReviewCount; // Suggestions nécessitant révision
    }

    /**
     * Transaction non réconciliée (bancaire ou comptable)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnmatchedTransactionDTO {
        private Long id;
        private String type; // "BANK" ou "GL"
        private LocalDateTime date;
        private BigDecimal amount;
        private String description;
        private String reference;
        private String reason; // Pourquoi aucune correspondance n'a été trouvée
    }
}
