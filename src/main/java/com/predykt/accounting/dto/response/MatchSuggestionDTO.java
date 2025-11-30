package com.predykt.accounting.dto.response;

import com.predykt.accounting.domain.enums.PendingItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Suggestion de matching automatique entre transaction bancaire et écriture comptable
 * Score de confiance :
 * - 100% = Correspondance exacte (montant + date + référence)
 * - 90-99% = Montant exact, date proche (±3 jours)
 * - 70-89% = Montant proche (±5%), date proche
 * - <70% = À vérifier manuellement
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchSuggestionDTO {

    /**
     * ID unique de la suggestion (pour validation/rejet)
     */
    private String suggestionId;

    /**
     * Type d'opération en suspens identifiée
     */
    private PendingItemType suggestedItemType;

    /**
     * Score de confiance (0-100)
     */
    private BigDecimal confidenceScore;

    /**
     * Niveau de confiance en texte
     */
    private String confidenceLevel; // "EXCELLENT", "GOOD", "FAIR", "LOW"

    /**
     * Transaction bancaire source (si applicable)
     */
    private Long bankTransactionId;
    private LocalDate bankTransactionDate;
    private BigDecimal bankTransactionAmount;
    private String bankTransactionDescription;
    private String bankReference;

    /**
     * Écriture comptable correspondante (si trouvée)
     */
    private Long glEntryId;
    private LocalDate glEntryDate;
    private BigDecimal glEntryAmount;
    private String glEntryDescription;
    private String glEntryReference;
    private String accountNumber;
    private String accountName;

    /**
     * Détails de la suggestion
     */
    private BigDecimal suggestedAmount;
    private String description;
    private String thirdParty;
    private LocalDate transactionDate;

    /**
     * Raison du matching
     */
    private String matchingReason;

    /**
     * Indique si cette suggestion nécessite une action du comptable
     */
    private boolean requiresManualReview;

    /**
     * Calcule le niveau de confiance basé sur le score
     */
    public String calculateConfidenceLevel() {
        if (confidenceScore.compareTo(new BigDecimal("95")) >= 0) {
            return "EXCELLENT";
        } else if (confidenceScore.compareTo(new BigDecimal("80")) >= 0) {
            return "GOOD";
        } else if (confidenceScore.compareTo(new BigDecimal("60")) >= 0) {
            return "FAIR";
        } else {
            return "LOW";
        }
    }
}
