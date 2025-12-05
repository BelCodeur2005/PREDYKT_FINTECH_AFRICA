package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.GeneralLedger;
import com.predykt.accounting.dto.response.AuxiliaryJournalResponse;
import com.predykt.accounting.dto.response.AuxiliaryJournalResponse.*;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.repository.GeneralLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service pour générer les journaux auxiliaires conformes OHADA
 *
 * Les 6 journaux obligatoires:
 * - VE: Journal des Ventes
 * - AC: Journal des Achats
 * - BQ: Journal de Banque
 * - CA: Journal de Caisse
 * - OD: Journal des Opérations Diverses
 * - AN: Journal à Nouveaux (ouverture exercice)
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AuxiliaryJournalsService {

    private final GeneralLedgerRepository generalLedgerRepository;
    private final CompanyRepository companyRepository;

    private static final BigDecimal VAT_RATE_CAMEROON = new BigDecimal("0.1925");

    /**
     * Génère le journal des VENTES (VE)
     * Toutes les factures de ventes avec TVA collectée
     */
    public AuxiliaryJournalResponse getSalesJournal(Long companyId, LocalDate startDate, LocalDate endDate) {
        log.info("Génération journal des ventes pour entreprise {} du {} au {}", companyId, startDate, endDate);

        Company company = getCompany(companyId);

        List<GeneralLedger> entries = generalLedgerRepository.findByCompanyAndJournalCodeAndEntryDateBetween(
            company, "VE", startDate, endDate);

        List<JournalEntry> journalEntries = buildJournalEntries(entries, "VE");
        JournalStatistics stats = buildSalesStatistics(entries, journalEntries);

        return buildJournalResponse(company, "VE", "Journal des Ventes",
            startDate, endDate, journalEntries, stats);
    }

    /**
     * Génère le journal des ACHATS (AC)
     * Toutes les factures d'achats avec TVA déductible
     */
    public AuxiliaryJournalResponse getPurchasesJournal(Long companyId, LocalDate startDate, LocalDate endDate) {
        log.info("Génération journal des achats pour entreprise {} du {} au {}", companyId, startDate, endDate);

        Company company = getCompany(companyId);

        List<GeneralLedger> entries = generalLedgerRepository.findByCompanyAndJournalCodeAndEntryDateBetween(
            company, "AC", startDate, endDate);

        List<JournalEntry> journalEntries = buildJournalEntries(entries, "AC");
        JournalStatistics stats = buildPurchasesStatistics(entries, journalEntries);

        return buildJournalResponse(company, "AC", "Journal des Achats",
            startDate, endDate, journalEntries, stats);
    }

    /**
     * Génère le journal de BANQUE (BQ)
     * Tous les mouvements bancaires
     */
    public AuxiliaryJournalResponse getBankJournal(Long companyId, LocalDate startDate, LocalDate endDate) {
        log.info("Génération journal de banque pour entreprise {} du {} au {}", companyId, startDate, endDate);

        Company company = getCompany(companyId);

        List<GeneralLedger> entries = generalLedgerRepository.findByCompanyAndJournalCodeAndEntryDateBetween(
            company, "BQ", startDate, endDate);

        List<JournalEntry> journalEntries = buildJournalEntries(entries, "BQ");
        JournalStatistics stats = buildBankStatistics(entries, journalEntries, company, startDate);

        return buildJournalResponse(company, "BQ", "Journal de Banque",
            startDate, endDate, journalEntries, stats);
    }

    /**
     * Génère le journal de CAISSE (CA)
     * Tous les mouvements de caisse
     */
    public AuxiliaryJournalResponse getCashJournal(Long companyId, LocalDate startDate, LocalDate endDate) {
        log.info("Génération journal de caisse pour entreprise {} du {} au {}", companyId, startDate, endDate);

        Company company = getCompany(companyId);

        List<GeneralLedger> entries = generalLedgerRepository.findByCompanyAndJournalCodeAndEntryDateBetween(
            company, "CA", startDate, endDate);

        List<JournalEntry> journalEntries = buildJournalEntries(entries, "CA");
        JournalStatistics stats = buildCashStatistics(entries, journalEntries, company, startDate);

        return buildJournalResponse(company, "CA", "Journal de Caisse",
            startDate, endDate, journalEntries, stats);
    }

    /**
     * Génère le journal des OPÉRATIONS DIVERSES (OD)
     * Écritures diverses (provisions, corrections, régularisations, etc.)
     */
    public AuxiliaryJournalResponse getGeneralJournal(Long companyId, LocalDate startDate, LocalDate endDate) {
        log.info("Génération journal des opérations diverses pour entreprise {} du {} au {}",
            companyId, startDate, endDate);

        Company company = getCompany(companyId);

        List<GeneralLedger> entries = generalLedgerRepository.findByCompanyAndJournalCodeAndEntryDateBetween(
            company, "OD", startDate, endDate);

        List<JournalEntry> journalEntries = buildJournalEntries(entries, "OD");
        JournalStatistics stats = buildGeneralJournalStatistics(entries);

        return buildJournalResponse(company, "OD", "Journal des Opérations Diverses",
            startDate, endDate, journalEntries, stats);
    }

    /**
     * Génère le journal À NOUVEAUX (AN)
     * Écritures d'ouverture d'un exercice (soldes N-1)
     */
    public AuxiliaryJournalResponse getOpeningJournal(Long companyId, Integer fiscalYear) {
        log.info("Génération journal à nouveaux pour entreprise {} - Exercice {}", companyId, fiscalYear);

        Company company = getCompany(companyId);

        LocalDate startDate = LocalDate.of(fiscalYear, 1, 1);
        LocalDate endDate = LocalDate.of(fiscalYear, 1, 31); // Généralement en janvier

        List<GeneralLedger> entries = generalLedgerRepository.findByCompanyAndJournalCodeAndEntryDateBetween(
            company, "AN", startDate, endDate);

        List<JournalEntry> journalEntries = buildJournalEntries(entries, "AN");
        JournalStatistics stats = new JournalStatistics(); // Pas de stats spécifiques pour AN

        return buildJournalResponse(company, "AN", "Journal à Nouveaux (Ouverture " + fiscalYear + ")",
            startDate, endDate, journalEntries, stats);
    }

    /**
     * Construit les lignes du journal à partir des écritures du grand livre
     */
    private List<JournalEntry> buildJournalEntries(List<GeneralLedger> entries, String journalCode) {
        // Grouper par pièce comptable pour analyser les écritures complètes
        Map<String, List<GeneralLedger>> entriesByPiece = entries.stream()
            .collect(Collectors.groupingBy(
                e -> e.getPieceNumber() != null ? e.getPieceNumber() : e.getReferenceNumber(),
                LinkedHashMap::new,
                Collectors.toList()
            ));

        List<JournalEntry> journalEntries = new ArrayList<>();
        BigDecimal cumulativeBalance = BigDecimal.ZERO;

        for (Map.Entry<String, List<GeneralLedger>> pieceGroup : entriesByPiece.entrySet()) {
            List<GeneralLedger> pieceEntries = pieceGroup.getValue();

            // Pour chaque ligne de la pièce
            for (GeneralLedger entry : pieceEntries) {
                BigDecimal amount = entry.getDebitAmount().subtract(entry.getCreditAmount());
                cumulativeBalance = cumulativeBalance.add(amount);

                JournalEntry journalEntry = JournalEntry.builder()
                    .entryDate(entry.getEntryDate())
                    .pieceNumber(entry.getPieceNumber())
                    .reference(entry.getReferenceNumber())
                    .accountNumber(entry.getAccountNumber())
                    .accountName(entry.getAccountName())
                    .description(entry.getDescription())
                    .debitAmount(entry.getDebitAmount())
                    .creditAmount(entry.getCreditAmount())
                    .balance(cumulativeBalance)
                    .isReconciled(entry.getIsReconciled())
                    .isLocked(entry.getIsLocked())
                    .build();

                // Enrichir avec données spécifiques selon journal
                enrichJournalEntry(journalEntry, entry, pieceEntries, journalCode);

                journalEntries.add(journalEntry);
            }
        }

        return journalEntries;
    }

    /**
     * Enrichit une ligne de journal avec des données spécifiques selon le type
     */
    private void enrichJournalEntry(JournalEntry journalEntry, GeneralLedger entry,
                                    List<GeneralLedger> pieceEntries, String journalCode) {

        if ("VE".equals(journalCode) || "AC".equals(journalCode)) {
            // Pour ventes/achats: extraire HT, TVA, TTC
            enrichSalesOrPurchaseEntry(journalEntry, pieceEntries, journalCode);
        } else if ("BQ".equals(journalCode)) {
            // Pour banque: extraire infos paiement
            enrichBankEntry(journalEntry, entry);
        }
    }

    /**
     * Enrichit une écriture vente/achat avec HT, TVA, TTC
     */
    private void enrichSalesOrPurchaseEntry(JournalEntry journalEntry,
                                           List<GeneralLedger> pieceEntries, String journalCode) {

        // Rechercher le compte de TVA
        String vatAccountPrefix = "VE".equals(journalCode) ? "4431" : "4452"; // Collectée ou Déductible

        Optional<GeneralLedger> vatEntry = pieceEntries.stream()
            .filter(e -> e.getAccountNumber() != null && e.getAccountNumber().startsWith(vatAccountPrefix))
            .findFirst();

        if (vatEntry.isPresent()) {
            BigDecimal vatAmount = "VE".equals(journalCode)
                ? vatEntry.get().getCreditAmount()
                : vatEntry.get().getDebitAmount();

            journalEntry.setVatAmount(vatAmount);
            journalEntry.setVatRate(VAT_RATE_CAMEROON.multiply(new BigDecimal("100")));

            // Calculer HT à partir de la TVA
            BigDecimal amountHT = vatAmount.divide(VAT_RATE_CAMEROON, 2, RoundingMode.HALF_UP);
            BigDecimal amountTTC = amountHT.add(vatAmount);

            journalEntry.setAmountHT(amountHT);
            journalEntry.setAmountTTC(amountTTC);
        }

        // Extraire nom client/fournisseur de la description
        if (journalEntry.getDescription() != null) {
            String[] parts = journalEntry.getDescription().split("-");
            if (parts.length > 1) {
                journalEntry.setThirdPartyName(parts[1].trim());
            }
        }
    }

    /**
     * Enrichit une écriture banque
     */
    private void enrichBankEntry(JournalEntry journalEntry, GeneralLedger entry) {
        // Extraire méthode de paiement de la description
        if (entry.getDescription() != null) {
            String desc = entry.getDescription().toUpperCase();
            if (desc.contains("CHEQUE") || desc.contains("CHQ")) {
                journalEntry.setPaymentMethod("CHEQUE");
            } else if (desc.contains("VIREMENT") || desc.contains("VIR")) {
                journalEntry.setPaymentMethod("VIREMENT");
            } else if (desc.contains("CARTE") || desc.contains("CB")) {
                journalEntry.setPaymentMethod("CB");
            } else if (desc.contains("PRELEVEMENT")) {
                journalEntry.setPaymentMethod("PRELEVEMENT");
            }
        }
    }

    /**
     * Construit les statistiques du journal des VENTES
     */
    private JournalStatistics buildSalesStatistics(List<GeneralLedger> entries, List<JournalEntry> journalEntries) {

        BigDecimal totalSalesHT = entries.stream()
            .filter(e -> e.getAccountNumber() != null && e.getAccountNumber().startsWith("7"))
            .map(GeneralLedger::getCreditAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVATCollected = entries.stream()
            .filter(e -> e.getAccountNumber() != null && e.getAccountNumber().startsWith("4431"))
            .map(GeneralLedger::getCreditAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSalesTTC = totalSalesHT.add(totalVATCollected);

        int numberOfInvoices = (int) journalEntries.stream()
            .map(JournalEntry::getPieceNumber)
            .distinct()
            .count();

        BigDecimal avgInvoice = numberOfInvoices > 0
            ? totalSalesTTC.divide(new BigDecimal(numberOfInvoices), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return JournalStatistics.builder()
            .totalSalesHT(totalSalesHT)
            .totalVATCollected(totalVATCollected)
            .totalSalesTTC(totalSalesTTC)
            .numberOfInvoices(numberOfInvoices)
            .averageInvoiceAmount(avgInvoice)
            .topCustomers(new ArrayList<>()) // À implémenter si besoin
            .build();
    }

    /**
     * Construit les statistiques du journal des ACHATS
     */
    private JournalStatistics buildPurchasesStatistics(List<GeneralLedger> entries, List<JournalEntry> journalEntries) {

        BigDecimal totalPurchasesHT = entries.stream()
            .filter(e -> e.getAccountNumber() != null && e.getAccountNumber().startsWith("6"))
            .map(GeneralLedger::getDebitAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVATDeductible = entries.stream()
            .filter(e -> e.getAccountNumber() != null && e.getAccountNumber().startsWith("4452"))
            .map(GeneralLedger::getDebitAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPurchasesTTC = totalPurchasesHT.add(totalVATDeductible);

        int numberOfBills = (int) journalEntries.stream()
            .map(JournalEntry::getPieceNumber)
            .distinct()
            .count();

        BigDecimal avgBill = numberOfBills > 0
            ? totalPurchasesTTC.divide(new BigDecimal(numberOfBills), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return JournalStatistics.builder()
            .totalPurchasesHT(totalPurchasesHT)
            .totalVATDeductible(totalVATDeductible)
            .totalPurchasesTTC(totalPurchasesTTC)
            .numberOfBills(numberOfBills)
            .averageBillAmount(avgBill)
            .topSuppliers(new ArrayList<>())
            .build();
    }

    /**
     * Construit les statistiques du journal de BANQUE
     */
    private JournalStatistics buildBankStatistics(List<GeneralLedger> entries, List<JournalEntry> journalEntries,
                                                  Company company, LocalDate startDate) {

        BigDecimal totalDebits = entries.stream()
            .filter(e -> e.getAccountNumber() != null && e.getAccountNumber().startsWith("52"))
            .map(GeneralLedger::getDebitAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = entries.stream()
            .filter(e -> e.getAccountNumber() != null && e.getAccountNumber().startsWith("52"))
            .map(GeneralLedger::getCreditAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netCashFlow = totalDebits.subtract(totalCredits);

        // Solde d'ouverture (approximation)
        BigDecimal openingBalance = getBankBalanceAtDate(company, startDate.minusDays(1));
        BigDecimal closingBalance = openingBalance.add(netCashFlow);

        return JournalStatistics.builder()
            .totalDebits(totalDebits)
            .totalCredits(totalCredits)
            .netCashFlow(netCashFlow)
            .numberOfTransactions(journalEntries.size())
            .openingBalance(openingBalance)
            .closingBalance(closingBalance)
            .build();
    }

    /**
     * Construit les statistiques du journal de CAISSE
     */
    private JournalStatistics buildCashStatistics(List<GeneralLedger> entries, List<JournalEntry> journalEntries,
                                                  Company company, LocalDate startDate) {

        BigDecimal cashReceipts = entries.stream()
            .filter(e -> e.getAccountNumber() != null && e.getAccountNumber().startsWith("57"))
            .map(GeneralLedger::getDebitAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cashPayments = entries.stream()
            .filter(e -> e.getAccountNumber() != null && e.getAccountNumber().startsWith("57"))
            .map(GeneralLedger::getCreditAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netCashMovement = cashReceipts.subtract(cashPayments);

        BigDecimal openingCash = getCashBalanceAtDate(company, startDate.minusDays(1));
        BigDecimal closingCash = openingCash.add(netCashMovement);

        return JournalStatistics.builder()
            .cashReceipts(cashReceipts)
            .cashPayments(cashPayments)
            .netCashMovement(netCashMovement)
            .openingCash(openingCash)
            .closingCash(closingCash)
            .build();
    }

    /**
     * Construit les statistiques du journal OPÉRATIONS DIVERSES
     */
    private JournalStatistics buildGeneralJournalStatistics(List<GeneralLedger> entries) {

        int corrections = (int) entries.stream()
            .filter(e -> e.getDescription() != null &&
                (e.getDescription().contains("correction") || e.getDescription().contains("régularisation")))
            .count();

        int provisions = (int) entries.stream()
            .filter(e -> e.getAccountNumber() != null && e.getAccountNumber().startsWith("691"))
            .count();

        int depreciations = (int) entries.stream()
            .filter(e -> e.getAccountNumber() != null && e.getAccountNumber().startsWith("681"))
            .count();

        return JournalStatistics.builder()
            .numberOfCorrections(corrections)
            .numberOfProvisions(provisions)
            .numberOfDepreciations(depreciations)
            .numberOfOtherOperations(entries.size() - corrections - provisions - depreciations)
            .build();
    }

    /**
     * Construit la réponse finale du journal
     */
    private AuxiliaryJournalResponse buildJournalResponse(Company company, String journalCode, String journalName,
                                                          LocalDate startDate, LocalDate endDate,
                                                          List<JournalEntry> entries, JournalStatistics stats) {

        BigDecimal totalDebit = entries.stream()
            .map(JournalEntry::getDebitAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = entries.stream()
            .map(JournalEntry::getCreditAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean isBalanced = totalDebit.compareTo(totalCredit) == 0;

        return AuxiliaryJournalResponse.builder()
            .companyId(company.getId())
            .companyName(company.getName())
            .journalCode(journalCode)
            .journalName(journalName)
            .startDate(startDate)
            .endDate(endDate)
            .currency("XAF")
            .entries(entries)
            .totalDebit(totalDebit)
            .totalCredit(totalCredit)
            .numberOfEntries(entries.size())
            .isBalanced(isBalanced)
            .statistics(stats)
            .build();
    }

    private Company getCompany(Long companyId) {
        return companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée: " + companyId));
    }

    private BigDecimal getBankBalanceAtDate(Company company, LocalDate date) {
        List<GeneralLedger> entries = generalLedgerRepository
            .findByCompanyAndAccountNumberStartingWithAndEntryDateLessThanEqual(company, "52", date);

        BigDecimal debit = entries.stream().map(GeneralLedger::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = entries.stream().map(GeneralLedger::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return debit.subtract(credit);
    }

    private BigDecimal getCashBalanceAtDate(Company company, LocalDate date) {
        List<GeneralLedger> entries = generalLedgerRepository
            .findByCompanyAndAccountNumberStartingWithAndEntryDateLessThanEqual(company, "57", date);

        BigDecimal debit = entries.stream().map(GeneralLedger::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = entries.stream().map(GeneralLedger::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return debit.subtract(credit);
    }
}
