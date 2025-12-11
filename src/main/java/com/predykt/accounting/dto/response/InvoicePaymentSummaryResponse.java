package com.predykt.accounting.dto.response;

import com.predykt.accounting.domain.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO de réponse pour le résumé des paiements d'une facture
 *
 * Utilisé pour afficher l'historique des paiements fractionnés
 * avec statistiques et informations de la facture.
 *
 * Conforme OHADA - Option B : Enregistrement séparé de chaque paiement
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoicePaymentSummaryResponse {

    // ==================== Informations de la facture ====================

    private Long invoiceId;
    private String invoiceNumber;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private InvoiceStatus status;

    // Client
    private Long customerId;
    private String customerName;

    // ==================== Montants ====================

    private BigDecimal totalTtc;           // Montant total de la facture
    private BigDecimal amountPaid;         // Montant déjà payé
    private BigDecimal amountDue;          // Montant restant dû
    private BigDecimal paymentPercentage;  // Pourcentage payé (0-100)

    // ==================== Statistiques des paiements ====================

    private Integer paymentCount;          // Nombre de paiements enregistrés
    private Boolean hasFractionalPayments; // Plus d'un paiement ?
    private Boolean isFullyPaid;           // Totalement payé ?
    private Boolean isOverdue;             // En retard ?
    private Integer daysOverdue;           // Nombre de jours de retard

    // ==================== Liste des paiements ====================

    @Builder.Default
    private List<PaymentResponse> payments = new ArrayList<>();

    // ==================== Historique des paiements (simplifié) ====================

    /**
     * DTO pour un paiement dans l'historique
     * Version allégée pour ne pas dupliquer toutes les données
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSummary {
        private Long paymentId;
        private String paymentNumber;
        private LocalDate paymentDate;
        private BigDecimal amount;
        private String paymentMethod;
        private Boolean isReconciled;
        private String description;
    }

    @Builder.Default
    private List<PaymentSummary> paymentHistory = new ArrayList<>();
}
