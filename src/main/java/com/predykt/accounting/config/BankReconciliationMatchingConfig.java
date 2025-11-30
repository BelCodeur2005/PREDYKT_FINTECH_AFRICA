package com.predykt.accounting.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Configuration externalisée pour l'algorithme de matching bancaire
 * Permet d'ajuster les seuils par tenant/entreprise sans recompiler
 *
 * VERSION 2.0 - Optimisée pour haute performance
 */
@Configuration
@ConfigurationProperties(prefix = "predykt.reconciliation.matching")
@Data
public class BankReconciliationMatchingConfig {

    /**
     * Scores de confiance pour le matching
     */
    private Scores scores = new Scores();

    /**
     * Seuils de différence de dates (en jours)
     */
    private DateThresholds dateThresholds = new DateThresholds();

    /**
     * Tolérance de montant (pourcentage) - DEPRECATED: utiliser amountTolerance
     */
    @Deprecated
    private BigDecimal amountTolerancePercent = new BigDecimal("0.05"); // 5%

    /**
     * Configuration avancée de tolérance de montant (contextuelle)
     */
    private AmountTolerance amountTolerance = new AmountTolerance();

    /**
     * Seuil d'auto-approbation (confiance minimale)
     */
    private BigDecimal autoApproveThreshold = new BigDecimal("95");

    /**
     * Configuration du matching multiple
     */
    private MultipleMatching multipleMatching = new MultipleMatching();

    /**
     * Configuration de performance (timeout, limites)
     */
    private Performance performance = new Performance();

    /**
     * Configuration de la similarité textuelle
     */
    private TextSimilarity textSimilarity = new TextSimilarity();

    @Data
    public static class Scores {
        /**
         * Score pour une correspondance exacte (montant + date identiques)
         */
        private int exactMatch = 100;

        /**
         * Score pour une bonne correspondance (montant exact, date proche)
         */
        private int goodMatch = 90;

        /**
         * Score pour une correspondance acceptable
         */
        private int fairMatch = 70;

        /**
         * Score pour une correspondance faible
         */
        private int lowMatch = 50;
    }

    @Data
    public static class DateThresholds {
        /**
         * Différence de jours pour une correspondance exacte
         */
        private long exactMatchDays = 0;

        /**
         * Différence de jours pour une bonne correspondance
         */
        private long goodMatchDays = 3;

        /**
         * Différence de jours pour une correspondance acceptable
         */
        private long fairMatchDays = 7;

        /**
         * Différence de jours pour une correspondance faible
         */
        private long lowMatchDays = 15;
    }

    @Data
    public static class MultipleMatching {
        /**
         * Activer/désactiver le matching multiple
         */
        private boolean enabled = true;

        /**
         * Nombre minimum de transactions pour un matching multiple
         */
        private int minTransactions = 2;

        /**
         * Nombre maximum de transactions pour un matching multiple
         */
        private int maxTransactions = 5;

        /**
         * Fenêtre temporelle maximale (en jours) pour regrouper les transactions
         */
        private long maxDateRangeDays = 7;

        /**
         * Score de confiance pour les matchings multiples
         * (généralement plus bas car nécessite révision manuelle)
         */
        private BigDecimal confidenceScore = new BigDecimal("75");
    }

    /**
     * Heuristiques pour la détection automatique
     */
    @Data
    public static class Heuristics {
        /**
         * Mots-clés pour détecter les virements
         */
        private String[] virementKeywords = {"virement", "vir ", "transfer"};

        /**
         * Mots-clés pour détecter les frais bancaires
         */
        private String[] feesKeywords = {"frais", "commission", "fees"};

        /**
         * Mots-clés pour détecter les intérêts
         */
        private String[] interestKeywords = {"intérêt", "interet", "interest"};

        /**
         * Mots-clés pour détecter les agios
         */
        private String[] agiosKeywords = {"agios", "interet debiteur", "overdraft"};

        /**
         * Mots-clés pour détecter les prélèvements
         */
        private String[] directDebitKeywords = {"prelevement", "prel ", "direct debit"};

        /**
         * Mots-clés pour détecter les chèques
         */
        private String[] chequeKeywords = {"chq", "cheque", "chèque", "check"};
    }

    private Heuristics heuristics = new Heuristics();

    /**
     * Configuration avancée de tolérance de montant (contextuelle)
     */
    @Data
    public static class AmountTolerance {
        /**
         * Tolérance pour petits/moyens montants (< seuil)
         */
        private BigDecimal smallAmountPercent = new BigDecimal("0.05"); // 5%

        /**
         * Tolérance pour gros montants (>= seuil)
         */
        private BigDecimal largeAmountPercent = new BigDecimal("0.01"); // 1%

        /**
         * Tolérance minimale absolue (en F CFA)
         * Même si 5% est petit, on accepte au minimum cet écart
         */
        private BigDecimal minimumAbsolute = new BigDecimal("500");

        /**
         * Tolérance maximale absolue (en F CFA)
         * Même si 1% est gros, on n'accepte pas plus que cet écart
         */
        private BigDecimal maximumAbsolute = new BigDecimal("10000");

        /**
         * Seuil définissant un "gros montant" (en F CFA)
         */
        private BigDecimal largeAmountThreshold = new BigDecimal("1000000"); // 1 million
    }

    /**
     * Configuration de performance et limites
     */
    @Data
    public static class Performance {
        /**
         * Timeout global pour l'analyse (en secondes)
         * Au-delà, l'analyse s'arrête avec résultats partiels
         */
        private long timeoutSeconds = 90; // 90 secondes

        /**
         * Nombre maximum de candidats à analyser pour matching multiple
         * Au-delà, on filtre pour garder les plus gros montants
         */
        private int maxCandidatesForMultipleMatching = 30;

        /**
         * Limite stricte d'items à analyser par phase
         * Protection contre l'explosion combinatoire
         */
        private int maxItemsPerPhase = 200;

        /**
         * Activer le mode haute performance (sacrifie un peu de précision)
         * En mode HP: utilise uniquement l'algorithme glouton (pas subset sum)
         */
        private boolean highPerformanceMode = false;

        /**
         * Nombre maximum d'états atteignables dans subset sum
         * Limite la consommation mémoire
         */
        private int maxSubsetSumStates = 5000;
    }

    /**
     * Configuration de la similarité textuelle
     */
    @Data
    public static class TextSimilarity {
        /**
         * Algorithme à utiliser: JACCARD, LEVENSHTEIN, JARO_WINKLER, ADVANCED
         */
        private SimilarityAlgorithm algorithm = SimilarityAlgorithm.ADVANCED;

        /**
         * Seuil de similarité pour bonus de points (0.0 à 1.0)
         */
        private double threshold = 0.70; // 70%

        /**
         * Poids de la similarité textuelle dans le score total
         * Valeur par défaut: 5 points
         */
        private int weight = 5;

        /**
         * Normaliser les textes (enlever accents, ponctuation, etc.)
         */
        private boolean normalize = true;

        public enum SimilarityAlgorithm {
            /**
             * Jaccard (mots communs) - Rapide mais basique
             */
            JACCARD,

            /**
             * Distance de Levenshtein - Bon pour fautes de frappe
             */
            LEVENSHTEIN,

            /**
             * Jaro-Winkler - Excellent pour noms propres
             */
            JARO_WINKLER,

            /**
             * Combinaison optimale (Jaro-Winkler 60% + Levenshtein 30% + Contenance 10%)
             * Recommandé pour meilleure précision
             */
            ADVANCED
        }
    }
}
