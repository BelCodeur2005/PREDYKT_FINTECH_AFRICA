package com.predykt.accounting.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour TextNormalizer
 * Vérifie la normalisation de texte, la gestion des synonymes, et les performances du cache
 */
@DisplayName("TextNormalizer - Normalisation avancée de texte")
class TextNormalizerTest {

    private TextNormalizer textNormalizer;

    @BeforeEach
    void setUp() {
        textNormalizer = new TextNormalizer();
        textNormalizer.clearCache(); // Nettoyer le cache avant chaque test
    }

    // ============================================
    // TESTS DE NORMALISATION DE BASE
    // ============================================

    @Test
    @DisplayName("Normalisation - Devrait convertir en minuscules")
    void testNormalize_ShouldConvertToLowercase() {
        String result = textNormalizer.normalize("VOITURE DE TOURISME");
        assertThat(result).isEqualTo("voiture de tourisme");
    }

    @Test
    @DisplayName("Normalisation - Devrait supprimer les accents")
    void testNormalize_ShouldRemoveAccents() {
        String result = textNormalizer.normalize("Véhicule à moteur électrique");
        assertThat(result).isEqualTo("vehicule a moteur electrique");
    }

    @Test
    @DisplayName("Normalisation - Devrait normaliser les tirets et underscores")
    void testNormalize_ShouldNormalizeDashesAndUnderscores() {
        String result = textNormalizer.normalize("pick-up_diesel");
        assertThat(result).isEqualTo("pick up diesel");
    }

    @Test
    @DisplayName("Normalisation - Devrait supprimer les apostrophes variées")
    void testNormalize_ShouldNormalizeApostrophes() {
        String result = textNormalizer.normalize("Aujourd'hui l'essence");
        assertThat(result).isEqualTo("aujourd hui l essence");
    }

    @Test
    @DisplayName("Normalisation - Devrait normaliser les espaces multiples")
    void testNormalize_ShouldNormalizeMultipleSpaces() {
        String result = textNormalizer.normalize("Voiture    de   tourisme");
        assertThat(result).isEqualTo("voiture de tourisme");
    }

    @Test
    @DisplayName("Normalisation - Devrait gérer les textes vides")
    void testNormalize_ShouldHandleEmptyText() {
        assertThat(textNormalizer.normalize(null)).isEqualTo("");
        assertThat(textNormalizer.normalize("")).isEqualTo("");
        assertThat(textNormalizer.normalize("   ")).isEqualTo("");
    }

    @Test
    @DisplayName("Normalisation - Devrait supprimer les caractères spéciaux")
    void testNormalize_ShouldRemoveSpecialCharacters() {
        String result = textNormalizer.normalize("Carburant (diesel) @ 1.50€/L !");
        assertThat(result).isEqualTo("carburant diesel 1 50 l");
    }

    @Test
    @DisplayName("Normalisation complète - Cas réel complexe")
    void testNormalize_RealWorldComplexCase() {
        String input = "Achat d'un véhicule utilitaire (Pick-Up Diesel) - Réf: VU-2024_001";
        String result = textNormalizer.normalize(input);
        assertThat(result).isEqualTo("achat d un vehicule utilitaire pick up diesel ref vu 2024 001");
    }

    // ============================================
    // TESTS D'EXPANSION DE SYNONYMES
    // ============================================

    @Test
    @DisplayName("Synonymes - Devrait ajouter les synonymes de 'voiture'")
    void testNormalizeWithSynonyms_ShouldAddCarSynonyms() {
        String result = textNormalizer.normalizeWithSynonyms("voiture");
        assertThat(result)
            .contains("voiture")
            .contains("auto")
            .contains("automobile")
            .contains("vehicule");
    }

    @Test
    @DisplayName("Synonymes - Devrait ajouter les synonymes de 'carburant'")
    void testNormalizeWithSynonyms_ShouldAddFuelSynonyms() {
        String result = textNormalizer.normalizeWithSynonyms("carburant");
        assertThat(result)
            .contains("carburant")
            .contains("essence")
            .contains("gasoil")
            .contains("diesel");
    }

    @Test
    @DisplayName("Synonymes - Devrait gérer plusieurs mots avec synonymes")
    void testNormalizeWithSynonyms_MultipleWords() {
        String result = textNormalizer.normalizeWithSynonyms("voiture essence");
        assertThat(result)
            .contains("voiture", "auto", "automobile") // Synonymes de voiture
            .contains("essence", "carburant", "fuel"); // Synonymes d'essence
    }

    @Test
    @DisplayName("Synonymes - Devrait gérer les mots sans synonymes")
    void testNormalizeWithSynonyms_WordsWithoutSynonyms() {
        String result = textNormalizer.normalizeWithSynonyms("achat facture");
        assertThat(result).contains("achat", "facture");
    }

