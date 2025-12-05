package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO pour les Notes Annexes OHADA
 *
 * Les 12 notes obligatoires selon le système comptable OHADA
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotesAnnexesResponse {

    private Long companyId;
    private String companyName;
    private Integer fiscalYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private String currency;

    // Les 12 notes OHADA
    private Note1_PrincipesComptables note1;
    private Note2_Immobilisations note2;
    private Note3_ImmobilisationsFinancieres note3;
    private Note4_Stocks note4;
    private Note5_CreancesEtDettes note5;
    private Note6_CapitauxPropres note6;
    private Note7_EmpruntsEtDettes note7;
    private Note8_AutresPassifs note8;
    private Note9_ProduitsEtCharges note9;
    private Note10_ImpotsEtTaxes note10;
    private Note11_EngagementsHorsBilan note11;
    private Note12_EvenementsPosterieur note12;

    /**
     * NOTE 1: Principes et méthodes comptables
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Note1_PrincipesComptables {
        private String referentielComptable; // "OHADA - Système normal"
        private String methodesEvaluation;
        private String methodesAmortissement;
        private String methodesStocks; // FIFO, CMUP, etc.
        private String principesRetenus;
        private List<ChangementMethode> changementsMethodes;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ChangementMethode {
            private String ancienneMethode;
            private String nouvelleMethode;
            private String justification;
            private BigDecimal impactMontant;
        }
    }

    /**
     * NOTE 2: Immobilisations corporelles et incorporelles
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Note2_Immobilisations {
        private TableauMouvements immobilisationsIncorporelles;
        private TableauMouvements immobilisationsCorporelles;
        private List<MethodeAmortissement> methodesAmortissement;
        private List<Cession> cessions;
        private String commentaire;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class TableauMouvements {
            private String categorie;
            private BigDecimal valeurBruteDebut;
            private BigDecimal amortissementsCumules;
            private BigDecimal valeurNetteDebut;
            private BigDecimal acquisitions;
            private BigDecimal cessions;
            private BigDecimal dotationsAmortissement;
            private BigDecimal valeurBruteFin;
            private BigDecimal valeurNetteFin;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class MethodeAmortissement {
            private String categorie;
            private String methode; // Linéaire, Dégressif
            private Integer dureeAns;
            private BigDecimal tauxPourcentage;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Cession {
            private String description;
            private BigDecimal vnc; // Valeur nette comptable
            private BigDecimal prixVente;
            private BigDecimal plusMoinsValue;
        }
    }

    /**
     * NOTE 3: Immobilisations financières
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Note3_ImmobilisationsFinancieres {
        private List<Participation> participations;
        private List<Pret> pretsLongTerme;
        private List<DepotGarantie> depotsEtCautionnements;
        private BigDecimal total;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Participation {
            private String societe;
            private BigDecimal pourcentageDetention;
            private BigDecimal valeurComptable;
            private BigDecimal valeurMarche;
            private String commentaire;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Pret {
            private String beneficiaire;
            private BigDecimal montant;
            private BigDecimal tauxInteret;
            private LocalDate dateEcheance;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DepotGarantie {
            private String nature;
            private BigDecimal montant;
            private String beneficiaire;
        }
    }

    /**
     * NOTE 4: Stocks
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Note4_Stocks {
        private String methodeEvaluation; // FIFO, CMUP, etc.
        private List<CategorieStock> categories;
        private BigDecimal totalStocksDebut;
        private BigDecimal totalStocksFin;
        private BigDecimal variationStocks;
        private BigDecimal provisionsDepreciation;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CategorieStock {
            private String categorie;
            private BigDecimal quantite;
            private BigDecimal valeurUnitaire;
            private BigDecimal valeurTotale;
            private String commentaire;
        }
    }

    /**
     * NOTE 5: Créances et dettes
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Note5_CreancesEtDettes {
        private EcheancierCreances creances;
        private EcheancierDettes dettes;
        private BigDecimal provisionsCreancesDouteuses;
        private String commentaire;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class EcheancierCreances {
            private BigDecimal creancesClients;
            private BigDecimal aEchoir; // Non échues
            private BigDecimal echuMoins30Jours;
            private BigDecimal echu30A90Jours;
            private BigDecimal echuPlus90Jours;
            private BigDecimal creancesDouteuses;
            private BigDecimal autresCreances;
            private BigDecimal total;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class EcheancierDettes {
            private BigDecimal dettesFournisseurs;
            private BigDecimal dettesCourtTerme;
            private BigDecimal dettesMoyenTerme;
            private BigDecimal dettesLongTerme;
            private BigDecimal autresDettes;
            private BigDecimal total;
        }
    }

    /**
     * NOTE 6: Capitaux propres
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Note6_CapitauxPropres {
        private List<ComposanteCapitaux> composantes;
        private TableauVariation tableauVariation;
        private BigDecimal capitalDebut;
        private BigDecimal capitalFin;
        private String commentaire;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ComposanteCapitaux {
            private String libelle;
            private BigDecimal montantDebut;
            private BigDecimal variation;
            private BigDecimal montantFin;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class TableauVariation {
            private BigDecimal capitalSocial;
            private BigDecimal primes;
            private BigDecimal reserves;
            private BigDecimal reportANouveau;
            private BigDecimal resultatExercice;
            private BigDecimal total;
        }
    }

    /**
     * NOTE 7: Emprunts et dettes financières
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Note7_EmpruntsEtDettes {
        private List<Emprunt> emprunts;
        private EcheancierRemboursements echeancier;
        private BigDecimal totalEmpruntsLongTerme;
        private BigDecimal totalEmpruntsCourtTerme;
        private String commentaire;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Emprunt {
            private String organisme;
            private BigDecimal montantInitial;
            private BigDecimal capitalRestantDu;
            private BigDecimal tauxInteret;
            private LocalDate dateEcheance;
            private String garanties;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class EcheancierRemboursements {
            private BigDecimal anneeN_Plus1;
            private BigDecimal anneeN_Plus2;
            private BigDecimal anneeN_Plus3;
            private BigDecimal anneeN_Plus4;
            private BigDecimal anneeN_Plus5;
            private BigDecimal auDela;
            private BigDecimal total;
        }
    }

    /**
     * NOTE 8: Autres passifs
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Note8_AutresPassifs {
        private List<PassifCategorie> categories;
        private BigDecimal provisionsRisques;
        private BigDecimal provisionsCharges;
        private BigDecimal produitsConstatesAvance;
        private BigDecimal total;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PassifCategorie {
            private String nature;
            private BigDecimal montant;
            private String commentaire;
        }
    }

    /**
     * NOTE 9: Produits et charges (détail par nature)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Note9_ProduitsEtCharges {
        private DetailProduits produits;
        private DetailCharges charges;
        private String commentaire;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DetailProduits {
            private BigDecimal ventesLocales;
            private BigDecimal ventesExport;
            private BigDecimal prestationsServices;
            private BigDecimal produitsAccessoires;
            private BigDecimal produitsFinanciers;
            private BigDecimal produitsExceptionnels;
            private BigDecimal total;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DetailCharges {
            private BigDecimal achatsMarchandises;
            private BigDecimal achatsMatieresPremieres;
            private BigDecimal chargesPersonnel;
            private BigDecimal chargesExploitation;
            private BigDecimal chargesFinancieres;
            private BigDecimal chargesExceptionnelles;
            private BigDecimal impotsSurBenefices;
            private BigDecimal total;
        }
    }

    /**
     * NOTE 10: Impôts et taxes
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Note10_ImpotsEtTaxes {
        private DetailImpots imposSurBenefices;
        private DetailTVA tva;
        private List<AutreImpot> autresImpots;
        private BigDecimal totalImpotsEtTaxes;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DetailImpots {
            private BigDecimal resultatAvantImpot;
            private BigDecimal tauxImposition; // 30% Cameroun
            private BigDecimal impotTheorique;
            private BigDecimal reintegrationsNonDeductibles;
            private BigDecimal deductionsFiscales;
            private BigDecimal impotDu;
            private BigDecimal acomptesVerses;
            private BigDecimal impotARegulariser;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DetailTVA {
            private BigDecimal tvaCollectee; // 19.25%
            private BigDecimal tvaDeductible; // 19.25%
            private BigDecimal tvaADecaisser;
            private BigDecimal creditTVA;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AutreImpot {
            private String nature;
            private BigDecimal montant;
            private String periodicite;
        }
    }

    /**
     * NOTE 11: Engagements hors bilan
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Note11_EngagementsHorsBilan {
        private List<Engagement> engagementsRecus;
        private List<Engagement> engagementsDonnes;
        private List<Engagement> engagementsReciproques;
        private String commentaire;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Engagement {
            private String nature;
            private BigDecimal montant;
            private String contrepartie;
            private LocalDate echeance;
            private String description;
        }
    }

    /**
     * NOTE 12: Événements postérieurs à la clôture
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Note12_EvenementsPosterieur {
        private List<Evenement> evenements;
        private Boolean existeEvenementsSignificatifs;
        private String commentaireGeneral;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Evenement {
            private LocalDate dateEvenement;
            private String nature;
            private String description;
            private BigDecimal impactEstime;
            private Boolean affecteComptesAnnee;
            private String traitement;
        }
    }
}
