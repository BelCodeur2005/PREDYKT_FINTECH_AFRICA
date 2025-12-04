package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Réponse contenant un rapport de balance âgée (clients ou fournisseurs)
 * Analyse par ancienneté: 0-30j, 30-60j, 60-90j, >90j
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgingReportResponse {

    private Long companyId;
    private String companyName;
    private LocalDate asOfDate;
    private String reportType; // "CUSTOMERS" ou "SUPPLIERS"

    // Liste des éléments (clients ou fournisseurs)
    private List<AgingItem> items;

    // Totaux par tranche d'âge
    private AgingSummary summary;

    // Analyse et recommandations
    private Analysis analysis;

    /**
     * Élément de la balance âgée (un client ou un fournisseur)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgingItem {
        private Long id;
        private String accountNumber;
        private String name;
        private String contactEmail;
        private String contactPhone;

        // Montants par tranche d'âge
        private BigDecimal current;        // 0-30 jours
        private BigDecimal days30to60;     // 30-60 jours
        private BigDecimal days60to90;     // 60-90 jours
        private BigDecimal over90Days;     // > 90 jours

        // Total
        private BigDecimal totalAmount;

        // Plus ancienne facture
        private LocalDate oldestInvoiceDate;
        private Integer oldestInvoiceDays;

        // Statut
        private String status; // "OK", "WARNING", "CRITICAL"
        private String statusIcon; // "✅", "⚠️", "🔴"

        // Nombre de factures en retard
        private Integer overdueInvoicesCount;
    }

    /**
     * Résumé des totaux par tranche d'âge
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgingSummary {
        // Totaux par tranche
        private BigDecimal totalCurrent;
        private BigDecimal totalDays30to60;
        private BigDecimal totalDays60to90;
        private BigDecimal totalOver90Days;

        // Grand total
        private BigDecimal grandTotal;

        // Pourcentages
        private BigDecimal percentCurrent;
        private BigDecimal percentDays30to60;
        private BigDecimal percentDays60to90;
        private BigDecimal percentOver90Days;

        // Nombre d'items
        private Integer totalItems;
        private Integer itemsOk;
        private Integer itemsWarning;
        private Integer itemsCritical;
    }

    /**
     * Analyse et recommandations
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Analysis {
        // Alertes
        private List<String> alerts;

        // Recommandations
        private List<String> recommendations;

        // Provision suggérée (pour créances douteuses)
        private BigDecimal suggestedProvision;

        // Items critiques (> 90 jours)
        private List<AgingItem> criticalItems;

        // Délai moyen de paiement
        private Integer averagePaymentDays;

        // Taux de retard
        private BigDecimal overdueRate; // % du total en retard
    }
}
