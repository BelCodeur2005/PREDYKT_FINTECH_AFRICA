package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.FinancialRatio;
import com.predykt.accounting.dto.response.BalanceSheetResponse;
import com.predykt.accounting.dto.response.FinancialRatiosResponse;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.repository.FinancialRatioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de calcul et gestion des ratios financiers
 * Conformément aux standards OHADA et aux exigences PREDYKT
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialRatioService {
    
    private final CompanyRepository companyRepository;
    private final FinancialRatioRepository ratioRepository;
    private final GeneralLedgerService glService;
    private final FinancialReportService reportService;
    
    /**
     * Calcule tous les ratios pour une période donnée
     */
    @Transactional
    public FinancialRatio calculateAndSaveRatios(Long companyId, LocalDate startDate, LocalDate endDate) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));
        
        log.info("Calcul des ratios pour {} du {} au {}", company.getName(), startDate, endDate);
        
        // Récupérer les données financières de base
        var incomeStatement = reportService.generateIncomeStatement(companyId, startDate, endDate);
        var balanceSheet = reportService.generateBalanceSheet(companyId, endDate);
        
        // Créer l'entité FinancialRatio
        FinancialRatio ratio = new FinancialRatio();
        ratio.setCompany(company);
        ratio.setFiscalYear(String.valueOf(endDate.getYear()));
        ratio.setPeriodStart(startDate);
        ratio.setPeriodEnd(endDate);
        
        // Données brutes
        ratio.setTotalRevenue(incomeStatement.getTotalRevenue());
        ratio.setTotalExpenses(incomeStatement.getTotalExpenses());
        ratio.setNetIncome(incomeStatement.getNetIncome());
        ratio.setTotalAssets(balanceSheet.getTotalAssets());
        ratio.setTotalEquity(balanceSheet.getEquity());
        ratio.setTotalDebt(balanceSheet.getLongTermLiabilities().add(balanceSheet.getCurrentLiabilities()));
        
        // Calcul du BFR (Working Capital)
        BigDecimal workingCapital = calculateWorkingCapital(balanceSheet);
        ratio.setWorkingCapital(workingCapital);
        
        // === RATIOS DE RENTABILITÉ ===
        ratio.setGrossMarginPct(calculatePercentage(
            incomeStatement.getGrossProfit(), 
            incomeStatement.getTotalRevenue()
        ));
        
        ratio.setNetMarginPct(calculatePercentage(
            incomeStatement.getNetIncome(), 
            incomeStatement.getTotalRevenue()
        ));
        
        // ROA (Return on Assets) = Résultat Net / Total Actif
        ratio.setRoaPct(calculatePercentage(
            incomeStatement.getNetIncome(), 
            balanceSheet.getTotalAssets()
        ));
        
        // ROE (Return on Equity) = Résultat Net / Capitaux Propres
        ratio.setRoePct(calculatePercentage(
            incomeStatement.getNetIncome(), 
            balanceSheet.getEquity()
        ));
        
        // === RATIOS DE LIQUIDITÉ ===
        // Ratio de liquidité générale = Actif Circulant / Dettes CT
        BigDecimal currentAssets = balanceSheet.getCurrentAssets().add(balanceSheet.getCash());
        BigDecimal currentLiabilities = balanceSheet.getCurrentLiabilities();
        ratio.setCurrentRatio(divide(currentAssets, currentLiabilities));
        
        // Ratio de liquidité réduite = (Actif Circulant - Stocks) / Dettes CT
        BigDecimal quickAssets = currentAssets; // Approximation (exclure stocks si disponible)
        ratio.setQuickRatio(divide(quickAssets, currentLiabilities));
        
        // Ratio de liquidité immédiate = Trésorerie / Dettes CT
        ratio.setCashRatio(divide(balanceSheet.getCash(), currentLiabilities));
        
        // === RATIOS DE SOLVABILITÉ ===
        // Taux d'endettement = Total Dettes / Total Actif
        BigDecimal totalDebt = ratio.getTotalDebt();
        ratio.setDebtRatioPct(calculatePercentage(totalDebt, balanceSheet.getTotalAssets()));
        
        // Dette / Capitaux Propres
        ratio.setDebtToEquity(divide(totalDebt, balanceSheet.getEquity()));
        
        // Couverture des intérêts = EBIT / Charges financières
        BigDecimal interestExpense = incomeStatement.getFinancialExpenses();
        if (interestExpense.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ebit = incomeStatement.getOperatingIncome();
            ratio.setInterestCoverage(divide(ebit, interestExpense));
        }
        
        // === RATIOS D'ACTIVITÉ ===
        // Rotation des actifs = CA / Total Actif
        ratio.setAssetTurnover(divide(
            incomeStatement.getTotalRevenue(), 
            balanceSheet.getTotalAssets()
        ));
        
        // === DÉLAIS MOYENS (en jours) ===
        // DSO (Days Sales Outstanding) = Délai moyen de recouvrement
        BigDecimal receivables = getAccountBalance(companyId, "411", endDate); // Clients
        BigDecimal dailySales = divide(incomeStatement.getTotalRevenue(), BigDecimal.valueOf(365));
        if (dailySales.compareTo(BigDecimal.ZERO) > 0) {
            ratio.setDsoDays(divide(receivables, dailySales).intValue());
        }
        
        // DIO (Days Inventory Outstanding) = Délai moyen de stockage
        BigDecimal inventory = getAccountBalance(companyId, "3", endDate); // Stocks
        BigDecimal dailyCogs = divide(incomeStatement.getPurchasesCost(), BigDecimal.valueOf(365));
        if (dailyCogs.compareTo(BigDecimal.ZERO) > 0) {
            ratio.setDioDays(divide(inventory, dailyCogs).intValue());
        }
        
        // DPO (Days Payable Outstanding) = Délai moyen de paiement fournisseurs
        BigDecimal payables = getAccountBalance(companyId, "401", endDate); // Fournisseurs
        if (dailyCogs.compareTo(BigDecimal.ZERO) > 0) {
            ratio.setDpoDays(divide(payables, dailyCogs).intValue());
        }
        
        // Cycle de conversion de trésorerie = DSO + DIO - DPO
        if (ratio.getDsoDays() != null && ratio.getDioDays() != null && ratio.getDpoDays() != null) {
            ratio.setCashConversionCycle(
                ratio.getDsoDays() + ratio.getDioDays() - ratio.getDpoDays()
            );
        }
        
        // Sauvegarder
        FinancialRatio saved = ratioRepository.save(ratio);
        log.info("Ratios calculés et sauvegardés: ID={}", saved.getId());
        
        return saved;
    }
    
    /**
     * Récupère les ratios d'une entreprise pour une année
     */
    public FinancialRatio getRatiosByYear(Long companyId, String fiscalYear) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));
        
        return ratioRepository.findByCompanyAndFiscalYear(company, fiscalYear)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Aucun ratio trouvé pour l'année " + fiscalYear
            ));
    }
    
    /**
     * Récupère l'historique des ratios
     */
    public List<FinancialRatiosResponse> getRatiosHistory(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));
        
        List<FinancialRatio> ratios = ratioRepository.findByCompanyOrderByFiscalYearDesc(company);
        
        return ratios.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Compare les ratios entre deux périodes
     */
    public RatioComparison compareRatios(Long companyId, String year1, String year2) {
        FinancialRatio ratios1 = getRatiosByYear(companyId, year1);
        FinancialRatio ratios2 = getRatiosByYear(companyId, year2);
        
        return RatioComparison.builder()
            .year1(year1)
            .year2(year2)
            .grossMarginEvolution(calculateEvolution(ratios1.getGrossMarginPct(), ratios2.getGrossMarginPct()))
            .netMarginEvolution(calculateEvolution(ratios1.getNetMarginPct(), ratios2.getNetMarginPct()))
            .roaEvolution(calculateEvolution(ratios1.getRoaPct(), ratios2.getRoaPct()))
            .roeEvolution(calculateEvolution(ratios1.getRoePct(), ratios2.getRoePct()))
            .currentRatioEvolution(calculateEvolution(ratios1.getCurrentRatio(), ratios2.getCurrentRatio()))
            .debtRatioEvolution(calculateEvolution(ratios1.getDebtRatioPct(), ratios2.getDebtRatioPct()))
            .build();
    }
    
    // ========== MÉTHODES UTILITAIRES ==========
    
    private BigDecimal calculateWorkingCapital(BalanceSheetResponse balanceSheet) {
        return balanceSheet.getCurrentAssets()
            .add(balanceSheet.getCash())
            .subtract(balanceSheet.getCurrentLiabilities());
    }
    
    private BigDecimal getAccountBalance(Long companyId, String accountPrefix, LocalDate asOfDate) {
        try {
            return glService.getAccountBalance(companyId, accountPrefix, asOfDate);
        } catch (Exception e) {
            log.warn("Impossible de récupérer le solde du compte {}: {}", accountPrefix, e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    private BigDecimal calculatePercentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateEvolution(BigDecimal oldValue, BigDecimal newValue) {
        if (oldValue == null || oldValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return newValue.subtract(oldValue)
            .divide(oldValue, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);
    }
    
    private FinancialRatiosResponse toResponse(FinancialRatio ratio) {
        return FinancialRatiosResponse.builder()
            .fiscalYear(ratio.getFiscalYear())
            .periodStart(ratio.getPeriodStart())
            .periodEnd(ratio.getPeriodEnd())
            .grossMarginPct(ratio.getGrossMarginPct())
            .netMarginPct(ratio.getNetMarginPct())
            .roaPct(ratio.getRoaPct())
            .roePct(ratio.getRoePct())
            .currentRatio(ratio.getCurrentRatio())
            .debtRatioPct(ratio.getDebtRatioPct())
            .dsoDays(ratio.getDsoDays())
            .totalRevenue(ratio.getTotalRevenue())
            .netIncome(ratio.getNetIncome())
            .calculatedAt(ratio.getCalculatedAt())
            .build();
    }
    
    @lombok.Data
    @lombok.Builder
    public static class RatioComparison {
        private String year1;
        private String year2;
        private BigDecimal grossMarginEvolution;
        private BigDecimal netMarginEvolution;
        private BigDecimal roaEvolution;
        private BigDecimal roeEvolution;
        private BigDecimal currentRatioEvolution;
        private BigDecimal debtRatioEvolution;
    }
}