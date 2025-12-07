package com.predykt.accounting.service.ml;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Planificateur d'entra\u00eenement ML automatique
 * Ex\u00e9cute l'entra\u00eenement nightly pour toutes les entreprises
 *
 * Cron: Tous les jours \u00e0 3h00 du matin
 * Condition: predykt.ml.auto-training.enabled=true
 *
 * @author PREDYKT ML Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "predykt.ml.auto-training.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class MLTrainingScheduler {

    private final CompanyRepository companyRepository;
    private final MLTrainingService trainingService;
    private final MLMatchingService matchingService;

    /**
     * Entra\u00eenement automatique nightly
     * Cron: Tous les jours \u00e0 3h00 (heure serveur)
     */
    @Scheduled(cron = "${predykt.ml.training-cron:0 0 3 * * ?}")
    public void scheduledTraining() {
        log.info("=== D\u00e9marrage entra\u00eenement ML automatique ===");
        long startTime = System.currentTimeMillis();

        List<Company> companies = companyRepository.findAll();
        int trained = 0;
        int skipped = 0;

        for (Company company : companies) {
            try {
                log.info("V\u00e9rification company {}: {}", company.getId(), company.getName());

                // V\u00e9rifier si besoin d'entra\u00eenement
                if (!shouldRetrain(company)) {
                    log.info("Company {}: pas besoin d'entra\u00eenement", company.getId());
                    skipped++;
                    continue;
                }

                // Entra\u00eener et d\u00e9ployer si meilleur
                boolean success = trainingService.trainAndDeployIfBetter(company);

                if (success) {
                    trained++;
                    log.info("Company {}: entra\u00eenement termin\u00e9 avec succ\u00e8s", company.getId());
                } else {
                    skipped++;
                    log.warn("Company {}: entra\u00eenement \u00e9chou\u00e9 (pas assez de donn\u00e9es ?)", company.getId());
                }

            } catch (Exception e) {
                log.error("Erreur entra\u00eenement ML pour company {}: {}",
                    company.getId(), e.getMessage(), e);
                skipped++;
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("=== Entra\u00eenement ML termin\u00e9: {} succ\u00e8s, {} ignor\u00e9s, dur\u00e9e {}ms ===",
            trained, skipped, duration);
    }

    /**
     * D\u00e9termine si une entreprise a besoin d'entra\u00eenement
     */
    private boolean shouldRetrain(Company company) {
        // Crit\u00e8re 1: Assez de donn\u00e9es d'entra\u00eenement ?
        if (!trainingService.hasEnoughTrainingData(company)) {
            log.debug("Company {}: pas assez de donn\u00e9es d'entra\u00eenement", company.getId());
            return false;
        }

        // Crit\u00e8re 2: Mod\u00e8le n\u00e9cessite un refresh ?
        boolean needsRetraining = matchingService.needsRetraining(company);

        if (needsRetraining) {
            log.info("Company {}: besoin de r\u00e9-entra\u00eenement d\u00e9tect\u00e9", company.getId());
        }

        return needsRetraining;
    }

    /**
     * Nettoyage hebdomadaire des anciens mod\u00e8les
     * Cron: Tous les dimanches \u00e0 4h00
     */
    @Scheduled(cron = "${predykt.ml.cleanup-cron:0 0 4 * * SUN}")
    public void scheduledCleanup() {
        log.info("=== D\u00e9marrage nettoyage mod\u00e8les ML ===");

        List<Company> companies = companyRepository.findAll();
        int cleaned = 0;

        for (Company company : companies) {
            try {
                // Nettoie les anciens mod\u00e8les (garde les 5 derniers)
                com.predykt.accounting.service.ml.MLModelStorageService storageService =
                    new com.predykt.accounting.service.ml.MLModelStorageService();
                storageService.cleanupOldModels(company.getId());
                cleaned++;

            } catch (Exception e) {
                log.error("Erreur nettoyage mod\u00e8les company {}: {}",
                    company.getId(), e.getMessage());
            }
        }

        log.info("=== Nettoyage termin\u00e9: {} entreprises ===", cleaned);
    }

    /**
     * Monitoring hebdomadaire des performances ML
     * Cron: Tous les lundis \u00e0 9h00
     */
    @Scheduled(cron = "${predykt.ml.monitoring-cron:0 0 9 * * MON}")
    public void scheduledMonitoring() {
        log.info("=== D\u00e9marrage monitoring ML hebdomadaire ===");

        List<Company> companies = companyRepository.findAll();

        for (Company company : companies) {
            try {
                var stats = matchingService.getModelStats(company);

                if ("NO_MODEL".equals(stats.get("status"))) {
                    log.warn("Company {}: AUCUN MOD\u00c8LE ACTIF", company.getId());
                    continue;
                }

                log.info("Company {}: Accuracy={}, RealAccuracy={}, AvgLatency={}ms",
                    company.getId(),
                    stats.get("accuracy"),
                    stats.get("realWorldAccuracy"),
                    stats.get("avgLatencyMs")
                );

                // Alerter si drift important
                if (stats.containsKey("accuracyDrift")) {
                    double drift = (Double) stats.get("accuracyDrift");
                    if (drift > 0.10) {
                        log.warn("⚠️  Company {}: DRIFT D\u00c9TECT\u00c9 ({:.1f}%)",
                            company.getId(), drift * 100);
                    }
                }

            } catch (Exception e) {
                log.error("Erreur monitoring company {}: {}",
                    company.getId(), e.getMessage());
            }
        }

        log.info("=== Monitoring termin\u00e9 ===");
    }

    /**
     * Ex\u00e9cution manuelle d'entra\u00eenement pour une entreprise
     */
    public void manualTraining(Long companyId) {
        log.info("Entra\u00eenement manuel demand\u00e9 pour company {}", companyId);

        companyRepository.findById(companyId).ifPresentOrElse(
            company -> {
                try {
                    boolean success = trainingService.trainAndDeployIfBetter(company);
                    if (success) {
                        log.info("Entra\u00eenement manuel r\u00e9ussi pour company {}", companyId);
                    } else {
                        log.warn("Entra\u00eenement manuel \u00e9chou\u00e9 pour company {}", companyId);
                    }
                } catch (Exception e) {
                    log.error("Erreur entra\u00eenement manuel company {}: {}",
                        companyId, e.getMessage(), e);
                }
            },
            () -> log.error("Company {} introuvable", companyId)
        );
    }
}