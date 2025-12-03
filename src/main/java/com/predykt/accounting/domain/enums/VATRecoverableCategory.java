package com.predykt.accounting.domain.enums;

import lombok.Getter;

/**
 * Catégories de TVA récupérable / non récupérable
 * Selon le Code Général des Impôts du Cameroun
 */
@Getter
public enum VATRecoverableCategory {

    /**
     * TVA 100% récupérable - Cas normal
     */
    FULLY_RECOVERABLE(
        "Totalement récupérable",
        100.0,
        "TVA intégralement déductible (achats professionnels normaux)"
    ),

    /**
     * TVA 80% récupérable - Carburant véhicules utilitaires (VU)
     */
    RECOVERABLE_80_PERCENT(
        "Récupérable à 80%",
        80.0,
        "TVA partiellement déductible (carburant VU selon réglementation)"
    ),

    /**
     * TVA 0% récupérable - Véhicules de tourisme
     * Catégorie principale pour TVA non récupérable
     */
    NON_RECOVERABLE_TOURISM_VEHICLE(
        "Non récupérable - Véhicule de tourisme",
        0.0,
        "TVA sur véhicules de tourisme (VP < 9 places)"
    ),

    /**
     * TVA 0% récupérable - Carburant véhicules de tourisme
     */
    NON_RECOVERABLE_FUEL_VP(
        "Non récupérable - Carburant VP",
        0.0,
        "TVA sur carburant pour véhicules de tourisme"
    ),

    /**
     * TVA 0% récupérable - Frais de représentation
     */
    NON_RECOVERABLE_REPRESENTATION(
        "Non récupérable - Représentation",
        0.0,
        "TVA sur frais de représentation (restaurants, hôtels non justifiés)"
    ),

    /**
     * TVA 0% récupérable - Dépenses somptuaires
     */
    NON_RECOVERABLE_LUXURY(
        "Non récupérable - Dépenses de luxe",
        0.0,
        "TVA sur dépenses somptuaires et de luxe"
    ),

    /**
     * TVA 0% récupérable - Services personnels
     */
    NON_RECOVERABLE_PERSONAL(
        "Non récupérable - Services personnels",
        0.0,
        "TVA sur services à usage personnel (non professionnel)"
    );

    private final String displayName;
    private final Double recoverablePercentage;
    private final String description;

    VATRecoverableCategory(String displayName, Double recoverablePercentage, String description) {
        this.displayName = displayName;
        this.recoverablePercentage = recoverablePercentage;
        this.description = description;
    }

    /**
     * Vérifie si la TVA est totalement récupérable
     */
    public boolean isFullyRecoverable() {
        return recoverablePercentage == 100.0;
    }

    /**
     * Vérifie si la TVA est partiellement récupérable
     */
    public boolean isPartiallyRecoverable() {
        return recoverablePercentage > 0.0 && recoverablePercentage < 100.0;
    }

    /**
     * Vérifie si la TVA est non récupérable
     */
    public boolean isNonRecoverable() {
        return recoverablePercentage == 0.0;
    }

    /**
     * Calcule le montant de TVA récupérable
     */
    public java.math.BigDecimal calculateRecoverableAmount(java.math.BigDecimal totalVAT) {
        if (totalVAT == null) {
            return java.math.BigDecimal.ZERO;
        }
        return totalVAT
            .multiply(java.math.BigDecimal.valueOf(recoverablePercentage))
            .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Calcule le montant de TVA non récupérable
     */
    public java.math.BigDecimal calculateNonRecoverableAmount(java.math.BigDecimal totalVAT) {
        if (totalVAT == null) {
            return java.math.BigDecimal.ZERO;
        }
        return totalVAT.subtract(calculateRecoverableAmount(totalVAT));
    }
}
