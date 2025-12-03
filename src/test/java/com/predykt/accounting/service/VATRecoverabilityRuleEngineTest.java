package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.RecoverabilityRule;
import com.predykt.accounting.domain.enums.VATRecoverableCategory;
import com.predykt.accounting.repository.RecoverabilityRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour VATRecoverabilityRuleEngine
 * Vérifie le moteur de règles optimisé avec scoring, priorités, et apprentissage
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VATRecoverabilityRuleEngine - Moteur de règles optimisé")
class VATRecoverabilityRuleEngineTest {

    @Mock
    private RecoverabilityRuleRepository ruleRepository;

    @Mock
    private TextNormalizer textNormalizer;

    @InjectMocks
    private VATRecoverabilityRuleEngine ruleEngine;

    private RecoverabilityRule tourismVehicleRule;
    private RecoverabilityRule utilityVehicleRule;
    private RecoverabilityRule fuelVpRule;
    private RecoverabilityRule fuelVuRule;

    @BeforeEach
    void setUp() {
        // Mock TextNormalizer behavior
        when(textNormalizer.normalize(anyString())).thenAnswer(invocation ->
            invocation.getArgument(0, String.class).toLowerCase()
        );
        when(textNormalizer.normalizeWithSynonyms(anyString())).thenAnswer(invocation ->
            invocation.getArgument(0, String.class).toLowerCase() + " auto vehicule"
        );
        when(textNormalizer.containsAllKeywords(anyString(), any())).thenReturn(true);
        when(textNormalizer.containsExcludedKeyword(anyString(), any())).thenReturn(false);

        // Créer des règles de test
        tourismVehicleRule = RecoverabilityRule.builder()
            .id(1L)
            .name("Véhicules de tourisme")
            .priority(10)
            .confidenceScore(95)
            .accountPattern("^2441")
            .descriptionPattern("(?i)\\b(tourisme|voiture|berline)\\b")
            .excludedKeywords("utilitaire,camion")
            .category(VATRecoverableCategory.NON_RECOVERABLE_TOURISM_VEHICLE)
            .reason("Véhicule de tourisme - TVA non récupérable")
            .isActive(true)
            .matchCount(0L)
            .correctionCount(0L)
            .accuracyRate(BigDecimal.valueOf(100))
            .build();

        utilityVehicleRule = RecoverabilityRule.builder()
            .id(2L)
            .name("Véhicules utilitaires")
            .priority(11)
            .confidenceScore(95)
            .accountPattern("^2441")
            .descriptionPattern("(?i)\\b(utilitaire|camion|fourgon)\\b")
            .excludedKeywords("tourisme,voiture")
            .category(VATRecoverableCategory.FULLY_RECOVERABLE)
            .reason("Véhicule utilitaire - TVA 100% récupérable")
            .isActive(true)
            .matchCount(0L)
            .correctionCount(0L)
            .accuracyRate(BigDecimal.valueOf(100))
            .build();

        fuelVpRule = RecoverabilityRule.builder()
            .id(3L)
            .name("Carburant VP")
            .priority(20)
            .confidenceScore(90)
            .accountPattern("^605")
            .descriptionPattern("(?i)\\b(carburant|essence|diesel)\\b.*(vp|voiture|tourisme)\\b")
            .category(VATRecoverableCategory.NON_RECOVERABLE_FUEL_VP)
            .reason("Carburant VP - TVA non récupérable")
            .isActive(true)
            .matchCount(0L)
            .correctionCount(0L)
            .accuracyRate(BigDecimal.valueOf(100))
            .build();

        fuelVuRule = RecoverabilityRule.builder()
            .id(4L)
            .name("Carburant VU")
            .priority(21)
            .confidenceScore(90)
            .accountPattern("^605")
            .descriptionPattern("(?i)\\b(carburant|essence|diesel)\\b.*(vu|utilitaire|camion)\\b")
            .category(VATRecoverableCategory.RECOVERABLE_80_PERCENT)
            .reason("Carburant VU - TVA 80% récupérable")
            .isActive(true)
            .matchCount(0L)
            .correctionCount(0L)
            .accuracyRate(BigDecimal.valueOf(100))
            .build();
    }

    // ============================================
    // TESTS DE DÉTECTION DE BASE
    // ============================================

