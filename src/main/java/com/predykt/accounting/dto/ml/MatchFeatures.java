package com.predykt.accounting.dto.ml;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO représentant les features ML pour matching bancaire
 * Convertit (BankTransaction, GeneralLedger) → Features numériques
 *
 * @author PREDYKT ML Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchFeatures {

    // ==================== Features numériques ====================

    /**
     * Différence de montant en valeur absolue
     */
    private Double amountDifference;

    /**
     * Différence de jours entre les dates
     */
    private Long dateDiffDays;

    /**
     * Similarité textuelle des descriptions (0-1)
     */
    private Double textSimilarity;

    /**
     * Ratio entre les montants BT/GL
     */
    private Double amountRatio;

    // ==================== Features binaires (0/1) ====================

    /**
     * 1 si même sens débit/crédit, 0 sinon
     */
    private Double sameSense;

    /**
     * 1 si références identiques, 0 sinon
     */
    private Double referenceMatch;

    /**
     * 1 si montant rond (divisible par 1000), 0 sinon
     */
    private Double isRoundNumber;

    /**
     * 1 si fin de mois, 0 sinon
     */
    private Double isMonthEnd;

    // ==================== Features catégorielles ====================

    /**
     * Jour de la semaine BT (1-7)
     */
    private Double dayOfWeekBT;

    /**
     * Jour de la semaine GL (1-7)
     */
    private Double dayOfWeekGL;

    // ==================== Features historiques ====================

    /**
     * Taux de match historique entre ces comptes (0-1)
     */
    private Double historicalMatchRate;

    /**
     * Délai moyen historique en jours
     */
    private Double avgDaysHistorical;

    // ==================== Méthodes de conversion ====================

    /**
     * Convertit vers array pour Smile ML
     * L'ordre DOIT correspondre à getFeatureNames()
     */
    public double[] toArray() {
        return new double[] {
            nvl(amountDifference),
            nvl(dateDiffDays),
            nvl(textSimilarity),
            nvl(amountRatio),
            nvl(sameSense),
            nvl(referenceMatch),
            nvl(isRoundNumber),
            nvl(isMonthEnd),
            nvl(dayOfWeekBT),
            nvl(dayOfWeekGL),
            nvl(historicalMatchRate),
            nvl(avgDaysHistorical)
        };
    }

    /**
     * Noms des features (ordre DOIT correspondre à toArray())
     */
    public static String[] getFeatureNames() {
        return new String[] {
            "amount_difference",
            "date_diff_days",
            "text_similarity",
            "amount_ratio",
            "same_sense",
            "reference_match",
            "is_round_number",
            "is_month_end",
            "day_of_week_bt",
            "day_of_week_gl",
            "historical_match_rate",
            "avg_days_historical"
        };
    }

    /**
     * Convertit vers Map pour stockage JSON
     */
    public Map<String, Object> toMap() {
        String[] names = getFeatureNames();
        double[] values = toArray();
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < names.length; i++) {
            map.put(names[i], values[i]);
        }
        return map;
    }

    /**
     * Construit depuis Map JSON
     */
    public static MatchFeatures fromMap(Map<String, Object> map) {
        return MatchFeatures.builder()
            .amountDifference(getDouble(map, "amount_difference"))
            .dateDiffDays(getLong(map, "date_diff_days"))
            .textSimilarity(getDouble(map, "text_similarity"))
            .amountRatio(getDouble(map, "amount_ratio"))
            .sameSense(getDouble(map, "same_sense"))
            .referenceMatch(getDouble(map, "reference_match"))
            .isRoundNumber(getDouble(map, "is_round_number"))
            .isMonthEnd(getDouble(map, "is_month_end"))
            .dayOfWeekBT(getDouble(map, "day_of_week_bt"))
            .dayOfWeekGL(getDouble(map, "day_of_week_gl"))
            .historicalMatchRate(getDouble(map, "historical_match_rate"))
            .avgDaysHistorical(getDouble(map, "avg_days_historical"))
            .build();
    }

    // ==================== Helpers ====================

    private double nvl(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    private static Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    private static Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    /**
     * Normalise les features (min-max scaling)
     */
    public void normalize() {
        // Normaliser amount_difference (0-100000 → 0-1)
        if (amountDifference != null) {
            amountDifference = Math.min(amountDifference / 100000.0, 1.0);
        }

        // Normaliser date_diff_days (0-30 → 0-1)
        if (dateDiffDays != null) {
            dateDiffDays = Math.min(dateDiffDays / 30L, 30L);
        }

        // text_similarity déjà 0-1
        // Autres features déjà normalisées
    }
}