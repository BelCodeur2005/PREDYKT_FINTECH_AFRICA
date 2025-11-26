package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.ChartOfAccounts;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.ImportedActivity;
import com.predykt.accounting.repository.ChartOfAccountsRepository;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.repository.ImportedActivityRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service pour la gestion des activités importées depuis CSV
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ImportedActivityService {

    private final ImportedActivityRepository activityRepository;
    private final CompanyRepository companyRepository;
    private final ChartOfAccountsRepository chartOfAccountsRepository;

    /**
     * Importe une activité depuis un fichier CSV
     */
    public ImportedActivity importActivity(ImportedActivity activity) {
        log.info("Import d'une activité pour l'entreprise {}: {}",
            activity.getCompany().getId(), activity.getDescription());

        // Valider l'entreprise
        Company company = companyRepository.findById(activity.getCompany().getId())
            .orElseThrow(() -> new EntityNotFoundException(
                "Entreprise non trouvée avec l'ID: " + activity.getCompany().getId()));

        activity.setCompany(company);

        return activityRepository.save(activity);
    }

    /**
     * Importe plusieurs activités en lot
     */
    public List<ImportedActivity> importActivities(List<ImportedActivity> activities) {
        log.info("Import en lot de {} activités", activities.size());

        return activities.stream()
            .map(this::importActivity)
            .toList();
    }

    /**
     * Mappe automatiquement une activité à un compte OHADA
     */
    public ImportedActivity mapActivityToAccount(Long activityId) {
        log.info("Mapping de l'activité {} à un compte OHADA", activityId);

        ImportedActivity activity = getActivityById(activityId);

        // Rechercher le meilleur compte correspondant
        ChartOfAccounts bestMatch = findBestAccountMatch(activity);

        if (bestMatch != null) {
            BigDecimal confidence = calculateConfidence(activity, bestMatch);
            activity.markAsMapped(bestMatch, confidence);
            return activityRepository.save(activity);
        } else {
            log.warn("Aucun compte trouvé pour l'activité {}", activityId);
            return activity;
        }
    }

    /**
     * Trouve le meilleur compte OHADA correspondant à une activité
     */
    private ChartOfAccounts findBestAccountMatch(ImportedActivity activity) {
        Long companyId = activity.getCompany().getId();
        String description = activity.getDescription().toLowerCase();

        // Rechercher par mots-clés dans la description
        List<ChartOfAccounts> accounts = chartOfAccountsRepository.findByCompanyId(companyId);

        return accounts.stream()
            .filter(account -> {
                String accountName = account.getName().toLowerCase();
                String[] keywords = description.split("\\s+");

                // Vérifier si au moins 2 mots-clés correspondent
                long matchCount = 0;
                for (String keyword : keywords) {
                    if (accountName.contains(keyword)) {
                        matchCount++;
                    }
                }
                return matchCount >= 2;
            })
            .findFirst()
            .orElse(null);
    }

    /**
     * Calcule le score de confiance du mapping
     */
    private BigDecimal calculateConfidence(ImportedActivity activity, ChartOfAccounts account) {
        String description = activity.getDescription().toLowerCase();
        String accountName = account.getName().toLowerCase();

        String[] keywords = description.split("\\s+");
        long matchCount = 0;

        for (String keyword : keywords) {
            if (accountName.contains(keyword)) {
                matchCount++;
            }
        }

        // Calculer le pourcentage de correspondance
        double confidence = (double) matchCount / keywords.length * 100;
        return BigDecimal.valueOf(Math.min(confidence, 100.0));
    }

    /**
     * Valide et traite une activité mappée
     */
    public ImportedActivity processActivity(Long activityId) {
        log.info("Traitement de l'activité {}", activityId);

        ImportedActivity activity = getActivityById(activityId);

        if (activity.getMappedAccount() == null) {
            throw new IllegalStateException("L'activité doit être mappée avant d'être traitée");
        }

        // TODO: Créer une écriture comptable dans GeneralLedger
        // Cette logique nécessiterait l'injection de GeneralLedgerService

        activity.markAsProcessed(null); // Passer l'écriture créée
        return activityRepository.save(activity);
    }

    /**
     * Rejette une activité
     */
    public ImportedActivity rejectActivity(Long activityId, String reason) {
        log.info("Rejet de l'activité {} - Raison: {}", activityId, reason);

        ImportedActivity activity = getActivityById(activityId);
        activity.markAsRejected(reason);

        return activityRepository.save(activity);
    }

    /**
     * Récupère une activité par ID
     */
    @Transactional(readOnly = true)
    public ImportedActivity getActivityById(Long activityId) {
        return activityRepository.findById(activityId)
            .orElseThrow(() -> new EntityNotFoundException("Activité non trouvée avec l'ID: " + activityId));
    }

    /**
     * Récupère toutes les activités d'une entreprise
     */
    @Transactional(readOnly = true)
    public List<ImportedActivity> getCompanyActivities(Long companyId) {
        return activityRepository.findByCompanyId(companyId);
    }

    /**
     * Récupère les activités par statut
     */
    @Transactional(readOnly = true)
    public List<ImportedActivity> getActivitiesByStatus(Long companyId, String status) {
        return activityRepository.findByCompanyIdAndStatus(companyId, status);
    }

    /**
     * Récupère les activités en attente de traitement
     */
    @Transactional(readOnly = true)
    public List<ImportedActivity> getPendingActivities(Long companyId) {
        return activityRepository.findPendingByCompanyId(companyId);
    }

    /**
     * Récupère les activités avec mapping de faible confiance
     */
    @Transactional(readOnly = true)
    public List<ImportedActivity> getLowConfidenceActivities(Long companyId, BigDecimal threshold) {
        return activityRepository.findLowConfidenceMappingsByCompanyId(companyId, threshold);
    }

    /**
     * Récupère les activités pour une période
     */
    @Transactional(readOnly = true)
    public List<ImportedActivity> getActivitiesByPeriod(Long companyId, LocalDate startDate, LocalDate endDate) {
        return activityRepository.findByCompanyIdAndPeriod(companyId, startDate, endDate);
    }

    /**
     * Compte les activités par statut
     */
    @Transactional(readOnly = true)
    public Long countActivitiesByStatus(Long companyId, String status) {
        return activityRepository.countByCompanyIdAndStatus(companyId, status);
    }

    /**
     * Calcule le montant total des activités traitées
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalProcessedAmount(Long companyId) {
        return activityRepository.calculateTotalProcessedAmount(companyId);
    }

    /**
     * Récupère les activités par catégorie
     */
    @Transactional(readOnly = true)
    public List<ImportedActivity> getActivitiesByCategory(Long companyId, String category) {
        return activityRepository.findByCompanyIdAndCategory(companyId, category);
    }

    /**
     * Récupère les activités importées par utilisateur
     */
    @Transactional(readOnly = true)
    public List<ImportedActivity> getActivitiesByImporter(Long companyId, String importedBy) {
        return activityRepository.findByCompanyIdAndImportedBy(companyId, importedBy);
    }

    /**
     * Génère des statistiques d'import
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getImportStatistics(Long companyId) {
        List<Object[]> rawStats = activityRepository.getImportStatisticsByCompanyId(companyId);

        Map<String, Object> statistics = new HashMap<>();

        for (Object[] row : rawStats) {
            String status = (String) row[0];
            Long count = (Long) row[1];
            BigDecimal totalAmount = (BigDecimal) row[2];

            Map<String, Object> statusStats = new HashMap<>();
            statusStats.put("count", count);
            statusStats.put("totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO);

            statistics.put(status, statusStats);
        }

        return statistics;
    }

    /**
     * Traite automatiquement toutes les activités en attente
     */
    public int processAllPendingActivities(Long companyId) {
        log.info("Traitement automatique de toutes les activités en attente pour l'entreprise {}", companyId);

        List<ImportedActivity> pending = getPendingActivities(companyId);
        int processedCount = 0;

        for (ImportedActivity activity : pending) {
            try {
                mapActivityToAccount(activity.getId());

                // Traiter si confiance >= 80%
                if (activity.isHighConfidence()) {
                    processActivity(activity.getId());
                    processedCount++;
                }
            } catch (Exception e) {
                log.error("Erreur lors du traitement de l'activité {}: {}",
                    activity.getId(), e.getMessage());
            }
        }

        log.info("{} activités traitées automatiquement", processedCount);
        return processedCount;
    }

    /**
     * Nettoyage automatique des activités rejetées (tous les premiers du mois)
     */
    @Scheduled(cron = "0 0 2 1 * *")
    public void cleanupRejectedActivities() {
        log.info("Début du nettoyage des activités rejetées");

        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(3);
        activityRepository.deleteOldRejectedActivities(cutoffDate);

        log.info("Nettoyage des activités rejetées terminé");
    }
}
