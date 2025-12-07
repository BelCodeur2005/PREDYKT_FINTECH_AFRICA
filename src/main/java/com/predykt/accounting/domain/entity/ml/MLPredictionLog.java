package com.predykt.accounting.domain.entity.ml;

import com.predykt.accounting.domain.entity.BankReconciliation;
import com.predykt.accounting.domain.entity.BankTransaction;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.GeneralLedger;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Log de toutes les prédictions ML pour monitoring et debugging
 * Permet de tracer chaque prédiction et mesurer la performance réelle
 *
 * @author PREDYKT ML Team
 * @version 1.0
 */
@Entity
@Table(name = "ml_predictions_log", indexes = {
    @Index(name = "idx_ml_predictions_company", columnList = "company_id"),
    @Index(name = "idx_ml_predictions_model", columnList = "model_id"),
    @Index(name = "idx_ml_predictions_reconciliation", columnList = "reconciliation_id"),
    @Index(name = "idx_ml_predictions_date", columnList = "predicted_at"),
    @Index(name = "idx_ml_predictions_correctness", columnList = "was_correct")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MLPredictionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    private MLModel model;

    // ==================== Contexte de prédiction ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_id")
    private BankReconciliation reconciliation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_transaction_id", nullable = false)
    private BankTransaction bankTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_entry_id", nullable = false)
    private GeneralLedger glEntry;

    // ==================== Résultat de prédiction ====================

    /**
     * TRUE si le modèle prédit un match, FALSE sinon
     */
    @Column(name = "predicted_match", nullable = false)
    private Boolean predictedMatch;

    /**
     * Score de confiance de la prédiction (0-100)
     */
    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    // ==================== Features utilisées ====================

    @Type(JsonBinaryType.class)
    @Column(name = "features", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> features;

    // ==================== Métadonnées de performance ====================

    /**
     * Temps de calcul de la prédiction en millisecondes
     */
    @Column(name = "prediction_time_ms")
    private Integer predictionTimeMs;

    // ==================== Résultat final (feedback utilisateur) ====================

    /**
     * Résultat RÉEL après validation utilisateur
     * NULL si pas encore validé
     * TRUE si l'utilisateur a accepté (APPLIED)
     * FALSE si l'utilisateur a rejeté (REJECTED)
     */
    @Column(name = "actual_outcome")
    private Boolean actualOutcome;

    /**
     * TRUE si prédiction = outcome (prédiction correcte)
     * Calculé après validation utilisateur
     */
    @Column(name = "was_correct")
    private Boolean wasCorrect;

    // ==================== Timestamp ====================

    @Column(name = "predicted_at", nullable = false)
    @Builder.Default
    private LocalDateTime predictedAt = LocalDateTime.now();

    // ==================== Méthodes métier ====================

    /**
     * Met à jour le résultat final après validation utilisateur
     */
    public void updateOutcome(Boolean outcome) {
        this.actualOutcome = outcome;
        this.wasCorrect = this.predictedMatch.equals(outcome);
    }

    /**
     * Vérifie si la prédiction a été validée par l'utilisateur
     */
    public boolean isValidated() {
        return actualOutcome != null;
    }

    /**
     * Retourne si la prédiction était correcte (après validation)
     */
    public boolean wasCorrect() {
        return Boolean.TRUE.equals(wasCorrect);
    }
}