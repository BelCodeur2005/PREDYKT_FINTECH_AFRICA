package com.predykt.accounting.domain.enums;

import lombok.Getter;

/**
 * Méthodes d'amortissement conformes OHADA et fiscalité camerounaise
 * Référence: Code Général des Impôts Cameroun (CGI)
 */
@Getter
public enum DepreciationMethod {

    /**
     * AMORTISSEMENT LINÉAIRE (LINEAR)
     * Méthode par défaut conforme OHADA
     * Dotation constante chaque année
     * Formule: Valeur d'origine × Taux linéaire
     */
    LINEAR(
        "Amortissement linéaire",
        "Dotation constante sur toute la durée de vie",
        true,
        "Méthode recommandée OHADA - Obligatoire pour immeubles"
    ),

    /**
     * AMORTISSEMENT DÉGRESSIF (DECLINING_BALANCE)
     * Autorisé au Cameroun pour certains biens
     * Dotations plus élevées en début de vie
     * Formule: VNC × Taux dégressif (Taux linéaire × Coefficient)
     *
     * Coefficients fiscaux Cameroun:
     * - Durée 3-4 ans: coefficient 1,5
     * - Durée 5-6 ans: coefficient 2,0
     * - Durée > 6 ans: coefficient 2,5
     */
    DECLINING_BALANCE(
        "Amortissement dégressif",
        "Dotation décroissante - Dotations plus élevées au début",
        true,
        "Autorisé pour matériel, véhicules, équipements - Interdit pour immeubles"
    ),

    /**
     * AMORTISSEMENT VARIABLE (Usage ou production)
     * Basé sur l'utilisation réelle (heures, kilomètres, unités produites)
     * Formule: (Valeur - Résiduelle) × (Unités période / Total unités estimées)
     */
    VARIABLE(
        "Amortissement variable",
        "Basé sur l'utilisation réelle (heures, km, unités)",
        false,
        "Pour matériel à usage intensif - Nécessite suivi détaillé"
    ),

    /**
     * AMORTISSEMENT EXCEPTIONNEL
     * Amortissement accéléré sur 12 ou 24 mois
     * Autorisé par administration fiscale pour certains secteurs
     */
    EXCEPTIONAL(
        "Amortissement exceptionnel",
        "Amortissement accéléré autorisé fiscalement",
        false,
        "Nécessite agrément fiscal - Secteurs prioritaires uniquement"
    );

    private final String displayName;
    private final String description;
    private final Boolean isStandard; // Méthode standard (LINEAR, DECLINING_BALANCE)
    private final String fiscalNotes;

    DepreciationMethod(String displayName, String description, Boolean isStandard, String fiscalNotes) {
        this.displayName = displayName;
        this.description = description;
        this.isStandard = isStandard;
        this.fiscalNotes = fiscalNotes;
    }

    /**
     * Calculer le coefficient dégressif selon la durée de vie (règles Cameroun)
     */
    public java.math.BigDecimal getDecliningBalanceCoefficient(Integer usefulLifeYears) {
        if (this != DECLINING_BALANCE || usefulLifeYears == null) {
            return java.math.BigDecimal.ONE;
        }

        if (usefulLifeYears <= 4) {
            return java.math.BigDecimal.valueOf(1.5);
        } else if (usefulLifeYears <= 6) {
            return java.math.BigDecimal.valueOf(2.0);
        } else {
            return java.math.BigDecimal.valueOf(2.5);
        }
    }

    /**
     * Vérifier si la méthode est autorisée pour une catégorie d'immobilisation
     */
    public boolean isAllowedForCategory(AssetCategory category) {
        if (category == null) return false;

        return switch (this) {
            case LINEAR ->
                // Linéaire autorisé pour tout
                    true;

            case DECLINING_BALANCE ->
                // Dégressif interdit pour terrains, immeubles, immobilisations incorporelles
                    category != AssetCategory.LAND &&
                            category != AssetCategory.BUILDING &&
                            category != AssetCategory.INTANGIBLE &&
                            category != AssetCategory.FINANCIAL;

            case VARIABLE ->
                // Variable surtout pour matériel et véhicules
                    category == AssetCategory.EQUIPMENT ||
                            category == AssetCategory.VEHICLE ||
                            category == AssetCategory.IT_EQUIPMENT;

            case EXCEPTIONAL ->
                // Exceptionnel sur autorisation uniquement
                    false; // Nécessite vérification manuelle
        };
    }

    /**
     * Obtenir la méthode par défaut pour une catégorie
     */
    public static DepreciationMethod getDefaultForCategory(AssetCategory category) {
        if (category == null) {
            return LINEAR;
        }

        return switch (category) {
            case LAND, FINANCIAL ->
                // Terrains et financiers ne s'amortissent pas
                    null;

            case BUILDING, INTANGIBLE ->
                // Immeubles et incorporels: linéaire obligatoire
                    LINEAR;

            case EQUIPMENT, VEHICLE, IT_EQUIPMENT ->
                // Matériel: dégressif recommandé (si autorisé fiscalement)
                    DECLINING_BALANCE;

            default ->
                // Par défaut: linéaire
                    LINEAR;
        };
    }
}
