package com.predykt.accounting.domain.enums;

/**
 * Type de paiement
 */
public enum PaymentType {
    /**
     * Encaissement client (paiement d'une facture client)
     */
    CUSTOMER_PAYMENT,

    /**
     * Décaissement fournisseur (paiement d'une facture fournisseur)
     */
    SUPPLIER_PAYMENT
}
