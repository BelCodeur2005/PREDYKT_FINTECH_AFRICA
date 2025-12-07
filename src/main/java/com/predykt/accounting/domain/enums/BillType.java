package com.predykt.accounting.domain.enums;

/**
 * Type de facture fournisseur
 */
public enum BillType {
    /**
     * Achat de marchandises ou fournitures
     */
    PURCHASE,

    /**
     * Prestation de services
     */
    SERVICES,

    /**
     * Loyer (soumis à IRPP Loyer 15%)
     */
    RENT,

    /**
     * Services publics (eau, électricité, téléphone)
     */
    UTILITIES
}
