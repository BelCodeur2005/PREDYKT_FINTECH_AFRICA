package com.predykt.accounting.service.ml;

import com.predykt.accounting.domain.entity.BankTransaction;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.GeneralLedger;
import com.predykt.accounting.domain.entity.ml.MLModel;
import com.predykt.accounting.domain.entity.ml.MLPredictionLog;
import com.predykt.accounting.dto.ml.MLPredictionResult;
import com.predykt.accounting.dto.ml.MatchFeatures;
import com.predykt.accounting.repository.ml.MLModelRepository;
import com.predykt.accounting.repository.ml.MLPredictionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import smile.classification.RandomForest;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service ML pour matching bancaire
 * Utilise Random Forest pour pr\u00e9dire les matches BT ↔ GL
 *
 * Pipeline:
 * 1. Extract features (MLFeatureExtractor)
 * 2. Load active model
 * 3. Predict with Random Forest
 * 4. Log prediction
 * 5. Return MLPredictionResult
 *
 * @author PREDYKT ML Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MLMatchingService {

    private final MLFeatureExtractor featureExtractor;
    private final MLModelRepository modelRepository;
    private final MLPredictionLogRepository predictionLogRepository;
    private final MLModelStorageService modelStorageService;

    /**
     * Pr\u00e9dit le meilleur match GL pour une BankTransaction
     *
     * @param bt Transaction bancaire
     * @param glCandidates Candidats GL possibles
     * @param company Entreprise
     * @return Meilleur match pr\u00e9dit avec score de confiance
     */
    @Transactional
    public Optional<MLPredictionResult> predictBestMatch(
        BankTransaction bt,
        List<GeneralLedger> glCandidates,
        Company company
    ) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. Charger le mod\u00e8le actif
            Optional<MLModel> modelOpt = loadActiveModel(company);
            if (modelOpt.isEmpty()) {
                log.warn("Aucun mod\u00e8le ML actif pour company {}", company.getId());
                return Optional.empty();
            }

            MLModel model = modelOpt.get();
            RandomForest rf = modelStorageService.loadModel(model);

            if (rf == null) {
                log.error("Impossible de charger le mod\u00e8le Random Forest depuis {}", model.getModelPath());
                return Optional.empty();
            }

            // 2. Extraire features + pr\u00e9dire pour tous les candidats
            List<CandidateScore> scores = new ArrayList<>();

            for (GeneralLedger gl : glCandidates) {
                MatchFeatures features = featureExtractor.extract(bt, gl);
                double[] featureArray = features.toArray();

                // Pr\u00e9diction
                int prediction = rf.predict(featureArray);  // 0 ou 1
                double[] probabilities = rf.predict(featureArray, new double[2]);

                // Confiance = probabilit\u00e9 de la classe 1 (match)
                double confidence = probabilities[1] * 100.0;

                scores.add(new CandidateScore(gl, features, confidence, prediction == 1));
            }

            // 3. Trouver le meilleur candidat (confiance max)
            Optional<CandidateScore> bestOpt = scores.stream()
                .filter(cs -> cs.isPredictedMatch)
                .max(Comparator.comparingDouble(cs -> cs.confidence));

            if (bestOpt.isEmpty()) {
                log.info("ML: Aucun match pr\u00e9dit pour BT {} parmi {} candidats",
                    bt.getId(), glCandidates.size());
                return Optional.empty();
            }

            CandidateScore best = bestOpt.get();

            // 4. Construire le r\u00e9sultat
            long predictionTime = System.currentTimeMillis() - startTime;

            MLPredictionResult result = MLPredictionResult.builder()
                .bankTransaction(bt)
                .glEntry(best.gl)
                .confidenceScore(best.confidence)
                .features(best.features)
                .modelVersion(model.getModelVersion())
                .predictionTimeMs(predictionTime)
                .build();

            result.generateExplanation();

            // 5. Logger la pr\u00e9diction
            logPrediction(result, model, company);

            log.info("ML: Pr\u00e9diction BT {} → GL {} avec confiance {:.1f}% ({}ms)",
                bt.getId(), best.gl.getId(), best.confidence, predictionTime);

            return Optional.of(result);

        } catch (Exception e) {
            log.error("Erreur pr\u00e9diction ML pour BT {}: {}", bt.getId(), e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Pr\u00e9dit pour plusieurs transactions en batch
     */
    @Transactional
    public List<MLPredictionResult> predictBatch(
        List<BankTransaction> btList,
        List<GeneralLedger> glPool,
        Company company
    ) {
        List<MLPredictionResult> results = new ArrayList<>();

        for (BankTransaction bt : btList) {
            Optional<MLPredictionResult> prediction = predictBestMatch(bt, glPool, company);
            prediction.ifPresent(results::add);
        }

        log.info("ML: Batch pr\u00e9diction termin\u00e9e - {} matches trouv\u00e9s sur {} transactions",
            results.size(), btList.size());

        return results;
    }

    /**
     * Pr\u00e9dit avec filtrage des candidats (optimisation)
     */
    @Transactional
    public Optional<MLPredictionResult> predictWithFiltering(
        BankTransaction bt,
        List<GeneralLedger> glCandidates,
        Company company
    ) {
        // Pré-filtre: montant ±50%, dates ±30 jours
        List<GeneralLedger> filtered = glCandidates.stream()
            .filter(gl -> isReasonableCandidate(bt, gl))
            .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            log.debug("ML: Pr\u00e9-filtrage a \u00e9limin\u00e9 tous les candidats pour BT {}", bt.getId());
            return Optional.empty();
        }

        log.debug("ML: Pr\u00e9-filtrage {} → {} candidats pour BT {}",
            glCandidates.size(), filtered.size(), bt.getId());

        return predictBestMatch(bt, filtered, company);
    }

    /**
     * Charge le mod\u00e8le actif (avec cache)
     */
    @Cacheable(value = "mlModels", key = "#company.id")
    public Optional<MLModel> loadActiveModel(Company company) {
        return modelRepository.findByCompanyAndIsActiveTrue(company);
    }

    /**
     * Invalide le cache du mod\u00e8le (apr\u00e8s entra\u00eenement)
     */
    public void invalidateModelCache(Company company) {
        // Cache eviction g\u00e9r\u00e9 par @CacheEvict dans MLTrainingService
        log.info("Cache mod\u00e8le ML invalid\u00e9 pour company {}", company.getId());
    }

    /**
     * V\u00e9rifie si un candidat est raisonnable (pr\u00e9-filtrage)
     */
    private boolean isReasonableCandidate(BankTransaction bt, GeneralLedger gl) {
        // Montant: ±50%
        double btAmount = Math.abs(bt.getAmount().doubleValue());
        double glAmount = Math.abs(gl.getDebitAmount().subtract(gl.getCreditAmount()).doubleValue());

        if (glAmount == 0) return false;

        double ratio = btAmount / glAmount;
        if (ratio < 0.5 || ratio > 2.0) return false;

        // Date: ±30 jours
        long daysDiff = Math.abs(
            java.time.temporal.ChronoUnit.DAYS.between(
                bt.getTransactionDate(),
                gl.getEntryDate()
            )
        );

        return daysDiff <= 30;
    }

    /**
     * Log la pr\u00e9diction pour monitoring
     */
    private void logPrediction(MLPredictionResult result, MLModel model, Company company) {
        try {
            MLPredictionLog log = MLPredictionLog.builder()
                .company(company)
                .model(model)
                .bankTransaction(result.getBankTransaction())
                .glEntry(result.getGlEntry())
                .predictedMatch(true)
                .confidenceScore(result.getConfidenceScore())
                .features(result.getFeatures().toMap())
                .predictionTimeMs(result.getPredictionTimeMs())
                .predictedAt(LocalDateTime.now())
                .build();

            MLPredictionLog saved = predictionLogRepository.save(log);
            result.setPredictionLogId(saved.getId());

        } catch (Exception e) {
            // Ne pas bloquer la pr\u00e9diction si log \u00e9choue
            this.log.error("Erreur log pr\u00e9diction: {}", e.getMessage());
        }
    }

    /**
     * Classe interne pour scorer les candidats
     */
    private static class CandidateScore {
        GeneralLedger gl;
        MatchFeatures features;
        double confidence;
        boolean isPredictedMatch;

        CandidateScore(GeneralLedger gl, MatchFeatures features,
                      double confidence, boolean isPredictedMatch) {
            this.gl = gl;
            this.features = features;
            this.confidence = confidence;
            this.isPredictedMatch = isPredictedMatch;
        }
    }

    /**
     * V\u00e9rifie si le mod\u00e8le a besoin d'un r\u00e9-entra\u00eenement
     */
    public boolean needsRetraining(Company company) {
        Optional<MLModel> modelOpt = loadActiveModel(company);
        if (modelOpt.isEmpty()) return true;

        MLModel model = modelOpt.get();

        // Crit\u00e8re 1: Mod\u00e8le trop ancien (>30 jours)
        if (model.getCreatedAt().isBefore(LocalDateTime.now().minusDays(30))) {
            return true;
        }

        // Crit\u00e8re 2: Accuracy trop faible (<85%)
        if (model.getAccuracy().doubleValue() < 0.85) {
            return true;
        }

        // Crit\u00e8re 3: Drift d\u00e9tect\u00e9 (accuracy r\u00e9elle vs entra\u00eenement)
        Double realAccuracy = predictionLogRepository.calculateRealWorldAccuracy(
            company,
            LocalDateTime.now().minusDays(7)
        );

        if (realAccuracy != null) {
            double drift = Math.abs(model.getAccuracy().doubleValue() - realAccuracy);
            if (drift > 0.10) {  // 10% de diff\u00e9rence
                log.warn("Drift d\u00e9tect\u00e9 pour company {}: accuracy model={:.2f}%, r\u00e9el={:.2f}%",
                    company.getId(), model.getAccuracy().doubleValue() * 100, realAccuracy * 100);
                return true;
            }
        }

        return false;
    }

    /**
     * Retourne les statistiques du mod\u00e8le actif
     */
    public Map<String, Object> getModelStats(Company company) {
        Optional<MLModel> modelOpt = loadActiveModel(company);
        if (modelOpt.isEmpty()) {
            return Map.of("status", "NO_MODEL");
        }

        MLModel model = modelOpt.get();
        Map<String, Object> stats = new HashMap<>();

        stats.put("modelVersion", model.getModelVersion());
        stats.put("accuracy", model.getAccuracy());
        stats.put("f1Score", model.getF1Score());
        stats.put("createdAt", model.getCreatedAt());
        stats.put("trainingDataCount", model.getTrainingDataCount());

        // Latence moyenne sur 7 derniers jours
        Double avgLatency = predictionLogRepository.calculateAverageLatency(
            company,
            LocalDateTime.now().minusDays(7)
        );
        stats.put("avgLatencyMs", avgLatency);

        // Accuracy r\u00e9elle
        Double realAccuracy = predictionLogRepository.calculateRealWorldAccuracy(
            company,
            LocalDateTime.now().minusDays(7)
        );
        stats.put("realWorldAccuracy", realAccuracy);

        // Drift
        if (realAccuracy != null) {
            double drift = Math.abs(model.getAccuracy().doubleValue() - realAccuracy);
            stats.put("accuracyDrift", drift);
            stats.put("needsRetraining", drift > 0.10);
        }

        return stats;
    }
}