package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.FixedAsset;
import com.predykt.accounting.domain.entity.GeneralLedger;
import com.predykt.accounting.dto.response.BalanceSheetResponse;
import com.predykt.accounting.dto.response.IncomeStatementResponse;
import com.predykt.accounting.dto.response.TAFIREResponse;
import com.predykt.accounting.dto.response.TAFIREResponse.*;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour générer le TAFIRE (Tableau Financier des Ressources et Emplois)
 * Conforme OHADA - Obligatoire pour grandes entreprises
 *
 * Le TAFIRE explique la variation du fonds de roulement et de la trésorerie
 * sur un exercice en analysant les ressources et emplois stables
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class TAFIREService {

    private final CompanyRepository companyRepository;
    private final FinancialReportService financialReportService;
    private final GeneralLedgerRepository generalLedgerRepository;
    private final FixedAssetRepository fixedAssetRepository;

    /**
     * Génère le TAFIRE complet pour un exercice fiscal
     *
     * @param companyId ID de l'entreprise
     * @param fiscalYear Année fiscale
     * @return TAFIREResponse complet conforme OHADA
     */
    public TAFIREResponse generateTAFIRE(Long companyId, Integer fiscalYear) {
        log.info("Génération TAFIRE pour entreprise {} - Exercice {}", companyId, fiscalYear);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée: " + companyId));

        LocalDate startDate = LocalDate.of(fiscalYear, 1, 1);
        LocalDate endDate = LocalDate.of(fiscalYear, 12, 31);

        // 1. Récupérer les bilans N et N-1
        BalanceSheetResponse bilanN = financialReportService.generateBalanceSheet(companyId, endDate);
        BalanceSheetResponse bilanN1 = financialReportService.generateBalanceSheet(companyId,
            LocalDate.of(fiscalYear - 1, 12, 31));

        // 2. Récupérer le compte de résultat N
        IncomeStatementResponse compteResultat = financialReportService.generateIncomeStatement(
            companyId, startDate, endDate);

        // 3. Calculer les composantes du TAFIRE
        RessourcesStables ressources = calculateRessourcesStables(company, fiscalYear, compteResultat);
        EmploisStables emplois = calculateEmploisStables(company, fiscalYear);
        BigDecimal variationFRNG = ressources.getTotalRessourcesStables()
            .subtract(emplois.getTotalEmploisStables());

        VariationBFR varBFR = calculateVariationBFR(bilanN, bilanN1);
        VariationTresorerie varTresorerie = calculateVariationTresorerie(
            bilanN, bilanN1, variationFRNG, varBFR.getVariationBFR());

        // 4. Analyser et construire la réponse
        TAFIREResponse tafire = TAFIREResponse.builder()
            .companyId(companyId)
            .companyName(company.getName())
            .fiscalYear(fiscalYear)
            .startDate(startDate)
            .endDate(endDate)
            .currency("XAF")
            .ressourcesStables(ressources)
            .emploisStables(emplois)
            .variationFRNG(variationFRNG)
            .variationBFR(varBFR)
            .variationTresorerie(varTresorerie)
            .isBalanced(varTresorerie.getIsVerified())
            .analysisComment(generateAnalysisComment(variationFRNG, varBFR, varTresorerie))
            .build();

        log.info("TAFIRE généré - FRNG: {}, BFR: {}, Trésorerie: {}, Équilibré: {}",
            variationFRNG, varBFR.getVariationBFR(),
            varTresorerie.getVariationTresorerieCalculee(), tafire.getIsBalanced());

        return tafire;
    }

    /**
     * I. CALCUL DES RESSOURCES STABLES
     */
    private RessourcesStables calculateRessourcesStables(
            Company company, Integer fiscalYear, IncomeStatementResponse compteResultat) {

        // 1. Calculer la CAF (Capacité d'Autofinancement)
        CAFDetail caf = calculateCAF(company, fiscalYear, compteResultat);

        // 2. Cessions d'immobilisations (compte 754)
        BigDecimal cessions = getSoldeCompte(company, fiscalYear, "754");

        // 3. Augmentation de capital (variation compte 101)
        BigDecimal augmentationCapital = getVariationCompte(company, fiscalYear, "101");
        if (augmentationCapital.compareTo(BigDecimal.ZERO) < 0) {
            augmentationCapital = BigDecimal.ZERO; // Ignorer les diminutions
        }

        // 4. Emprunts à long terme (variation comptes 16x)
        BigDecimal emprunts = getVariationCompte(company, fiscalYear, "16");
        if (emprunts.compareTo(BigDecimal.ZERO) < 0) {
            emprunts = BigDecimal.ZERO; // Uniquement les nouveaux emprunts
        }

        // 5. Subventions d'investissement (compte 14x)
        BigDecimal subventions = getVariationCompte(company, fiscalYear, "14");
        if (subventions.compareTo(BigDecimal.ZERO) < 0) {
            subventions = BigDecimal.ZERO;
        }

        BigDecimal totalInternes = caf.getCapaciteAutofinancement().add(cessions);
        BigDecimal totalExternes = augmentationCapital.add(emprunts).add(subventions);
        BigDecimal total = totalInternes.add(totalExternes);

        return RessourcesStables.builder()
            .capaciteAutofinancement(caf.getCapaciteAutofinancement())
            .cessionsDImmobilisations(cessions)
            .augmentationCapital(augmentationCapital)
            .empruntsLongTerme(emprunts)
            .subventionsDInvestissement(subventions)
            .autresRessources(BigDecimal.ZERO)
            .totalRessourcesInternes(totalInternes)
            .totalRessourcesExternes(totalExternes)
            .totalRessourcesStables(total)
            .cafDetail(caf)
            .build();
    }

    /**
     * Calcul de la Capacité d'Autofinancement (CAF)
     * Méthode additive (à partir du résultat net)
     *
     * CAF = Résultat net
     *     + Dotations aux amortissements (681)
     *     + Dotations aux provisions (691)
     *     - Reprises sur provisions (791)
     *     + VNC des cessions d'actifs (654)
     *     - Produits de cessions (754)
     */
    private CAFDetail calculateCAF(Company company, Integer fiscalYear,
                                   IncomeStatementResponse compteResultat) {

        BigDecimal resultatNet = compteResultat.getNetIncome();

        // Dotations aux amortissements (compte 681)
        BigDecimal dotationsAmort = getSoldeCompte(company, fiscalYear, "681");

        // Dotations aux provisions (compte 691)
        BigDecimal dotationsProvisions = getSoldeCompte(company, fiscalYear, "691");

        // Reprises sur provisions (compte 791)
        BigDecimal reprises = getSoldeCompte(company, fiscalYear, "791");

        // VNC des cessions (compte 654)
        BigDecimal vncCessions = getSoldeCompte(company, fiscalYear, "654");

        // Produits de cessions (compte 754)
        BigDecimal produitsCessions = getSoldeCompte(company, fiscalYear, "754");

        // Calcul CAF
        BigDecimal caf = resultatNet
            .add(dotationsAmort)
            .add(dotationsProvisions)
            .subtract(reprises)
            .add(vncCessions)
            .subtract(produitsCessions);

        return CAFDetail.builder()
            .resultatNet(resultatNet)
            .dotationsAuxAmortissements(dotationsAmort)
            .dotationsAuxProvisions(dotationsProvisions)
            .reprisesProvisions(reprises)
            .vncCessionsActifs(vncCessions)
            .produitsCessionsActifs(produitsCessions)
            .autresRetraitements(BigDecimal.ZERO)
            .capaciteAutofinancement(caf)
            .calculMethod("ADDITIVE")
            .build();
    }

    /**
     * II. CALCUL DES EMPLOIS STABLES
     */
    private EmploisStables calculateEmploisStables(Company company, Integer fiscalYear) {

        // 1. Acquisitions d'immobilisations (variation comptes 2x en positif)
        BigDecimal immoIncorp = getVariationCompte(company, fiscalYear, "21");
        if (immoIncorp.compareTo(BigDecimal.ZERO) < 0) immoIncorp = BigDecimal.ZERO;

        BigDecimal immoCorp = getVariationCompte(company, fiscalYear, "23")
            .add(getVariationCompte(company, fiscalYear, "24"))
            .add(getVariationCompte(company, fiscalYear, "25"));
        if (immoCorp.compareTo(BigDecimal.ZERO) < 0) immoCorp = BigDecimal.ZERO;

        BigDecimal immoFin = getVariationCompte(company, fiscalYear, "26")
            .add(getVariationCompte(company, fiscalYear, "27"));
        if (immoFin.compareTo(BigDecimal.ZERO) < 0) immoFin = BigDecimal.ZERO;

        // 2. Remboursements emprunts LT (variation négative comptes 16x)
        BigDecimal remboursements = getVariationCompte(company, fiscalYear, "16");
        if (remboursements.compareTo(BigDecimal.ZERO) > 0) {
            remboursements = BigDecimal.ZERO; // Prendre uniquement les remboursements (négatifs)
        } else {
            remboursements = remboursements.abs();
        }

        // 3. Dividendes versés (compte 46x ou impact résultat reporté)
        BigDecimal dividendes = getSoldeCompte(company, fiscalYear, "465")
            .add(getSoldeCompte(company, fiscalYear, "4661"));

        BigDecimal totalAcquis = immoIncorp.add(immoCorp).add(immoFin);
        BigDecimal totalAutres = remboursements.add(dividendes);
        BigDecimal total = totalAcquis.add(totalAutres);

        return EmploisStables.builder()
            .immobilisationsIncorporelles(immoIncorp)
            .immobilisationsCorporelles(immoCorp)
            .immobilisationsFinancieres(immoFin)
            .remboursementsEmpruntsLongTerme(remboursements)
            .dividendesVerses(dividendes)
            .autresEmplois(BigDecimal.ZERO)
            .totalAcquisitionsImmobilisations(totalAcquis)
            .totalAutresEmplois(totalAutres)
            .totalEmploisStables(total)
            .build();
    }

    /**
     * IV. CALCUL DE LA VARIATION DU BFR
     * BFR = (Stocks + Créances) - (Dettes fournisseurs + Dettes fiscales/sociales)
     */
    private VariationBFR calculateVariationBFR(BalanceSheetResponse bilanN, BalanceSheetResponse bilanN1) {

        // Variation actif circulant (hors trésorerie)
        BigDecimal varStocks = bilanN.getCurrentAssets().subtract(bilanN1.getCurrentAssets());
        BigDecimal varCreances = BigDecimal.ZERO; // À affiner avec détail des créances
        BigDecimal varAutresCreances = BigDecimal.ZERO;

        // Variation dettes circulantes (hors trésorerie)
        BigDecimal varFournisseurs = bilanN.getCurrentLiabilities().subtract(bilanN1.getCurrentLiabilities());
        BigDecimal varDettesFiscales = BigDecimal.ZERO; // À affiner
        BigDecimal varAutresDettes = BigDecimal.ZERO;

        BigDecimal totalVarActif = varStocks.add(varCreances).add(varAutresCreances);
        BigDecimal totalVarDettes = varFournisseurs.add(varDettesFiscales).add(varAutresDettes);

        // BFR = Actif circulant - Dettes circulantes (hors trésorerie)
        BigDecimal bfrN = bilanN.getCurrentAssets().subtract(bilanN.getCurrentLiabilities());
        BigDecimal bfrN1 = bilanN1.getCurrentAssets().subtract(bilanN1.getCurrentLiabilities());
        BigDecimal varBFR = bfrN.subtract(bfrN1);

        return VariationBFR.builder()
            .variationStocks(varStocks)
            .variationCreancesClients(varCreances)
            .variationAutresCreances(varAutresCreances)
            .variationDettesFournisseurs(varFournisseurs)
            .variationDettesFiscalesSociales(varDettesFiscales)
            .variationAutresDettes(varAutresDettes)
            .totalVariationActifCirculant(totalVarActif)
            .totalVariationDettesCirculantes(totalVarDettes)
            .bfrDebutExercice(bfrN1)
            .bfrFinExercice(bfrN)
            .variationBFR(varBFR)
            .build();
    }

    /**
     * V. CALCUL DE LA VARIATION DE TRÉSORERIE
     * Trésorerie = FRNG - BFR
     */
    private VariationTresorerie calculateVariationTresorerie(
            BalanceSheetResponse bilanN, BalanceSheetResponse bilanN1,
            BigDecimal variationFRNG, BigDecimal variationBFR) {

        BigDecimal tresoN = bilanN.getCash();
        BigDecimal tresoN1 = bilanN1.getCash();

        BigDecimal varTresoCalculee = variationFRNG.subtract(variationBFR);
        BigDecimal varTresoReelle = tresoN.subtract(tresoN1);
        BigDecimal ecart = varTresoCalculee.subtract(varTresoReelle);

        boolean isVerified = ecart.abs().compareTo(new BigDecimal("1000")) < 0; // Tolérance 1000 XAF

        return VariationTresorerie.builder()
            .tresorerieDebutExercice(tresoN1)
            .tresorerieFinExercice(tresoN)
            .variationFRNG(variationFRNG)
            .variationBFR(variationBFR)
            .variationTresorerieCalculee(varTresoCalculee)
            .variationTresorerieRelle(varTresoReelle)
            .ecart(ecart)
            .isVerified(isVerified)
            .banqueDebutExercice(tresoN1) // Approximation (à affiner avec détail banque/caisse)
            .banqueFinExercice(tresoN)
            .caisseDebutExercice(BigDecimal.ZERO)
            .caisseFinExercice(BigDecimal.ZERO)
            .build();
    }

    /**
     * Récupère le solde d'un compte pour un exercice
     */
    private BigDecimal getSoldeCompte(Company company, Integer fiscalYear, String accountPrefix) {
        LocalDate startDate = LocalDate.of(fiscalYear, 1, 1);
        LocalDate endDate = LocalDate.of(fiscalYear, 12, 31);

        List<GeneralLedger> entries = generalLedgerRepository.findByCompanyAndAccountNumberStartingWithAndEntryDateBetween(
            company, accountPrefix, startDate, endDate);

        BigDecimal debit = entries.stream()
            .map(GeneralLedger::getDebitAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal credit = entries.stream()
            .map(GeneralLedger::getCreditAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Comptes de charges (6x) et d'actif (2x, 3x, 4x, 5x) = débit - crédit
        // Comptes de produits (7x) et de passif (1x, 4x) = crédit - débit
        char firstChar = accountPrefix.charAt(0);
        if (firstChar == '6' || firstChar == '2' || firstChar == '3' || firstChar == '5') {
            return debit.subtract(credit);
        } else {
            return credit.subtract(debit);
        }
    }

    /**
     * Calcule la variation d'un compte entre N-1 et N
     */
    private BigDecimal getVariationCompte(Company company, Integer fiscalYear, String accountPrefix) {
        BigDecimal soldeN = getSoldeCompteCumule(company, fiscalYear, accountPrefix);
        BigDecimal soldeN1 = getSoldeCompteCumule(company, fiscalYear - 1, accountPrefix);
        return soldeN.subtract(soldeN1);
    }

    /**
     * Solde cumulé d'un compte à la fin d'un exercice
     */
    private BigDecimal getSoldeCompteCumule(Company company, Integer fiscalYear, String accountPrefix) {
        LocalDate endDate = LocalDate.of(fiscalYear, 12, 31);

        List<GeneralLedger> entries = generalLedgerRepository.findByCompanyAndAccountNumberStartingWithAndEntryDateLessThanEqual(
            company, accountPrefix, endDate);

        BigDecimal debit = entries.stream()
            .map(GeneralLedger::getDebitAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal credit = entries.stream()
            .map(GeneralLedger::getCreditAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        char firstChar = accountPrefix.charAt(0);
        if (firstChar == '6' || firstChar == '2' || firstChar == '3' || firstChar == '5') {
            return debit.subtract(credit);
        } else {
            return credit.subtract(debit);
        }
    }

    /**
     * Génère un commentaire d'analyse automatique
     */
    private String generateAnalysisComment(BigDecimal variationFRNG, VariationBFR varBFR,
                                          VariationTresorerie varTreso) {

        StringBuilder comment = new StringBuilder();

        // Analyse FRNG
        if (variationFRNG.compareTo(BigDecimal.ZERO) > 0) {
            comment.append("Amélioration du fonds de roulement (+")
                .append(formatAmount(variationFRNG))
                .append(" XAF). ");
        } else if (variationFRNG.compareTo(BigDecimal.ZERO) < 0) {
            comment.append("Détérioration du fonds de roulement (")
                .append(formatAmount(variationFRNG))
                .append(" XAF). ");
        }

        // Analyse BFR
        if (varBFR.getVariationBFR().compareTo(BigDecimal.ZERO) > 0) {
            comment.append("Augmentation du besoin en fonds de roulement (")
                .append(formatAmount(varBFR.getVariationBFR()))
                .append(" XAF). ");
        } else if (varBFR.getVariationBFR().compareTo(BigDecimal.ZERO) < 0) {
            comment.append("Diminution du besoin en fonds de roulement (")
                .append(formatAmount(varBFR.getVariationBFR().abs()))
                .append(" XAF). ");
        }

        // Analyse trésorerie
        if (varTreso.getVariationTresorerieCalculee().compareTo(BigDecimal.ZERO) > 0) {
            comment.append("Amélioration de la trésorerie (+")
                .append(formatAmount(varTreso.getVariationTresorerieCalculee()))
                .append(" XAF).");
        } else {
            comment.append("Dégradation de la trésorerie (")
                .append(formatAmount(varTreso.getVariationTresorerieCalculee()))
                .append(" XAF).");
        }

        return comment.toString();
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("%,.0f", amount);
    }
}
