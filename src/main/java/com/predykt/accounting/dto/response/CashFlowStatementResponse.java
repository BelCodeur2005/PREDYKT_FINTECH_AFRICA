package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Réponse contenant le tableau de flux de trésorerie (Cash Flow Statement)
 * Conforme OHADA - OBLIGATOIRE dans les états financiers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowStatementResponse {

    private Long companyId;
    private String companyName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String fiscalYear;

    // A. FLUX DE TRÉSORERIE LIÉS À L'EXPLOITATION
    private OperatingCashFlow operatingCashFlow;

    // B. FLUX DE TRÉSORERIE LIÉS AUX INVESTISSEMENTS
    private InvestingCashFlow investingCashFlow;

    // C. FLUX DE TRÉSORERIE LIÉS AU FINANCEMENT
    private FinancingCashFlow financingCashFlow;

    // RÉSUMÉ
    private CashFlowSummary summary;

    /**
     * Section A: Flux de trésorerie d'exploitation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperatingCashFlow {
        // Résultat net
        private BigDecimal netIncome;

        // Ajustements pour éléments sans effet de trésorerie
        private BigDecimal depreciationAndAmortization;
        private BigDecimal provisionsIncrease;
        private BigDecimal provisionsDecrease;
        private BigDecimal gainOnAssetDisposal;
        private BigDecimal lossOnAssetDisposal;

        // Résultat avant variation BFR
        private BigDecimal incomeBeforeWorkingCapitalChanges;

        // Variations du besoin en fonds de roulement
        private BigDecimal inventoryChange;
        private BigDecimal receivablesChange;
        private BigDecimal prepaidExpensesChange;
        private BigDecimal payablesChange;
        private BigDecimal accruedExpensesChange;

        // Total flux d'exploitation
        private BigDecimal netOperatingCashFlow;
    }

    /**
     * Section B: Flux de trésorerie d'investissement
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvestingCashFlow {
        // Acquisitions d'immobilisations
        private BigDecimal intangibleAssetsAcquisitions;
        private BigDecimal tangibleAssetsAcquisitions;
        private BigDecimal financialAssetsAcquisitions;

        // Cessions d'immobilisations
        private BigDecimal intangibleAssetDisposals;
        private BigDecimal tangibleAssetDisposals;
        private BigDecimal financialAssetDisposals;

        // Détail des principales acquisitions
        private List<AssetTransaction> majorAcquisitions;

        // Détail des principales cessions
        private List<AssetTransaction> majorDisposals;

        // Total flux d'investissement
        private BigDecimal netInvestingCashFlow;
    }

    /**
     * Section C: Flux de trésorerie de financement
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancingCashFlow {
        // Apports en capital
        private BigDecimal capitalIncreases;
        private BigDecimal capitalDecreases;

        // Emprunts
        private BigDecimal borrowingsReceived;
        private BigDecimal borrowingsRepaid;

        // Dividendes
        private BigDecimal dividendsPaid;

        // Subventions
        private BigDecimal subsidiesReceived;

        // Total flux de financement
        private BigDecimal netFinancingCashFlow;
    }

    /**
     * Transaction d'actif (acquisition ou cession)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetTransaction {
        private LocalDate date;
        private String description;
        private String accountNumber;
        private BigDecimal amount;
        private String transactionType; // "ACQUISITION" ou "DISPOSAL"
    }

    /**
     * Résumé du tableau de flux
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CashFlowSummary {
        // Flux nets par catégorie
        private BigDecimal netOperatingCashFlow;
        private BigDecimal netInvestingCashFlow;
        private BigDecimal netFinancingCashFlow;

        // Variation nette de trésorerie
        private BigDecimal netCashChange;

        // Trésorerie début/fin
        private BigDecimal beginningCash;
        private BigDecimal endingCash;

        // Vérification
        private BigDecimal calculatedEndingCash;
        private Boolean isBalanced;

        // Ratios
        private BigDecimal cashFlowFromOperationsRatio; // % du résultat net
        private BigDecimal freeCashFlow; // Operating - Investing
    }
}
