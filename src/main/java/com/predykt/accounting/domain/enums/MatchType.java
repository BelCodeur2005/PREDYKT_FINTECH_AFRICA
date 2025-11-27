package com.predykt.accounting.domain.enums;

/**
 * Type de matching pour les règles de mapping d'activités
 */
public enum MatchType {
    /**
     * Contient le mot-clé (case insensitive par défaut)
     * Ex: "vente" matche "Vente client ABC"
     */
    CONTAINS,

    /**
     * Correspond exactement
     * Ex: "Ventes" matche uniquement "Ventes"
     */
    EXACT,

    /**
     * Commence par
     * Ex: "Achat" matche "Achat marchandises"
     */
    STARTS_WITH,

    /**
     * Se termine par
     * Ex: "export" matche "Vente export"
     */
    ENDS_WITH,

    /**
     * Expression régulière
     * Ex: "vente.*export" matche "Vente à l'export"
     */
    REGEX
}
