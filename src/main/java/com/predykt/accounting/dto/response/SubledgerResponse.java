package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO pour les Grands Livres Auxiliaires (Clients et Fournisseurs)
 * Conforme OHADA
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubledgerResponse {

    private Long companyId;
    private String companyName;
    private String subledgerType; // "CLIENTS" ou "FOURNISSEURS"
    private LocalDate startDate;
    private LocalDate endDate;
    private String currency;

    // Détail par tiers
    private List<TiersDetail> tiersDetails;

    // Totaux globaux
    private BigDecimal totalSoldeOuverture;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private BigDecimal totalSoldeCloture;
    private Integer nombreTiers;
    private Integer nombreEcritures;

    // Statistiques
    private SubledgerStatistics statistics;

    /**
     * Détail d'un tiers (Client ou Fournisseur)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TiersDetail {
        private String accountNumber; // 411xxx ou 401xxx
        private String tiersName;
        private String tiersNiu; // Numéro d'identification unique
        private String tiersContact;

        // Soldes
        private BigDecimal soldeOuverture;
        private BigDecimal soldeCloture;

        // Mouvements
        private List<SubledgerEntry> entries;
        private BigDecimal totalDebits;
        private BigDecimal totalCredits;
        private Integer nombreEcritures;

        // Analyse
        private AnalyseTiers analyse;
    }

    /**
     * Ligne d'écriture dans le grand livre auxiliaire
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubledgerEntry {
        private LocalDate entryDate;
        private String pieceNumber;
        private String reference;
        private String description;
        private BigDecimal debitAmount;
        private BigDecimal creditAmount;
        private BigDecimal balance; // Solde cumulé après cette écriture
        private Boolean isReconciled;
        private Boolean isLocked;

        // Informations complémentaires pour clients/fournisseurs
        private String invoiceNumber;
        private LocalDate dueDate;
        private Integer delaiPaiementJours;
        private String paymentMethod;
    }

    /**
     * Analyse d'un tiers (client ou fournisseur)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalyseTiers {
        // Pour clients
        private BigDecimal creancesEnRetard;
        private BigDecimal creancesDouteuses;
        private Integer delaiMoyenPaiement; // En jours
        private BigDecimal chiffreAffairesAnnuel;

        // Pour fournisseurs
        private BigDecimal dettesEnRetard;
        private Integer delaiMoyenReglement; // En jours
        private BigDecimal volumeAchatsAnnuel;

        // Général
        private String categorieRisque; // "FAIBLE", "MOYEN", "ÉLEVÉ"
        private String commentaire;
    }

    /**
     * Statistiques globales du grand livre auxiliaire
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubledgerStatistics {
        // Pour grand livre clients
        private BigDecimal totalCreancesClients;
        private BigDecimal creancesAEchoir;
        private BigDecimal creancesEchues;
        private BigDecimal creancesDouteuses;
        private Integer delaiMoyenPaiementGlobal;
        private List<TopClient> topClients;

        // Pour grand livre fournisseurs
        private BigDecimal totalDettesFournisseurs;
        private BigDecimal dettesAEchoir;
        private BigDecimal dettesEchues;
        private Integer delaiMoyenReglementGlobal;
        private List<TopFournisseur> topFournisseurs;

        // Analyse par échéance
        private RepartitionEcheances repartitionEcheances;
    }

    /**
     * Top client (par chiffre d'affaires)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopClient {
        private String clientName;
        private String accountNumber;
        private BigDecimal chiffreAffaires;
        private BigDecimal soldeEnCours;
        private Integer nombreFactures;
        private Integer delaiMoyenPaiement;
    }

    /**
     * Top fournisseur (par volume d'achats)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopFournisseur {
        private String fournisseurName;
        private String accountNumber;
        private BigDecimal volumeAchats;
        private BigDecimal soldeEnCours;
        private Integer nombreFactures;
        private Integer delaiMoyenReglement;
    }

    /**
     * Répartition par échéance
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepartitionEcheances {
        private BigDecimal aEchoir; // Non échues
        private BigDecimal echuMoins30Jours;
        private BigDecimal echu30A60Jours;
        private BigDecimal echu60A90Jours;
        private BigDecimal echuPlus90Jours;
        private BigDecimal total;
    }
}
