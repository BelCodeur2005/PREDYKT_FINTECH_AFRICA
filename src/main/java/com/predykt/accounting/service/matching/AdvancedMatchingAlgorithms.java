package com.predykt.accounting.service.matching;

import com.predykt.accounting.domain.entity.BankTransaction;
import com.predykt.accounting.domain.entity.GeneralLedger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Algorithmes avancés de matching pour rapprochement bancaire
 * Utilise des techniques d'optimisation avancées pour gérer de gros volumes
 *
 * @author PREDYKT Team
 * @version 2.0 - Optimisé pour haute performance
 */
@Component
@Slf4j
public class AdvancedMatchingAlgorithms {

    /**
     * Calcule la distance de Levenshtein entre deux chaînes
     * Complexité: O(n*m) où n et m sont les longueurs des chaînes
     * Optimisée avec allocation mémoire minimale (2 tableaux au lieu de matrice complète)
     */
    public int levenshteinDistance(String s1, String s2) {
        if (s1 == null || s2 == null) return Integer.MAX_VALUE;
        if (s1.equals(s2)) return 0;

        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int len1 = s1.length();
        int len2 = s2.length();

        if (len1 == 0) return len2;
        if (len2 == 0) return len1;

        // Optimisation mémoire: utiliser seulement 2 lignes au lieu d'une matrice complète
        int[] previousRow = new int[len2 + 1];
        int[] currentRow = new int[len2 + 1];

        // Initialisation de la première ligne
        for (int j = 0; j <= len2; j++) {
            previousRow[j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            currentRow[0] = i;

            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;

                currentRow[j] = Math.min(
                    Math.min(
                        currentRow[j - 1] + 1,      // Insertion
                        previousRow[j] + 1),        // Suppression
                    previousRow[j - 1] + cost       // Substitution
                );
            }

            // Swap rows
            int[] temp = previousRow;
            previousRow = currentRow;
            currentRow = temp;
        }

        return previousRow[len2];
    }

