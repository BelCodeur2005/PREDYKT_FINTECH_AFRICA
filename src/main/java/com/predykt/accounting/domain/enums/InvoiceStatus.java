package com.predykt.accounting.domain.enums;

/**
 * Statuts de facture pour CabinetInvoice (MODE CABINET)
 */
public enum InvoiceStatus {
    /**
     * En attente de paiement
     */
    PENDING,

    /**
     * Payée
     */
    PAID,

    /**
     * En retard (calculé dynamiquement)
     */
    OVERDUE,

    /**
     * Annulée
     */
    CANCELLED
}
