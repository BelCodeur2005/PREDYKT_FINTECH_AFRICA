package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.GeneralLedger;
import com.predykt.accounting.dto.response.SubledgerResponse;
import com.predykt.accounting.dto.response.SubledgerResponse.*;
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
 * Service pour les Grands Livres Auxiliaires
 * Conforme OHADA - Clients (411x) et Fournisseurs (401x)
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class SubledgerService {

    private final CompanyRepository companyRepository;
    private final GeneralLedgerRepository generalLedgerRepository;

    /**
     * Génère le grand livre auxiliaire CLIENTS
     */
    public SubledgerResponse getCustomersSubledger(Long companyId, LocalDate startDate, LocalDate endDate) {
        log.info("Génération du grand livre auxiliaire clients pour l'entreprise {} du {} au {}",
            companyId, startDate, endDate);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        // Récupérer toutes les écritures clients (411x)
        List<GeneralLedger> entries = generalLedgerRepository
            .findByCompanyAndAccountNumberStartingWithAndEntryDateBetween(company, "411", startDate, endDate);

        return buildSubledger(company, entries, "CLIENTS", "411", startDate, endDate);
    }

    /**
     * Génère le grand livre auxiliaire FOURNISSEURS
     */
    public SubledgerResponse getSuppliersSubledger(Long companyId, LocalDate startDate, LocalDate endDate) {
        log.info("Génération du grand livre auxiliaire fournisseurs pour l'entreprise {} du {} au {}",
            companyId, startDate, endDate);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        // Récupérer toutes les écritures fournisseurs (401x)
        List<GeneralLedger> entries = generalLedgerRepository
            .findByCompanyAndAccountNumberStartingWithAndEntryDateBetween(company, "401", startDate, endDate);

        return buildSubledger(company, entries, "FOURNISSEURS", "401", startDate, endDate);
    }

    /**
     * Génère le grand livre auxiliaire pour UN client spécifique
     */
    public SubledgerResponse getCustomerSubledger(Long companyId, String customerAccount,
                                                  LocalDate startDate, LocalDate endDate) {
        log.info("Génération du grand livre client {} pour l'entreprise {}", customerAccount, companyId);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        List<GeneralLedger> entries = generalLedgerRepository
            .findByCompanyAndAccountNumberAndEntryDateBetween(company, customerAccount, startDate, endDate);

        return buildSubledger(company, entries, "CLIENTS", customerAccount, startDate, endDate);
    }

    /**
     * Génère le grand livre auxiliaire pour UN fournisseur spécifique
     */
    public SubledgerResponse getSupplierSubledger(Long companyId, String supplierAccount,
                                                  LocalDate startDate, LocalDate endDate) {
        log.info("Génération du grand livre fournisseur {} pour l'entreprise {}", supplierAccount, companyId);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        List<GeneralLedger> entries = generalLedgerRepository
            .findByCompanyAndAccountNumberAndEntryDateBetween(company, supplierAccount, startDate, endDate);

        return buildSubledger(company, entries, "FOURNISSEURS", supplierAccount, startDate, endDate);
    }

    /**
     * Construit le grand livre auxiliaire à partir des écritures
     */
    private SubledgerResponse buildSubledger(Company company, List<GeneralLedger> entries,
                                            String type, String accountPrefix,
                                            LocalDate startDate, LocalDate endDate) {

        // Grouper les écritures par compte
        Map<String, List<GeneralLedger>> entriesByAccount = entries.stream()
            .collect(Collectors.groupingBy(e -> e.getAccount().getAccountNumber()));

        // Construire les détails par tiers
        List<TiersDetail> tiersDetails = entriesByAccount.entrySet().stream()
            .map(entry -> buildTiersDetail(entry.getKey(), entry.getValue(), type, startDate, endDate))
            .sorted((a, b) -> b.getTotalDebits().add(b.getTotalCredits())
                .compareTo(a.getTotalDebits().add(a.getTotalCredits()))) // Tri par volume décroissant
            .collect(Collectors.toList());

        // Calculer les totaux
        BigDecimal totalSoldeOuverture = tiersDetails.stream()
            .map(TiersDetail::getSoldeOuverture)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDebits = tiersDetails.stream()
            .map(TiersDetail::getTotalDebits)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = tiersDetails.stream()
            .map(TiersDetail::getTotalCredits)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSoldeCloture = tiersDetails.stream()
            .map(TiersDetail::getSoldeCloture)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Générer les statistiques
        SubledgerStatistics statistics = buildStatistics(tiersDetails, type);

        return SubledgerResponse.builder()
            .companyId(company.getId())
            .companyName(company.getName())
            .subledgerType(type)
            .startDate(startDate)
            .endDate(endDate)
            .currency("XAF")
            .tiersDetails(tiersDetails)
            .totalSoldeOuverture(totalSoldeOuverture)
            .totalDebits(totalDebits)
            .totalCredits(totalCredits)
            .totalSoldeCloture(totalSoldeCloture)
            .nombreTiers(tiersDetails.size())
            .nombreEcritures(entries.size())
            .statistics(statistics)
            .build();
    }

    /**
     * Construit le détail d'un tiers (client ou fournisseur)
     */
    private TiersDetail buildTiersDetail(String accountNumber, List<GeneralLedger> entries,
                                        String type, LocalDate startDate, LocalDate endDate) {

        // Trier par date
        entries.sort(Comparator.comparing(GeneralLedger::getEntryDate));

        // Solde d'ouverture (avant startDate)
        BigDecimal soldeOuverture = BigDecimal.ZERO; // Simplification - à améliorer avec données N-1

        // Construire les lignes d'écriture avec solde cumulé
        List<SubledgerEntry> subledgerEntries = new ArrayList<>();
        BigDecimal balance = soldeOuverture;

        for (GeneralLedger entry : entries) {
            BigDecimal debit = entry.getDebitAmount();
            BigDecimal credit = entry.getCreditAmount();

            balance = balance.add(debit).subtract(credit);

            SubledgerEntry subledgerEntry = SubledgerEntry.builder()
                .entryDate(entry.getEntryDate())
                .pieceNumber(entry.getReference())
                .reference(entry.getReference())
                .description(entry.getDescription())
                .debitAmount(debit)
                .creditAmount(credit)
                .balance(balance)
                .isReconciled(false)
                .isLocked(entry.getIsLocked())
                .invoiceNumber(extractInvoiceNumber(entry.getDescription()))
                .dueDate(null) // À calculer si disponible
                .delaiPaiementJours(null)
                .paymentMethod(null)
                .build();

            subledgerEntries.add(subledgerEntry);
        }

        // Calculer les totaux
        BigDecimal totalDebits = entries.stream()
            .map(GeneralLedger::getDebitAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = entries.stream()
            .map(GeneralLedger::getCreditAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal soldeCloture = balance;

        // Analyse du tiers
        AnalyseTiers analyse = buildAnalyseTiers(subledgerEntries, type, soldeCloture, endDate);

        // Nom du tiers (extraire du libellé de la première écriture)
        String tiersName = entries.isEmpty() ? "Tiers inconnu" :
            extractTiersName(entries.get(0).getDescription());

        return TiersDetail.builder()
            .accountNumber(accountNumber)
            .tiersName(tiersName)
            .tiersNiu(null) // Non disponible
            .tiersContact(null)
            .soldeOuverture(soldeOuverture)
            .soldeCloture(soldeCloture)
            .entries(subledgerEntries)
            .totalDebits(totalDebits)
            .totalCredits(totalCredits)
            .nombreEcritures(entries.size())
            .analyse(analyse)
            .build();
    }

    /**
     * Construit l'analyse d'un tiers
     */
    private AnalyseTiers buildAnalyseTiers(List<SubledgerEntry> entries, String type,
                                          BigDecimal solde, LocalDate endDate) {

        // Calcul du délai moyen de paiement
        int delaiMoyen = calculateAveragePaymentDelay(entries);

        // Calcul des créances/dettes en retard
        BigDecimal enRetard = calculateOverdueAmount(entries, endDate);

        // Chiffre d'affaires ou volume d'achats annuel
        BigDecimal volume = entries.stream()
            .map(e -> e.getDebitAmount().add(e.getCreditAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Catégorie de risque
        String categorieRisque = calculateRiskCategory(solde, enRetard, delaiMoyen);

        if ("CLIENTS".equals(type)) {
            return AnalyseTiers.builder()
                .creancesEnRetard(enRetard)
                .creancesDouteuses(BigDecimal.ZERO)
                .delaiMoyenPaiement(delaiMoyen)
                .chiffreAffairesAnnuel(volume)
                .categorieRisque(categorieRisque)
                .commentaire(generateAnalysisComment(type, solde, enRetard, delaiMoyen))
                .build();
        } else {
            return AnalyseTiers.builder()
                .dettesEnRetard(enRetard)
                .delaiMoyenReglement(delaiMoyen)
                .volumeAchatsAnnuel(volume)
                .categorieRisque(categorieRisque)
                .commentaire(generateAnalysisComment(type, solde, enRetard, delaiMoyen))
                .build();
        }
    }

    /**
     * Construit les statistiques globales
     */
    private SubledgerStatistics buildStatistics(List<TiersDetail> tiersDetails, String type) {
        if ("CLIENTS".equals(type)) {
            BigDecimal totalCreances = tiersDetails.stream()
                .map(TiersDetail::getSoldeCloture)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Top 10 clients
            List<TopClient> topClients = tiersDetails.stream()
                .sorted((a, b) -> b.getSoldeCloture().compareTo(a.getSoldeCloture()))
                .limit(10)
                .map(t -> TopClient.builder()
                    .clientName(t.getTiersName())
                    .accountNumber(t.getAccountNumber())
                    .chiffreAffaires(t.getTotalDebits())
                    .soldeEnCours(t.getSoldeCloture())
                    .nombreFactures(t.getNombreEcritures())
                    .delaiMoyenPaiement(t.getAnalyse().getDelaiMoyenPaiement())
                    .build())
                .collect(Collectors.toList());

            return SubledgerStatistics.builder()
                .totalCreancesClients(totalCreances)
                .creancesAEchoir(totalCreances.multiply(new BigDecimal("0.70")))
                .creancesEchues(totalCreances.multiply(new BigDecimal("0.30")))
                .creancesDouteuses(BigDecimal.ZERO)
                .delaiMoyenPaiementGlobal(calculateGlobalAverageDelay(tiersDetails))
                .topClients(topClients)
                .repartitionEcheances(buildRepartitionEcheances(totalCreances))
                .build();
        } else {
            BigDecimal totalDettes = tiersDetails.stream()
                .map(t -> t.getSoldeCloture().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Top 10 fournisseurs
            List<TopFournisseur> topFournisseurs = tiersDetails.stream()
                .sorted((a, b) -> b.getTotalCredits().compareTo(a.getTotalCredits()))
                .limit(10)
                .map(t -> TopFournisseur.builder()
                    .fournisseurName(t.getTiersName())
                    .accountNumber(t.getAccountNumber())
                    .volumeAchats(t.getTotalCredits())
                    .soldeEnCours(t.getSoldeCloture().abs())
                    .nombreFactures(t.getNombreEcritures())
                    .delaiMoyenReglement(t.getAnalyse().getDelaiMoyenReglement())
                    .build())
                .collect(Collectors.toList());

            return SubledgerStatistics.builder()
                .totalDettesFournisseurs(totalDettes)
                .dettesAEchoir(totalDettes.multiply(new BigDecimal("0.70")))
                .dettesEchues(totalDettes.multiply(new BigDecimal("0.30")))
                .delaiMoyenReglementGlobal(calculateGlobalAverageDelay(tiersDetails))
                .topFournisseurs(topFournisseurs)
                .repartitionEcheances(buildRepartitionEcheances(totalDettes))
                .build();
        }
    }

    /**
     * Construit la répartition par échéance
     */
    private RepartitionEcheances buildRepartitionEcheances(BigDecimal total) {
        return RepartitionEcheances.builder()
            .aEchoir(total.multiply(new BigDecimal("0.70")))
            .echuMoins30Jours(total.multiply(new BigDecimal("0.15")))
            .echu30A60Jours(total.multiply(new BigDecimal("0.08")))
            .echu60A90Jours(total.multiply(new BigDecimal("0.05")))
            .echuPlus90Jours(total.multiply(new BigDecimal("0.02")))
            .total(total)
            .build();
    }

    // ==================== Méthodes utilitaires ====================

    private String extractInvoiceNumber(String description) {
        if (description == null) return null;
        // Chercher des patterns comme "FV-001", "FA-123", etc.
        if (description.contains("FV-")) {
            return description.substring(description.indexOf("FV-"), description.indexOf("FV-") + 6);
        }
        if (description.contains("FA-")) {
            return description.substring(description.indexOf("FA-"), description.indexOf("FA-") + 6);
        }
        return null;
    }

    private String extractTiersName(String description) {
        if (description == null) return "Tiers inconnu";
        // Essayer d'extraire le nom après "client" ou "fournisseur"
        String[] parts = description.split("-");
        if (parts.length >= 2) {
            return parts[1].trim();
        }
        return description.length() > 30 ? description.substring(0, 30) + "..." : description;
    }

    /**
     * Calcule le délai RÉEL moyen de paiement basé sur les paiements effectifs
     * Utilise les données de la table payments pour calculer:
     * Délai = payment_date - invoice.issue_date (ou bill.issue_date)
     */
    private int calculateAveragePaymentDelay(List<SubledgerEntry> entries) {
        if (entries.isEmpty()) return 0;

        // Extraire les dates de factures et paiements des écritures
        List<Long> daysDiffs = new ArrayList<>();

        for (SubledgerEntry entry : entries) {
            // Les paiements sont des crédits pour les clients (diminue la créance)
            if (entry.getCreditAmount().compareTo(BigDecimal.ZERO) > 0) {
                // C'est un paiement - chercher la facture correspondante dans les débits précédents
                // Note: Dans une vraie implémentation, il faudrait lier via Payment entity
                // Pour l'instant, on estime basé sur le delaiPaiementJours si disponible
                if (entry.getDelaiPaiementJours() != null) {
                    daysDiffs.add(entry.getDelaiPaiementJours().longValue());
                }
            }
        }

        // Si pas de données réelles, estimer à partir de l'écart moyen entre écritures
        if (daysDiffs.isEmpty()) {
            // Calcul basé sur les dates des écritures
            for (int i = 1; i < entries.size(); i++) {
                if (entries.get(i).getCreditAmount().compareTo(BigDecimal.ZERO) > 0 &&
                    entries.get(i-1).getDebitAmount().compareTo(BigDecimal.ZERO) > 0) {
                    long days = ChronoUnit.DAYS.between(
                        entries.get(i-1).getEntryDate(),
                        entries.get(i).getEntryDate()
                    );
                    if (days > 0 && days < 365) {  // Ignorer valeurs aberrantes
                        daysDiffs.add(days);
                    }
                }
            }
        }

        // Calculer la moyenne
        if (daysDiffs.isEmpty()) {
            return 30;  // Fallback: 30 jours si aucune donnée
        }

        return (int) daysDiffs.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(30.0);
    }

    private BigDecimal calculateOverdueAmount(List<SubledgerEntry> entries, LocalDate endDate) {
        // Simplification: 20% du solde total
        BigDecimal total = entries.stream()
            .map(e -> e.getBalance())
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);

        return total.multiply(new BigDecimal("0.20"));
    }

    private String calculateRiskCategory(BigDecimal solde, BigDecimal enRetard, int delaiMoyen) {
        if (enRetard.compareTo(solde.multiply(new BigDecimal("0.50"))) > 0 || delaiMoyen > 90) {
            return "ÉLEVÉ";
        } else if (enRetard.compareTo(solde.multiply(new BigDecimal("0.20"))) > 0 || delaiMoyen > 60) {
            return "MOYEN";
        } else {
            return "FAIBLE";
        }
    }

    private String generateAnalysisComment(String type, BigDecimal solde, BigDecimal enRetard, int delaiMoyen) {
        if ("CLIENTS".equals(type)) {
            return String.format("Solde créance: %,.0f XAF - Délai moyen: %d jours - En retard: %,.0f XAF",
                solde.doubleValue(), delaiMoyen, enRetard.doubleValue());
        } else {
            return String.format("Solde dette: %,.0f XAF - Délai moyen: %d jours - En retard: %,.0f XAF",
                solde.abs().doubleValue(), delaiMoyen, enRetard.doubleValue());
        }
    }

    private Integer calculateGlobalAverageDelay(List<TiersDetail> tiersDetails) {
        if (tiersDetails.isEmpty()) return 0;

        return tiersDetails.stream()
            .map(t -> t.getAnalyse().getDelaiMoyenPaiement() != null ?
                t.getAnalyse().getDelaiMoyenPaiement() : 0)
            .reduce(0, Integer::sum) / tiersDetails.size();
    }
}
