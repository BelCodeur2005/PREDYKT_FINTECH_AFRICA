package com.predykt.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO intermédiaire pour l'import d'activités
 * Utilisé par tous les parsers pour normaliser les données
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityImportDto {

    // Données sources
    private LocalDate date;
    private String activity;           // Nom de l'activité
    private String description;
    private BigDecimal amount;
    private String type;                // Revenu, Dépense, Capex, Financing

    // Détection automatique (rempli par ActivityMappingService)
    private String detectedAccount;     // Compte OHADA détecté
    private String accountName;         // Nom du compte
    private String journalCode;         // Code journal suggéré (VE, AC, BQ, OD)
    private Integer confidenceScore;    // 0-100

    // Métadonnées
    private Integer rowNumber;          // Numéro de ligne dans le fichier source
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();  // Données supplémentaires

    /**
     * Ajoute un warning
     */
    public void addWarning(String warning) {
        if (this.warnings == null) {
            this.warnings = new ArrayList<>();
        }
        this.warnings.add(warning);
    }

    /**
     * Ajoute une erreur
     */
    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
    }

    /**
     * Vérifie si la ligne est valide (pas d'erreurs)
     */
    public boolean isValid() {
        return errors == null || errors.isEmpty();
    }

    /**
     * Détermine le niveau de confiance
     */
    public String getConfidenceLevel() {
        if (confidenceScore == null) {
            return "UNKNOWN";
        }
        if (confidenceScore >= 80) {
            return "HIGH";
        }
        if (confidenceScore >= 50) {
            return "MEDIUM";
        }
        return "LOW";
    }
}
