package com.predykt.accounting.domain.enums;

import lombok.Getter;

/**
 * Types de déclarations de TVA au Cameroun
 */
@Getter
public enum VATDeclarationType {

    /**
     * CA3 - Déclaration mensuelle de TVA
     * À soumettre avant le 15 du mois suivant
     */
    CA3_MONTHLY(
        "CA3",
        "Déclaration mensuelle de TVA",
        "Mensuelle",
        15
    ),

    /**
     * CA12 - Déclaration annuelle de TVA
     * Récapitulatif annuel
     */
    CA12_ANNUAL(
        "CA12",
        "Déclaration annuelle de TVA",
        "Annuelle",
        null
    );

    private final String code;
    private final String displayName;
    private final String frequency;
    private final Integer dueDay;  // Jour limite de dépôt (null si variable)

    VATDeclarationType(String code, String displayName, String frequency, Integer dueDay) {
        this.code = code;
        this.displayName = displayName;
        this.frequency = frequency;
        this.dueDay = dueDay;
    }

    /**
     * Retourne le message d'échéance
     */
    public String getDueMessage() {
        if (dueDay != null) {
            return String.format("À soumettre avant le %d du mois suivant", dueDay);
        }
        return "Échéance variable";
    }
}
