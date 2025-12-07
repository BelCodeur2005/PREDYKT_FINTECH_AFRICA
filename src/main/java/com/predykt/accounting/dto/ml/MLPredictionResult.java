package com.predykt.accounting.dto.ml;

import com.predykt.accounting.domain.entity.BankTransaction;
import com.predykt.accounting.domain.entity.GeneralLedger;
import lombok.*;

/**
 * DTO représentant le résultat d'une prédiction ML
 *
 * @author PREDYKT ML Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLPredictionResult {

    /**
     * Transaction bancaire
     */
    private BankTransaction bankTransaction;

    /**
     * Écriture comptable
     */
    private GeneralLedger glEntry;

    /**
     * Score de confiance ML (0-100)
     */
    private Double confidenceScore;

    /**
     * Features utilisées pour la prédiction
     */
    private MatchFeatures features;

    /**
     * Version du modèle ML utilisé
     */
    private String modelVersion;

    /**
     * Temps de calcul en millisecondes
     */
    private Long predictionTimeMs;

    /**
     * Explication de la prédiction (pour UI)
     */
    private String explanation;

    /**
     * ID du log de prédiction (pour traçabilité)
     */
    private Long predictionLogId;

    /**
     * Vérifie si la confiance est suffisante pour auto-validation
     */
    public boolean isHighConfidence() {
        return confidenceScore != null && confidenceScore >= 95.0;
    }

    /**
     * Génère une explication textuelle de la prédiction
     */
    public void generateExplanation() {
        if (features == null) {
            this.explanation = "Prédiction ML basée sur modèle " + modelVersion;
            return;
        }

        StringBuilder sb = new StringBuilder("Match ML suggéré car:\n");

        // Analyse des features importantes
        if (features.getAmountDifference() != null && features.getAmountDifference() < 100) {
            sb.append(String.format("✅ Montants quasi-identiques (diff: %.0f XAF)\n",
                features.getAmountDifference()));
        }

        if (features.getDateDiffDays() != null && features.getDateDiffDays() <= 3) {
            sb.append(String.format("✅ Dates proches (%d jour(s))\n",
                features.getDateDiffDays()));
        }

        if (features.getTextSimilarity() != null && features.getTextSimilarity() > 0.7) {
            sb.append(String.format("✅ Descriptions similaires (%.0f%%)\n",
                features.getTextSimilarity() * 100));
        }

        if (Boolean.TRUE.equals(features.getReferenceMatch() > 0)) {
            sb.append("✅ Références identiques\n");
        }

        sb.append(String.format("\nConfiance ML: %.1f%%", confidenceScore));

        this.explanation = sb.toString();
    }
}