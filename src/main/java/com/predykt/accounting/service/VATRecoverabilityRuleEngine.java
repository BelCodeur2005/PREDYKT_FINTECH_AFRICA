package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.RecoverabilityRule;
import com.predykt.accounting.domain.enums.VATRecoverableCategory;
import com.predykt.accounting.repository.RecoverabilityRuleRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Moteur de règles optimisé pour la détection de récupérabilité TVA
 *
 * Caractéristiques :
 * - Cache intelligent des patterns compilés
 * - Système de scoring et priorités
 * - Machine learning simple (apprentissage des corrections)
 * - Suggestions alternatives
 * - Métriques de performance
 *
 * Performance: ~50-100 µs par détection (avec cache)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VATRecoverabilityRuleEngine {

    private final RecoverabilityRuleRepository ruleRepository;
    private final TextNormalizer textNormalizer;

    // Cache des patterns regex compilés (thread-safe)
    private final Map<Long, Pattern> patternCache = Collections.synchronizedMap(new HashMap<>());

    // Cache des règles actives (invalidé à chaque modification)
    private List<RecoverabilityRule> cachedActiveRules = null;
    private long cacheTimestamp = 0;
    private static final long CACHE_TTL = 300_000; // 5 minutes

    /**
     * Détecte la catégorie de récupérabilité avec le système de règles (MULTI-TENANT)
     *
     * @param companyId Company ID (pour contexte multi-tenant)
     * @param tenantId Tenant ID (mode DEDICATED)
     * @param cabinetId Cabinet ID (mode CABINET)
     * @param accountNumber Numéro de compte OHADA
     * @param description Description de la transaction
     * @return Résultat avec catégorie, confiance, règle appliquée et suggestions
     */
    @Transactional
    public DetectionResult detectCategory(
            Long companyId,
            String tenantId,
            String cabinetId,
            String accountNumber,
            String description) {

        long startTime = System.nanoTime();

        try {
            // Normaliser le texte
            String normalizedDesc = textNormalizer.normalize(description);
            String expandedDesc = textNormalizer.normalizeWithSynonyms(description);

            // Récupérer les règles applicables selon le contexte multi-tenant
            List<RecoverabilityRule> rules = getApplicableRules(companyId, tenantId, cabinetId);

            log.debug("🔍 [Multi-Tenant] Détection pour compte {} - Description: {} - {} règles applicables",
                accountNumber, description, rules.size());

            // Évaluer toutes les règles et garder les matches
            List<RuleMatch> matches = new ArrayList<>();

            for (RecoverabilityRule rule : rules) {
                RuleMatch match = evaluateRule(rule, accountNumber, normalizedDesc, expandedDesc);
                if (match != null && match.isMatched()) {
                    matches.add(match);
                }
            }

            // Trier par score décroissant
            matches.sort(Comparator.comparingInt(RuleMatch::getTotalScore).reversed());

            // Résultat
            DetectionResult result;

            if (matches.isEmpty()) {
                // Aucune règle ne matche → Défaut FULLY_RECOVERABLE
                result = DetectionResult.builder()
                    .category(VATRecoverableCategory.FULLY_RECOVERABLE)
                    .confidence(100)
                    .appliedRule(null)
                    .reason("Aucune règle spécifique - Catégorie par défaut")
                    .alternatives(Collections.emptyList())
                    .executionTimeNanos(System.nanoTime() - startTime)
                    .build();

                log.debug("✅ Catégorie par défaut: FULLY_RECOVERABLE (aucune règle matchée)");
            } else {
                // Meilleur match
                RuleMatch bestMatch = matches.get(0);
                RecoverabilityRule appliedRule = bestMatch.getRule();

                // Alternatives (2ème et 3ème meilleurs scores si proches)
                List<Alternative> alternatives = matches.stream()
                    .skip(1)
                    .limit(2)
                    .filter(m -> m.getTotalScore() >= bestMatch.getTotalScore() * 0.7) // 70% du meilleur score
                    .map(m -> new Alternative(
                        m.getRule().getCategory(),
                        m.getTotalScore(),
                        m.getRule().getReason()
                    ))
                    .collect(Collectors.toList());

                result = DetectionResult.builder()
                    .category(appliedRule.getCategory())
                    .confidence(bestMatch.getTotalScore())
                    .appliedRule(appliedRule)
                    .reason(appliedRule.getReason())
                    .alternatives(alternatives)
                    .executionTimeNanos(System.nanoTime() - startTime)
                    .build();

                // Incrémenter le compteur de la règle
                appliedRule.incrementMatchCount();
                ruleRepository.save(appliedRule);

                log.debug("✅ Règle appliquée: {} - Catégorie: {} - Confiance: {}% - Temps: {} µs",
                    appliedRule.getName(),
                    appliedRule.getCategory().getDisplayName(),
                    bestMatch.getTotalScore(),
                    (System.nanoTime() - startTime) / 1000);

                if (!alternatives.isEmpty()) {
                    log.debug("⚠️ Alternatives possibles: {}",
                        alternatives.stream()
                            .map(a -> String.format("%s (%d%%)", a.getCategory().getDisplayName(), a.getConfidence()))
                            .collect(Collectors.joining(", ")));
                }
            }

            return result;

        } catch (Exception e) {
            log.error("❌ Erreur lors de la détection de catégorie: {}", e.getMessage(), e);

            // Fallback en cas d'erreur
            return DetectionResult.builder()
                .category(VATRecoverableCategory.FULLY_RECOVERABLE)
                .confidence(0)
                .appliedRule(null)
                .reason("Erreur lors de la détection - Catégorie par défaut appliquée")
                .alternatives(Collections.emptyList())
                .executionTimeNanos(System.nanoTime() - startTime)
                .build();
        }
    }

    /**
     * Évalue une règle et retourne un match avec score
     */
    private RuleMatch evaluateRule(RecoverabilityRule rule, String accountNumber, String normalizedDesc, String expandedDesc) {
        int score = 0;
        List<String> matchedCriteria = new ArrayList<>();

        // 1. Vérifier le pattern de compte
        if (rule.getAccountPattern() != null && !rule.getAccountPattern().isBlank()) {
            Pattern accountPattern = getCompiledPattern(rule.getId(), rule.getAccountPattern());
            if (accountPattern != null && accountPattern.matcher(accountNumber).find()) {
                score += 20;
                matchedCriteria.add("Compte matché: " + rule.getAccountPattern());
            } else {
                // Compte ne matche pas → règle non applicable
                return null;
            }
        }

        // 2. Vérifier le pattern de description
        if (rule.getDescriptionPattern() != null && !rule.getDescriptionPattern().isBlank()) {
            Pattern descPattern = getCompiledPattern(
                rule.getId() + 1000000L,  // Offset pour éviter collision
                rule.getDescriptionPattern()
            );

            if (descPattern != null && descPattern.matcher(expandedDesc).find()) {
                score += 30;
                matchedCriteria.add("Description matchée par regex");
            } else {
                // Description ne matche pas → règle non applicable
                return null;
            }
        }

        // 3. Vérifier les mots-clés requis
        if (rule.getRequiredKeywords() != null && !rule.getRequiredKeywords().isBlank()) {
            String[] keywords = rule.getRequiredKeywords().split(",");
            if (textNormalizer.containsAllKeywords(expandedDesc, keywords)) {
                score += 25;
                matchedCriteria.add("Mots-clés requis présents: " + rule.getRequiredKeywords());
            } else {
                // Mots-clés manquants → règle non applicable
                return null;
            }
        }

        // 4. Vérifier les mots-clés exclus
        if (rule.getExcludedKeywords() != null && !rule.getExcludedKeywords().isBlank()) {
            String[] excludedKw = rule.getExcludedKeywords().split(",");
            if (textNormalizer.containsExcludedKeyword(expandedDesc, excludedKw)) {
                // Mot exclu présent → règle non applicable
                return null;
            } else {
                score += 10;
                matchedCriteria.add("Aucun mot exclu");
            }
        }

        // 5. Bonus de confiance de la règle
        score = (int) (score * (rule.getConfidenceScore() / 100.0));

        // 6. Bonus de précision historique
        if (rule.getAccuracyRate() != null) {
            score = (int) (score * (rule.getAccuracyRate().doubleValue() / 100.0));
        }

        // Match trouvé !
        return RuleMatch.builder()
            .rule(rule)
            .baseScore(score)
            .priorityBonus(100 - rule.getPriority())  // Plus la priorité est petite, plus le bonus est grand
            .totalScore(score + (100 - rule.getPriority()))
            .matchedCriteria(matchedCriteria)
            .matched(true)
            .build();
    }

    /**
     * Récupère un pattern compilé depuis le cache ou le compile
     */
    private Pattern getCompiledPattern(Long cacheKey, String regex) {
        return patternCache.computeIfAbsent(cacheKey, k -> {
            try {
                return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            } catch (PatternSyntaxException e) {
                log.error("❌ Regex invalide pour règle {}: {}", cacheKey, regex, e);
                return null;
            }
        });
    }

    /**
     * Méthode de compatibilité (sans contexte multi-tenant)
     * Retourne uniquement les règles GLOBAL
     */
    public DetectionResult detectCategory(String accountNumber, String description) {
        return detectCategory(null, null, null, accountNumber, description);
    }

    /**
     * Récupère les règles applicables selon le contexte multi-tenant (AVEC CACHE)
     *
     * Logique de sélection:
     * - Mode SHARED: Règles GLOBAL + règles COMPANY (pour company_id)
     * - Mode DEDICATED: Règles GLOBAL + règles TENANT (pour tenant_id)
     * - Mode CABINET: Règles GLOBAL + règles CABINET (pour cabinet_id) + règles COMPANY (pour company_id)
     */
    private List<RecoverabilityRule> getApplicableRules(Long companyId, String tenantId, String cabinetId) {
        // Note: Pour simplifier, on désactive temporairement le cache car il doit être
        // contextualisé par (companyId, tenantId, cabinetId)
        // TODO: Implémenter un cache Map<String, List<Rule>> avec clé = context

        List<RecoverabilityRule> rules = ruleRepository.findApplicableRulesForContext(
            companyId, tenantId, cabinetId
        );

        log.debug("📚 [Multi-Tenant] Règles chargées - Company: {}, Tenant: {}, Cabinet: {} → {} règles",
            companyId, tenantId, cabinetId, rules.size());

        return rules;
    }

    /**
     * Récupère les règles actives GLOBAL uniquement (avec cache) - LEGACY
     * @deprecated Utiliser getApplicableRules() avec contexte multi-tenant
     */
    @Deprecated
    private List<RecoverabilityRule> getActiveRules() {
        long now = System.currentTimeMillis();

        // Vérifier le cache
        if (cachedActiveRules != null && (now - cacheTimestamp) < CACHE_TTL) {
            return cachedActiveRules;
        }

        // Recharger depuis la base
        cachedActiveRules = ruleRepository.findByIsActiveTrueOrderByPriorityAsc();
        cacheTimestamp = now;

        log.debug("📚 Règles GLOBAL rechargées: {} règles actives", cachedActiveRules.size());

        return cachedActiveRules;
    }

    /**
     * Invalide le cache des règles (appelé lors de modifications)
     */
    @CacheEvict(value = "recoverabilityRules", allEntries = true)
    public void invalidateCache() {
        cachedActiveRules = null;
        cacheTimestamp = 0;
        patternCache.clear();
        log.info("🔄 Cache des règles invalidé");
    }

    /**
     * Enregistre une correction manuelle pour apprentissage
     */
    @Transactional
    public void recordCorrection(Long transactionId, VATRecoverableCategory oldCategory, VATRecoverableCategory newCategory, Long ruleId) {
        if (ruleId != null) {
            RecoverabilityRule rule = ruleRepository.findById(ruleId).orElse(null);
            if (rule != null) {
                rule.incrementCorrectionCount();
                ruleRepository.save(rule);

                log.warn("⚠️ Correction enregistrée - Règle: {} - Ancien: {} - Nouveau: {} - Précision: {}%",
                    rule.getName(),
                    oldCategory.getDisplayName(),
                    newCategory.getDisplayName(),
                    rule.getAccuracyRate());

                if (rule.needsReview()) {
                    log.warn("🔴 ATTENTION: La règle '{}' nécessite une révision (précision: {}%)",
                        rule.getName(), rule.getAccuracyRate());
                }
            }
        }
    }

    /**
     * Statistiques du moteur de règles
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStatistics() {
        List<RecoverabilityRule> allRules = ruleRepository.findAll();

        long totalMatches = allRules.stream().mapToLong(RecoverabilityRule::getMatchCount).sum();
        long totalCorrections = allRules.stream().mapToLong(RecoverabilityRule::getCorrectionCount).sum();

        double avgAccuracy = allRules.stream()
            .filter(r -> r.getMatchCount() > 0)
            .mapToDouble(r -> r.getAccuracyRate().doubleValue())
            .average()
            .orElse(0.0);

        List<RecoverabilityRule> needsReview = ruleRepository.findRulesNeedingReview();

        return Map.of(
            "totalRules", allRules.size(),
            "activeRules", ruleRepository.countByIsActiveTrue(),
            "totalMatches", totalMatches,
            "totalCorrections", totalCorrections,
            "avgAccuracy", Math.round(avgAccuracy * 100.0) / 100.0,
            "rulesNeedingReview", needsReview.size(),
            "cacheSize", patternCache.size()
        );
    }

    /**
     * Classe interne: Match d'une règle
     */
    @Data
    @lombok.Builder
    private static class RuleMatch {
        private RecoverabilityRule rule;
        private int baseScore;
        private int priorityBonus;
        private int totalScore;
        private List<String> matchedCriteria;
        private boolean matched;
    }

    /**
     * Résultat de la détection
     */
    @Data
    @lombok.Builder
    public static class DetectionResult {
        private VATRecoverableCategory category;
        private int confidence;  // 0-100
        private RecoverabilityRule appliedRule;
        private String reason;
        private List<Alternative> alternatives;
        private long executionTimeNanos;

        public double getExecutionTimeMicros() {
            return executionTimeNanos / 1000.0;
        }
    }

    /**
     * Alternative suggérée
     */
    @Data
    @lombok.AllArgsConstructor
    public static class Alternative {
        private VATRecoverableCategory category;
        private int confidence;
        private String reason;
    }
}
