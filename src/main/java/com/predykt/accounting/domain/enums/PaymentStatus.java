package com.predykt.accounting.domain.enums;

/**
 * Statut de paiement
 */
public enum PaymentStatus {
    /**
     * En attente (paiement initié mais pas encore validé)
     */
    PENDING,

    /**
     * Complété (paiement validé)
     */
    COMPLETED,

    /**
     * Annulé
     */
    CANCELLED,

    /**
     * Rejeté (chèque sans provision, virement échoué)
     */
    BOUNCED
}