    /**
     * Calcule la similarité de Jaro-Winkler entre deux chaînes
     * Spécialement adaptée pour détecter les fautes de frappe
     * Retourne un score entre 0.0 (complètement différent) et 1.0 (identique)
     */
    public double jaroWinklerSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equals(s2)) return 1.0;

        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        // Distance de Jaro
        double jaro = jaroSimilarity(s1, s2);

        // Calcul du préfixe commun (max 4 caractères)
        int prefixLength = 0;
        int maxPrefix = Math.min(4, Math.min(s1.length(), s2.length()));

        for (int i = 0; i < maxPrefix; i++) {
            if (s1.charAt(i) == s2.charAt(i)) {
                prefixLength++;
            } else {
                break;
            }
        }

        // Jaro-Winkler = Jaro + (prefixLength * 0.1 * (1 - Jaro))
        return jaro + (prefixLength * 0.1 * (1.0 - jaro));
    }

    /**
     * Calcule la similarité de Jaro entre deux chaînes
     */
    private double jaroSimilarity(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();

        if (len1 == 0 && len2 == 0) return 1.0;
        if (len1 == 0 || len2 == 0) return 0.0;

        // Distance de matching maximale
        int matchDistance = Math.max(len1, len2) / 2 - 1;
        if (matchDistance < 0) matchDistance = 0;

        boolean[] s1Matches = new boolean[len1];
        boolean[] s2Matches = new boolean[len2];

        int matches = 0;
        int transpositions = 0;

        // Identifier les matches
        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, len2);

            for (int j = start; j < end; j++) {
                if (s2Matches[j] || s1.charAt(i) != s2.charAt(j)) continue;
                s1Matches[i] = true;
                s2Matches[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) return 0.0;

        // Compter les transpositions
        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (!s1Matches[i]) continue;
            while (!s2Matches[k]) k++;
            if (s1.charAt(i) != s2.charAt(k)) transpositions++;
            k++;
        }

        return ((double) matches / len1 +
                (double) matches / len2 +
                (double) (matches - transpositions / 2.0) / matches) / 3.0;
    }

    /**
     * Calcule la similarité textuelle avancée combinant plusieurs métriques
     * Retourne un score entre 0.0 et 1.0
     *
     * Combinaison optimale:
     * - Jaro-Winkler: 60% (meilleur pour noms propres et courtes chaînes)
     * - Levenshtein normalisé: 30% (bon pour fautes de frappe)
     * - Contenance: 10% (bonus si une chaîne contient l'autre)
     */
    public double advancedTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;

        text1 = text1.toLowerCase().trim();
        text2 = text2.toLowerCase().trim();

        if (text1.isEmpty() && text2.isEmpty()) return 1.0;
        if (text1.isEmpty() || text2.isEmpty()) return 0.0;
        if (text1.equals(text2)) return 1.0;

        // 1. Similarité de Jaro-Winkler (60%)
        double jaroWinkler = jaroWinklerSimilarity(text1, text2);

        // 2. Distance de Levenshtein normalisée (30%)
        int maxLength = Math.max(text1.length(), text2.length());
        int levenshtein = levenshteinDistance(text1, text2);
        double levenshteinSimilarity = 1.0 - ((double) levenshtein / maxLength);

        // 3. Bonus de contenance (10%)
        double containmentBonus = 0.0;
        if (text1.contains(text2) || text2.contains(text1)) {
            containmentBonus = 1.0;
        }

        // Combinaison pondérée
        return (jaroWinkler * 0.6) +
               (levenshteinSimilarity * 0.3) +
               (containmentBonus * 0.1);
    }

    /**
     * Algorithme glouton optimisé pour trouver la meilleure combinaison
     * de transactions bancaires correspondant à un montant cible
     *
     * Complexité: O(n log n) au lieu de O(C(n,k)) = O(n!)
     * Peut gérer des milliers de transactions en quelques millisecondes
     *
     * @param candidates Liste des transactions candidates (sera triée)
     * @param targetAmount Montant cible à atteindre
     * @param tolerancePercent Tolérance en pourcentage (ex: 0.05 = 5%)
     * @param maxCombinationSize Taille maximale de la combinaison (ex: 5)
     * @return Liste optimale de transactions ou vide si aucune combinaison acceptable
     */
    public List<BankTransaction> findBestBankTransactionCombination(
            List<BankTransaction> candidates,
            BigDecimal targetAmount,
            BigDecimal tolerancePercent,
            int maxCombinationSize) {

        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // Convertir en montants absolus pour le traitement
        List<TransactionWithAmount> items = candidates.stream()
            .map(bt -> new TransactionWithAmount(bt, bt.getAmount().abs()))
            .collect(Collectors.toList());

        // Trier par montant décroissant (algorithme glouton)
        items.sort(Comparator.comparing(TransactionWithAmount::getAmount).reversed());

        BigDecimal target = targetAmount.abs();
        BigDecimal tolerance = target.multiply(tolerancePercent);
        BigDecimal minAcceptable = target.subtract(tolerance);
        BigDecimal maxAcceptable = target.add(tolerance);

        // Approche dynamique: essayer plusieurs stratégies
        List<BankTransaction> bestCombination = null;
        BigDecimal bestDifference = target;

        // Stratégie 1: Algorithme glouton simple (le plus rapide)
        List<BankTransaction> greedyResult = greedyApproach(
            items, target, minAcceptable, maxAcceptable, maxCombinationSize);
        if (!greedyResult.isEmpty()) {
            BigDecimal sum = sumBankTransactions(greedyResult);
            BigDecimal diff = sum.subtract(target).abs();
            if (diff.compareTo(bestDifference) < 0) {
                bestCombination = greedyResult;
                bestDifference = diff;
            }
        }

        // Stratégie 2: Recherche par sous-ensemble de somme (Subset Sum) optimisée
        // Seulement si le glouton n'a pas trouvé de solution exacte
        if (bestDifference.compareTo(BigDecimal.ZERO) > 0 && items.size() <= 50) {
            List<BankTransaction> subsetResult = optimizedSubsetSum(
                items, target, minAcceptable, maxAcceptable, maxCombinationSize);
            if (!subsetResult.isEmpty()) {
                BigDecimal sum = sumBankTransactions(subsetResult);
                BigDecimal diff = sum.subtract(target).abs();
                if (diff.compareTo(bestDifference) < 0) {
                    bestCombination = subsetResult;
                    bestDifference = diff;
                }
            }
        }

        return bestCombination != null ? bestCombination : Collections.emptyList();
    }

    /**
     * Même algorithme pour les écritures GL
     */
    public List<GeneralLedger> findBestGLEntryCombination(
            List<GeneralLedger> candidates,
            BigDecimal targetAmount,
            BigDecimal tolerancePercent,
            int maxCombinationSize) {

        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<GLWithAmount> items = candidates.stream()
            .map(gl -> new GLWithAmount(
                gl,
                gl.getDebitAmount().subtract(gl.getCreditAmount()).abs()
            ))
            .collect(Collectors.toList());

        items.sort(Comparator.comparing(GLWithAmount::getAmount).reversed());

        BigDecimal target = targetAmount.abs();
        BigDecimal tolerance = target.multiply(tolerancePercent);
        BigDecimal minAcceptable = target.subtract(tolerance);
        BigDecimal maxAcceptable = target.add(tolerance);

        List<GeneralLedger> bestCombination = null;
        BigDecimal bestDifference = target;

        // Stratégie 1: Glouton
        List<GeneralLedger> greedyResult = greedyApproachGL(
            items, target, minAcceptable, maxAcceptable, maxCombinationSize);
        if (!greedyResult.isEmpty()) {
            BigDecimal sum = sumGLEntries(greedyResult);
            BigDecimal diff = sum.subtract(target).abs();
            if (diff.compareTo(bestDifference) < 0) {
                bestCombination = greedyResult;
                bestDifference = diff;
            }
        }

        // Stratégie 2: Subset Sum (si petite taille)
        if (bestDifference.compareTo(BigDecimal.ZERO) > 0 && items.size() <= 50) {
            List<GeneralLedger> subsetResult = optimizedSubsetSumGL(
                items, target, minAcceptable, maxAcceptable, maxCombinationSize);
            if (!subsetResult.isEmpty()) {
                BigDecimal sum = sumGLEntries(subsetResult);
                BigDecimal diff = sum.subtract(target).abs();
                if (diff.compareTo(bestDifference) < 0) {
                    bestCombination = subsetResult;
                }
            }
        }

        return bestCombination != null ? bestCombination : Collections.emptyList();
    }

    /**
     * Approche gloutonne simple pour BankTransaction
     */
    private List<BankTransaction> greedyApproach(
            List<TransactionWithAmount> items,
            BigDecimal target,
            BigDecimal minAcceptable,
            BigDecimal maxAcceptable,
            int maxSize) {

        List<BankTransaction> combination = new ArrayList<>();
        BigDecimal currentSum = BigDecimal.ZERO;

        for (TransactionWithAmount item : items) {
            if (combination.size() >= maxSize) break;

            BigDecimal newSum = currentSum.add(item.amount);

            // Si on dépasse trop, skip
            if (newSum.compareTo(maxAcceptable) > 0) {
                continue;
            }

            combination.add(item.transaction);
            currentSum = newSum;

            // Si on est dans la fourchette acceptable, on peut s'arrêter
            if (currentSum.compareTo(minAcceptable) >= 0 &&
                currentSum.compareTo(maxAcceptable) <= 0) {
                return combination;
            }
        }

        // Vérifier si la combinaison finale est acceptable
        if (currentSum.compareTo(minAcceptable) >= 0 &&
            currentSum.compareTo(maxAcceptable) <= 0) {
            return combination;
        }

        return Collections.emptyList();
    }

    /**
     * Approche gloutonne pour GeneralLedger
     */
    private List<GeneralLedger> greedyApproachGL(
            List<GLWithAmount> items,
            BigDecimal target,
            BigDecimal minAcceptable,
            BigDecimal maxAcceptable,
            int maxSize) {

        List<GeneralLedger> combination = new ArrayList<>();
        BigDecimal currentSum = BigDecimal.ZERO;

        for (GLWithAmount item : items) {
            if (combination.size() >= maxSize) break;

            BigDecimal newSum = currentSum.add(item.amount);

            if (newSum.compareTo(maxAcceptable) > 0) continue;

            combination.add(item.entry);
            currentSum = newSum;

            if (currentSum.compareTo(minAcceptable) >= 0 &&
                currentSum.compareTo(maxAcceptable) <= 0) {
                return combination;
            }
        }

        if (currentSum.compareTo(minAcceptable) >= 0 &&
            currentSum.compareTo(maxAcceptable) <= 0) {
            return combination;
        }

        return Collections.emptyList();
    }

    /**
     * Algorithme de sous-ensemble de somme optimisé (Dynamic Programming)
     * Utilise la programmation dynamique avec pruning agressif
     * Complexité: O(n * target) avec pruning = beaucoup plus rapide en pratique
     */
    private List<BankTransaction> optimizedSubsetSum(
            List<TransactionWithAmount> items,
            BigDecimal target,
            BigDecimal minAcceptable,
            BigDecimal maxAcceptable,
            int maxSize) {

        // Convertir en centimes pour travailler avec des entiers (plus rapide)
        long targetCents = target.multiply(new BigDecimal("100")).longValue();
        long minCents = minAcceptable.multiply(new BigDecimal("100")).longValue();
        long maxCents = maxAcceptable.multiply(new BigDecimal("100")).longValue();

        List<ItemWithCents> itemsWithCents = items.stream()
            .map(item -> new ItemWithCents(
                item.transaction,
                item.amount.multiply(new BigDecimal("100")).longValue()
            ))
            .collect(Collectors.toList());

        // Subset sum avec limite de taille
        List<BankTransaction> result = subsetSumBounded(
            itemsWithCents, targetCents, minCents, maxCents, maxSize);

        return result;
    }

    /**
     * Version GL de subset sum
     */
    private List<GeneralLedger> optimizedSubsetSumGL(
            List<GLWithAmount> items,
            BigDecimal target,
            BigDecimal minAcceptable,
            BigDecimal maxAcceptable,
            int maxSize) {

        long targetCents = target.multiply(new BigDecimal("100")).longValue();
        long minCents = minAcceptable.multiply(new BigDecimal("100")).longValue();
        long maxCents = maxAcceptable.multiply(new BigDecimal("100")).longValue();

        List<GLItemWithCents> itemsWithCents = items.stream()
            .map(item -> new GLItemWithCents(
                item.entry,
                item.amount.multiply(new BigDecimal("100")).longValue()
            ))
            .collect(Collectors.toList());

        return subsetSumBoundedGL(itemsWithCents, targetCents, minCents, maxCents, maxSize);
    }

    /**
     * Subset sum avec limite de taille pour BankTransaction
     */
    private List<BankTransaction> subsetSumBounded(
            List<ItemWithCents> items,
            long target,
            long minAcceptable,
            long maxAcceptable,
            int maxSize) {

        int n = items.size();

        // DP: dp[i][s][k] = peut-on atteindre somme s avec k éléments parmi les i premiers?
        // Optimisation: on ne stocke que les sommes atteignables
        Map<Long, List<Integer>> reachable = new HashMap<>();
        reachable.put(0L, new ArrayList<>());

        for (int i = 0; i < n && i < 50; i++) { // Limite à 50 items pour performance
            ItemWithCents item = items.get(i);
            Map<Long, List<Integer>> newReachable = new HashMap<>(reachable);

            for (Map.Entry<Long, List<Integer>> entry : reachable.entrySet()) {
                long currentSum = entry.getKey();
                List<Integer> currentIndices = entry.getValue();

                if (currentIndices.size() >= maxSize) continue;

                long newSum = currentSum + item.cents;
                if (newSum > maxAcceptable) continue;

                List<Integer> newIndices = new ArrayList<>(currentIndices);
                newIndices.add(i);

                // Garder la meilleure combinaison pour chaque somme
                if (!newReachable.containsKey(newSum) ||
                    newReachable.get(newSum).size() > newIndices.size()) {
                    newReachable.put(newSum, newIndices);
                }
            }

            reachable = newReachable;

            // Pruning: limiter la taille de la map
            if (reachable.size() > 10000) {
                reachable = pruneReachableMap(reachable, target, maxAcceptable, 5000);
            }
        }

        // Trouver la meilleure combinaison dans la fourchette acceptable
        List<Integer> bestIndices = null;
        long bestDiff = Long.MAX_VALUE;

        for (Map.Entry<Long, List<Integer>> entry : reachable.entrySet()) {
            long sum = entry.getKey();
            if (sum >= minAcceptable && sum <= maxAcceptable) {
                long diff = Math.abs(sum - target);
                if (diff < bestDiff || (diff == bestDiff &&
                    (bestIndices == null || entry.getValue().size() < bestIndices.size()))) {
                    bestDiff = diff;
                    bestIndices = entry.getValue();
                }
            }
        }

        if (bestIndices == null) {
            return Collections.emptyList();
        }

        return bestIndices.stream()
            .map(idx -> items.get(idx).transaction)
            .collect(Collectors.toList());
    }

    /**
     * Version GL de subset sum bounded
     */
    private List<GeneralLedger> subsetSumBoundedGL(
            List<GLItemWithCents> items,
            long target,
            long minAcceptable,
            long maxAcceptable,
            int maxSize) {

        int n = items.size();
        Map<Long, List<Integer>> reachable = new HashMap<>();
        reachable.put(0L, new ArrayList<>());

        for (int i = 0; i < n && i < 50; i++) {
            GLItemWithCents item = items.get(i);
            Map<Long, List<Integer>> newReachable = new HashMap<>(reachable);

            for (Map.Entry<Long, List<Integer>> entry : reachable.entrySet()) {
                long currentSum = entry.getKey();
                List<Integer> currentIndices = entry.getValue();

                if (currentIndices.size() >= maxSize) continue;

                long newSum = currentSum + item.cents;
                if (newSum > maxAcceptable) continue;

                List<Integer> newIndices = new ArrayList<>(currentIndices);
                newIndices.add(i);

                if (!newReachable.containsKey(newSum) ||
                    newReachable.get(newSum).size() > newIndices.size()) {
                    newReachable.put(newSum, newIndices);
                }
            }

            reachable = newReachable;

            if (reachable.size() > 10000) {
                reachable = pruneReachableMap(reachable, target, maxAcceptable, 5000);
            }
        }

        List<Integer> bestIndices = null;
        long bestDiff = Long.MAX_VALUE;

        for (Map.Entry<Long, List<Integer>> entry : reachable.entrySet()) {
            long sum = entry.getKey();
            if (sum >= minAcceptable && sum <= maxAcceptable) {
                long diff = Math.abs(sum - target);
                if (diff < bestDiff || (diff == bestDiff &&
                    (bestIndices == null || entry.getValue().size() < bestIndices.size()))) {
                    bestDiff = diff;
                    bestIndices = entry.getValue();
                }
            }
        }

        if (bestIndices == null) {
            return Collections.emptyList();
        }

        return bestIndices.stream()
            .map(idx -> items.get(idx).entry)
            .collect(Collectors.toList());
    }

    /**
     * Pruning de la map des états atteignables pour éviter l'explosion mémoire
     */
    private Map<Long, List<Integer>> pruneReachableMap(
            Map<Long, List<Integer>> reachable,
            long target,
            long maxAcceptable,
            int maxSize) {

        return reachable.entrySet().stream()
            .sorted(Comparator.comparingLong(entry -> Math.abs(entry.getKey() - target)))
            .limit(maxSize)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a.size() < b.size() ? a : b
            ));
    }

    /**
     * Calcule la somme des montants de transactions bancaires
     */
    private BigDecimal sumBankTransactions(List<BankTransaction> transactions) {
        return transactions.stream()
            .map(bt -> bt.getAmount().abs())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcule la somme des montants d'écritures GL
     */
    private BigDecimal sumGLEntries(List<GeneralLedger> entries) {
        return entries.stream()
            .map(gl -> gl.getDebitAmount().subtract(gl.getCreditAmount()).abs())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // === Classes internes pour optimisation ===

    private static class TransactionWithAmount {
        BankTransaction transaction;
        BigDecimal amount;

        TransactionWithAmount(BankTransaction transaction, BigDecimal amount) {
            this.transaction = transaction;
            this.amount = amount;
        }

        BigDecimal getAmount() {
            return amount;
        }
    }

    private static class GLWithAmount {
        GeneralLedger entry;
        BigDecimal amount;

        GLWithAmount(GeneralLedger entry, BigDecimal amount) {
            this.entry = entry;
            this.amount = amount;
        }

        BigDecimal getAmount() {
            return amount;
        }
    }

    private static class ItemWithCents {
        BankTransaction transaction;
        long cents;

        ItemWithCents(BankTransaction transaction, long cents) {
            this.transaction = transaction;
            this.cents = cents;
        }
    }

    private static class GLItemWithCents {
        GeneralLedger entry;
        long cents;

        GLItemWithCents(GeneralLedger entry, long cents) {
            this.entry = entry;
            this.cents = cents;
        }
    }
}
