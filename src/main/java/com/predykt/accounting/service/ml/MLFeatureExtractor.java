package com.predykt.accounting.service.ml;

import com.predykt.accounting.domain.entity.BankTransaction;
import com.predykt.accounting.domain.entity.GeneralLedger;
import com.predykt.accounting.dto.ml.MatchFeatures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Extracteur de features ML pour matching bancaire
 * Convertit (BankTransaction, GeneralLedger) → MatchFeatures
 *
 * Features extraites:
 * 1. amount_difference: Différence de montant absolue
 * 2. date_diff_days: Nombre de jours entre les dates
 * 3. text_similarity: Similarité des descriptions (Jaccard)
 * 4. amount_ratio: Ratio BT/GL
 * 5-12. Features binaires et catégorielles
 *
 * @author PREDYKT ML Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MLFeatureExtractor {

    /**
     * Extrait toutes les features depuis BT et GL
     */
    public MatchFeatures extract(BankTransaction bt, GeneralLedger gl) {
        MatchFeatures features = MatchFeatures.builder()
            .amountDifference(calculateAmountDifference(bt, gl))
            .dateDiffDays(calculateDateDiff(bt, gl))
            .textSimilarity(calculateTextSimilarity(bt.getDescription(), gl.getDescription()))
            .amountRatio(calculateAmountRatio(bt, gl))
            .sameSense(sameSense(bt, gl) ? 1.0 : 0.0)
            .referenceMatch(referenceMatch(bt, gl) ? 1.0 : 0.0)
            .isRoundNumber(isRoundNumber(bt.getAmount()) ? 1.0 : 0.0)
            .isMonthEnd(isMonthEnd(bt.getTransactionDate()) ? 1.0 : 0.0)
            .dayOfWeekBT((double) bt.getTransactionDate().getDayOfWeek().getValue())
            .dayOfWeekGL((double) gl.getEntryDate().getDayOfWeek().getValue())
            .historicalMatchRate(0.5)  // TODO: Calculer depuis historique
            .avgDaysHistorical(30.0)   // TODO: Calculer délai moyen
            .build();

        return features;
    }

    /**
     * 1. Calcule la différence de montant (valeur absolue)
     */
    private Double calculateAmountDifference(BankTransaction bt, GeneralLedger gl) {
        BigDecimal btAmount = bt.getAmount().abs();
        BigDecimal glAmount = gl.getDebitAmount().subtract(gl.getCreditAmount()).abs();
        return btAmount.subtract(glAmount).abs().doubleValue();
    }

    /**
     * 2. Calcule la différence de jours entre les dates
     */
    private Long calculateDateDiff(BankTransaction bt, GeneralLedger gl) {
        return Math.abs(ChronoUnit.DAYS.between(
            bt.getTransactionDate(),
            gl.getEntryDate()
        ));
    }

    /**
     * 3. Calcule la similarité textuelle (Jaccard + Contains)
     */
    private Double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;

        text1 = normalize(text1);
        text2 = normalize(text2);

        // Cas trivial: égalité
        if (text1.equals(text2)) return 1.0;

        // Cas simple: contenance (bonus 80%)
        if (text1.contains(text2) || text2.contains(text1)) {
            return 0.8;
        }

        // Similarité de Jaccard (mots communs)
        String[] words1 = text1.split("\\s+");
        String[] words2 = text2.split("\\s+");

        Set<String> set1 = new HashSet<>(Arrays.asList(words1));
        Set<String> set2 = new HashSet<>(Arrays.asList(words2));

        // Intersection
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        // Union
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) return 0.0;

        return (double) intersection.size() / union.size();
    }

    /**
     * 4. Calcule le ratio entre les montants BT/GL
     */
    private Double calculateAmountRatio(BankTransaction bt, GeneralLedger gl) {
        BigDecimal btAmount = bt.getAmount().abs();
        BigDecimal glAmount = gl.getDebitAmount().subtract(gl.getCreditAmount()).abs();

        if (glAmount.compareTo(BigDecimal.ZERO) == 0) return 0.0;

        return btAmount.divide(glAmount, 4, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 5. Vérifie si même sens débit/crédit
     */
    private boolean sameSense(BankTransaction bt, GeneralLedger gl) {
        boolean btIsDebit = bt.getAmount().compareTo(BigDecimal.ZERO) > 0;
        boolean glIsDebit = gl.getDebitAmount().compareTo(BigDecimal.ZERO) > 0;
        return btIsDebit == glIsDebit;
    }

    /**
     * 6. Vérifie si références identiques
     */
    private boolean referenceMatch(BankTransaction bt, GeneralLedger gl) {
        if (bt.getReference() == null || gl.getReference() == null) {
            return false;
        }
        return normalize(bt.getReference()).equals(normalize(gl.getReference()));
    }

    /**
     * 7. Vérifie si montant rond (divisible par 1000)
     */
    private boolean isRoundNumber(BigDecimal amount) {
        BigDecimal abs = amount.abs();
        BigDecimal[] divideAndRemainder = abs.divideAndRemainder(new BigDecimal("1000"));
        return divideAndRemainder[1].compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * 8. Vérifie si fin de mois (jour >= 28)
     */
    private boolean isMonthEnd(LocalDate date) {
        return date.getDayOfMonth() >= 28;
    }

    /**
     * Normalise une chaîne (lowercase, trim, supprime accents)
     */
    private String normalize(String text) {
        if (text == null) return "";
        return text.toLowerCase().trim()
            .replaceAll("[àáâãäå]", "a")
            .replaceAll("[èéêë]", "e")
            .replaceAll("[ìíîï]", "i")
            .replaceAll("[òóôõö]", "o")
            .replaceAll("[ùúûü]", "u")
            .replaceAll("[ç]", "c")
            .replaceAll("[^a-z0-9\\s]", "");  // Supprime ponctuation
    }

    /**
     * Extrait plusieurs paires en batch (optimisation)
     */
    public java.util.List<MatchFeatures> extractBatch(
        java.util.List<BankTransaction> btList,
        java.util.List<GeneralLedger> glList
    ) {
        java.util.List<MatchFeatures> results = new java.util.ArrayList<>();

        for (BankTransaction bt : btList) {
            for (GeneralLedger gl : glList) {
                try {
                    results.add(extract(bt, gl));
                } catch (Exception e) {
                    log.warn("Erreur extraction features BT {} - GL {}: {}",
                        bt.getId(), gl.getId(), e.getMessage());
                }
            }
        }

        return results;
    }
}