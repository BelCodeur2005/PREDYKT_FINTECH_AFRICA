package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.enums.AccountType;
import com.predykt.accounting.dto.response.BalanceSheetResponse;
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
    
    // Méthodes utilitaires privées
    private BigDecimal calculateAccountClassBalance(Long companyId, String classPrefix, LocalDate asOfDate) {
        return chartService.getActiveAccounts(companyId).stream()
            .filter(account -> account.getAccountNumber().startsWith(classPrefix))
            .map(account -> glService.getAccountBalance(companyId, account.getAccountNumber(), asOfDate))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private BigDecimal calculateAccountClassBalance(Long companyId, String classPrefix, 
                                                    LocalDate asOfDate, String excludePrefix) {
        return chartService.getActiveAccounts(companyId).stream()
            .filter(account -> account.getAccountNumber().startsWith(classPrefix) 
                            && !account.getAccountNumber().startsWith(excludePrefix))
            .map(account -> glService.getAccountBalance(companyId, account.getAccountNumber(), asOfDate))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private BigDecimal calculateAccountBalance(Long companyId, String accountNumber, 
                                               LocalDate startDate, LocalDate endDate) {
        try {
            return glService.getAccountBalance(companyId, accountNumber, endDate)
                .subtract(glService.getAccountBalance(companyId, accountNumber, startDate.minusDays(1)));
        } catch (ResourceNotFoundException e) {
            return BigDecimal.ZERO;
        }
    }
    
    private BigDecimal calculateAccountClassBalance(Long companyId, String classPrefix, 
                                                    LocalDate startDate, LocalDate endDate) {
        return chartService.getActiveAccounts(companyId).stream()
            .filter(account -> account.getAccountNumber().startsWith(classPrefix))
            .map(account -> calculateAccountBalance(companyId, account.getAccountNumber(), startDate, endDate))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}