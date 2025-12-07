package com.predykt.accounting.domain.enums;

/**
 * Type de facture client
 */
public enum InvoiceType {
    /**
     * Facture standard (vente normale)
     */
    STANDARD,

    /**
     * Facture proforma (devis transformé en facture)
     */
    PROFORMA,

    /**
     * Avoir (note de crédit - annulation partielle ou totale)
     */
    AVOIR
}
