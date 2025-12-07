package com.predykt.accounting.domain.entity.ml;

import com.predykt.accounting.domain.entity.BaseEntity;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.User;
import com.predykt.accounting.domain.enums.MLModelStatus;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import smile.classification.RandomForest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Entité représentant un modèle ML entraîné
 * Registry central de tous les modèles (versioning, métriques, déploiement)
 *
 * Cycle de vie d'un modèle:
 * 1. TRAINING → Entraînement en cours
 * 2. TRAINED → Entraîné avec succès
 * 3. DEPLOYED → Déployé en production
 * 4. DEPRECATED → Remplacé par une version plus récente
 *
 * @author PREDYKT ML Team
 * @version 1.0
 */
@Entity
@Table(name = "ml_models", indexes = {
    @Index(name = "idx_ml_models_company", columnList = "company_id"),
    @Index(name = "idx_ml_models_active", columnList = "is_active"),
    @Index(name = "idx_ml_models_status", columnList = "status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_ml_model_version",
                     columnNames = {"company_id", "model_name", "model_version"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MLModel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;  // NULL = modèle global partagé entre toutes les entreprises

    // ==================== Identifiant et version ====================

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;  // Ex: "bank_matching_rf", "rule_based_baseline"

    @Column(name = "model_version", nullable = false, length = 20)
    private String modelVersion;  // Ex: "v1.0", "v2.3"

    // ==================== Type de modèle ====================

    /**
     * Algorithme utilisé
     * RANDOM_FOREST, XGBOOST, NEURAL_NETWORK, RULE_BASED
     */
    @Column(name = "algorithm", nullable = false, length = 50)
    private String algorithm;

    // ==================== Métriques de performance ====================

    @Column(name = "accuracy", precision = 5, scale = 4)
    private BigDecimal accuracy;  // Précision globale (0-1)

    @Column(name = "precision_score", precision = 5, scale = 4)
    private BigDecimal precisionScore;  // Précision (TP / TP+FP)

    @Column(name = "recall_score", precision = 5, scale = 4)
    private BigDecimal recallScore;  // Rappel (TP / TP+FN)

    @Column(name = "f1_score", precision = 5, scale = 4)
    private BigDecimal f1Score;  // F1 = 2 * (precision * recall) / (precision + recall)

    @Column(name = "roc_auc", precision = 5, scale = 4)
    private BigDecimal rocAuc;  // AUC de la courbe ROC

    // ==================== Données d'entraînement ====================

    @Column(name = "training_samples_count")
    private Integer trainingSamplesCount;

    @Column(name = "training_start_date")
    private LocalDateTime trainingStartDate;

    @Column(name = "training_end_date")
    private LocalDateTime trainingEndDate;

    @Column(name = "training_duration_seconds")
    private Integer trainingDurationSeconds;

    // ==================== Features utilisées ====================

    /**
     * Liste des features avec leur importance
     * Exemple: ["amount_difference", "date_diff_days", "text_similarity", ...]
     */
    @Type(JsonBinaryType.class)
    @Column(name = "features_list", columnDefinition = "jsonb")
    private List<String> featuresList;

    // ==================== Statut et déploiement ====================

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private MLModelStatus status = MLModelStatus.TRAINING;

    /**
     * TRUE si ce modèle est actuellement utilisé en production
     * Un seul modèle actif par (company_id, model_name)
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    @Column(name = "deployed_at")
    private LocalDateTime deployedAt;

    @Column(name = "deprecated_at")
    private LocalDateTime deprecatedAt;

    // ==================== Configuration hyperparamètres ====================

    /**
     * Hyperparamètres du modèle
     * Exemple pour Random Forest: {
     *   "n_trees": 100,
     *   "max_depth": 10,
     *   "min_samples_leaf": 5
     * }
     */
    @Type(JsonBinaryType.class)
    @Column(name = "hyperparameters", columnDefinition = "jsonb")
    private Map<String, Object> hyperparameters;

    // ==================== Stockage du modèle ====================

    @Column(name = "model_path", columnDefinition = "TEXT")
    private String modelPath;  // Chemin vers le fichier .model sérialisé

    /**
     * Modèle Random Forest chargé en mémoire (transient, pas persisté en BDD)
     * Chargé à la demande depuis modelPath
     */
    @Transient
    private RandomForest randomForest;

    // ==================== Utilisateur créateur ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    // ==================== Méthodes métier ====================

    /**
     * Déploie ce modèle en production (active)
     */
    public void deploy() {
        this.isActive = true;
        this.status = MLModelStatus.DEPLOYED;
        this.deployedAt = LocalDateTime.now();
    }

    /**
     * Déprécie ce modèle (remplacé par une version plus récente)
     */
    public void deprecate() {
        this.isActive = false;
        this.status = MLModelStatus.DEPRECATED;
        this.deprecatedAt = LocalDateTime.now();
    }

    /**
     * Vérifie si le modèle est utilisable pour des prédictions
     */
    public boolean isUsableForPrediction() {
        return this.isActive &&
               this.status == MLModelStatus.DEPLOYED &&
               this.accuracy != null &&
               this.accuracy.compareTo(new BigDecimal("0.70")) >= 0;  // Min 70% accuracy
    }

    /**
     * Retourne un résumé du modèle pour logging
     */
    public String getSummary() {
        return String.format("%s v%s (%s) - Accuracy: %.2f%% - %d samples - %s",
            modelName,
            modelVersion,
            algorithm,
            accuracy != null ? accuracy.multiply(new BigDecimal("100")).doubleValue() : 0.0,
            trainingSamplesCount != null ? trainingSamplesCount : 0,
            status
        );
    }

    /**
     * Calcule la durée d'entraînement en secondes
     */
    public void calculateTrainingDuration() {
        if (trainingStartDate != null && trainingEndDate != null) {
            this.trainingDurationSeconds = (int) java.time.Duration.between(
                trainingStartDate, trainingEndDate
            ).getSeconds();
        }
    }
}