    @Test
    @DisplayName("Détection - Véhicule de tourisme (compte 2441)")
    void testDetection_TourismVehicle() {
        when(ruleRepository.findByIsActiveTrueOrderByPriorityAsc())
            .thenReturn(Arrays.asList(tourismVehicleRule, utilityVehicleRule));

        // Mock pour que le pattern matche
        when(textNormalizer.normalizeWithSynonyms("Achat voiture de tourisme"))
            .thenReturn("achat voiture de tourisme berline auto");

        VATRecoverabilityRuleEngine.DetectionResult result =
            ruleEngine.detectCategory("2441", "Achat voiture de tourisme");

        assertThat(result.getCategory()).isEqualTo(VATRecoverableCategory.NON_RECOVERABLE_TOURISM_VEHICLE);
        assertThat(result.getAppliedRule()).isNotNull();
        assertThat(result.getAppliedRule().getName()).isEqualTo("Véhicules de tourisme");
        assertThat(result.getConfidence()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Détection - Véhicule utilitaire (compte 2441)")
    void testDetection_UtilityVehicle() {
        when(ruleRepository.findByIsActiveTrueOrderByPriorityAsc())
            .thenReturn(Arrays.asList(tourismVehicleRule, utilityVehicleRule));

        when(textNormalizer.normalizeWithSynonyms("Achat camion utilitaire"))
            .thenReturn("achat camion utilitaire fourgon");

        VATRecoverabilityRuleEngine.DetectionResult result =
            ruleEngine.detectCategory("2441", "Achat camion utilitaire");

        assertThat(result.getCategory()).isEqualTo(VATRecoverableCategory.FULLY_RECOVERABLE);
        assertThat(result.getAppliedRule()).isNotNull();
        assertThat(result.getAppliedRule().getName()).isEqualTo("Véhicules utilitaires");
    }

    @Test
    @DisplayName("Détection - Carburant pour VP (compte 605)")
    void testDetection_FuelForTourismVehicle() {
        when(ruleRepository.findByIsActiveTrueOrderByPriorityAsc())
            .thenReturn(Collections.singletonList(fuelVpRule));

        when(textNormalizer.normalizeWithSynonyms("Carburant diesel pour voiture"))
            .thenReturn("carburant diesel pour voiture vp essence");

        VATRecoverabilityRuleEngine.DetectionResult result =
            ruleEngine.detectCategory("605", "Carburant diesel pour voiture");

        assertThat(result.getCategory()).isEqualTo(VATRecoverableCategory.NON_RECOVERABLE_FUEL_VP);
        assertThat(result.getAppliedRule()).isNotNull();
    }

    @Test
    @DisplayName("Détection - Carburant pour VU (compte 605)")
    void testDetection_FuelForUtilityVehicle() {
        when(ruleRepository.findByIsActiveTrueOrderByPriorityAsc())
            .thenReturn(Collections.singletonList(fuelVuRule));

        when(textNormalizer.normalizeWithSynonyms("Carburant diesel pour camion"))
            .thenReturn("carburant diesel pour camion utilitaire vu");

        VATRecoverabilityRuleEngine.DetectionResult result =
            ruleEngine.detectCategory("605", "Carburant diesel pour camion");

        assertThat(result.getCategory()).isEqualTo(VATRecoverableCategory.RECOVERABLE_80_PERCENT);
        assertThat(result.getAppliedRule()).isNotNull();
    }

    @Test
    @DisplayName("Détection - Aucune règle ne matche = FULLY_RECOVERABLE par défaut")
    void testDetection_NoMatchDefaultFullyRecoverable() {
        when(ruleRepository.findByIsActiveTrueOrderByPriorityAsc())
            .thenReturn(Collections.singletonList(tourismVehicleRule));

        when(textNormalizer.normalizeWithSynonyms("Achat fournitures de bureau"))
            .thenReturn("achat fournitures de bureau");

        VATRecoverabilityRuleEngine.DetectionResult result =
            ruleEngine.detectCategory("607", "Achat fournitures de bureau");

        assertThat(result.getCategory()).isEqualTo(VATRecoverableCategory.FULLY_RECOVERABLE);
        assertThat(result.getAppliedRule()).isNull();
        assertThat(result.getConfidence()).isEqualTo(100);
        assertThat(result.getReason()).contains("Aucune règle spécifique");
    }

    // ============================================
    // TESTS DE PRIORITÉ ET SCORING
    // ============================================

    @Test
    @DisplayName("Priorité - Règle avec priorité plus haute (plus petit numéro) devrait gagner")
    void testPriority_HigherPriorityWins() {
        RecoverabilityRule highPriorityRule = RecoverabilityRule.builder()
            .id(10L)
            .name("Règle haute priorité")
            .priority(5) // Plus petite priorité = plus haute importance
            .confidenceScore(80)
            .accountPattern("^2441")
            .descriptionPattern("(?i)voiture")
            .category(VATRecoverableCategory.NON_RECOVERABLE_TOURISM_VEHICLE)
            .reason("Haute priorité")
            .isActive(true)
            .matchCount(0L)
            .correctionCount(0L)
            .accuracyRate(BigDecimal.valueOf(100))
            .build();

        RecoverabilityRule lowPriorityRule = RecoverabilityRule.builder()
            .id(11L)
            .name("Règle basse priorité")
            .priority(50) // Plus grande priorité = plus basse importance
            .confidenceScore(95)
            .accountPattern("^2441")
            .descriptionPattern("(?i)voiture")
            .category(VATRecoverableCategory.FULLY_RECOVERABLE)
            .reason("Basse priorité")
            .isActive(true)
            .matchCount(0L)
            .correctionCount(0L)
            .accuracyRate(BigDecimal.valueOf(100))
            .build();

        when(ruleRepository.findByIsActiveTrueOrderByPriorityAsc())
            .thenReturn(Arrays.asList(highPriorityRule, lowPriorityRule));

        when(textNormalizer.normalizeWithSynonyms("Achat voiture"))
            .thenReturn("achat voiture auto");

        VATRecoverabilityRuleEngine.DetectionResult result =
            ruleEngine.detectCategory("2441", "Achat voiture");

        // La règle avec priorité 5 devrait gagner (score total plus élevé grâce au bonus de priorité)
        assertThat(result.getAppliedRule().getName()).isEqualTo("Règle haute priorité");
    }

    @Test
    @DisplayName("Scoring - Confidence score affecte le score final")
    void testScoring_ConfidenceAffectsFinalScore() {
        // Deux règles identiques sauf le confidence score
        RecoverabilityRule highConfidenceRule = RecoverabilityRule.builder()
            .id(20L)
            .name("Haute confiance")
            .priority(10)
            .confidenceScore(100)
            .accountPattern("^2441")
            .descriptionPattern("(?i)voiture")
            .category(VATRecoverableCategory.NON_RECOVERABLE_TOURISM_VEHICLE)
            .reason("Haute confiance")
            .isActive(true)
            .matchCount(0L)
            .correctionCount(0L)
            .accuracyRate(BigDecimal.valueOf(100))
            .build();

        RecoverabilityRule lowConfidenceRule = RecoverabilityRule.builder()
            .id(21L)
            .name("Basse confiance")
            .priority(10)
            .confidenceScore(50)
            .accountPattern("^2441")
            .descriptionPattern("(?i)voiture")
            .category(VATRecoverableCategory.FULLY_RECOVERABLE)
            .reason("Basse confiance")
            .isActive(true)
            .matchCount(0L)
            .correctionCount(0L)
            .accuracyRate(BigDecimal.valueOf(100))
            .build();

        when(ruleRepository.findByIsActiveTrueOrderByPriorityAsc())
            .thenReturn(Arrays.asList(highConfidenceRule, lowConfidenceRule));

        when(textNormalizer.normalizeWithSynonyms("Achat voiture"))
            .thenReturn("achat voiture auto");

        VATRecoverabilityRuleEngine.DetectionResult result =
            ruleEngine.detectCategory("2441", "Achat voiture");

        // La règle avec confidence 100 devrait avoir un score plus élevé
        assertThat(result.getAppliedRule().getName()).isEqualTo("Haute confiance");
        assertThat(result.getConfidence()).isGreaterThan(50);
    }

    // ============================================
    // TESTS D'ALTERNATIVES
    // ============================================

    @Test
    @DisplayName("Alternatives - Devrait suggérer des alternatives proches")
    void testAlternatives_ShouldSuggestCloseMatches() {
        // Deux règles avec des scores proches
        RecoverabilityRule rule1 = RecoverabilityRule.builder()
            .id(30L)
            .name("Règle 1")
            .priority(10)
            .confidenceScore(95)
            .accountPattern("^2441")
            .descriptionPattern("(?i)voiture")
            .category(VATRecoverableCategory.NON_RECOVERABLE_TOURISM_VEHICLE)
            .reason("Raison 1")
            .isActive(true)
            .matchCount(0L)
            .correctionCount(0L)
            .accuracyRate(BigDecimal.valueOf(100))
            .build();

        RecoverabilityRule rule2 = RecoverabilityRule.builder()
            .id(31L)
            .name("Règle 2")
            .priority(11)
            .confidenceScore(90)
            .accountPattern("^2441")
            .descriptionPattern("(?i)voiture")
            .category(VATRecoverableCategory.FULLY_RECOVERABLE)
            .reason("Raison 2")
            .isActive(true)
            .matchCount(0L)
            .correctionCount(0L)
            .accuracyRate(BigDecimal.valueOf(100))
            .build();

        when(ruleRepository.findByIsActiveTrueOrderByPriorityAsc())
            .thenReturn(Arrays.asList(rule1, rule2));

        when(textNormalizer.normalizeWithSynonyms("Achat voiture"))
            .thenReturn("achat voiture auto");

        VATRecoverabilityRuleEngine.DetectionResult result =
            ruleEngine.detectCategory("2441", "Achat voiture");

        // Devrait avoir au moins une alternative
        assertThat(result.getAlternatives()).isNotEmpty();
        assertThat(result.getAlternatives().get(0).getCategory()).isEqualTo(VATRecoverableCategory.FULLY_RECOVERABLE);
    }

    // ============================================
    // TESTS D'APPRENTISSAGE (MACHINE LEARNING)
    // ============================================

    @Test
    @DisplayName("Apprentissage - Enregistrer une correction")
    void testLearning_RecordCorrection() {
        when(ruleRepository.findById(1L)).thenReturn(java.util.Optional.of(tourismVehicleRule));

        ruleEngine.recordCorrection(
            100L, // transactionId
            VATRecoverableCategory.NON_RECOVERABLE_TOURISM_VEHICLE, // old
            VATRecoverableCategory.FULLY_RECOVERABLE, // new
            1L // ruleId
        );

        // Vérifier que la règle a été mise à jour
        assertThat(tourismVehicleRule.getCorrectionCount()).isEqualTo(1L);
        verify(ruleRepository, times(1)).save(tourismVehicleRule);
    }

    @Test
    @DisplayName("Apprentissage - Incrémenter match count après détection")
    void testLearning_IncrementMatchCount() {
        when(ruleRepository.findByIsActiveTrueOrderByPriorityAsc())
            .thenReturn(Collections.singletonList(tourismVehicleRule));

        when(textNormalizer.normalizeWithSynonyms("Achat voiture de tourisme"))
            .thenReturn("achat voiture de tourisme berline");

        ruleEngine.detectCategory("2441", "Achat voiture de tourisme");

        // Vérifier que le match count a été incrémenté
        verify(ruleRepository, times(1)).save(argThat(rule ->
            rule.getId().equals(1L) && rule.getMatchCount() > 0
        ));
    }

    @Test
    @DisplayName("Apprentissage - Règle nécessite révision si trop de corrections")
    void testLearning_RuleNeedsReview() {
        tourismVehicleRule.setMatchCount(30L);
        tourismVehicleRule.setCorrectionCount(10L);

        // Recalculer la précision
        long correctMatches = 30L - 10L;
        BigDecimal accuracy = BigDecimal.valueOf(correctMatches)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(30L), 2, java.math.RoundingMode.HALF_UP);
        tourismVehicleRule.setAccuracyRate(accuracy);

        // 66.67% < 70% → nécessite révision
        assertThat(tourismVehicleRule.needsReview()).isTrue();
    }

    // ============================================
    // TESTS DE STATISTIQUES
    // ============================================

    @Test
    @DisplayName("Statistiques - Devrait calculer les métriques du moteur")
    void testStatistics_ShouldCalculateEngineMetrics() {
        tourismVehicleRule.setMatchCount(100L);
        tourismVehicleRule.setCorrectionCount(5L);
        tourismVehicleRule.setAccuracyRate(BigDecimal.valueOf(95));

        utilityVehicleRule.setMatchCount(150L);
        utilityVehicleRule.setCorrectionCount(2L);
        utilityVehicleRule.setAccuracyRate(BigDecimal.valueOf(98.67));

        when(ruleRepository.findAll())
            .thenReturn(Arrays.asList(tourismVehicleRule, utilityVehicleRule));
        when(ruleRepository.countByIsActiveTrue()).thenReturn(2L);
        when(ruleRepository.findRulesNeedingReview()).thenReturn(Collections.emptyList());

        Map<String, Object> stats = ruleEngine.getStatistics();

        assertThat(stats).containsKeys(
            "totalRules", "activeRules", "totalMatches", "totalCorrections",
            "avgAccuracy", "rulesNeedingReview", "cacheSize"
        );
        assertThat(stats.get("totalRules")).isEqualTo(2);
        assertThat(stats.get("activeRules")).isEqualTo(2L);
        assertThat(stats.get("totalMatches")).isEqualTo(250L); // 100 + 150
        assertThat(stats.get("totalCorrections")).isEqualTo(7L); // 5 + 2
    }

    // ============================================
    // TESTS DE CACHE
    // ============================================

    @Test
    @DisplayName("Cache - Devrait invalider le cache")
    void testCache_ShouldInvalidate() {
        // Charger les règles dans le cache
        when(ruleRepository.findByIsActiveTrueOrderByPriorityAsc())
            .thenReturn(Collections.singletonList(tourismVehicleRule));

        ruleEngine.detectCategory("2441", "Test");

        // Invalider le cache
        ruleEngine.invalidateCache();

        // Vérifier que les règles sont rechargées à la prochaine détection
        ruleEngine.detectCategory("2441", "Test 2");

        verify(ruleRepository, atLeast(2)).findByIsActiveTrueOrderByPriorityAsc();
    }

    // ============================================
    // TESTS DE ROBUSTESSE
    // ============================================

    @Test
    @DisplayName("Robustesse - Devrait gérer les patterns regex invalides")
    void testRobustness_InvalidRegexPattern() {
        RecoverabilityRule invalidRule = RecoverabilityRule.builder()
            .id(100L)
            .name("Règle invalide")
            .priority(10)
            .confidenceScore(95)
            .accountPattern("^2441")
            .descriptionPattern("[invalid(regex") // Regex invalide
            .category(VATRecoverableCategory.NON_RECOVERABLE_TOURISM_VEHICLE)
            .reason("Test")
            .isActive(true)
            .matchCount(0L)
            .correctionCount(0L)
            .accuracyRate(BigDecimal.valueOf(100))
            .build();

        when(ruleRepository.findByIsActiveTrueOrderByPriorityAsc())
            .thenReturn(Collections.singletonList(invalidRule));

        // Ne devrait pas lever d'exception
        VATRecoverabilityRuleEngine.DetectionResult result =
            ruleEngine.detectCategory("2441", "Test");

        // Devrait retourner FULLY_RECOVERABLE par défaut
        assertThat(result.getCategory()).isEqualTo(VATRecoverableCategory.FULLY_RECOVERABLE);
    }

    @Test
    @DisplayName("Robustesse - Devrait gérer les entrées null")
    void testRobustness_NullInputs() {
        when(ruleRepository.findByIsActiveTrueOrderByPriorityAsc())
            .thenReturn(Collections.emptyList());

        VATRecoverabilityRuleEngine.DetectionResult result1 =
            ruleEngine.detectCategory(null, "Description");
        VATRecoverabilityRuleEngine.DetectionResult result2 =
            ruleEngine.detectCategory("2441", null);
        VATRecoverabilityRuleEngine.DetectionResult result3 =
            ruleEngine.detectCategory(null, null);

        assertThat(result1.getCategory()).isEqualTo(VATRecoverableCategory.FULLY_RECOVERABLE);
        assertThat(result2.getCategory()).isEqualTo(VATRecoverableCategory.FULLY_RECOVERABLE);
        assertThat(result3.getCategory()).isEqualTo(VATRecoverableCategory.FULLY_RECOVERABLE);
    }

    @Test
    @DisplayName("Robustesse - Devrait gérer les exceptions gracieusement")
    void testRobustness_ExceptionHandling() {
        when(ruleRepository.findByIsActiveTrueOrderByPriorityAsc())
            .thenThrow(new RuntimeException("Database error"));

        // Ne devrait pas lever d'exception
        VATRecoverabilityRuleEngine.DetectionResult result =
            ruleEngine.detectCategory("2441", "Test");

        // Devrait retourner FULLY_RECOVERABLE en fallback
        assertThat(result.getCategory()).isEqualTo(VATRecoverableCategory.FULLY_RECOVERABLE);
        assertThat(result.getConfidence()).isEqualTo(0);
        assertThat(result.getReason()).contains("Erreur");
    }

    // ============================================
    // TESTS DE PERFORMANCE
    // ============================================

    @Test
    @DisplayName("Performance - Détection rapide (<100µs)")
    void testPerformance_FastDetection() {
        when(ruleRepository.findByIsActiveTrueOrderByPriorityAsc())
            .thenReturn(Arrays.asList(tourismVehicleRule, utilityVehicleRule, fuelVpRule, fuelVuRule));

        when(textNormalizer.normalizeWithSynonyms(anyString()))
            .thenReturn("achat voiture tourisme");

        VATRecoverabilityRuleEngine.DetectionResult result =
            ruleEngine.detectCategory("2441", "Achat voiture de tourisme");

        // Vérifier que le temps d'exécution est < 100 microsecondes
        assertThat(result.getExecutionTimeMicros()).isLessThan(100.0);
    }
}