    // ============================================
    // TESTS DE RECHERCHE PAR MOTS-CLÉS
    // ============================================

    @Test
    @DisplayName("Mots-clés - Devrait trouver un mot-clé simple")
    void testContainsKeywords_SimpleKeyword() {
        assertThat(textNormalizer.containsKeywords("Achat voiture de tourisme", "voiture")).isTrue();
        assertThat(textNormalizer.containsKeywords("Achat camion", "voiture")).isFalse();
    }

    @Test
    @DisplayName("Mots-clés - Devrait trouver via synonymes")
    void testContainsKeywords_ViaSynonyms() {
        // "auto" est un synonyme de "voiture"
        assertThat(textNormalizer.containsKeywords("Achat automobile", "voiture")).isTrue();
    }

    @Test
    @DisplayName("Mots-clés - Devrait trouver malgré les accents")
    void testContainsKeywords_WithAccents() {
        assertThat(textNormalizer.containsKeywords("Véhicule de tourisme", "vehicule")).isTrue();
    }

    @Test
    @DisplayName("Mots-clés - Tous les mots requis présents")
    void testContainsAllKeywords_AllPresent() {
        assertThat(textNormalizer.containsAllKeywords(
            "Carburant diesel pour véhicule utilitaire",
            "carburant", "utilitaire"
        )).isTrue();
    }

    @Test
    @DisplayName("Mots-clés - Pas tous les mots requis présents")
    void testContainsAllKeywords_NotAllPresent() {
        assertThat(textNormalizer.containsAllKeywords(
            "Carburant diesel",
            "carburant", "utilitaire"
        )).isFalse();
    }

    @Test
    @DisplayName("Mots exclus - Devrait détecter les mots exclus")
    void testContainsExcludedKeyword() {
        assertThat(textNormalizer.containsExcludedKeyword(
            "Voiture de tourisme",
            "tourisme", "luxe"
        )).isTrue();

        assertThat(textNormalizer.containsExcludedKeyword(
            "Camion utilitaire",
            "tourisme", "luxe"
        )).isFalse();
    }

    // ============================================
    // TESTS D'EXTRACTION DE MOTS-CLÉS
    // ============================================

    @Test
    @DisplayName("Extraction - Devrait extraire les mots significatifs")
    void testExtractKeywords_SignificantWords() {
        List<String> keywords = textNormalizer.extractKeywords(
            "Achat de véhicule utilitaire pour le transport"
        );

        assertThat(keywords)
            .contains("achat", "vehicule", "utilitaire", "transport")
            .doesNotContain("de", "le", "pour"); // Stop words exclus
    }

    @Test
    @DisplayName("Extraction - Devrait filtrer les mots trop courts")
    void testExtractKeywords_FilterShortWords() {
        List<String> keywords = textNormalizer.extractKeywords("Un vp de la SA");

        assertThat(keywords)
            .doesNotContain("un", "de", "la"); // Mots < 3 caractères exclus
    }

    @Test
    @DisplayName("Extraction - Devrait dédupliquer les mots")
    void testExtractKeywords_Deduplicate() {
        List<String> keywords = textNormalizer.extractKeywords(
            "Voiture voiture voiture automobile"
        );

        // Compte le nombre d'occurrences de "voiture"
        long count = keywords.stream().filter(k -> k.equals("voiture")).count();
        assertThat(count).isEqualTo(1); // Une seule occurrence après déduplication
    }

    // ============================================
    // TESTS DE SIMILARITÉ
    // ============================================

    @Test
    @DisplayName("Similarité - Textes identiques = 100%")
    void testCalculateSimilarity_IdenticalTexts() {
        int similarity = textNormalizer.calculateSimilarity(
            "Voiture de tourisme",
            "Voiture de tourisme"
        );
        assertThat(similarity).isEqualTo(100);
    }

    @Test
    @DisplayName("Similarité - Textes complètement différents = 0%")
    void testCalculateSimilarity_CompletelyDifferent() {
        int similarity = textNormalizer.calculateSimilarity(
            "Voiture de tourisme",
            "Ordinateur portable"
        );
        assertThat(similarity).isEqualTo(0);
    }

    @Test
    @DisplayName("Similarité - Textes partiellement similaires")
    void testCalculateSimilarity_PartiallySimilar() {
        int similarity = textNormalizer.calculateSimilarity(
            "Voiture utilitaire diesel",
            "Voiture de tourisme essence"
        );
        // Mots communs: "voiture" (1/5 = 20%)
        assertThat(similarity).isGreaterThan(0).isLessThan(50);
    }

    @Test
    @DisplayName("Similarité - Devrait ignorer les accents")
    void testCalculateSimilarity_IgnoreAccents() {
        int similarity = textNormalizer.calculateSimilarity(
            "Véhicule électrique",
            "Vehicule electrique"
        );
        assertThat(similarity).isEqualTo(100);
    }

