package com.predykt.accounting.domain.enums;

/**
 * Statut d'un rapprochement bancaire
 * Conforme aux pratiques OHADA
 */
public enum ReconciliationStatus {
    DRAFT("Brouillon", "Rapprochement en cours de préparation"),
    PENDING_REVIEW("En attente de révision", "Rapprochement terminé, en attente de validation"),
    REVIEWED("Révisé", "Rapprochement validé par le comptable"),
    APPROVED("Approuvé", "Rapprochement approuvé par le responsable"),
    REJECTED("Rejeté", "Rapprochement rejeté, corrections nécessaires"),
    ARCHIVED("Archivé", "Rapprochement clôturé et archivé");

    private final String displayName;
    private final String description;

    ReconciliationStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isFinal() {
        return this == APPROVED || this == ARCHIVED;
    }

    public boolean canEdit() {
        return this == DRAFT || this == REJECTED;
    }
}
