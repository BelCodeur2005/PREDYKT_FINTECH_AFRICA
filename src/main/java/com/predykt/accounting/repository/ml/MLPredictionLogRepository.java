package com.predykt.accounting.repository.ml;

import com.predykt.accounting.domain.entity.BankReconciliation;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.ml.MLPredictionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository pour MLPredictionLog
 * Gère les logs de prédictions ML pour monitoring
 *
 * @author PREDYKT ML Team
 */
@Repository
public interface MLPredictionLogRepository extends JpaRepository<MLPredictionLog, Long> {

    List<MLPredictionLog> findByReconciliationOrderByPredictedAtDesc(
        BankReconciliation reconciliation
    );

    /**
     * Compte les prédictions correctes
     */
    @Query("SELECT COUNT(p) FROM MLPredictionLog p " +
           "WHERE p.company = :company " +
           "AND p.wasCorrect = true " +
           "AND p.predictedAt >= :since")
    long countCorrectPredictions(
        @Param("company") Company company,
        @Param("since") LocalDateTime since
    );

    /**
     * Compte les prédictions validées
     */
    @Query("SELECT COUNT(p) FROM MLPredictionLog p " +
           "WHERE p.company = :company " +
           "AND p.actualOutcome IS NOT NULL " +
           "AND p.predictedAt >= :since")
    long countValidatedPredictions(
        @Param("company") Company company,
        @Param("since") LocalDateTime since
    );

    /**
     * Calcule la latence moyenne
     */
    @Query("SELECT AVG(p.predictionTimeMs) FROM MLPredictionLog p " +
           "WHERE p.company = :company " +
           "AND p.predictedAt >= :since")
    Double calculateAverageLatency(
        @Param("company") Company company,
        @Param("since") LocalDateTime since
    );

    /**
     * Calcule l'accuracy réelle du modèle
     */
    @Query("SELECT CAST(SUM(CASE WHEN p.wasCorrect = true THEN 1 ELSE 0 END) AS double) / " +
           "CAST(COUNT(p) AS double) FROM MLPredictionLog p " +
           "WHERE p.company = :company " +
           "AND p.actualOutcome IS NOT NULL " +
           "AND p.predictedAt >= :since")
    Double calculateRealWorldAccuracy(
        @Param("company") Company company,
        @Param("since") LocalDateTime since
    );
}