    @Test
    @DisplayName("Similarité - Textes null = 0%")
    void testCalculateSimilarity_NullTexts() {
        assertThat(textNormalizer.calculateSimilarity(null, "test")).isEqualTo(0);
        assertThat(textNormalizer.calculateSimilarity("test", null)).isEqualTo(0);
        assertThat(textNormalizer.calculateSimilarity(null, null)).isEqualTo(0);
    }

    // ============================================
    // TESTS DE CACHE
    // ============================================

    @Test
    @DisplayName("Cache - Devrait mettre en cache les résultats")
    void testCache_ShouldCacheResults() {
        String input = "Véhicule de tourisme électrique";

        // Première normalisation
        String result1 = textNormalizer.normalize(input);

        // Vérifier que le cache contient maintenant 1 entrée
        Map<String, Object> stats = textNormalizer.getCacheStats();
        assertThat(stats.get("size")).isEqualTo(1);

        // Deuxième normalisation (devrait utiliser le cache)
        String result2 = textNormalizer.normalize(input);

        assertThat(result1).isEqualTo(result2);
        // Le cache devrait toujours contenir 1 entrée
        stats = textNormalizer.getCacheStats();
        assertThat(stats.get("size")).isEqualTo(1);
    }

    @Test
    @DisplayName("Cache - Devrait limiter la taille du cache à 1000 entrées")
    void testCache_ShouldLimitSize() {
        // Remplir le cache avec 1100 entrées
        for (int i = 0; i < 1100; i++) {
            textNormalizer.normalize("Test entry " + i);
        }

        Map<String, Object> stats = textNormalizer.getCacheStats();
        assertThat((Integer) stats.get("size")).isLessThanOrEqualTo(1000);
        assertThat(stats.get("maxSize")).isEqualTo(1000);
    }

    @Test
    @DisplayName("Cache - Devrait nettoyer le cache")
    void testCache_ShouldClear() {
        textNormalizer.normalize("Test 1");
        textNormalizer.normalize("Test 2");

        Map<String, Object> stats = textNormalizer.getCacheStats();
        assertThat((Integer) stats.get("size")).isGreaterThan(0);

        textNormalizer.clearCache();

        stats = textNormalizer.getCacheStats();
        assertThat(stats.get("size")).isEqualTo(0);
    }

    // ============================================
    // TESTS DE PERFORMANCE
    // ============================================

    @Test
    @DisplayName("Performance - Normalisation rapide (<10ms pour 1000 itérations)")
    void testPerformance_NormalizationSpeed() {
        String input = "Achat véhicule utilitaire (Pick-Up Diesel) - Réf: VU-2024_001";

        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            textNormalizer.normalize(input);
        }
        long duration = (System.nanoTime() - startTime) / 1_000_000; // Convertir en ms

        // Devrait prendre moins de 10ms pour 1000 normalisations (grâce au cache)
        assertThat(duration).isLessThan(10);
    }

    // ============================================
    // TESTS DE CAS RÉELS
    // ============================================

    @Test
    @DisplayName("Cas réel - Achat voiture de tourisme")
    void testRealWorld_TourismVehicle() {
        String description = "Achat véhicule de tourisme Peugeot 308 - VP";
        String normalized = textNormalizer.normalize(description);

        assertThat(normalized).contains("vehicule", "tourisme", "vp");
        assertThat(textNormalizer.containsKeywords(description, "voiture")).isTrue();
        assertThat(textNormalizer.containsKeywords(description, "tourisme")).isTrue();
    }

    @Test
    @DisplayName("Cas réel - Carburant diesel VU")
    void testRealWorld_DieselFuelUtility() {
        String description = "Carburant diesel pour fourgon utilitaire Renault Master";
        String normalized = textNormalizer.normalize(description);

        assertThat(normalized).contains("carburant", "diesel", "fourgon", "utilitaire");
        assertThat(textNormalizer.containsAllKeywords(description, "carburant", "utilitaire")).isTrue();
        assertThat(textNormalizer.containsExcludedKeyword(description, "tourisme", "vp")).isFalse();
    }

    @Test
    @DisplayName("Cas réel - Frais de représentation")
    void testRealWorld_RepresentationExpenses() {
        String description = "Restaurant d'affaires - Réception clients";
        String normalized = textNormalizer.normalize(description);

        assertThat(normalized).contains("restaurant", "reception");
        assertThat(textNormalizer.containsKeywords(description, "restaurant", "représentation")).isTrue();
    }

    @Test
    @DisplayName("Cas réel - Dépense de luxe")
    void testRealWorld_LuxuryExpenses() {
        String description = "Cotisation club de golf pour partenariat";
        String normalized = textNormalizer.normalize(description);

        assertThat(normalized).contains("golf");
        assertThat(textNormalizer.containsKeywords(description, "luxe", "golf")).isTrue();
    }
}
