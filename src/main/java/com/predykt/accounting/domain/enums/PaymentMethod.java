package com.predykt.accounting.domain.enums;

/**
 * Moyen de paiement
 */
public enum PaymentMethod {
    /**
     * Espèces (Cash)
     */
    CASH,

    /**
     * Virement bancaire
     */
    BANK_TRANSFER,

    /**
     * Chèque
     */
    CHEQUE,

    /**
     * Mobile Money (MTN Mobile Money, Orange Money)
     */
    MOBILE_MONEY,

    /**
     * Carte bancaire
     */
    CARD
}
