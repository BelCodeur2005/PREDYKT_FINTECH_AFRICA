package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO pour les journaux auxiliaires OHADA
 *
 * Types de journaux:
 * - VE: Ventes
 * - AC: Achats
 * - BQ: Banque
 * - CA: Caisse
 * - OD: Opérations Diverses
 * - AN: À Nouveaux (ouverture exercice)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuxiliaryJournalResponse {

    private Long companyId;
    private String companyName;
    private String journalCode;
    private String journalName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String currency;

    // Lignes du journal
    private List<JournalEntry> entries;

    // Totaux
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private Integer numberOfEntries;
    private Boolean isBalanced;

    // Statistiques spécifiques selon type journal
    private JournalStatistics statistics;

    /**
     * Ligne d'écriture dans un journal
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JournalEntry {
        private LocalDate entryDate;
        private String pieceNumber;
        private String reference;
        private String accountNumber;
        private String accountName;
        private String description;
        private BigDecimal debitAmount;
        private BigDecimal creditAmount;
        private BigDecimal balance; // Solde cumulé

        // Champs spécifiques journaux Ventes/Achats
        private String invoiceNumber;
        private String thirdPartyName; // Client ou fournisseur
        private String thirdPartyNiu; // NIU client/fournisseur
        private BigDecimal amountHT;
        private BigDecimal vatAmount;
        private BigDecimal amountTTC;
        private BigDecimal vatRate; // 19.25% Cameroun

        // Champs spécifiques journal Banque
        private String bankAccountNumber;
        private String checkNumber;
        private String paymentMethod; // VIREMENT, CHEQUE, CB, PRELEVEMENT

        // État
        private Boolean isReconciled;
        private Boolean isLocked;
    }

    /**
     * Statistiques spécifiques selon type de journal
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JournalStatistics {

        // Pour journal VENTES (VE)
        private BigDecimal totalSalesHT;
        private BigDecimal totalVATCollected;
        private BigDecimal totalSalesTTC;
        private Integer numberOfInvoices;
        private BigDecimal averageInvoiceAmount;
        private List<TopCustomer> topCustomers;

        // Pour journal ACHATS (AC)
        private BigDecimal totalPurchasesHT;
        private BigDecimal totalVATDeductible;
        private BigDecimal totalPurchasesTTC;
        private Integer numberOfBills;
        private BigDecimal averageBillAmount;
        private List<TopSupplier> topSuppliers;

        // Pour journal BANQUE (BQ)
        private BigDecimal totalDebits;
        private BigDecimal totalCredits;
        private BigDecimal netCashFlow;
        private Integer numberOfTransactions;
        private BigDecimal openingBalance;
        private BigDecimal closingBalance;

        // Pour journal CAISSE (CA)
        private BigDecimal cashReceipts;
        private BigDecimal cashPayments;
        private BigDecimal netCashMovement;
        private BigDecimal openingCash;
        private BigDecimal closingCash;

        // Pour journal OPERATIONS DIVERSES (OD)
        private Integer numberOfCorrections;
        private Integer numberOfProvisions;
        private Integer numberOfDepreciations;
        private Integer numberOfOtherOperations;
    }

    /**
     * Top client (pour statistiques journal ventes)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCustomer {
        private String customerName;
        private String customerAccount;
        private BigDecimal totalSales;
        private Integer numberOfInvoices;
    }

    /**
     * Top fournisseur (pour statistiques journal achats)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopSupplier {
        private String supplierName;
        private String supplierAccount;
        private BigDecimal totalPurchases;
        private Integer numberOfBills;
    }
}
