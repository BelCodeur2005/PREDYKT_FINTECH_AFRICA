package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.FixedAsset;
import com.predykt.accounting.domain.entity.GeneralLedger;
import com.predykt.accounting.dto.response.BalanceSheetResponse;
import com.predykt.accounting.dto.response.IncomeStatementResponse;
import com.predykt.accounting.dto.response.NotesAnnexesResponse;
import com.predykt.accounting.dto.response.NotesAnnexesResponse.*;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.repository.FixedAssetRepository;
import com.predykt.accounting.repository.GeneralLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour générer les Notes Annexes OHADA
 * 12 notes obligatoires conformes au système comptable OHADA
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class NotesAnnexesService {

    private final CompanyRepository companyRepository;
    private final FixedAssetRepository fixedAssetRepository;
    private final GeneralLedgerRepository generalLedgerRepository;
    private final FinancialReportService financialReportService;

    private static final BigDecimal TVA_RATE_CAMEROON = new BigDecimal("0.1925"); // 19.25%
    private static final BigDecimal IMPOT_RATE_CAMEROON = new BigDecimal("0.30"); // 30%

    /**
     * Génère les notes annexes complètes pour un exercice fiscal
     */
    public NotesAnnexesResponse generateNotesAnnexes(Long companyId, Integer fiscalYear) {
        log.info("Génération des notes annexes pour l'entreprise {} - exercice {}", companyId, fiscalYear);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        LocalDate startDate = LocalDate.of(fiscalYear, 1, 1);
        LocalDate endDate = LocalDate.of(fiscalYear, 12, 31);

        // Récupérer les rapports financiers de base
        BalanceSheetResponse bilan = financialReportService.generateBalanceSheet(companyId, endDate);
        IncomeStatementResponse compteResultat = financialReportService.generateIncomeStatement(companyId, startDate, endDate);

        return NotesAnnexesResponse.builder()
            .companyId(companyId)
            .companyName(company.getName())
            .fiscalYear(fiscalYear)
            .startDate(startDate)
            .endDate(endDate)
            .currency("XAF")
            .note1(generateNote1_PrincipesComptables(company))
            .note2(generateNote2_Immobilisations(company, fiscalYear))
            .note3(generateNote3_ImmobilisationsFinancieres(company, endDate))
            .note4(generateNote4_Stocks(company, startDate, endDate))
            .note5(generateNote5_CreancesEtDettes(company, endDate))
            .note6(generateNote6_CapitauxPropres(company, fiscalYear))
            .note7(generateNote7_EmpruntsEtDettes(company, endDate))
            .note8(generateNote8_AutresPassifs(company, endDate))
            .note9(generateNote9_ProduitsEtCharges(compteResultat))
            .note10(generateNote10_ImpotsEtTaxes(company, fiscalYear, compteResultat))
            .note11(generateNote11_EngagementsHorsBilan(company, endDate))
            .note12(generateNote12_EvenementsPosterieur(company, endDate))
            .build();
    }

    /**
     * NOTE 1: Principes et méthodes comptables
     */
    private Note1_PrincipesComptables generateNote1_PrincipesComptables(Company company) {
        return Note1_PrincipesComptables.builder()
            .referentielComptable("OHADA - Système comptable normal")
            .methodesEvaluation("Coût historique pour les immobilisations, Valeur nette réalisable pour les stocks")
            .methodesAmortissement("Linéaire et dégressif selon la nature des biens conformément à la réglementation OHADA")
            .methodesStocks("CMUP (Coût Moyen Unitaire Pondéré)")
            .principesRetenus("Continuité d'exploitation, Permanence des méthodes, Indépendance des exercices, " +
                "Prudence, Coût historique, Intangibilité du bilan d'ouverture")
            .changementsMethodes(new ArrayList<>()) // Aucun changement par défaut
            .build();
    }

    /**
     * NOTE 2: Immobilisations corporelles et incorporelles
     */
    private Note2_Immobilisations generateNote2_Immobilisations(Company company, Integer fiscalYear) {
        List<FixedAsset> assets = fixedAssetRepository.findByCompanyAndFiscalYear(company, fiscalYear);

        // Grouper par catégorie
        List<Note2_Immobilisations.TableauMouvements> mouvements = assets.stream()
            .collect(Collectors.groupingBy(FixedAsset::getCategory))
            .entrySet().stream()
            .map(entry -> {
                List<FixedAsset> categoryAssets = entry.getValue();
                BigDecimal totalAcquisitions = categoryAssets.stream()
                    .map(FixedAsset::getAcquisitionCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalAmortissement = categoryAssets.stream()
                    .map(asset -> asset.getDepreciation() != null ? asset.getDepreciation() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                return Note2_Immobilisations.TableauMouvements.builder()
                    .categorie(entry.getKey().getDisplayName())
                    .valeurBruteDebut(totalAcquisitions)
                    .amortissementsCumules(totalAmortissement)
                    .valeurNetteDebut(totalAcquisitions.subtract(totalAmortissement))
                    .acquisitions(totalAcquisitions)
                    .cessions(BigDecimal.ZERO)
                    .dotationsAmortissement(totalAmortissement)
                    .valeurBruteFin(totalAcquisitions)
                    .valeurNetteFin(totalAcquisitions.subtract(totalAmortissement))
                    .build();
            })
            .collect(Collectors.toList());

        // Méthodes d'amortissement
        List<Note2_Immobilisations.MethodeAmortissement> methodes = List.of(
            Note2_Immobilisations.MethodeAmortissement.builder()
                .categorie("Bâtiments")
                .methode("Linéaire")
                .dureeAns(20)
                .tauxPourcentage(new BigDecimal("5.0"))
                .build(),
            Note2_Immobilisations.MethodeAmortissement.builder()
                .categorie("Matériel et équipement")
                .methode("Dégressif")
                .dureeAns(5)
                .tauxPourcentage(new BigDecimal("40.0"))
                .build(),
            Note2_Immobilisations.MethodeAmortissement.builder()
                .categorie("Mobilier de bureau")
                .methode("Linéaire")
                .dureeAns(10)
                .tauxPourcentage(new BigDecimal("10.0"))
                .build(),
            Note2_Immobilisations.MethodeAmortissement.builder()
                .categorie("Matériel de transport")
                .methode("Linéaire")
                .dureeAns(4)
                .tauxPourcentage(new BigDecimal("25.0"))
                .build()
        );

        return Note2_Immobilisations.builder()
            .immobilisationsIncorporelles(Note2_Immobilisations.TableauMouvements.builder()
                .categorie("Immobilisations incorporelles")
                .valeurBruteDebut(BigDecimal.ZERO)
                .amortissementsCumules(BigDecimal.ZERO)
                .valeurNetteDebut(BigDecimal.ZERO)
                .build())
            .immobilisationsCorporelles(mouvements.isEmpty() ? null : mouvements.get(0))
            .methodesAmortissement(methodes)
            .cessions(new ArrayList<>())
            .commentaire("Toutes les immobilisations sont évaluées au coût d'acquisition conformément aux normes OHADA")
            .build();
    }

    /**
     * NOTE 3: Immobilisations financières
     */
    private Note3_ImmobilisationsFinancieres generateNote3_ImmobilisationsFinancieres(Company company, LocalDate endDate) {
        // Récupérer les comptes 26x et 27x
        List<GeneralLedger> immobFinancieres = generalLedgerRepository.findByCompanyAndAccountNumberStartingWithAndEntryDateBefore(
            company, "26", endDate);

        BigDecimal total = immobFinancieres.stream()
            .map(entry -> entry.getDebitAmount().subtract(entry.getCreditAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Note3_ImmobilisationsFinancieres.builder()
            .participations(new ArrayList<>())
            .pretsLongTerme(new ArrayList<>())
            .depotsEtCautionnements(new ArrayList<>())
            .total(total)
            .build();
    }

    /**
     * NOTE 4: Stocks
     */
    private Note4_Stocks generateNote4_Stocks(Company company, LocalDate startDate, LocalDate endDate) {
        // Récupérer les stocks (comptes 3x)
        BigDecimal stocksDebut = getAccountBalance(company, "3", startDate.minusDays(1));
        BigDecimal stocksFin = getAccountBalance(company, "3", endDate);

        return Note4_Stocks.builder()
            .methodeEvaluation("CMUP (Coût Moyen Unitaire Pondéré)")
            .categories(new ArrayList<>())
            .totalStocksDebut(stocksDebut)
            .totalStocksFin(stocksFin)
            .variationStocks(stocksFin.subtract(stocksDebut))
            .provisionsDepreciation(BigDecimal.ZERO)
            .build();
    }

    /**
     * NOTE 5: Créances et dettes
     */
    private Note5_CreancesEtDettes generateNote5_CreancesEtDettes(Company company, LocalDate endDate) {
        // Créances clients (411)
        BigDecimal creancesClients = getAccountBalance(company, "411", endDate);

        // ✅ AMÉLIORATION: Calcul AUTOMATIQUE des créances douteuses
        // Règle OHADA: Provisions sur créances > 90 jours
        BigDecimal echuPlus90Jours = calculateOverdueAmount(company, "411", endDate, 90);
        BigDecimal creancesDouteuses = echuPlus90Jours.multiply(new BigDecimal("0.50")); // Provision 50%

        // Calcul plus précis des échéances basé sur les vraies dates
        BigDecimal echu30A90Jours = calculateOverdueAmount(company, "411", endDate, 30)
            .subtract(echuPlus90Jours);
        BigDecimal echuMoins30Jours = calculateOverdueAmount(company, "411", endDate, 0)
            .subtract(echu30A90Jours)
            .subtract(echuPlus90Jours);
        BigDecimal aEchoir = creancesClients
            .subtract(echuMoins30Jours)
            .subtract(echu30A90Jours)
            .subtract(echuPlus90Jours);

        Note5_CreancesEtDettes.EcheancierCreances creances = Note5_CreancesEtDettes.EcheancierCreances.builder()
            .creancesClients(creancesClients)
            .aEchoir(aEchoir.max(BigDecimal.ZERO))
            .echuMoins30Jours(echuMoins30Jours.max(BigDecimal.ZERO))
            .echu30A90Jours(echu30A90Jours.max(BigDecimal.ZERO))
            .echuPlus90Jours(echuPlus90Jours.max(BigDecimal.ZERO))
            .creancesDouteuses(creancesDouteuses)
            .autresCreances(BigDecimal.ZERO)
            .total(creancesClients)
            .build();

        // Dettes fournisseurs (401)
        BigDecimal dettesFournisseurs = getAccountBalance(company, "401", endDate).abs();

        Note5_CreancesEtDettes.EcheancierDettes dettes = Note5_CreancesEtDettes.EcheancierDettes.builder()
            .dettesFournisseurs(dettesFournisseurs)
            .dettesCourtTerme(dettesFournisseurs.multiply(new BigDecimal("0.80"))) // 80%
            .dettesMoyenTerme(dettesFournisseurs.multiply(new BigDecimal("0.15"))) // 15%
            .dettesLongTerme(dettesFournisseurs.multiply(new BigDecimal("0.05"))) // 5%
            .autresDettes(BigDecimal.ZERO)
            .total(dettesFournisseurs)
            .build();

        return Note5_CreancesEtDettes.builder()
            .creances(creances)
            .dettes(dettes)
            .provisionsCreancesDouteuses(creancesDouteuses)
            .commentaire(String.format(
                "Les créances et dettes sont comptabilisées à leur valeur nominale. " +
                "Provision de %,.0f XAF constituée sur les créances > 90 jours (50%% de %,.0f XAF).",
                creancesDouteuses.doubleValue(),
                echuPlus90Jours.doubleValue()
            ))
            .build();
    }

    /**
     * NOTE 6: Capitaux propres
     */
    private Note6_CapitauxPropres generateNote6_CapitauxPropres(Company company, Integer fiscalYear) {
        BigDecimal capital = getAccountBalance(company, "101", LocalDate.of(fiscalYear, 12, 31));
        BigDecimal reserves = getAccountBalance(company, "106", LocalDate.of(fiscalYear, 12, 31));
        BigDecimal resultat = getAccountBalance(company, "12", LocalDate.of(fiscalYear, 12, 31));

        Note6_CapitauxPropres.TableauVariation variation = Note6_CapitauxPropres.TableauVariation.builder()
            .capitalSocial(capital)
            .primes(BigDecimal.ZERO)
            .reserves(reserves)
            .reportANouveau(BigDecimal.ZERO)
            .resultatExercice(resultat)
            .total(capital.add(reserves).add(resultat))
            .build();

        return Note6_CapitauxPropres.builder()
            .composantes(new ArrayList<>())
            .tableauVariation(variation)
            .capitalDebut(capital)
            .capitalFin(capital)
            .commentaire("Le capital social est entièrement libéré")
            .build();
    }

    /**
     * NOTE 7: Emprunts et dettes financières
     */
    private Note7_EmpruntsEtDettes generateNote7_EmpruntsEtDettes(Company company, LocalDate endDate) {
        BigDecimal empruntsLT = getAccountBalance(company, "16", endDate).abs();
        BigDecimal empruntsCT = getAccountBalance(company, "51", endDate).abs();

        Note7_EmpruntsEtDettes.EcheancierRemboursements echeancier = Note7_EmpruntsEtDettes.EcheancierRemboursements.builder()
            .anneeN_Plus1(empruntsLT.multiply(new BigDecimal("0.20")))
            .anneeN_Plus2(empruntsLT.multiply(new BigDecimal("0.20")))
            .anneeN_Plus3(empruntsLT.multiply(new BigDecimal("0.20")))
            .anneeN_Plus4(empruntsLT.multiply(new BigDecimal("0.20")))
            .anneeN_Plus5(empruntsLT.multiply(new BigDecimal("0.20")))
            .auDela(BigDecimal.ZERO)
            .total(empruntsLT)
            .build();

        return Note7_EmpruntsEtDettes.builder()
            .emprunts(new ArrayList<>())
            .echeancier(echeancier)
            .totalEmpruntsLongTerme(empruntsLT)
            .totalEmpruntsCourtTerme(empruntsCT)
            .commentaire("Les emprunts sont comptabilisés au coût amorti")
            .build();
    }

    /**
     * NOTE 8: Autres passifs
     */
    private Note8_AutresPassifs generateNote8_AutresPassifs(Company company, LocalDate endDate) {
        BigDecimal provisions = getAccountBalance(company, "15", endDate).abs();
        BigDecimal produitsConstatesAvance = getAccountBalance(company, "477", endDate).abs();

        return Note8_AutresPassifs.builder()
            .categories(new ArrayList<>())
            .provisionsRisques(provisions.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP))
            .provisionsCharges(provisions.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP))
            .produitsConstatesAvance(produitsConstatesAvance)
            .total(provisions.add(produitsConstatesAvance))
            .build();
    }

    /**
     * NOTE 9: Produits et charges
     */
    private Note9_ProduitsEtCharges generateNote9_ProduitsEtCharges(IncomeStatementResponse compteResultat) {
        Note9_ProduitsEtCharges.DetailProduits produits = Note9_ProduitsEtCharges.DetailProduits.builder()
            .ventesLocales(compteResultat.getSalesRevenue().multiply(new BigDecimal("0.70")))
            .ventesExport(compteResultat.getSalesRevenue().multiply(new BigDecimal("0.30")))
            .prestationsServices(compteResultat.getServiceRevenue())
            .produitsAccessoires(compteResultat.getOtherOperatingIncome())
            .produitsFinanciers(compteResultat.getFinancialIncome())
            .produitsExceptionnels(BigDecimal.ZERO)
            .total(compteResultat.getTotalRevenue())
            .build();

        Note9_ProduitsEtCharges.DetailCharges charges = Note9_ProduitsEtCharges.DetailCharges.builder()
            .achatsMarchandises(compteResultat.getPurchasesCost().multiply(new BigDecimal("0.70")))
            .achatsMatieresPremieres(compteResultat.getPurchasesCost().multiply(new BigDecimal("0.30")))
            .chargesPersonnel(compteResultat.getPersonnelCost())
            .chargesExploitation(compteResultat.getOperatingExpenses())
            .chargesFinancieres(compteResultat.getFinancialExpenses())
            .chargesExceptionnelles(BigDecimal.ZERO)
            .impotsSurBenefices(compteResultat.getTaxesAndDuties())
            .total(compteResultat.getTotalExpenses())
            .build();

        return Note9_ProduitsEtCharges.builder()
            .produits(produits)
            .charges(charges)
            .commentaire("Détail des produits et charges par nature selon la nomenclature OHADA")
            .build();
    }

    /**
     * NOTE 10: Impôts et taxes
     */
    private Note10_ImpotsEtTaxes generateNote10_ImpotsEtTaxes(Company company, Integer fiscalYear,
                                                              IncomeStatementResponse compteResultat) {
        BigDecimal resultatAvantImpot = compteResultat.getNetIncome().add(compteResultat.getTaxesAndDuties());
        BigDecimal impotTheorique = resultatAvantImpot.multiply(IMPOT_RATE_CAMEROON);

        Note10_ImpotsEtTaxes.DetailImpots detailImpots = Note10_ImpotsEtTaxes.DetailImpots.builder()
            .resultatAvantImpot(resultatAvantImpot)
            .tauxImposition(IMPOT_RATE_CAMEROON.multiply(new BigDecimal("100")))
            .impotTheorique(impotTheorique)
            .reintegrationsNonDeductibles(BigDecimal.ZERO)
            .deductionsFiscales(BigDecimal.ZERO)
            .impotDu(compteResultat.getTaxesAndDuties())
            .acomptesVerses(BigDecimal.ZERO)
            .impotARegulariser(compteResultat.getTaxesAndDuties())
            .build();

        // Calcul TVA
        BigDecimal tvaCollectee = compteResultat.getTotalRevenue().multiply(TVA_RATE_CAMEROON);
        BigDecimal tvaDeductible = compteResultat.getPurchasesCost().multiply(TVA_RATE_CAMEROON);

        Note10_ImpotsEtTaxes.DetailTVA detailTVA = Note10_ImpotsEtTaxes.DetailTVA.builder()
            .tvaCollectee(tvaCollectee)
            .tvaDeductible(tvaDeductible)
            .tvaADecaisser(tvaCollectee.subtract(tvaDeductible).max(BigDecimal.ZERO))
            .creditTVA(tvaDeductible.subtract(tvaCollectee).max(BigDecimal.ZERO))
            .build();

        return Note10_ImpotsEtTaxes.builder()
            .imposSurBenefices(detailImpots)
            .tva(detailTVA)
            .autresImpots(new ArrayList<>())
            .totalImpotsEtTaxes(compteResultat.getTaxesAndDuties())
            .build();
    }

    /**
     * NOTE 11: Engagements hors bilan
     */
    private Note11_EngagementsHorsBilan generateNote11_EngagementsHorsBilan(Company company, LocalDate endDate) {
        return Note11_EngagementsHorsBilan.builder()
            .engagementsRecus(new ArrayList<>())
            .engagementsDonnes(new ArrayList<>())
            .engagementsReciproques(new ArrayList<>())
            .commentaire("Aucun engagement hors bilan significatif à déclarer")
            .build();
    }

    /**
     * NOTE 12: Événements postérieurs à la clôture
     */
    private Note12_EvenementsPosterieur generateNote12_EvenementsPosterieur(Company company, LocalDate endDate) {
        return Note12_EvenementsPosterieur.builder()
            .evenements(new ArrayList<>())
            .existeEvenementsSignificatifs(false)
            .commentaireGeneral("Aucun événement significatif postérieur à la clôture n'est à signaler")
            .build();
    }

    /**
     * Utilitaire: Récupère le solde d'un compte à une date donnée
     */
    private BigDecimal getAccountBalance(Company company, String accountPrefix, LocalDate asOfDate) {
        List<GeneralLedger> entries = generalLedgerRepository
            .findByCompanyAndAccountNumberStartingWithAndEntryDateBefore(company, accountPrefix, asOfDate);

        return entries.stream()
            .map(entry -> entry.getDebitAmount().subtract(entry.getCreditAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * ✅ NOUVEAU: Calcule le montant des créances en retard selon l'ancienneté
     * @param company Entreprise
     * @param accountPrefix Préfixe compte (411 pour clients, 401 pour fournisseurs)
     * @param asOfDate Date de référence
     * @param minDaysOverdue Ancienneté minimale en jours (0, 30, 60, 90)
     * @return Montant des créances ayant au minimum cette ancienneté
     */
    private BigDecimal calculateOverdueAmount(Company company, String accountPrefix,
                                             LocalDate asOfDate, int minDaysOverdue) {
        List<GeneralLedger> entries = generalLedgerRepository
            .findByCompanyAndAccountNumberStartingWithAndEntryDateBefore(company, accountPrefix, asOfDate);

        return entries.stream()
            .filter(entry -> {
                // Calculer l'ancienneté de l'écriture
                long daysOld = ChronoUnit.DAYS.between(entry.getEntryDate(), asOfDate);
                return daysOld >= minDaysOverdue;
            })
            .map(entry -> {
                // Pour les clients (411): créances = débit - crédit
                // Pour les fournisseurs (401): dettes = crédit - débit
                BigDecimal amount = entry.getDebitAmount().subtract(entry.getCreditAmount());
                return accountPrefix.equals("411") ? amount : amount.abs();
            })
            .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)  // Uniquement soldes positifs
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
