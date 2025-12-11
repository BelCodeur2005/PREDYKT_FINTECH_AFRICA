package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.enums.AccountType;
import com.predykt.accounting.dto.response.BalanceSheetResponse;
import com.predykt.accounting.dto.response.CashFlowStatementResponse;
import com.predykt.accounting.dto.response.IncomeStatementResponse;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialReportService {

    private final CompanyRepository companyRepository;
    private final GeneralLedgerService glService;
    private final ChartOfAccountsService chartService;
    private final com.predykt.accounting.repository.FinancialRatioRepository financialRatioRepository;
    private final com.predykt.accounting.repository.GeneralLedgerRepository generalLedgerRepository;

    // Getters pour export
    public com.predykt.accounting.repository.FinancialRatioRepository getFinancialRatioRepository() {
        return financialRatioRepository;
    }

    public com.predykt.accounting.repository.GeneralLedgerRepository getGeneralLedgerRepository() {
        return generalLedgerRepository;
    }
    
    /**
     * Générer le Bilan (Balance Sheet)
     */
    public BalanceSheetResponse generateBalanceSheet(Long companyId, LocalDate asOfDate) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));
        
        log.info("Génération du bilan pour l'entreprise {} au {}", companyId, asOfDate);
        
        // ACTIF
        BigDecimal fixedAssets = calculateAccountClassBalance(companyId, "2", asOfDate);  // Classe 2
        BigDecimal currentAssets = calculateAccountClassBalance(companyId, "3", asOfDate) // Classe 3
            .add(calculateAccountClassBalance(companyId, "4", asOfDate, "411"));  // + Créances clients
        BigDecimal cash = calculateAccountClassBalance(companyId, "5", asOfDate);  // Classe 5
        
        BigDecimal totalAssets = fixedAssets.add(currentAssets).add(cash);
        
        // PASSIF
        BigDecimal equity = calculateAccountClassBalance(companyId, "1", asOfDate);  // Classe 1
        BigDecimal longTermLiabilities = calculateAccountClassBalance(companyId, "16", asOfDate);  // Emprunts
        BigDecimal currentLiabilities = calculateAccountClassBalance(companyId, "4", asOfDate, "40");  // Dettes fournisseurs
        
        BigDecimal totalLiabilities = equity.add(longTermLiabilities).add(currentLiabilities);
        
        // Vérification de l'équilibre
        if (totalAssets.compareTo(totalLiabilities) != 0) {
            log.warn("Bilan déséquilibré : Actif={}, Passif={}", totalAssets, totalLiabilities);
        }
        
        return BalanceSheetResponse.builder()
            .asOfDate(asOfDate)
            .currency(company.getCurrency())
            .fixedAssets(fixedAssets)
            .currentAssets(currentAssets)
            .cash(cash)
            .totalAssets(totalAssets)
            .equity(equity)
            .longTermLiabilities(longTermLiabilities)
            .currentLiabilities(currentLiabilities)
            .totalLiabilities(totalLiabilities)
            .build();
    }
    
    /**
     * Générer le Compte de Résultat (Income Statement)
     */
    public IncomeStatementResponse generateIncomeStatement(Long companyId, 
                                                           LocalDate startDate, 
                                                           LocalDate endDate) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));
        
        log.info("Génération du compte de résultat pour l'entreprise {} du {} au {}", 
                 companyId, startDate, endDate);
        
        // PRODUITS (Classe 7)
        BigDecimal salesRevenue = calculateAccountBalance(companyId, "701", startDate, endDate);
        BigDecimal serviceRevenue = calculateAccountBalance(companyId, "706", startDate, endDate);
        BigDecimal financialIncome = calculateAccountClassBalance(companyId, "77", startDate, endDate);
        BigDecimal otherIncome = calculateAccountClassBalance(companyId, "7", startDate, endDate)
            .subtract(salesRevenue).subtract(serviceRevenue).subtract(financialIncome);
        
        BigDecimal totalRevenue = salesRevenue.add(serviceRevenue)
            .add(financialIncome).add(otherIncome);
        
        // CHARGES (Classe 6)
        BigDecimal purchasesCost = calculateAccountBalance(companyId, "601", startDate, endDate);
        BigDecimal personnelCost = calculateAccountClassBalance(companyId, "66", startDate, endDate);
        BigDecimal financialExpenses = calculateAccountClassBalance(companyId, "67", startDate, endDate);
        BigDecimal taxesAndDuties = calculateAccountClassBalance(companyId, "64", startDate, endDate);
        BigDecimal otherExpenses = calculateAccountClassBalance(companyId, "6", startDate, endDate)
            .subtract(purchasesCost).subtract(personnelCost)
            .subtract(financialExpenses).subtract(taxesAndDuties);
        
        BigDecimal totalExpenses = purchasesCost.add(personnelCost)
            .add(financialExpenses).add(taxesAndDuties).add(otherExpenses);
        
        // RÉSULTATS
        BigDecimal grossProfit = totalRevenue.subtract(purchasesCost);
        BigDecimal operatingIncome = totalRevenue.subtract(totalExpenses).add(financialExpenses); // Avant charges financières
        BigDecimal netIncome = totalRevenue.subtract(totalExpenses);
        
        // RATIOS
        BigDecimal grossMarginPct = totalRevenue.compareTo(BigDecimal.ZERO) > 0 
            ? grossProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;
        
        BigDecimal netMarginPct = totalRevenue.compareTo(BigDecimal.ZERO) > 0
            ? netIncome.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;
        
        return IncomeStatementResponse.builder()
            .startDate(startDate)
            .endDate(endDate)
            .currency(company.getCurrency())
            .salesRevenue(salesRevenue)
            .serviceRevenue(serviceRevenue)
            .otherOperatingIncome(otherIncome)
            .financialIncome(financialIncome)
            .totalRevenue(totalRevenue)
            .purchasesCost(purchasesCost)
            .personnelCost(personnelCost)
            .operatingExpenses(otherExpenses)
            .financialExpenses(financialExpenses)
            .taxesAndDuties(taxesAndDuties)
            .totalExpenses(totalExpenses)
            .grossProfit(grossProfit)
            .operatingIncome(operatingIncome)
            .netIncome(netIncome)
            .grossMarginPercentage(grossMarginPct)
            .netMarginPercentage(netMarginPct)
            .build();
    }

    /**
     * Générer le Tableau de flux de trésorerie (Cash Flow Statement)
     * Conforme OHADA - OBLIGATOIRE
     */
    public CashFlowStatementResponse generateCashFlowStatement(Long companyId,
                                                               LocalDate startDate,
                                                               LocalDate endDate) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        log.info("Génération du tableau de flux de trésorerie pour l'entreprise {} du {} au {}",
                companyId, startDate, endDate);

        // Récupérer le résultat net depuis le compte de résultat
        IncomeStatementResponse incomeStatement = generateIncomeStatement(companyId, startDate, endDate);
        BigDecimal netIncome = incomeStatement.getNetIncome();

        // ========== A. FLUX DE TRÉSORERIE D'EXPLOITATION ==========

        // Ajustements pour éléments sans effet de trésorerie
        BigDecimal depreciation = calculateAccountBalance(companyId, "681", startDate, endDate).abs(); // Dotations aux amortissements
        BigDecimal provisionsIncrease = calculateAccountBalance(companyId, "69", startDate, endDate).abs(); // Provisions
        BigDecimal provisionsDecrease = calculateAccountBalance(companyId, "79", startDate, endDate).abs(); // Reprises

        // Gains/pertes sur cessions (compte 81x ou 65x/75x selon OHADA)
        BigDecimal gainOnDisposal = calculateAccountBalance(companyId, "758", startDate, endDate).abs();
        BigDecimal lossOnDisposal = calculateAccountBalance(companyId, "658", startDate, endDate).abs();

        // Résultat avant variation du BFR
        BigDecimal incomeBeforeWC = netIncome
            .add(depreciation)
            .add(provisionsIncrease)
            .subtract(provisionsDecrease)
            .subtract(gainOnDisposal)
            .add(lossOnDisposal);

        // Variations du besoin en fonds de roulement
        LocalDate previousPeriodEnd = startDate.minusDays(1);

        BigDecimal inventoryChange = calculateAccountClassBalance(companyId, "3", endDate)
            .subtract(calculateAccountClassBalance(companyId, "3", previousPeriodEnd));

        BigDecimal receivablesChange = calculateAccountClassBalance(companyId, "411", endDate)
            .subtract(calculateAccountClassBalance(companyId, "411", previousPeriodEnd));

        BigDecimal prepaidChange = calculateAccountClassBalance(companyId, "47", endDate, "471")
            .subtract(calculateAccountClassBalance(companyId, "47", previousPeriodEnd, "471"));

        BigDecimal payablesChange = calculateAccountClassBalance(companyId, "401", endDate)
            .subtract(calculateAccountClassBalance(companyId, "401", previousPeriodEnd));

        BigDecimal accruedChange = calculateAccountClassBalance(companyId, "47", endDate, "472")
            .subtract(calculateAccountClassBalance(companyId, "47", previousPeriodEnd, "472"));

        // Flux net d'exploitation
        BigDecimal netOperatingCashFlow = incomeBeforeWC
            .subtract(inventoryChange)
            .subtract(receivablesChange)
            .subtract(prepaidChange)
            .add(payablesChange)
            .add(accruedChange);

        CashFlowStatementResponse.OperatingCashFlow operatingCF = CashFlowStatementResponse.OperatingCashFlow.builder()
            .netIncome(netIncome)
            .depreciationAndAmortization(depreciation)
            .provisionsIncrease(provisionsIncrease)
            .provisionsDecrease(provisionsDecrease)
            .gainOnAssetDisposal(gainOnDisposal)
            .lossOnAssetDisposal(lossOnDisposal)
            .incomeBeforeWorkingCapitalChanges(incomeBeforeWC)
            .inventoryChange(inventoryChange)
            .receivablesChange(receivablesChange)
            .prepaidExpensesChange(prepaidChange)
            .payablesChange(payablesChange)
            .accruedExpensesChange(accruedChange)
            .netOperatingCashFlow(netOperatingCashFlow)
            .build();

        // ========== B. FLUX DE TRÉSORERIE D'INVESTISSEMENT ==========

        // Acquisitions (débits des comptes 2x)
        BigDecimal intangibleAcq = calculateAccountClassBalance(companyId, "21", startDate, endDate).abs();
        BigDecimal tangibleAcq = calculateAccountClassBalance(companyId, "22", startDate, endDate).abs()
            .add(calculateAccountClassBalance(companyId, "23", startDate, endDate).abs())
            .add(calculateAccountClassBalance(companyId, "24", startDate, endDate).abs());
        BigDecimal financialAcq = calculateAccountClassBalance(companyId, "26", startDate, endDate).abs();

        // Cessions (crédits des comptes 2x) - Approximation via variations
        BigDecimal intangibleDisp = BigDecimal.ZERO; // À améliorer avec journal des cessions
        BigDecimal tangibleDisp = gainOnDisposal.add(lossOnDisposal); // Valeur de cession
        BigDecimal financialDisp = BigDecimal.ZERO;

        // Transactions majeures (simplifiée - à enrichir avec analyse du grand livre)
        List<CashFlowStatementResponse.AssetTransaction> majorAcquisitions = new ArrayList<>();
        List<CashFlowStatementResponse.AssetTransaction> majorDisposals = new ArrayList<>();

        BigDecimal netInvestingCashFlow = intangibleDisp.add(tangibleDisp).add(financialDisp)
            .subtract(intangibleAcq).subtract(tangibleAcq).subtract(financialAcq);

        CashFlowStatementResponse.InvestingCashFlow investingCF = CashFlowStatementResponse.InvestingCashFlow.builder()
            .intangibleAssetsAcquisitions(intangibleAcq.negate())
            .tangibleAssetsAcquisitions(tangibleAcq.negate())
            .financialAssetsAcquisitions(financialAcq.negate())
            .intangibleAssetDisposals(intangibleDisp)
            .tangibleAssetDisposals(tangibleDisp)
            .financialAssetDisposals(financialDisp)
            .majorAcquisitions(majorAcquisitions)
            .majorDisposals(majorDisposals)
            .netInvestingCashFlow(netInvestingCashFlow)
            .build();

        // ========== C. FLUX DE TRÉSORERIE DE FINANCEMENT ==========

        // Variations des capitaux propres
        BigDecimal capitalIncrease = calculateAccountBalance(companyId, "101", startDate, endDate).abs();
        BigDecimal capitalDecrease = BigDecimal.ZERO; // Rare

        // Variations des emprunts
        BigDecimal borrowingsCurrent = calculateAccountClassBalance(companyId, "16", endDate);
        BigDecimal borrowingsPrevious = calculateAccountClassBalance(companyId, "16", previousPeriodEnd);
        BigDecimal borrowingsChange = borrowingsCurrent.subtract(borrowingsPrevious);

        BigDecimal borrowingsReceived = borrowingsChange.compareTo(BigDecimal.ZERO) > 0 ? borrowingsChange : BigDecimal.ZERO;
        BigDecimal borrowingsRepaid = borrowingsChange.compareTo(BigDecimal.ZERO) < 0 ? borrowingsChange.abs() : BigDecimal.ZERO;

        // Dividendes versés (compte 46x)
        BigDecimal dividendsPaid = calculateAccountBalance(companyId, "465", startDate, endDate).abs();

        // Subventions reçues
        BigDecimal subsidies = calculateAccountBalance(companyId, "14", startDate, endDate).abs();

        BigDecimal netFinancingCashFlow = capitalIncrease
            .subtract(capitalDecrease)
            .add(borrowingsReceived)
            .subtract(borrowingsRepaid)
            .subtract(dividendsPaid)
            .add(subsidies);

        CashFlowStatementResponse.FinancingCashFlow financingCF = CashFlowStatementResponse.FinancingCashFlow.builder()
            .capitalIncreases(capitalIncrease)
            .capitalDecreases(capitalDecrease)
            .borrowingsReceived(borrowingsReceived)
            .borrowingsRepaid(borrowingsRepaid.negate())
            .dividendsPaid(dividendsPaid.negate())
            .subsidiesReceived(subsidies)
            .netFinancingCashFlow(netFinancingCashFlow)
            .build();

        // ========== RÉSUMÉ ==========

        BigDecimal netCashChange = netOperatingCashFlow
            .add(netInvestingCashFlow)
            .add(netFinancingCashFlow);

        // Trésorerie début et fin (comptes 5x)
        BigDecimal beginningCash = calculateAccountClassBalance(companyId, "5", previousPeriodEnd);
        BigDecimal endingCash = calculateAccountClassBalance(companyId, "5", endDate);
        BigDecimal calculatedEndingCash = beginningCash.add(netCashChange);

        // Vérification
        boolean isBalanced = endingCash.subtract(calculatedEndingCash).abs()
            .compareTo(new BigDecimal("0.01")) < 0; // Tolérance 0.01

        // Ratios
        BigDecimal cashFlowRatio = netIncome.compareTo(BigDecimal.ZERO) != 0
            ? netOperatingCashFlow.divide(netIncome, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;

        BigDecimal freeCashFlow = netOperatingCashFlow.add(netInvestingCashFlow);

        CashFlowStatementResponse.CashFlowSummary summary = CashFlowStatementResponse.CashFlowSummary.builder()
            .netOperatingCashFlow(netOperatingCashFlow)
            .netInvestingCashFlow(netInvestingCashFlow)
            .netFinancingCashFlow(netFinancingCashFlow)
            .netCashChange(netCashChange)
            .beginningCash(beginningCash)
            .endingCash(endingCash)
            .calculatedEndingCash(calculatedEndingCash)
            .isBalanced(isBalanced)
            .cashFlowFromOperationsRatio(cashFlowRatio)
            .freeCashFlow(freeCashFlow)
            .build();

        return CashFlowStatementResponse.builder()
            .companyId(companyId)
            .companyName(company.getName())
            .startDate(startDate)
            .endDate(endDate)
            .fiscalYear(String.valueOf(endDate.getYear()))
            .operatingCashFlow(operatingCF)
            .investingCashFlow(investingCF)
            .financingCashFlow(financingCF)
            .summary(summary)
            .build();
    }

    // ==================== MÉTHODES DÉLÉGUÉES À GeneralLedgerService ====================
    // 🟢 OPTIMISATION: Ces méthodes privées ont été remplacées par des appels directs
    // à GeneralLedgerService pour éliminer la duplication de code (Phase 3).
    //
    // Migration: FinancialReportService utilise maintenant:
    // - glService.getAccountClassBalance() au lieu de calculateAccountClassBalance()
    // - glService.getAccountBalanceChange() au lieu de calculateAccountBalance()
    // - glService.getAccountClassBalanceChange() au lieu de calculateAccountClassBalance(period)
    //
    // Les méthodes ci-dessous sont conservées pour compatibilité ascendante (deprecated).

    /**
     * @deprecated Utiliser {@link GeneralLedgerService#getAccountClassBalance(Long, String, LocalDate)}
     */
    @Deprecated(since = "Phase 3", forRemoval = true)
    private BigDecimal calculateAccountClassBalance(Long companyId, String classPrefix, LocalDate asOfDate) {
        return glService.getAccountClassBalance(companyId, classPrefix, asOfDate);
    }

    /**
     * @deprecated Utiliser {@link GeneralLedgerService#getAccountClassBalance(Long, String, LocalDate, String)}
     */
    @Deprecated(since = "Phase 3", forRemoval = true)
    private BigDecimal calculateAccountClassBalance(Long companyId, String classPrefix,
                                                    LocalDate asOfDate, String excludePrefix) {
        return glService.getAccountClassBalance(companyId, classPrefix, asOfDate, excludePrefix);
    }

    /**
     * @deprecated Utiliser {@link GeneralLedgerService#getAccountBalanceChange(Long, String, LocalDate, LocalDate)}
     */
    @Deprecated(since = "Phase 3", forRemoval = true)
    private BigDecimal calculateAccountBalance(Long companyId, String accountNumber,
                                               LocalDate startDate, LocalDate endDate) {
        return glService.getAccountBalanceChange(companyId, accountNumber, startDate, endDate);
    }

    /**
     * @deprecated Utiliser {@link GeneralLedgerService#getAccountClassBalanceChange(Long, String, LocalDate, LocalDate)}
     */
    @Deprecated(since = "Phase 3", forRemoval = true)
    private BigDecimal calculateAccountClassBalance(Long companyId, String classPrefix,
                                                    LocalDate startDate, LocalDate endDate) {
        return glService.getAccountClassBalanceChange(companyId, classPrefix, startDate, endDate);
    }
}