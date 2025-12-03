package com.predykt.accounting.domain.enums;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Types de taxes applicables au Cameroun (OHADA)
 * Conforme aux règles fiscales camerounaises
 */
@Getter
public enum TaxType {

    /**
     * TVA - Taxe sur la Valeur Ajoutée (19,25%)
     * Compte OHADA: 4431 (TVA collectée), 4451 (TVA déductible)
     * Échéance: 15 du mois suivant
     */
    VAT(
        "TVA",
        "Taxe sur la Valeur Ajoutée",
        new BigDecimal("19.25"),
        "4431",
        "CGI Art. 127 - TVA au taux normal",
        15,
        true,
        true
    ),

    /**
     * Acompte IS (IMF) - Impôt Minimum Forfaitaire (2,2%)
     * Compte OHADA: 4411 (Acompte IS)
     * Appliqué sur le chiffre d'affaires mensuel
     * Échéance: 15 du mois suivant
     */
    IS_ADVANCE(
        "ACOMPTE_IS",
        "Acompte IS (IMF) - Impôt Minimum Forfaitaire",
        new BigDecimal("2.2"),
        "4411",
        "CGI Art. 19 - Acompte mensuel IS",
        15,
        true,
        false
    ),

    /**
     * AIR avec NIU - Acompte sur Impôt sur le Revenu (2,2%)
     * Compte OHADA: 4478 (Autres impôts et contributions)
     * Précompte applicable si le fournisseur possède un NIU
     * Échéance: 15 du mois suivant
     */
    AIR_WITH_NIU(
        "AIR_AVEC_NIU",
        "AIR (Précompte) - Fournisseur avec NIU",
        new BigDecimal("2.2"),
        "4478",
        "CGI Art. 156 - Précompte avec NIU",
        15,
        false,
        true
    ),

    /**
     * AIR sans NIU - Pénalité (5,5%)
     * Compte OHADA: 4478 (Autres impôts et contributions)
     * Taux majoré si le fournisseur n'a pas de NIU
     * Génère une alerte automatique
     * Échéance: 15 du mois suivant
     */
    AIR_WITHOUT_NIU(
        "AIR_SANS_NIU",
        "AIR (Précompte) - Fournisseur SANS NIU (Pénalité)",
        new BigDecimal("5.5"),
        "4478",
        "CGI Art. 156 - Précompte sans NIU (pénalité)",
        15,
        false,
        true
    ),

    /**
     * IRPP Loyer - Impôt sur le Revenu des Personnes Physiques (15%)
     * Compte OHADA: 4471 (Impôt Général sur le revenu)
     * Retenue à la source sur les loyers
     * 85% payé au bailleur, 15% reversé à l'État
     * Échéance: 15 du mois suivant
     */
    IRPP_RENT(
        "IRPP_LOYER",
        "IRPP Loyer - Retenue à la source",
        new BigDecimal("15.0"),
        "4471",
        "CGI Art. 65 - IRPP sur loyers",
        15,
        false,
        true
    ),

    /**
     * CNPS - Caisse Nationale de Prévoyance Sociale (~20%)
     * Compte OHADA: 431 (Sécurité sociale)
     * Estimation pour provisions sur salaires
     * Échéance: Variable selon calendrier CNPS
     */
    CNPS(
        "CNPS",
        "CNPS - Cotisations sociales (estimation)",
        new BigDecimal("20.0"),
        "431",
        "Code de Prévoyance Sociale",
        null,
        false,
        false
    );

    private final String code;
    private final String displayName;
    private final BigDecimal defaultRate;
    private final String defaultAccountNumber;
    private final String legalReference;
    private final Integer dueDay;  // Jour du mois pour paiement (null si variable)
    private final boolean applyToSales;
    private final boolean applyToPurchases;

    TaxType(String code, String displayName, BigDecimal defaultRate,
            String defaultAccountNumber, String legalReference,
            Integer dueDay, boolean applyToSales, boolean applyToPurchases) {
        this.code = code;
        this.displayName = displayName;
        this.defaultRate = defaultRate;
        this.defaultAccountNumber = defaultAccountNumber;
        this.legalReference = legalReference;
        this.dueDay = dueDay;
        this.applyToSales = applyToSales;
        this.applyToPurchases = applyToPurchases;
    }

    /**
     * Détermine le type d'AIR selon la présence du NIU
     */
    public static TaxType getAIRType(boolean hasNIU) {
        return hasNIU ? AIR_WITH_NIU : AIR_WITHOUT_NIU;
    }

    /**
     * Vérifie si cette taxe génère une alerte automatique
     */
    public boolean generatesAlert() {
        return this == AIR_WITHOUT_NIU;
    }

    /**
     * Vérifie si cette taxe s'applique aux ventes
     */
    public boolean isApplicableToSales() {
        return applyToSales;
    }

    /**
     * Vérifie si cette taxe s'applique aux achats
     */
    public boolean isApplicableToPurchases() {
        return applyToPurchases;
    }

    /**
     * Retourne le message d'alerte pour AIR sans NIU
     */
    public String getAlertMessage() {
        if (this == AIR_WITHOUT_NIU) {
            return "⚠️ ALERTE: Fournisseur sans NIU - Taux majoré à 5,5% (au lieu de 2,2%). Veuillez régulariser le dossier fournisseur.";
        }
        return null;
    }

    /**
     * Vérifie si la taxe est obligatoire (non désactivable)
     */
    public boolean isMandatory() {
        return this == VAT || this == IS_ADVANCE || this == AIR_WITH_NIU || this == AIR_WITHOUT_NIU;
    }
}
