package com.predykt.accounting.domain.enums;

/**
 * Statut de facture fournisseur (Bill)
 */
public enum BillStatus {
    /**
     * Brouillon (en cours de création)
     */
    DRAFT,

    /**
     * Émise (reçue du fournisseur)
     */
    ISSUED,

    /**
     * Payée (totalement réglée)
     */
    PAID,

    /**
     * Partiellement payée
     */
    PARTIAL_PAID,

    /**
     * En retard (échéance dépassée)
     */
    OVERDUE,

    /**
     * Annulée
     */
    CANCELLED
}
