package com.predykt.accounting.service.ml;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.ml.MLModel;
import com.predykt.accounting.domain.entity.ml.MLTrainingData;
import com.predykt.accounting.domain.enums.MLModelStatus;
import com.predykt.accounting.repository.ml.MLModelRepository;
import com.predykt.accounting.repository.ml.MLTrainingDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import smile.classification.RandomForest;
import smile.data.formula.Formula;
import smile.validation.ClassificationMetrics;
import smile.validation.CrossValidation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Service d'entra\u00eenement ML pour matching bancaire
 * Entra\u00eene des mod\u00e8les Random Forest depuis les validations utilisateur
 *
 * Pipeline:
 * 1. R\u00e9cup\u00e9rer les donn\u00e9es d'entra\u00eenement (MLTrainingData)
 * 2. Convertir en matrices X (features) et y (labels)
 * 3. Entra\u00eener Random Forest avec Smile
 * 4. \u00c9valuer avec cross-validation
 * 5. Sauvegarder le mod\u00e8le si accuracy > seuil
 * 6. D\u00e9ployer le mod\u00e8le (marquer comme actif)
 *
 * @author PREDYKT ML Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MLTrainingService {

    private final MLTrainingDataRepository trainingDataRepository;
    private final MLModelRepository modelRepository;
    private final MLModelStorageService modelStorageService;

    // Param\u00e8tres Random Forest
    private static final int NUM_TREES = 100;
    private static final int MAX_DEPTH = 20;
    private static final int MIN_SAMPLES_SPLIT = 5;
    private static final int MIN_SAMPLES_LEAF = 2;

    // Seuils
    private static final int MIN_TRAINING_DATA = 50;
    private static final double MIN_ACCURACY = 0.70;  // 70%

    /**
     * Entra\u00eene un nouveau mod\u00e8le pour une entreprise
     *
     * @param company Entreprise
     * @return Mod\u00e8le entra\u00een\u00e9, ou null si pas assez de donn\u00e9es
     */
    @Transactional
    @CacheEvict(value = "mlModels", key = "#company.id")
    public MLModel trainNewModel(Company company) {
        long startTime = System.currentTimeMillis();
        log.info("D\u00e9marrage entra\u00eenement ML pour company {}", company.getId());

        // 1. R\u00e9cup\u00e9rer les donn\u00e9es d'entra\u00eenement
        List<MLTrainingData> trainingData = trainingDataRepository
            .findUsableTrainingData(company);

        if (trainingData.size() < MIN_TRAINING_DATA) {
            log.warn("Pas assez de donn\u00e9es d'entra\u00eenement pour company {}: {} (minimum {})",
                company.getId(), trainingData.size(), MIN_TRAINING_DATA);
            return null;
        }

        log.info("Donn\u00e9es d'entra\u00eenement: {} exemples", trainingData.size());

        // 2. Convertir en matrices
        TrainingDataset dataset = prepareDataset(trainingData);

        // 3. Entra\u00eener Random Forest
        RandomForest rf = trainRandomForest(dataset);

        // 4. \u00c9valuer le mod\u00e8le
        TrainingMetrics metrics = evaluateModel(rf, dataset);

        log.info("M\u00e9triques d'entra\u00eenement: Accuracy={:.2f}%, Precision={:.2f}%, Recall={:.2f}%, F1={:.2f}%",
            metrics.accuracy * 100, metrics.precision * 100, metrics.recall * 100, metrics.f1Score * 100);

        // 5. V\u00e9rifier si le mod\u00e8le est acceptable
        if (metrics.accuracy < MIN_ACCURACY) {
            log.warn("Mod\u00e8le rejet\u00e9: accuracy trop faible ({:.2f}% < {:.2f}%)",
                metrics.accuracy * 100, MIN_ACCURACY * 100);
            return null;
        }

        // 6. Sauvegarder le mod\u00e8le sur disque
        String version = generateVersion();
        String modelPath = modelStorageService.saveModel(rf, company.getId(), version);

        // 7. Cr\u00e9er l'entit\u00e9 MLModel
        MLModel model = MLModel.builder()
            .company(company)
            .modelName("RandomForest-BankMatching")
            .modelVersion(version)
            .modelPath(modelPath)
            .status(MLModelStatus.TRAINED)
            .isActive(false)  // Pas encore d\u00e9ploy\u00e9
            .accuracy(BigDecimal.valueOf(metrics.accuracy).setScale(4, RoundingMode.HALF_UP))
            .precision(BigDecimal.valueOf(metrics.precision).setScale(4, RoundingMode.HALF_UP))
            .recall(BigDecimal.valueOf(metrics.recall).setScale(4, RoundingMode.HALF_UP))
            .f1Score(BigDecimal.valueOf(metrics.f1Score).setScale(4, RoundingMode.HALF_UP))
            .trainingDataCount(trainingData.size())
            .trainingTimeMs(System.currentTimeMillis() - startTime)
            .build();

        MLModel saved = modelRepository.save(model);

        log.info("Mod\u00e8le ML entra\u00een\u00e9 avec succ\u00e8s: {} (accuracy={:.2f}%, {}ms)",
            saved.getModelVersion(), metrics.accuracy * 100, saved.getTrainingTimeMs());

        return saved;
    }

    /**
     * D\u00e9ploie un mod\u00e8le (marque comme actif, d\u00e9sactive les autres)
     */
    @Transactional
    @CacheEvict(value = "mlModels", key = "#model.company.id")
    public void deployModel(MLModel model) {
        // D\u00e9sactiver tous les mod\u00e8les actifs de cette entreprise
        List<MLModel> activeModels = modelRepository
            .findByCompanyAndStatus(model.getCompany(), MLModelStatus.DEPLOYED);

        for (MLModel active : activeModels) {
            active.setIsActive(false);
            active.setStatus(MLModelStatus.DEPRECATED);
            modelRepository.save(active);
            log.info("Mod\u00e8le {} d\u00e9sactiv\u00e9", active.getModelVersion());
        }

        // Activer le nouveau mod\u00e8le
        model.setIsActive(true);
        model.setStatus(MLModelStatus.DEPLOYED);
        modelRepository.save(model);

        log.info("Mod\u00e8le {} d\u00e9ploy\u00e9 pour company {}", model.getModelVersion(), model.getCompany().getId());
    }

    /**
     * Entra\u00eene et d\u00e9ploie automatiquement si meilleur que l'actuel
     */
    @Transactional
    public boolean trainAndDeployIfBetter(Company company) {
        MLModel newModel = trainNewModel(company);
        if (newModel == null) {
            return false;
        }

        // Comparer avec le mod\u00e8le actuel
        modelRepository.findByCompanyAndIsActiveTrue(company).ifPresentOrElse(
            currentModel -> {
                if (newModel.getAccuracy().compareTo(currentModel.getAccuracy()) > 0) {
                    log.info("Nouveau mod\u00e8le meilleur: {:.2f}% > {:.2f}%",
                        newModel.getAccuracy().doubleValue() * 100,
                        currentModel.getAccuracy().doubleValue() * 100);
                    deployModel(newModel);
                } else {
                    log.info("Nouveau mod\u00e8le moins bon, conservation de l'actuel");
                }
            },
            () -> {
                // Pas de mod\u00e8le actuel, d\u00e9ployer le nouveau
                log.info("Aucun mod\u00e8le actif, d\u00e9ploiement du nouveau");
                deployModel(newModel);
            }
        );

        // Nettoyer les anciens mod\u00e8les
        modelStorageService.cleanupOldModels(company.getId());

        return true;
    }

    /**
     * Pr\u00e9pare le dataset pour Smile
     */
    private TrainingDataset prepareDataset(List<MLTrainingData> trainingData) {
        int n = trainingData.size();
        int m = 12;  // 12 features

        double[][] X = new double[n][m];
        int[] y = new int[n];

        for (int i = 0; i < n; i++) {
            MLTrainingData data = trainingData.get(i);

            // Features
            X[i] = convertFeaturesToArray(data.getFeatures());

            // Label (0 = rejet\u00e9, 1 = accept\u00e9)
            y[i] = data.getLabel();
        }

        return new TrainingDataset(X, y);
    }

    /**
     * Convertit Map features → double[]
     */
    private double[] convertFeaturesToArray(Map<String, Object> features) {
        String[] featureNames = com.predykt.accounting.dto.ml.MatchFeatures.getFeatureNames();
        double[] array = new double[featureNames.length];

        for (int i = 0; i < featureNames.length; i++) {
            Object value = features.get(featureNames[i]);
            array[i] = (value instanceof Number) ? ((Number) value).doubleValue() : 0.0;
        }

        return array;
    }

    /**
     * Entra\u00eene un Random Forest
     */
    private RandomForest trainRandomForest(TrainingDataset dataset) {
        log.info("Entra\u00eenement Random Forest: {} arbres, profondeur max {}, min split {}",
            NUM_TREES, MAX_DEPTH, MIN_SAMPLES_SPLIT);

        return RandomForest.fit(
            Formula.lhs("y"),
            smile.data.DataFrame.of(dataset.X, "y", dataset.y),
            NUM_TREES,
            MAX_DEPTH,
            MIN_SAMPLES_SPLIT,
            MIN_SAMPLES_LEAF,
            1.0,  // subsample ratio
            dataset.X[0].length,  // mtry (toutes les features)
            smile.base.cart.SplitRule.GINI,
            null  // class weight (balanced automatiquement)
        );
    }

    /**
     * \u00c9value le mod\u00e8le avec cross-validation
     */
    private TrainingMetrics evaluateModel(RandomForest rf, TrainingDataset dataset) {
        int[] predictions = new int[dataset.y.length];

        for (int i = 0; i < dataset.X.length; i++) {
            predictions[i] = rf.predict(dataset.X[i]);
        }

        // Calculer les m\u00e9triques
        int tp = 0, fp = 0, tn = 0, fn = 0;

        for (int i = 0; i < dataset.y.length; i++) {
            if (predictions[i] == 1 && dataset.y[i] == 1) tp++;
            else if (predictions[i] == 1 && dataset.y[i] == 0) fp++;
            else if (predictions[i] == 0 && dataset.y[i] == 0) tn++;
            else if (predictions[i] == 0 && dataset.y[i] == 1) fn++;
        }

        double accuracy = (double) (tp + tn) / dataset.y.length;
        double precision = tp > 0 ? (double) tp / (tp + fp) : 0.0;
        double recall = tp > 0 ? (double) tp / (tp + fn) : 0.0;
        double f1 = (precision + recall > 0) ?
            2 * (precision * recall) / (precision + recall) : 0.0;

        return new TrainingMetrics(accuracy, precision, recall, f1);
    }

    /**
     * G\u00e9n\u00e8re une version de mod\u00e8le
     */
    private String generateVersion() {
        return "v" + LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    /**
     * Classe interne pour dataset
     */
    private static class TrainingDataset {
        double[][] X;  // Features
        int[] y;       // Labels

        TrainingDataset(double[][] X, int[] y) {
            this.X = X;
            this.y = y;
        }
    }

    /**
     * Classe interne pour m\u00e9triques
     */
    private static class TrainingMetrics {
        double accuracy;
        double precision;
        double recall;
        double f1Score;

        TrainingMetrics(double accuracy, double precision, double recall, double f1Score) {
            this.accuracy = accuracy;
            this.precision = precision;
            this.recall = recall;
            this.f1Score = f1Score;
        }
    }

    /**
     * Exporte les feature importances (explainability)
     */
    public Map<String, Double> getFeatureImportances(RandomForest rf) {
        Map<String, Double> importances = new HashMap<>();

        try {
            double[] importance = rf.importance();
            String[] featureNames = com.predykt.accounting.dto.ml.MatchFeatures.getFeatureNames();

            for (int i = 0; i < Math.min(importance.length, featureNames.length); i++) {
                importances.put(featureNames[i], importance[i]);
            }

            log.info("Feature importances: {}", importances);

        } catch (Exception e) {
            log.error("Erreur calcul feature importances: {}", e.getMessage());
        }

        return importances;
    }

    /**
     * V\u00e9rifie si une entreprise a assez de donn\u00e9es pour entra\u00eener
     */
    public boolean hasEnoughTrainingData(Company company) {
        long count = trainingDataRepository.countUsableTrainingData(company);
        return count >= MIN_TRAINING_DATA;
    }

    /**
     * Retourne les statistiques d'entra\u00eenement
     */
    public Map<String, Object> getTrainingStats(Company company) {
        Map<String, Object> stats = new HashMap<>();

        long totalData = trainingDataRepository.countUsableTrainingData(company);
        long acceptedCount = trainingDataRepository
            .findUsableTrainingData(company)
            .stream()
            .filter(MLTrainingData::getWasAccepted)
            .count();

        stats.put("totalTrainingData", totalData);
        stats.put("acceptedCount", acceptedCount);
        stats.put("rejectedCount", totalData - acceptedCount);
        stats.put("hasEnoughData", totalData >= MIN_TRAINING_DATA);
        stats.put("minRequired", MIN_TRAINING_DATA);

        return stats;
    }
}