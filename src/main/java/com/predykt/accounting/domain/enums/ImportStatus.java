package com.predykt.accounting.domain.enums;

/**
 * Statut d'un import d'activités
 */
public enum ImportStatus {
    PENDING("En attente"),
    PROCESSING("En cours de traitement"),
    COMPLETED("Terminé avec succès"),
    COMPLETED_WITH_ERRORS("Terminé avec erreurs"),
    FAILED("Échec"),
    CANCELLED("Annulé");

    private final String displayName;

    ImportStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
