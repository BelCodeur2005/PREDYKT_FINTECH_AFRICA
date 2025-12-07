package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.GeneralLedger;
import com.predykt.accounting.dto.response.AgingReportResponse;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service pour la génération des balances âgées (clients et fournisseurs)
 * Analyse par ancienneté: 0-30j, 30-60j, 60-90j, >90j
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgingReportService {

    private final CompanyRepository companyRepository;
    private final GeneralLedgerRepository generalLedgerRepository;

    /**
     * Générer la balance âgée des clients
     */
    @Transactional(readOnly = true)
    public AgingReportResponse generateCustomersAgingReport(Long companyId, LocalDate asOfDate) {
        log.info("Génération de la balance âgée clients pour l'entreprise {} au {}", companyId, asOfDate);

        return generateAgingReport(companyId, asOfDate, "411", "CUSTOMERS");
    }

    /**
     * Générer la balance âgée des fournisseurs
     */
    @Transactional(readOnly = true)
    public AgingReportResponse generateSuppliersAgingReport(Long companyId, LocalDate asOfDate) {
        log.info("Génération de la balance âgée fournisseurs pour l'entreprise {} au {}", companyId, asOfDate);

        return generateAgingReport(companyId, asOfDate, "401", "SUPPLIERS");
    }

    /**
     * Méthode générique pour générer un rapport de balance âgée
     */
    private AgingReportResponse generateAgingReport(Long companyId, LocalDate asOfDate,
                                                    String accountPrefix, String reportType) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        // Récupérer toutes les écritures des comptes clients ou fournisseurs jusqu'à la date donnée
        List<GeneralLedger> entries = generalLedgerRepository.findByCompanyAndAccountNumberStartingWithAndEntryDateLessThanEqual(
            company, accountPrefix, asOfDate);

        // Regrouper les écritures par compte (client ou fournisseur)
        Map<String, List<GeneralLedger>> groupedByAccount = entries.stream()
            .collect(Collectors.groupingBy(e -> e.getAccount().getAccountNumber()));

        // Calculer les montants par tranche d'âge pour chaque compte
        List<AgingReportResponse.AgingItem> agingItems = new ArrayList<>();

        for (Map.Entry<String, List<GeneralLedger>> entry : groupedByAccount.entrySet()) {
            String accountNumber = entry.getKey();
            List<GeneralLedger> accountEntries = entry.getValue();

            AgingReportResponse.AgingItem item = calculateAgingForAccount(
                accountNumber, accountEntries, asOfDate, reportType);

            if (item.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                agingItems.add(item);
            }
        }

        // Trier par montant total décroissant
        agingItems.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));

        // Calculer les totaux
        AgingReportResponse.AgingSummary summary = calculateSummary(agingItems);

        // Générer l'analyse et les recommandations
        AgingReportResponse.Analysis analysis = generateAnalysis(agingItems, summary, reportType);

        return AgingReportResponse.builder()
            .companyId(companyId)
            .companyName(company.getName())
            .asOfDate(asOfDate)
            .reportType(reportType)
            .items(agingItems)
            .summary(summary)
            .analysis(analysis)
            .build();
    }

    /**
     * Calculer le vieillissement pour un compte spécifique
     */
    private AgingReportResponse.AgingItem calculateAgingForAccount(
            String accountNumber, List<GeneralLedger> entries, LocalDate asOfDate, String reportType) {

        // Déterminer si c'est du débit ou crédit selon le type
        boolean isReceivable = reportType.equals("CUSTOMERS"); // Clients = créances (débit)

        // Calculer le solde total
        BigDecimal totalDebit = entries.stream()
            .map(GeneralLedger::getDebitAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = entries.stream()
            .map(GeneralLedger::getCreditAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmount = isReceivable
            ? totalDebit.subtract(totalCredit)
            : totalCredit.subtract(totalDebit);

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            // Pas de solde ou solde négatif, ne pas inclure
            return AgingReportResponse.AgingItem.builder()
                .accountNumber(accountNumber)
                .totalAmount(BigDecimal.ZERO)
                .build();
        }

        // Initialiser les tranches
        BigDecimal current = BigDecimal.ZERO;
        BigDecimal days30to60 = BigDecimal.ZERO;
        BigDecimal days60to90 = BigDecimal.ZERO;
        BigDecimal over90Days = BigDecimal.ZERO;

        LocalDate oldestDate = asOfDate;
        int overdueCount = 0;

        // Analyser chaque écriture pour déterminer son âge
        for (GeneralLedger entry : entries) {
            BigDecimal amount = isReceivable
                ? entry.getDebitAmount().subtract(entry.getCreditAmount())
                : entry.getCreditAmount().subtract(entry.getDebitAmount());

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue; // Ignorer les paiements (montant négatif)
            }

            long daysSinceEntry = ChronoUnit.DAYS.between(entry.getEntryDate(), asOfDate);

            if (daysSinceEntry <= 30) {
                current = current.add(amount);
            } else if (daysSinceEntry <= 60) {
                days30to60 = days30to60.add(amount);
                overdueCount++;
            } else if (daysSinceEntry <= 90) {
                days60to90 = days60to90.add(amount);
                overdueCount++;
            } else {
                over90Days = over90Days.add(amount);
                overdueCount++;
            }

            if (entry.getEntryDate().isBefore(oldestDate)) {
                oldestDate = entry.getEntryDate();
            }
        }

        // Déterminer le statut
        String status;
        String statusIcon;
        if (over90Days.compareTo(BigDecimal.ZERO) > 0 || days60to90.compareTo(totalAmount.multiply(new BigDecimal("0.3"))) > 0) {
            status = "CRITICAL";
            statusIcon = "🔴";
        } else if (days30to60.add(days60to90).compareTo(totalAmount.multiply(new BigDecimal("0.5"))) > 0) {
            status = "WARNING";
            statusIcon = "⚠️";
        } else {
            status = "OK";
            statusIcon = "✅";
        }

        // Extraire le nom depuis le tiers, la description ou le compte
        // Ordre de priorité: 1. Customer/Supplier name, 2. Description, 3. Account number
        String name = accountNumber;  // Fallback par défaut
        if (!entries.isEmpty()) {
            GeneralLedger firstEntry = entries.get(0);
            // Utiliser getTiersName() qui gère automatiquement la priorité Customer > Supplier > Description > Account
            name = firstEntry.getTiersName();
        }

        long oldestDays = ChronoUnit.DAYS.between(oldestDate, asOfDate);

        return AgingReportResponse.AgingItem.builder()
            .accountNumber(accountNumber)
            .name(name)
            .current(current)
            .days30to60(days30to60)
            .days60to90(days60to90)
            .over90Days(over90Days)
            .totalAmount(totalAmount)
            .oldestInvoiceDate(oldestDate)
            .oldestInvoiceDays((int) oldestDays)
            .status(status)
            .statusIcon(statusIcon)
            .overdueInvoicesCount(overdueCount)
            .build();
    }

    /**
     * Calculer le résumé des totaux
     */
    private AgingReportResponse.AgingSummary calculateSummary(List<AgingReportResponse.AgingItem> items) {
        BigDecimal totalCurrent = BigDecimal.ZERO;
        BigDecimal totalDays30to60 = BigDecimal.ZERO;
        BigDecimal totalDays60to90 = BigDecimal.ZERO;
        BigDecimal totalOver90Days = BigDecimal.ZERO;

        int itemsOk = 0;
        int itemsWarning = 0;
        int itemsCritical = 0;

        for (AgingReportResponse.AgingItem item : items) {
            totalCurrent = totalCurrent.add(item.getCurrent());
            totalDays30to60 = totalDays30to60.add(item.getDays30to60());
            totalDays60to90 = totalDays60to90.add(item.getDays60to90());
            totalOver90Days = totalOver90Days.add(item.getOver90Days());

            switch (item.getStatus()) {
                case "OK" -> itemsOk++;
                case "WARNING" -> itemsWarning++;
                case "CRITICAL" -> itemsCritical++;
            }
        }

        BigDecimal grandTotal = totalCurrent.add(totalDays30to60).add(totalDays60to90).add(totalOver90Days);

        // Calculer les pourcentages
        BigDecimal percentCurrent = grandTotal.compareTo(BigDecimal.ZERO) > 0
            ? totalCurrent.divide(grandTotal, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;

        BigDecimal percentDays30to60 = grandTotal.compareTo(BigDecimal.ZERO) > 0
            ? totalDays30to60.divide(grandTotal, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;

        BigDecimal percentDays60to90 = grandTotal.compareTo(BigDecimal.ZERO) > 0
            ? totalDays60to90.divide(grandTotal, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;

        BigDecimal percentOver90Days = grandTotal.compareTo(BigDecimal.ZERO) > 0
            ? totalOver90Days.divide(grandTotal, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;

        return AgingReportResponse.AgingSummary.builder()
            .totalCurrent(totalCurrent)
            .totalDays30to60(totalDays30to60)
            .totalDays60to90(totalDays60to90)
            .totalOver90Days(totalOver90Days)
            .grandTotal(grandTotal)
            .percentCurrent(percentCurrent)
            .percentDays30to60(percentDays30to60)
            .percentDays60to90(percentDays60to90)
            .percentOver90Days(percentOver90Days)
            .totalItems(items.size())
            .itemsOk(itemsOk)
            .itemsWarning(itemsWarning)
            .itemsCritical(itemsCritical)
            .build();
    }

    /**
     * Générer l'analyse et les recommandations
     */
    private AgingReportResponse.Analysis generateAnalysis(
            List<AgingReportResponse.AgingItem> items,
            AgingReportResponse.AgingSummary summary,
            String reportType) {

        List<String> alerts = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        List<AgingReportResponse.AgingItem> criticalItems = new ArrayList<>();

        // Identifier les items critiques
        for (AgingReportResponse.AgingItem item : items) {
            if ("CRITICAL".equals(item.getStatus())) {
                criticalItems.add(item);
            }
        }

        // Générer les alertes
        if (summary.getTotalOver90Days().compareTo(BigDecimal.ZERO) > 0) {
            alerts.add(String.format("⚠️ %s FCFA en retard de plus de 90 jours",
                summary.getTotalOver90Days().setScale(0, RoundingMode.HALF_UP)));
        }

        if (summary.getItemsCritical() > 0) {
            alerts.add(String.format("🔴 %d %s en situation critique",
                summary.getItemsCritical(),
                reportType.equals("CUSTOMERS") ? "client(s)" : "fournisseur(s)"));
        }

        BigDecimal overdueTotal = summary.getTotalDays30to60()
            .add(summary.getTotalDays60to90())
            .add(summary.getTotalOver90Days());

        BigDecimal overdueRate = summary.getGrandTotal().compareTo(BigDecimal.ZERO) > 0
            ? overdueTotal.divide(summary.getGrandTotal(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;

        if (overdueRate.compareTo(new BigDecimal("30")) > 0) {
            alerts.add(String.format("⚠️ Taux de retard élevé: %.2f%%", overdueRate));
        }

        // Générer les recommandations
        if (reportType.equals("CUSTOMERS")) {
            if (summary.getTotalOver90Days().compareTo(BigDecimal.ZERO) > 0) {
                recommendations.add("Envisager une provision pour créances douteuses");
                recommendations.add("Relancer les clients en retard > 90 jours");
            }
            if (summary.getItemsWarning() > 0) {
                recommendations.add("Envoyer des rappels aux clients en retard 30-90 jours");
            }
            if (overdueRate.compareTo(new BigDecimal("50")) > 0) {
                recommendations.add("Revoir la politique de crédit client");
            }
        } else {
            if (summary.getTotalOver90Days().compareTo(BigDecimal.ZERO) > 0) {
                recommendations.add("Risque de pénalités de retard ou blocage livraisons");
                recommendations.add("Prioriser le paiement des fournisseurs > 90 jours");
            }
            if (overdueRate.compareTo(new BigDecimal("40")) > 0) {
                recommendations.add("Améliorer la gestion de trésorerie");
            }
        }

        // Provision suggérée (pour clients uniquement)
        BigDecimal suggestedProvision = BigDecimal.ZERO;
        if (reportType.equals("CUSTOMERS")) {
            // Provision: 50% des créances > 90 jours
            suggestedProvision = summary.getTotalOver90Days()
                .multiply(new BigDecimal("0.50"))
                .setScale(0, RoundingMode.HALF_UP);
        }

        // Calculer le délai moyen (approximation basée sur les tranches)
        int averageDays = 0;
        if (summary.getGrandTotal().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal weightedDays = summary.getTotalCurrent().multiply(new BigDecimal("15"))
                .add(summary.getTotalDays30to60().multiply(new BigDecimal("45")))
                .add(summary.getTotalDays60to90().multiply(new BigDecimal("75")))
                .add(summary.getTotalOver90Days().multiply(new BigDecimal("120")));

            averageDays = weightedDays.divide(summary.getGrandTotal(), 0, RoundingMode.HALF_UP).intValue();
        }

        return AgingReportResponse.Analysis.builder()
            .alerts(alerts)
            .recommendations(recommendations)
            .suggestedProvision(suggestedProvision)
            .criticalItems(criticalItems)
            .averagePaymentDays(averageDays)
            .overdueRate(overdueRate)
            .build();
    }
}
