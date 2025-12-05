package com.predykt.accounting.domain.enums;

import lombok.Getter;
import java.math.BigDecimal;

/**
 * Catégories d'immobilisations selon la nomenclature OHADA
 * Référence: Plan Comptable OHADA - Classe 2
 */
@Getter
public enum AssetCategory {

    // Classe 21: Immobilisations incorporelles
    INTANGIBLE("Immobilisations incorporelles", "21", 5, true,
        "Frais de développement, brevets, licences, logiciels, fonds commercial"),

    // Classe 22: Terrains
    LAND("Terrains", "22", 0, false,
        "Terrains nus, terrains agricoles, terrains bâtis (non amortissables)"),

    // Classe 23: Bâtiments, installations techniques et agencements
    BUILDING("Bâtiments", "23", 20, true,
        "Bâtiments industriels, commerciaux, administratifs, installations techniques"),

    // Classe 24: Matériel
    EQUIPMENT("Matériel et outillage", "24", 5, true,
        "Matériel industriel, matériel et outillage, agencements et installations"),

    // Classe 245: Matériel de transport
    VEHICLE("Matériel de transport", "245", 4, true,
        "Véhicules de tourisme, utilitaires, camions, motos"),

    // Classe 2441: Mobilier de bureau
    FURNITURE("Mobilier et matériel de bureau", "2441", 10, true,
        "Tables, chaises, armoires, bureaux, rayonnages"),

    // Classe 2443: Matériel informatique
    IT_EQUIPMENT("Matériel informatique", "2443", 3, true,
        "Ordinateurs, serveurs, périphériques, équipements réseau"),

    // Classe 244: Matériel et mobilier (autres)
    OTHER_EQUIPMENT("Autres matériels", "244", 5, true,
        "Autres équipements non classés ailleurs"),

    // Classe 26: Immobilisations financières
    FINANCIAL("Immobilisations financières", "26", 0, false,
        "Titres de participation, prêts, dépôts et cautionnements");

    private final String displayName;
    private final String accountPrefix;
    private final Integer defaultUsefulLifeYears; // Durée fiscale Cameroun
    private final Boolean isDepreciable;
    private final String description;

    AssetCategory(String displayName, String accountPrefix, Integer defaultUsefulLifeYears,
                  Boolean isDepreciable, String description) {
        this.displayName = displayName;
        this.accountPrefix = accountPrefix;
        this.defaultUsefulLifeYears = defaultUsefulLifeYears;
        this.isDepreciable = isDepreciable;
        this.description = description;
    }

    /**
     * Obtenir le taux d'amortissement linéaire par défaut (en %)
     */
    public BigDecimal getDefaultLinearRate() {
        if (!isDepreciable || defaultUsefulLifeYears == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(100.0)
            .divide(BigDecimal.valueOf(defaultUsefulLifeYears),
                4, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Obtenir le compte d'amortissement correspondant (28x)
     */
    public String getDepreciationAccountNumber() {
        return "28" + accountPrefix.substring(1); // Ex: 21 → 281
    }

    /**
     * Obtenir le compte de dotation aux amortissements (681x)
     */
    public String getDepreciationExpenseAccountNumber() {
        return "681" + accountPrefix.substring(1); // Ex: 21 → 6811
    }

    /**
     * Trouver la catégorie à partir du numéro de compte OHADA
     */
    public static AssetCategory fromAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 2) {
            return null;
        }

        // Recherche exacte d'abord
        for (AssetCategory category : values()) {
            if (accountNumber.startsWith(category.accountPrefix)) {
                return category;
            }
        }

        // Recherche par classe générale (2 premiers chiffres)
        String classPrefix = accountNumber.substring(0, 2);
        for (AssetCategory category : values()) {
            if (category.accountPrefix.startsWith(classPrefix)) {
                return category;
            }
        }

        return null;
    }
}
