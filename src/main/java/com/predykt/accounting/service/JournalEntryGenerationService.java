package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.FixedAsset;
import com.predykt.accounting.domain.entity.GeneralLedger;
import com.predykt.accounting.dto.request.FixedAssetDisposalRequest;
import com.predykt.accounting.exception.AccountingException;
import com.predykt.accounting.repository.ChartOfAccountsRepository;
import com.predykt.accounting.repository.CompanyRepository;
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
import java.util.UUID;

/**
 * Service de génération automatique des écritures comptables
 * Conforme OHADA et CGI Cameroun
 *
 * Spécialisé dans la génération d'écritures complexes:
 * - Cession d'immobilisations (comptes 654, 754, 28x, 2xx, 485)
 * - Dotations aux amortissements (681x, 28x)
 * - Provisions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JournalEntryGenerationService {

    private final GeneralLedgerRepository generalLedgerRepository;
    private final ChartOfAccountsRepository chartOfAccountsRepository;
    private final CompanyRepository companyRepository;
    private final DepreciationService depreciationService;

    // Taux TVA Cameroun (19,25%)
    private static final BigDecimal VAT_RATE_CAMEROON = new BigDecimal("0.1925");

    /**
     * Générer les écritures comptables de cession d'une immobilisation
     * Conforme OHADA - Génère 2 ou 3 écritures selon le type de cession
     *
     * @param asset L'immobilisation cédée
     * @param netBookValue VNC au moment de la cession
     * @param gainLoss Plus-value (>0) ou Moins-value (<0)
     * @param request Détails de la cession
     * @return Liste des écritures générées
     */
    @Transactional
    public List<GeneralLedger> generateDisposalJournalEntries(
            FixedAsset asset,
            BigDecimal netBookValue,
            BigDecimal gainLoss,
            FixedAssetDisposalRequest request) {

        log.info("Génération écritures de cession - Asset: {} - Type: {} - Montant: {}",
                 asset.getAssetNumber(), request.getDisposalType(), request.getDisposalAmount());

        List<GeneralLedger> entries = new ArrayList<>();

        // Générer un numéro de pièce unique
        String pieceNumber = generatePieceNumber("CESSION", asset.getCompany(), request.getDisposalDate());

        // ÉCRITURE 1: Sortie de l'immobilisation de l'actif (OBLIGATOIRE)
        entries.addAll(generateAssetRemovalEntry(asset, netBookValue, pieceNumber, request));

        // ÉCRITURE 2: Constatation du produit de cession (si VENTE ou cession avec produit)
        if ("SALE".equals(request.getDisposalType()) ||
            (request.getDisposalAmount() != null && request.getDisposalAmount().compareTo(BigDecimal.ZERO) > 0)) {

            entries.addAll(generateDisposalRevenueEntry(asset, request, pieceNumber));
        }

        // ÉCRITURE 3: Encaissement (si paiement comptant spécifié)
        // Note: Dans la plupart des cas, l'encaissement sera enregistré séparément
        // Cette partie est commentée car elle dépend du mode de paiement

        // Sauvegarder toutes les écritures
        List<GeneralLedger> savedEntries = generalLedgerRepository.saveAll(entries);

        log.info("✅ {} écriture(s) de cession générée(s) - Pièce: {} - Plus/Moins-value: {} FCFA",
                 savedEntries.size(), pieceNumber, gainLoss);

        return savedEntries;
    }

    /**
     * ÉCRITURE 1: Sortie de l'immobilisation de l'actif
     *
     * Débit  2845 - Amortissements cumulés
     * Débit  654  - Valeur comptable des cessions (VNC)
     *        Crédit 245 - Immobilisation (valeur brute)
     */
    private List<GeneralLedger> generateAssetRemovalEntry(
            FixedAsset asset,
            BigDecimal netBookValue,
            String pieceNumber,
            FixedAssetDisposalRequest request) {

        List<GeneralLedger> entries = new ArrayList<>();

        BigDecimal grossValue = asset.getTotalCost() != null ? asset.getTotalCost() : asset.getAcquisitionCost();
        BigDecimal accumulatedDepreciation = grossValue.subtract(netBookValue);

        String description = String.format(
            "Sortie immobilisation %s - %s (cession %s)",
            asset.getAssetNumber(),
            asset.getAssetName(),
            request.getDisposalType().toLowerCase()
        );

        // 1. Débit 28xx - Amortissements cumulés (on les annule)
        if (accumulatedDepreciation.compareTo(BigDecimal.ZERO) > 0) {
            String depreciationAccount = asset.getCategory().getDepreciationAccountNumber();

            entries.add(createJournalEntry(
                asset.getCompany(),
                request.getDisposalDate(),
                depreciationAccount,
                "Amortissements " + asset.getCategory().getDisplayName(),
                description,
                accumulatedDepreciation,  // Débit
                BigDecimal.ZERO,          // Crédit
                "OD",
                pieceNumber,
                "DISPOSAL_ASSET_REMOVAL"
            ));
        }

        // 2. Débit 654 - Valeur comptable des cessions (VNC)
        entries.add(createJournalEntry(
            asset.getCompany(),
            request.getDisposalDate(),
            "654",
            "Valeur comptable des cessions d'immobilisations",
            description,
            netBookValue,            // Débit (la VNC = charge potentielle)
            BigDecimal.ZERO,         // Crédit
            "OD",
            pieceNumber,
            "DISPOSAL_NET_BOOK_VALUE"
        ));

        // 3. Crédit 2xxx - Immobilisation (sortie de l'actif)
        entries.add(createJournalEntry(
            asset.getCompany(),
            request.getDisposalDate(),
            asset.getAccountNumber(),
            asset.getCategory().getDisplayName(),
            description,
            BigDecimal.ZERO,         // Débit
            grossValue,              // Crédit (on sort la valeur brute)
            "OD",
            pieceNumber,
            "DISPOSAL_ASSET_GROSS_VALUE"
        ));

        log.info("Écriture 1 générée - Sortie actif: Valeur brute {} - Amort. cumulés {} - VNC {}",
                 grossValue, accumulatedDepreciation, netBookValue);

        return entries;
    }

    /**
     * ÉCRITURE 2: Constatation du produit de cession
     *
     * Débit  485  - Créances sur cessions d'immobilisations (TTC)
     *        Crédit 754  - Produits de cessions d'actifs (HT)
     *        Crédit 4431 - TVA collectée (si assujetti)
     */
    private List<GeneralLedger> generateDisposalRevenueEntry(
            FixedAsset asset,
            FixedAssetDisposalRequest request,
            String pieceNumber) {

        List<GeneralLedger> entries = new ArrayList<>();

        BigDecimal saleAmountHT = request.getDisposalAmount();

        // Calculer la TVA si c'est une VENTE (assujetti à TVA)
        BigDecimal vatAmount = BigDecimal.ZERO;
        BigDecimal totalTTC = saleAmountHT;

        if ("SALE".equals(request.getDisposalType())) {
            // TVA collectée = 19,25% au Cameroun
            vatAmount = saleAmountHT.multiply(VAT_RATE_CAMEROON).setScale(0, RoundingMode.HALF_UP);
            totalTTC = saleAmountHT.add(vatAmount);
        }

        String description = String.format(
            "Produit cession %s - %s - Acheteur: %s",
            asset.getAssetNumber(),
            asset.getAssetName(),
            request.getBuyerName() != null ? request.getBuyerName() : "N/A"
        );

        // 1. Débit 485 - Créances sur cessions (TTC)
        entries.add(createJournalEntry(
            asset.getCompany(),
            request.getDisposalDate(),
            "485",
            "Créances sur cessions d'immobilisations",
            description + " - Facture: " + request.getInvoiceNumber(),
            totalTTC,                // Débit (créance TTC)
            BigDecimal.ZERO,         // Crédit
            "VE",                    // Journal Ventes
            pieceNumber,
            "DISPOSAL_RECEIVABLE"
        ));

        // 2. Crédit 754 - Produits de cessions (HT)
        entries.add(createJournalEntry(
            asset.getCompany(),
            request.getDisposalDate(),
            "754",
            "Produits de cessions d'actifs immobilisés",
            description,
            BigDecimal.ZERO,         // Débit
            saleAmountHT,            // Crédit (produit HT)
            "VE",
            pieceNumber,
            "DISPOSAL_REVENUE"
        ));

        // 3. Crédit 4431 - TVA collectée (si assujetti)
        if (vatAmount.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(createJournalEntry(
                asset.getCompany(),
                request.getDisposalDate(),
                "4431",
                "TVA collectée",
                description + " - TVA 19,25%",
                BigDecimal.ZERO,     // Débit
                vatAmount,           // Crédit (TVA)
                "VE",
                pieceNumber,
                "DISPOSAL_VAT_COLLECTED"
            ));
        }

        log.info("Écriture 2 générée - Produit cession: HT {} - TVA {} - TTC {}",
                 saleAmountHT, vatAmount, totalTTC);

        return entries;
    }

    /**
     * Générer les écritures de dotations aux amortissements mensuelles
     *
     * Débit  681x - Dotations aux amortissements
     *        Crédit 28xx - Amortissements
     */
    @Transactional
    public List<GeneralLedger> generateMonthlyDepreciationEntries(
            Company company,
            Integer year,
            Integer month) {

        log.info("Génération dotations aux amortissements - Entreprise: {} - Période: {}/{}",
                 company.getId(), month, year);

        List<GeneralLedger> entries = new ArrayList<>();

        // Récupérer toutes les immobilisations amortissables actives
        List<FixedAsset> assets = depreciationService.getDepreciableAssets(company, year, month);

        LocalDate periodEnd = LocalDate.of(year, month, 1).withDayOfMonth(
            LocalDate.of(year, month, 1).lengthOfMonth()
        );

        String pieceNumber = generatePieceNumber("AMORT", company, periodEnd);

        for (FixedAsset asset : assets) {
            BigDecimal monthlyDepreciation = depreciationService.calculateMonthlyDepreciation(asset, year, month);

            if (monthlyDepreciation.compareTo(BigDecimal.ZERO) > 0) {
                String description = String.format(
                    "Dotation amortissement %s/%s - %s",
                    month, year, asset.getAssetNumber()
                );

                // Débit 681x - Dotations
                String expenseAccount = asset.getCategory().getDepreciationExpenseAccountNumber();
                entries.add(createJournalEntry(
                    company,
                    periodEnd,
                    expenseAccount,
                    "Dotations aux amortissements " + asset.getCategory().getDisplayName(),
                    description,
                    monthlyDepreciation,     // Débit (charge)
                    BigDecimal.ZERO,         // Crédit
                    "OD",
                    pieceNumber,
                    "DEPRECIATION_EXPENSE"
                ));

                // Crédit 28xx - Amortissements cumulés
                String depreciationAccount = asset.getCategory().getDepreciationAccountNumber();
                entries.add(createJournalEntry(
                    company,
                    periodEnd,
                    depreciationAccount,
                    "Amortissements " + asset.getCategory().getDisplayName(),
                    description,
                    BigDecimal.ZERO,         // Débit
                    monthlyDepreciation,     // Crédit (amortissement cumulé)
                    "OD",
                    pieceNumber,
                    "DEPRECIATION_ACCUMULATED"
                ));
            }
        }

        if (!entries.isEmpty()) {
            List<GeneralLedger> savedEntries = generalLedgerRepository.saveAll(entries);
            log.info("✅ {} écriture(s) de dotations générée(s) - Total: {} FCFA",
                     savedEntries.size(),
                     entries.stream()
                            .filter(e -> e.getDebitAmount().compareTo(BigDecimal.ZERO) > 0)
                            .map(GeneralLedger::getDebitAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
            return savedEntries;
        }

        log.info("Aucune dotation à enregistrer pour cette période");
        return entries;
    }

    /**
     * Créer une écriture de journal
     */
    private GeneralLedger createJournalEntry(
            Company company,
            LocalDate entryDate,
            String accountNumber,
            String accountName,
            String description,
            BigDecimal debitAmount,
            BigDecimal creditAmount,
            String journalCode,
            String pieceNumber,
            String entryType) {

        // Vérifier que le compte existe dans le plan comptable
        chartOfAccountsRepository.findByCompanyAndAccountNumber(company, accountNumber)
            .orElseThrow(() -> new AccountingException(
                "Compte OHADA non trouvé: " + accountNumber + " - Veuillez l'ajouter au plan comptable"));

        GeneralLedger entry = GeneralLedger.builder()
            .company(company)
            .entryDate(entryDate)
            .accountNumber(accountNumber)
            .accountName(accountName)
            .description(description)
            .debitAmount(debitAmount)
            .creditAmount(creditAmount)
            .journalCode(journalCode)
            .pieceNumber(pieceNumber)
            .referenceNumber(UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .fiscalYear(entryDate.getYear())
            .fiscalPeriod(entryDate.getMonthValue())
            .isLocked(false)
            .isReconciled(false)
            .build();

        // Métadonnées pour traçabilité
        entry.setCreatedBy("SYSTEM_AUTO_DISPOSAL");

        return entry;
    }

    /**
     * Générer un numéro de pièce comptable unique
     * Format: TYPE-YYYY-MM-SEQ (ex: CESSION-2024-12-001)
     */
    private String generatePieceNumber(String type, Company company, LocalDate date) {
        String prefix = String.format("%s-%d-%02d", type, date.getYear(), date.getMonthValue());

        // Trouver le dernier numéro de la période
        Long count = generalLedgerRepository.countByCompanyAndPieceNumberStartingWith(company, prefix);

        return String.format("%s-%03d", prefix, count + 1);
    }

    /**
     * Valider l'équilibre des écritures (débit total = crédit total)
     */
    public void validateEntriesBalance(List<GeneralLedger> entries) {
        BigDecimal totalDebit = entries.stream()
            .map(GeneralLedger::getDebitAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = entries.stream()
            .map(GeneralLedger::getCreditAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new AccountingException(
                String.format("Écritures déséquilibrées - Débit: %s - Crédit: %s - Écart: %s",
                              totalDebit, totalCredit, totalDebit.subtract(totalCredit)));
        }

        log.info("✅ Écritures équilibrées - Débit = Crédit = {} FCFA", totalDebit);
    }
}
