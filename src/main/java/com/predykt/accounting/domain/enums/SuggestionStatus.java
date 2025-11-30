package com.predykt.accounting.domain.enums;

import lombok.Getter;

/**
 * Statut d'une suggestion de matching automatique
 */
@Getter
public enum SuggestionStatus {

    /**
     * Suggestion générée par l'algorithme, en attente de décision du comptable
     */
    PENDING("En attente", "La suggestion est en attente de validation"),

    /**
     * Suggestion appliquée par le comptable (transformée en BankReconciliationItem)
     */
    APPLIED("Appliquée", "La suggestion a été appliquée au rapprochement"),

    /**
     * Suggestion rejetée par le comptable
     */
    REJECTED("Rejetée", "La suggestion a été rejetée par le comptable"),

    /**
     * Suggestion expirée (rapprochement approuvé sans appliquer cette suggestion)
     */
    EXPIRED("Expirée", "La suggestion a expiré");

    private final String displayName;
    private final String description;

    SuggestionStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
