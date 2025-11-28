package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.dto.response.BalanceSheetResponse;
import com.predykt.accounting.dto.response.IncomeStatementResponse;
import com.predykt.accounting.repository.BudgetRepository;
import com.predykt.accounting.repository.CashFlowProjectionRepository;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.repository.GeneralLedgerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Service pour generer les tableaux de bord et KPIs
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final CompanyRepository companyRepository;
    private final FinancialReportService reportService;
    private final FinancialRatioService ratioService;
    private final BudgetRepository budgetRepository;
    private final CashFlowProjectionRepository projectionRepository;
    private final GeneralLedgerRepository generalLedgerRepository;

    /**
     * Genere le dashboard complet pour une entreprise
     */
    public Map<String, Object> getCompanyDashboard(Long companyId, LocalDate asOfDate) {
        log.info("Generation du dashboard pour l'entreprise {} a la date {}", companyId, asOfDate);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvee avec l'ID: " + companyId));

        Map<String, Object> dashboard = new HashMap<>();

        // Informations entreprise
        dashboard.put("company", buildCompanyInfo(company));

        // KPIs financiers
        dashboard.put("kpis", calculateKPIs(company, asOfDate));

        // Situation financiere
        dashboard.put("financialPosition", getFinancialPosition(company, asOfDate));

        // Alertes et notifications
        dashboard.put("alerts", getAlerts(company));

        // Statistiques recentes
        dashboard.put("recentActivity", getRecentActivity(company));

        log.info("Dashboard genere avec succes");
        return dashboard;
    }

    /**
     * Construit les informations de base de l'entreprise
     */
    private Map<String, Object> buildCompanyInfo(Company company) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", company.getId());
        info.put("name", company.getName());
        info.put("taxId", company.getTaxId());
        info.put("currency", company.getCurrency());
        info.put("accountingStandard", company.getAccountingStandard());
        info.put("isActive", company.getIsActive());
        return info;
    }

    /**
     * Calcule les KPIs principaux
     */
    private Map<String, Object> calculateKPIs(Company company, LocalDate asOfDate) {
        Map<String, Object> kpis = new HashMap<>();

        LocalDate startOfYear = LocalDate.of(asOfDate.getYear(), 1, 1);
        LocalDate startOfMonth = asOfDate.withDayOfMonth(1);

        // KPIs du mois en cours
        IncomeStatementResponse monthlyIncome = reportService.generateIncomeStatement(
            company.getId(), startOfMonth, asOfDate
        );

        kpis.put("monthlyRevenue", monthlyIncome.getTotalRevenue());
        kpis.put("monthlyExpenses", monthlyIncome.getTotalExpenses());
        kpis.put("monthlyNetIncome", monthlyIncome.getNetIncome());

        // KPIs de l'annee
        IncomeStatementResponse yearlyIncome = reportService.generateIncomeStatement(
            company.getId(), startOfYear, asOfDate
        );

        kpis.put("yearlyRevenue", yearlyIncome.getTotalRevenue());
        kpis.put("yearlyExpenses", yearlyIncome.getTotalExpenses());
        kpis.put("yearlyNetIncome", yearlyIncome.getNetIncome());

        // Marge brute et nette
        if (yearlyIncome.getTotalRevenue().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal grossMargin = yearlyIncome.getGrossProfit()
                .divide(yearlyIncome.getTotalRevenue(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

            BigDecimal netMargin = yearlyIncome.getNetIncome()
                .divide(yearlyIncome.getTotalRevenue(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

            kpis.put("grossMarginPct", grossMargin.setScale(2, RoundingMode.HALF_UP));
            kpis.put("netMarginPct", netMargin.setScale(2, RoundingMode.HALF_UP));
        } else {
            kpis.put("grossMarginPct", BigDecimal.ZERO);
            kpis.put("netMarginPct", BigDecimal.ZERO);
        }

        // Tresorerie actuelle
        BalanceSheetResponse balanceSheet = reportService.generateBalanceSheet(company.getId(), asOfDate);
        kpis.put("currentCash", balanceSheet.getCash());
        kpis.put("totalAssets", balanceSheet.getTotalAssets());
        kpis.put("totalLiabilities", balanceSheet.getTotalLiabilities());
        kpis.put("totalEquity", balanceSheet.getEquity());

        // Ratio de liquidite
        if (balanceSheet.getCurrentLiabilities().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentRatio = balanceSheet.getCurrentAssets()
                .divide(balanceSheet.getCurrentLiabilities(), 2, RoundingMode.HALF_UP);
            kpis.put("currentRatio", currentRatio);
        } else {
            kpis.put("currentRatio", BigDecimal.ZERO);
        }

        // Ratio d'endettement
        if (balanceSheet.getTotalAssets().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal debtRatio = balanceSheet.getTotalLiabilities()
                .divide(balanceSheet.getTotalAssets(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            kpis.put("debtRatioPct", debtRatio.setScale(2, RoundingMode.HALF_UP));
        } else {
            kpis.put("debtRatioPct", BigDecimal.ZERO);
        }

        return kpis;
    }

    /**
     * Recupere la situation financiere
     */
    private Map<String, Object> getFinancialPosition(Company company, LocalDate asOfDate) {
        Map<String, Object> position = new HashMap<>();

        BalanceSheetResponse balanceSheet = reportService.generateBalanceSheet(company.getId(), asOfDate);

        position.put("assets", Map.of(
            "fixed", balanceSheet.getFixedAssets(),
            "current", balanceSheet.getCurrentAssets(),
            "cash", balanceSheet.getCash(),
            "total", balanceSheet.getTotalAssets()
        ));

        position.put("liabilities", Map.of(
            "longTerm", balanceSheet.getLongTermLiabilities(),
            "current", balanceSheet.getCurrentLiabilities(),
            "total", balanceSheet.getTotalLiabilities()
        ));

        position.put("equity", balanceSheet.getEquity());

        // Verification equilibre bilan
        position.put("isBalanced", balanceSheet.getTotalAssets()
            .compareTo(balanceSheet.getTotalLiabilities().add(balanceSheet.getEquity())) == 0);

        return position;
    }

    /**
     * Genere les alertes
     */
    private Map<String, Object> getAlerts(Company company) {
        Map<String, Object> alerts = new HashMap<>();
        int alertCount = 0;

        // Alertes tresorerie negative
        long negativeCashCount = projectionRepository.findNegativeCashFlowProjections(company).size();
        if (negativeCashCount > 0) {
            alerts.put("negativeCashProjections", Map.of(
                "count", negativeCashCount,
                "severity", "HIGH",
                "message", "Projections de tresorerie negative detectees"
            ));
            alertCount++;
        }

        // Alertes budgets depasses
        long overBudgetCount = budgetRepository.findOverBudgetByCompany(company).size();
        if (overBudgetCount > 0) {
            alerts.put("overBudgets", Map.of(
                "count", overBudgetCount,
                "severity", "MEDIUM",
                "message", "Budgets depasses detectes"
            ));
            alertCount++;
        }

        // Alertes ecritures non verrouillees
        long unlockedEntries = generalLedgerRepository.findUnlockedEntries(company).size();
        if (unlockedEntries > 50) {
            alerts.put("unlockedEntries", Map.of(
                "count", unlockedEntries,
                "severity", "LOW",
                "message", "Nombreuses ecritures non verrouillees"
            ));
            alertCount++;
        }

        alerts.put("totalAlerts", alertCount);

        return alerts;
    }

    /**
     * Recupere l'activite recente
     */
    private Map<String, Object> getRecentActivity(Company company) {
        Map<String, Object> activity = new HashMap<>();

        LocalDate today = LocalDate.now();
        LocalDate last30Days = today.minusDays(30);

        // Nombre d'ecritures recentes
        long recentEntries = generalLedgerRepository
            .findByCompanyAndEntryDateBetween(company, last30Days, today).size();

        activity.put("entriesLast30Days", recentEntries);

        // Nombre de budgets actifs
        long activeBudgets = budgetRepository.findByCompany(company).stream()
            .filter(b -> !b.getIsLocked()).count();

        activity.put("activeBudgets", activeBudgets);

        // Nombre de projections recentes
        long recentProjections = projectionRepository
            .findByCompanyAndProjectionDateBetween(company, last30Days, today).size();

        activity.put("projectionsLast30Days", recentProjections);

        return activity;
    }

    /**
     * Genere un resume pour une periode
     */
    public Map<String, Object> getPeriodSummary(Long companyId, LocalDate startDate, LocalDate endDate) {
        log.info("Generation du resume pour l'entreprise {} du {} au {}", companyId, startDate, endDate);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvee avec l'ID: " + companyId));

        Map<String, Object> summary = new HashMap<>();

        // Compte de resultat de la periode
        IncomeStatementResponse income = reportService.generateIncomeStatement(companyId, startDate, endDate);

        summary.put("period", Map.of("start", startDate, "end", endDate));
        summary.put("revenue", income.getTotalRevenue());
        summary.put("expenses", income.getTotalExpenses());
        summary.put("netIncome", income.getNetIncome());
        summary.put("grossProfit", income.getGrossProfit());
        summary.put("operatingIncome", income.getOperatingIncome());

        // Nombre d'ecritures
        long entryCount = generalLedgerRepository
            .findByCompanyAndEntryDateBetween(company, startDate, endDate).size();
        summary.put("transactionCount", entryCount);

        return summary;
    }

    /**
     * Compare deux periodes
     */
    public Map<String, Object> comparePeriods(
        Long companyId,
        LocalDate period1Start,
        LocalDate period1End,
        LocalDate period2Start,
        LocalDate period2End
    ) {
        log.info("Comparaison de periodes pour l'entreprise {}", companyId);

        Map<String, Object> comparison = new HashMap<>();

        IncomeStatementResponse period1 = reportService.generateIncomeStatement(companyId, period1Start, period1End);
        IncomeStatementResponse period2 = reportService.generateIncomeStatement(companyId, period2Start, period2End);

        comparison.put("period1", Map.of(
            "start", period1Start,
            "end", period1End,
            "revenue", period1.getTotalRevenue(),
            "expenses", period1.getTotalExpenses(),
            "netIncome", period1.getNetIncome()
        ));

        comparison.put("period2", Map.of(
            "start", period2Start,
            "end", period2End,
            "revenue", period2.getTotalRevenue(),
            "expenses", period2.getTotalExpenses(),
            "netIncome", period2.getNetIncome()
        ));

        // Calcul des variations
        Map<String, Object> variations = new HashMap<>();

        if (period1.getTotalRevenue().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal revenueGrowth = period2.getTotalRevenue()
                .subtract(period1.getTotalRevenue())
                .divide(period1.getTotalRevenue(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            variations.put("revenueGrowthPct", revenueGrowth.setScale(2, RoundingMode.HALF_UP));
        }

        if (period1.getTotalExpenses().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal expenseGrowth = period2.getTotalExpenses()
                .subtract(period1.getTotalExpenses())
                .divide(period1.getTotalExpenses(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            variations.put("expenseGrowthPct", expenseGrowth.setScale(2, RoundingMode.HALF_UP));
        }

        comparison.put("variations", variations);

        return comparison;
    }
}
