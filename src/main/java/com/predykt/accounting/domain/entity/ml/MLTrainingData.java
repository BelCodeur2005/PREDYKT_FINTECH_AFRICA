package com.predykt.accounting.domain.entity.ml;

import com.predykt.accounting.domain.entity.BaseEntity;
import com.predykt.accounting.domain.entity.BankReconciliationSuggestion;
import com.predykt.accounting.domain.entity.BankTransaction;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.GeneralLedger;
import com.predykt.accounting.domain.entity.User;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entité représentant une donnée d'apprentissage ML
 * Stocke l'historique des validations utilisateur pour entraînement
 *
 * Chaque validation (APPLIED/REJECTED) devient un exemple d'apprentissage
 * utilisé pour améliorer le modèle ML de matching bancaire
 *
 * @author PREDYKT ML Team
 * @version 1.0
 */
@Entity
@Table(name = "ml_training_data", indexes = {
    @Index(name = "idx_ml_training_company", columnList = "company_id"),
    @Index(name = "idx_ml_training_suggestion", columnList = "suggestion_id"),
    @Index(name = "idx_ml_training_accepted", columnList = "was_accepted"),
    @Index(name = "idx_ml_training_created", columnList = "created_at"),
    @Index(name = "idx_ml_training_model_version", columnList = "model_version")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MLTrainingData extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggestion_id")
    private BankReconciliationSuggestion suggestion;

    // ==================== Transaction bancaire et écriture comptable ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_transaction_id")
    private BankTransaction bankTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_entry_id")
    private GeneralLedger glEntry;

    // ==================== Label d'apprentissage ====================

    /**
     * Label: TRUE si bon match (suggestion acceptée), FALSE si mauvais match (rejetée)
     * C'est la variable cible à prédire par le modèle ML
     */
    @Column(name = "was_accepted", nullable = false)
    private Boolean wasAccepted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;  // Utilisateur ayant validé/rejeté

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;  // Pourquoi rejeté (optionnel)

    // ==================== Features ML (JSON) ====================

    /**
     * Features extraites au moment de la prédiction
     * Exemple: {
     *   "amount_difference": 0.0,
     *   "date_diff_days": 2,
     *   "text_similarity": 0.87,
     *   "same_sense": 1.0,
     *   "is_round_number": 0.0,
     *   ...
     * }
     */
    @Type(JsonBinaryType.class)
    @Column(name = "features", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> features;

    // ==================== Métadonnées ML ====================

    @Column(name = "model_version", length = 20)
    private String modelVersion;  // Version du modèle ayant généré cette prédiction

    @Column(name = "prediction_confidence", precision = 5, scale = 2)
    private BigDecimal predictionConfidence;  // Confiance du modèle (0-100)

    // ==================== Méthodes utilitaires ====================

    /**
     * Extrait une feature numérique du JSON features
     */
    public Double getFeature(String featureName) {
        if (features == null || !features.containsKey(featureName)) {
            return null;
        }
        Object value = features.get(featureName);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    /**
     * Vérifie si cet exemple est utilisable pour l'entraînement
     */
    public boolean isUsableForTraining() {
        return features != null &&
               !features.isEmpty() &&
               wasAccepted != null;
    }

    /**
     * Retourne le label pour l'entraînement (1 = match, 0 = pas match)
     */
    public int getLabel() {
        return Boolean.TRUE.equals(wasAccepted) ? 1 : 0;
    }
}
