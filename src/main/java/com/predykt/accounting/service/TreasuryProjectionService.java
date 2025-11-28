package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.CashFlowProjection;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.GeneralLedger;
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
import java.util.*;

/**
 * Service pour les projections de tresorerie
 * Calcule les previsions de flux de tresorerie a J+30, J+60, J+90
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TreasuryProjectionService {

    private final CashFlowProjectionRepository projectionRepository;
    private final CompanyRepository companyRepository;
    private final GeneralLedgerRepository generalLedgerRepository;

    /**
     * Cree une projection de tresorerie manuelle
     */
    public CashFlowProjection createProjection(CashFlowProjection projection) {
        log.info("Creation d'une projection de tresorerie pour l'entreprise {} - Horizon: {} jours",
            projection.getCompany().getId(), projection.getProjectionHorizon());

        // Verifier que l'entreprise existe
        Company company = companyRepository.findById(projection.getCompany().getId())
            .orElseThrow(() -> new EntityNotFoundException(
                "Entreprise non trouvee avec l'ID: " + projection.getCompany().getId()));

        projection.setCompany(company);
        projection.setCreatedAt(LocalDate.now());

        return projectionRepository.save(projection);
    }

    /**
     * Genere automatiquement des projections basees sur l'historique
     * Utilise les moyennes glissantes des 90 derniers jours
     */
    public List<CashFlowProjection> generateAutomaticProjections(Long companyId) {
        log.info("Generation automatique des projections de tresorerie pour l'entreprise {}", companyId);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvee avec l'ID: " + companyId));

        LocalDate today = LocalDate.now();
        LocalDate historicalStart = today.minusDays(90);

        // Calculer le solde actuel (compte 5 - Tresorerie)
        BigDecimal currentBalance = getCurrentCashBalance(company);

        // Calculer les moyennes historiques
        Map<String, BigDecimal> historicalAverages = calculateHistoricalAverages(company, historicalStart, today);

        List<CashFlowProjection> projections = new ArrayList<>();

        // Generer projections pour 30, 60 et 90 jours
        int[] horizons = {
            CashFlowProjection.HORIZON_30_DAYS,
            CashFlowProjection.HORIZON_60_DAYS,
            CashFlowProjection.HORIZON_90_DAYS
        };

        for (int horizon : horizons) {
            CashFlowProjection projection = buildProjection(
                company,
                today,
                horizon,
                currentBalance,
                historicalAverages
            );

            projections.add(projectionRepository.save(projection));
        }

        log.info("Genere {} projections automatiques", projections.size());
        return projections;
    }

    /**
     * Construit une projection pour un horizon donne
     */
    private CashFlowProjection buildProjection(
        Company company,
        LocalDate projectionDate,
        int horizon,
        BigDecimal openingBalance,
        Map<String, BigDecimal> historicalAverages
    ) {
        // Extraire les moyennes historiques
        BigDecimal avgDailyInflows = historicalAverages.getOrDefault("dailyInflows", BigDecimal.ZERO);
        BigDecimal avgDailyOutflows = historicalAverages.getOrDefault("dailyOutflows", BigDecimal.ZERO);
        BigDecimal avgReceivables = historicalAverages.getOrDefault("receivablesCollection", BigDecimal.ZERO);
        BigDecimal avgPayables = historicalAverages.getOrDefault("payablesPayment", BigDecimal.ZERO);
        BigDecimal avgPayroll = historicalAverages.getOrDefault("payrollPayment", BigDecimal.ZERO);

        // Projeter sur l'horizon
        BigDecimal projectedInflows = avgDailyInflows.multiply(BigDecimal.valueOf(horizon));
        BigDecimal projectedOutflows = avgDailyOutflows.multiply(BigDecimal.valueOf(horizon));

        // Calculer le solde projete
        BigDecimal projectedBalance = openingBalance
            .add(projectedInflows)
            .subtract(projectedOutflows);

        // Calculer le score de confiance (baisse avec l'horizon)
        BigDecimal confidenceScore = calculateConfidenceScore(horizon, historicalAverages);

        return CashFlowProjection.builder()
            .company(company)
            .projectionDate(projectionDate)
            .projectionHorizon(horizon)
            .openingBalance(openingBalance)
            .projectedBalance(projectedBalance)
            .projectedInflows(projectedInflows)
            .receivablesCollection(avgReceivables.multiply(BigDecimal.valueOf(horizon / 30)))
            .projectedOutflows(projectedOutflows)
            .payablesPayment(avgPayables.multiply(BigDecimal.valueOf(horizon / 30)))
            .payrollPayment(avgPayroll.multiply(BigDecimal.valueOf(horizon / 30)))
            .modelUsed("MOVING_AVERAGE")
            .confidenceScore(confidenceScore)
            .createdAt(LocalDate.now())
            .createdBy("system")
            .build();
    }

    /**
     * Calcule le solde de tresorerie actuel (compte classe 5)
     */
    private BigDecimal getCurrentCashBalance(Company company) {
        LocalDate today = LocalDate.now();
        LocalDate veryOldDate = LocalDate.of(2000, 1, 1);

        // Recuperer toutes les ecritures pour calculer le solde de tresorerie
        List<GeneralLedger> cashEntries = generalLedgerRepository
            .findByCompanyAndEntryDateBetween(company, veryOldDate, today);

        BigDecimal balance = BigDecimal.ZERO;
        for (GeneralLedger entry : cashEntries) {
            String accountNumber = entry.getAccount().getAccountNumber();
            // Comptes de tresorerie (classe 5)
            if (accountNumber.startsWith("5")) {
                balance = balance.add(entry.getDebitAmount()).subtract(entry.getCreditAmount());
            }
        }

        return balance;
    }

    /**
     * Calcule les moyennes historiques des flux
     */
    private Map<String, BigDecimal> calculateHistoricalAverages(
        Company company,
        LocalDate startDate,
        LocalDate endDate
    ) {
        Map<String, BigDecimal> averages = new HashMap<>();

        int daysInPeriod = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (daysInPeriod == 0) daysInPeriod = 1;

        // Recuperer les ecritures de la periode
        List<GeneralLedger> entries = generalLedgerRepository
            .findByCompanyAndEntryDateBetween(company, startDate, endDate);

        BigDecimal totalInflows = BigDecimal.ZERO;
        BigDecimal totalOutflows = BigDecimal.ZERO;
        BigDecimal totalReceivables = BigDecimal.ZERO;
        BigDecimal totalPayables = BigDecimal.ZERO;
        BigDecimal totalPayroll = BigDecimal.ZERO;

        for (GeneralLedger entry : entries) {
            String accountNumber = entry.getAccount().getAccountNumber();
            String accountClass = accountNumber.substring(0, 1);

            // Flux entrants (ventes - classe 7)
            if ("7".equals(accountClass)) {
                totalInflows = totalInflows.add(entry.getCreditAmount());
            }

            // Flux sortants (achats/charges - classe 6)
            if ("6".equals(accountClass)) {
                totalOutflows = totalOutflows.add(entry.getDebitAmount());

                // Salaires (661)
                if (accountNumber.startsWith("661")) {
                    totalPayroll = totalPayroll.add(entry.getDebitAmount());
                }
            }

            // Creances clients (411)
            if (accountNumber.startsWith("411")) {
                totalReceivables = totalReceivables.add(entry.getDebitAmount());
            }

            // Fournisseurs (401)
            if (accountNumber.startsWith("401")) {
                totalPayables = totalPayables.add(entry.getCreditAmount());
            }
        }

        // Calculer les moyennes journalieres
        averages.put("dailyInflows", totalInflows.divide(BigDecimal.valueOf(daysInPeriod), 2, RoundingMode.HALF_UP));
        averages.put("dailyOutflows", totalOutflows.divide(BigDecimal.valueOf(daysInPeriod), 2, RoundingMode.HALF_UP));
        averages.put("receivablesCollection", totalReceivables.divide(BigDecimal.valueOf(daysInPeriod / 30), 2, RoundingMode.HALF_UP));
        averages.put("payablesPayment", totalPayables.divide(BigDecimal.valueOf(daysInPeriod / 30), 2, RoundingMode.HALF_UP));
        averages.put("payrollPayment", totalPayroll.divide(BigDecimal.valueOf(daysInPeriod / 30), 2, RoundingMode.HALF_UP));

        return averages;
    }

    /**
     * Calcule le score de confiance (diminue avec l'horizon)
     */
    private BigDecimal calculateConfidenceScore(int horizon, Map<String, BigDecimal> historicalAverages) {
        // Base de confiance selon l'horizon
        BigDecimal baseConfidence;
        if (horizon <= 30) {
            baseConfidence = BigDecimal.valueOf(85);
        } else if (horizon <= 60) {
            baseConfidence = BigDecimal.valueOf(70);
        } else {
            baseConfidence = BigDecimal.valueOf(55);
        }

        // Ajuster selon la qualite des donnees historiques
        boolean hasGoodData = historicalAverages.values().stream()
            .anyMatch(value -> value.compareTo(BigDecimal.ZERO) > 0);

        if (!hasGoodData) {
            baseConfidence = baseConfidence.multiply(BigDecimal.valueOf(0.5));
        }

        return baseConfidence.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Recupere les dernieres projections pour une entreprise
     */
    @Transactional(readOnly = true)
    public Map<String, CashFlowProjection> getLatestProjections(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvee avec l'ID: " + companyId));

        Map<String, CashFlowProjection> latest = new HashMap<>();

        // Recuperer la derniere projection pour chaque horizon
        int[] horizons = {30, 60, 90};
        for (int horizon : horizons) {
            Optional<CashFlowProjection> projection = projectionRepository
                .findFirstByCompanyAndProjectionHorizonOrderByProjectionDateDesc(company, horizon);
            projection.ifPresent(p -> latest.put("J+" + horizon, p));
        }

        return latest;
    }

    /**
     * Recupere les alertes de tresorerie negative
     */
    @Transactional(readOnly = true)
    public List<CashFlowProjection> getNegativeCashFlowAlerts(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvee avec l'ID: " + companyId));

        return projectionRepository.findNegativeCashFlowProjections(company);
    }

    /**
     * Recupere toutes les projections d'une entreprise
     */
    @Transactional(readOnly = true)
    public List<CashFlowProjection> getCompanyProjections(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvee avec l'ID: " + companyId));

        return projectionRepository.findByCompanyOrderByProjectionDateDesc(company);
    }

    /**
     * Recupere les projections par horizon
     */
    @Transactional(readOnly = true)
    public List<CashFlowProjection> getProjectionsByHorizon(Long companyId, Integer horizon) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvee avec l'ID: " + companyId));

        return projectionRepository.findByCompanyAndProjectionHorizon(company, horizon);
    }

    /**
     * Supprime une projection
     */
    public void deleteProjection(Long projectionId) {
        log.info("Suppression de la projection {}", projectionId);

        CashFlowProjection projection = projectionRepository.findById(projectionId)
            .orElseThrow(() -> new EntityNotFoundException("Projection non trouvee avec l'ID: " + projectionId));

        projectionRepository.delete(projection);
    }

    /**
     * Nettoie les anciennes projections (> 6 mois)
     */
    public void cleanupOldProjections(Long companyId) {
        log.info("Nettoyage des anciennes projections pour l'entreprise {}", companyId);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvee avec l'ID: " + companyId));

        LocalDate cutoffDate = LocalDate.now().minusMonths(6);
        projectionRepository.deleteOldProjectionsByCompany(company, cutoffDate);

        log.info("Anciennes projections supprimees");
    }
}
