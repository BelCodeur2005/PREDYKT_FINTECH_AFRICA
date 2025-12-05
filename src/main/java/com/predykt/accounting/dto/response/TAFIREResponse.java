package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO pour le TAFIRE (Tableau Financier des Ressources et Emplois)
 * Conforme OHADA - Obligatoire pour grandes entreprises
 *
 * Le TAFIRE analyse les flux financiers de l'entreprise sur un exercice
 * et explique la variation du fonds de roulement et de la trésorerie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TAFIREResponse {

    private Long companyId;
    private String companyName;
    private Integer fiscalYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private String currency;

    // ==================== I. RESSOURCES STABLES ====================

    private RessourcesStables ressourcesStables;

    // ==================== II. EMPLOIS STABLES ====================

    private EmploisStables emploisStables;

    // ==================== III. VARIATION FRNG ====================

    private BigDecimal variationFRNG; // Variation Fonds de Roulement Net Global

    // ==================== IV. VARIATION BFR ====================

    private VariationBFR variationBFR;

    // ==================== V. VARIATION TRÉSORERIE ====================

    private VariationTresorerie variationTresorerie;

    // ==================== VÉRIFICATION ====================

    private Boolean isBalanced; // Vérification cohérence
    private String analysisComment; // Commentaire d'analyse

    /**
     * I. RESSOURCES STABLES DE L'EXERCICE
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RessourcesStables {

        // Ressources internes
        private BigDecimal capaciteAutofinancement; // CAF
        private BigDecimal cessionsDImmobilisations;

        // Ressources externes
        private BigDecimal augmentationCapital;
        private BigDecimal empruntsLongTerme;
        private BigDecimal subventionsDInvestissement;
        private BigDecimal autresRessources;

        // Total
        private BigDecimal totalRessourcesInternes;
        private BigDecimal totalRessourcesExternes;
        private BigDecimal totalRessourcesStables;

        // Détail CAF
        private CAFDetail cafDetail;
    }

    /**
     * Détail de la Capacité d'Autofinancement (CAF)
     * CAF = Résultat net + Dotations - Reprises + VNC cessions - Produits cessions
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CAFDetail {
        private BigDecimal resultatNet;
        private BigDecimal dotationsAuxAmortissements;
        private BigDecimal dotationsAuxProvisions;
        private BigDecimal reprisesProvisions;
        private BigDecimal vncCessionsActifs;
        private BigDecimal produitsCessionsActifs;
        private BigDecimal autresRetraitements;
        private BigDecimal capaciteAutofinancement; // CAF finale

        // Méthode de calcul utilisée
        private String calculMethod; // "ADDITIVE" ou "SOUSTRACTIVE"
    }

    /**
     * II. EMPLOIS STABLES DE L'EXERCICE
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmploisStables {

        // Acquisitions d'immobilisations
        private BigDecimal immobilisationsIncorporelles;
        private BigDecimal immobilisationsCorporelles;
        private BigDecimal immobilisationsFinancieres;

        // Autres emplois stables
        private BigDecimal remboursementsEmpruntsLongTerme;
        private BigDecimal dividendesVerses;
        private BigDecimal autresEmplois;

        // Total
        private BigDecimal totalAcquisitionsImmobilisations;
        private BigDecimal totalAutresEmplois;
        private BigDecimal totalEmploisStables;
    }

    /**
     * IV. VARIATION DU BESOIN EN FONDS DE ROULEMENT (BFR)
     * BFR = (Stocks + Créances) - (Dettes fournisseurs + Dettes fiscales/sociales)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariationBFR {

        // Variation de l'actif circulant (hors trésorerie)
        private BigDecimal variationStocks;
        private BigDecimal variationCreancesClients;
        private BigDecimal variationAutresCreances;

        // Variation des dettes circulantes (hors trésorerie)
        private BigDecimal variationDettesFournisseurs;
        private BigDecimal variationDettesFiscalesSociales;
        private BigDecimal variationAutresDettes;

        // Totaux
        private BigDecimal totalVariationActifCirculant;
        private BigDecimal totalVariationDettesCirculantes;

        // BFR
        private BigDecimal bfrDebutExercice;
        private BigDecimal bfrFinExercice;
        private BigDecimal variationBFR; // Positive = besoin accru
    }

    /**
     * V. VARIATION DE LA TRÉSORERIE
     * Trésorerie = FRNG - BFR
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariationTresorerie {

        // Trésorerie début et fin
        private BigDecimal tresorerieDebutExercice;
        private BigDecimal tresorerieFinExercice;

        // Calcul variation
        private BigDecimal variationFRNG;
        private BigDecimal variationBFR;
        private BigDecimal variationTresorerieCalculee; // FRNG - BFR

        // Vérification
        private BigDecimal variationTresorerieRelle; // Fin - Début
        private BigDecimal ecart; // Doit être 0
        private Boolean isVerified; // true si écart = 0

        // Décomposition trésorerie
        private BigDecimal banqueDebutExercice;
        private BigDecimal banqueFinExercice;
        private BigDecimal caisseDebutExercice;
        private BigDecimal caisseFinExercice;
    }

    /**
     * Analyse financière automatique du TAFIRE
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalyseFinanciere {
        private String situationTresorerie; // "EXCELLENTE", "BONNE", "TENDUE", "CRITIQUE"
        private String evolutionFRNG; // "AMELIORATION", "DETERIORATION", "STABLE"
        private String evolutionBFR; // "AUGMENTATION", "DIMINUTION", "STABLE"

        private List<String> pointsForts;
        private List<String> pointsVigilance;
        private List<String> recommandations;

        // Ratios d'analyse
        private BigDecimal tauxAutofinancement; // CAF / Total ressources stables
        private BigDecimal ratioInvestissementImmobilisations; // Acquis immo / Total emplois
        private BigDecimal couvertureBFRparFRNG; // FRNG / BFR
    }
}
