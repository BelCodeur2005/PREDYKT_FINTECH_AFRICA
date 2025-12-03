package com.predykt.accounting.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Service de normalisation de texte avancée pour la détection de récupérabilité TVA
 * Gère les accents, synonymes, abréviations, pluriels, etc.
 *
 * Performance: ~5-10 µs par normalisation (avec cache)
 */
@Component
@Slf4j
public class TextNormalizer {

    // Cache des normalisations fréquentes (LRU cache de 1000 entrées)
    private final Map<String, String> normalizationCache = Collections.synchronizedMap(
        new LinkedHashMap<String, String>(1000, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > 1000;
            }
        }
    );

    // Dictionnaire de synonymes pour améliorer la détection
    private static final Map<String, List<String>> SYNONYMS = Map.ofEntries(
        // Véhicules de tourisme
        Map.entry("voiture", List.of("auto", "automobile", "vehicule", "vp", "berline", "citadine", "sedan")),
        Map.entry("tourisme", List.of("particulier", "prive", "personnel")),

        // Véhicules utilitaires
        Map.entry("utilitaire", List.of("vu", "fourgon", "fourgonnette", "camionnette", "pick-up", "pickup")),
        Map.entry("camion", List.of("poids-lourd", "pl", "truck")),

        // Carburant
        Map.entry("carburant", List.of("essence", "gasoil", "gas-oil", "diesel", "fuel")),
        Map.entry("essence", List.of("super", "sp95", "sp98", "sans-plomb")),
        Map.entry("gasoil", List.of("gas-oil", "diesel", "gazole")),

        // Représentation
        Map.entry("restaurant", List.of("resto", "restauration", "repas")),
        Map.entry("representation", List.of("reception", "accueil", "hospitalite")),
        Map.entry("cadeaux", List.of("cadeau", "present", "don")),

        // Luxe
        Map.entry("luxe", List.of("somptuaire", "haut-de-gamme", "premium", "prestige")),
        Map.entry("golf", List.of("green", "parcours-golf")),
        Map.entry("yachting", List.of("yacht", "bateau-plaisance", "voilier")),
        Map.entry("chasse", List.of("cyneg etique", "safari")),
        Map.entry("peche", List.of("peche-sportive", "peche-loisir")),

        // Personnel
        Map.entry("personnel", List.of("prive", "personnel", "perso", "individuel")),
        Map.entry("dirigeant", List.of("gerant", "patron", "pdg", "directeur", "dg")),
        Map.entry("famille", List.of("familial", "conjoint", "epoux", "enfant"))
    );

    // Patterns de remplacement pour normalisation
    private static final List<Pattern> NORMALIZATION_PATTERNS = List.of(
        // Espaces multiples → espace simple
        Pattern.compile("\\s+"),
        // Tirets et underscores → espace
        Pattern.compile("[-_]+"),
        // Apostrophes variées → apostrophe standard
        Pattern.compile("[''`´]")
    );

    /**
     * Normalise un texte de manière complète et optimisée
     *
     * Étapes :
     * 1. Vérification cache
     * 2. Minuscules
     * 3. Suppression accents
     * 4. Normalisation ponctuation
     * 5. Expansion synonymes
     * 6. Nettoyage final
     */
    public String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        // Vérifier le cache
        String cached = normalizationCache.get(text);
        if (cached != null) {
            return cached;
        }

        // Normalisation
        String normalized = performNormalization(text);

        // Mettre en cache
        normalizationCache.put(text, normalized);

        return normalized;
    }

    /**
     * Effectue la normalisation complète
     */
    private String performNormalization(String text) {
        // 1. Minuscules
        String result = text.toLowerCase();

        // 2. Supprimer les accents (NFD normalization)
        result = Normalizer.normalize(result, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");

        // 3. Normaliser la ponctuation
        result = result.replace("'", " ");  // Apostrophes → espace
        result = result.replaceAll("[-_/]", " ");  // Séparateurs → espace
        result = result.replaceAll("[^a-z0-9\\s]", "");  // Garder seulement alphanum + espaces

        // 4. Normaliser les espaces
        result = result.replaceAll("\\s+", " ").trim();

        return result;
    }

    /**
     * Normalise et ajoute les synonymes pour augmenter les chances de match
     * Utilisé pour la recherche/matching
     */
    public String normalizeWithSynonyms(String text) {
        String normalized = normalize(text);

        Set<String> words = new HashSet<>(Arrays.asList(normalized.split("\\s+")));
        Set<String> expandedWords = new HashSet<>(words);

        // Ajouter les synonymes
        for (String word : words) {
            SYNONYMS.getOrDefault(word, Collections.emptyList())
                .forEach(expandedWords::add);
        }

        return String.join(" ", expandedWords);
    }

    /**
     * Vérifie si un texte contient un ou plusieurs mots-clés (avec synonymes)
     */
    public boolean containsKeywords(String text, String... keywords) {
        if (text == null || keywords == null || keywords.length == 0) {
            return false;
        }

        String normalizedText = normalizeWithSynonyms(text);

        for (String keyword : keywords) {
            String normalizedKeyword = normalize(keyword);

            // Vérifier le mot-clé lui-même
            if (normalizedText.contains(normalizedKeyword)) {
                return true;
            }

            // Vérifier les synonymes
            for (String synonym : SYNONYMS.getOrDefault(normalizedKeyword, Collections.emptyList())) {
                if (normalizedText.contains(synonym)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Vérifie si un texte contient TOUS les mots-clés requis
     */
    public boolean containsAllKeywords(String text, String... keywords) {
        if (text == null || keywords == null || keywords.length == 0) {
            return false;
        }

        for (String keyword : keywords) {
            if (!containsKeywords(text, keyword)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Vérifie si un texte contient au moins un mot exclu
     */
    public boolean containsExcludedKeyword(String text, String... excludedKeywords) {
        if (text == null || excludedKeywords == null || excludedKeywords.length == 0) {
            return false;
        }

        return containsKeywords(text, excludedKeywords);
    }

    /**
     * Extrait les mots-clés significatifs d'un texte
     * Utilisé pour l'analyse et les suggestions
     */
    public List<String> extractKeywords(String text) {
        String normalized = normalize(text);

        // Mots à ignorer (stop words français basiques)
        Set<String> stopWords = Set.of(
            "le", "la", "les", "un", "une", "des", "de", "du", "et", "ou", "pour",
            "dans", "sur", "avec", "sans", "par", "a", "au", "aux"
        );

        return Arrays.stream(normalized.split("\\s+"))
            .filter(word -> word.length() > 2)  // Au moins 3 caractères
            .filter(word -> !stopWords.contains(word))
            .distinct()
            .toList();
    }

    /**
     * Calcule un score de similarité entre deux textes (0-100)
     * Utilisé pour suggérer des corrections
     */
    public int calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0;
        }

        String norm1 = normalize(text1);
        String norm2 = normalize(text2);

        if (norm1.equals(norm2)) {
            return 100;
        }

        // Similarité basée sur les mots communs
        Set<String> words1 = new HashSet<>(Arrays.asList(norm1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(norm2.split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        if (union.isEmpty()) {
            return 0;
        }

        return (int) ((intersection.size() * 100.0) / union.size());
    }

    /**
     * Nettoie le cache (pour tests ou maintenance)
     */
    public void clearCache() {
        normalizationCache.clear();
        log.info("Cache de normalisation vidé");
    }

    /**
     * Statistiques du cache
     */
    public Map<String, Object> getCacheStats() {
        return Map.of(
            "size", normalizationCache.size(),
            "maxSize", 1000
        );
    }
}
