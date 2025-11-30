package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.BankReconciliation;
import com.predykt.accounting.domain.entity.BankReconciliationSuggestion;
import com.predykt.accounting.domain.enums.SuggestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour les suggestions de matching automatique
 * Gère la persistance et les requêtes sur les suggestions
 */
@Repository
public interface BankReconciliationSuggestionRepository extends JpaRepository<BankReconciliationSuggestion, Long> {

    /**
     * Trouve toutes les suggestions pour un rapprochement
     */
    List<BankReconciliationSuggestion> findByReconciliationOrderByConfidenceScoreDesc(
        BankReconciliation reconciliation
    );

    /**
     * Trouve les suggestions en attente pour un rapprochement
     */
    List<BankReconciliationSuggestion> findByReconciliationAndStatusOrderByConfidenceScoreDesc(
        BankReconciliation reconciliation,
        SuggestionStatus status
    );

    /**
     * Trouve les suggestions nécessitant révision manuelle
     */
    @Query("SELECT s FROM BankReconciliationSuggestion s " +
           "WHERE s.reconciliation = :reconciliation " +
           "AND s.requiresManualReview = true " +
           "AND s.status = 'PENDING' " +
           "ORDER BY s.confidenceScore DESC")
    List<BankReconciliationSuggestion> findManualReviewRequired(
        @Param("reconciliation") BankReconciliation reconciliation
    );

    /**
     * Trouve les suggestions auto-approuvables (confiance >= seuil)
     */
    @Query("SELECT s FROM BankReconciliationSuggestion s " +
           "WHERE s.reconciliation = :reconciliation " +
           "AND s.confidenceScore >= :threshold " +
           "AND s.status = 'PENDING' " +
           "ORDER BY s.confidenceScore DESC")
    List<BankReconciliationSuggestion> findAutoApprovable(
        @Param("reconciliation") BankReconciliation reconciliation,
        @Param("threshold") BigDecimal threshold
    );

    /**
     * Trouve les suggestions par type de matching
     */
    List<BankReconciliationSuggestion> findByReconciliationAndMatchTypeOrderByConfidenceScoreDesc(
        BankReconciliation reconciliation,
        String matchType
    );

    /**
     * Compte les suggestions par statut pour un rapprochement
     */
    long countByReconciliationAndStatus(
        BankReconciliation reconciliation,
        SuggestionStatus status
    );

    /**
     * Compte les suggestions en attente pour un rapprochement
     */
    @Query("SELECT COUNT(s) FROM BankReconciliationSuggestion s " +
           "WHERE s.reconciliation = :reconciliation " +
           "AND s.status = 'PENDING'")
    long countPendingSuggestions(@Param("reconciliation") BankReconciliation reconciliation);

    /**
     * Calcule le score de confiance moyen pour un rapprochement
     */
    @Query("SELECT AVG(s.confidenceScore) FROM BankReconciliationSuggestion s " +
           "WHERE s.reconciliation = :reconciliation " +
           "AND s.status = 'PENDING'")
    Optional<BigDecimal> calculateAverageConfidenceScore(
        @Param("reconciliation") BankReconciliation reconciliation
    );

    /**
     * Expire toutes les suggestions en attente pour un rapprochement
     * (appelé quand le rapprochement est approuvé sans appliquer certaines suggestions)
     */
    @Modifying
    @Query("UPDATE BankReconciliationSuggestion s " +
           "SET s.status = 'EXPIRED' " +
           "WHERE s.reconciliation = :reconciliation " +
           "AND s.status = 'PENDING'")
    int expirePendingSuggestions(@Param("reconciliation") BankReconciliation reconciliation);

    /**
     * Supprime les suggestions expirées de plus de N jours
     * (nettoyage périodique)
     */
    @Modifying
    @Query("DELETE FROM BankReconciliationSuggestion s " +
           "WHERE s.status = 'EXPIRED' " +
           "AND s.updatedAt < CURRENT_TIMESTAMP - :days * INTERVAL '1 day'")
    int deleteExpiredSuggestionsOlderThan(@Param("days") int days);

    /**
     * Trouve les suggestions rejetées avec leur raison
     * (utile pour améliorer l'algorithme)
     */
    @Query("SELECT s FROM BankReconciliationSuggestion s " +
           "WHERE s.status = 'REJECTED' " +
           "AND s.rejectionReason IS NOT NULL " +
           "ORDER BY s.updatedAt DESC")
    List<BankReconciliationSuggestion> findRejectedWithReason();

    /**
     * Statistiques de performance de l'algorithme
     */
    @Query("SELECT s.status, COUNT(s), AVG(s.confidenceScore) " +
           "FROM BankReconciliationSuggestion s " +
           "WHERE s.reconciliation = :reconciliation " +
           "GROUP BY s.status")
    List<Object[]> getMatchingStatistics(@Param("reconciliation") BankReconciliation reconciliation);

    /**
     * Trouve une suggestion par son ID pour un rapprochement spécifique
     * (sécurité : s'assurer que la suggestion appartient bien au rapprochement)
     */
    Optional<BankReconciliationSuggestion> findByIdAndReconciliation(
        Long id,
        BankReconciliation reconciliation
    );

    /**
     * Vérifie si une suggestion existe déjà pour une transaction bancaire
     * (éviter les doublons)
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM BankReconciliationSuggestion s " +
           "JOIN s.bankTransactions bt " +
           "WHERE s.reconciliation = :reconciliation " +
           "AND bt.id = :bankTransactionId " +
           "AND s.status = 'PENDING'")
    boolean existsPendingSuggestionForBankTransaction(
        @Param("reconciliation") BankReconciliation reconciliation,
        @Param("bankTransactionId") Long bankTransactionId
    );

    /**
     * Vérifie si une suggestion existe déjà pour une écriture GL
     * (éviter les doublons)
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM BankReconciliationSuggestion s " +
           "JOIN s.glEntries gl " +
           "WHERE s.reconciliation = :reconciliation " +
           "AND gl.id = :glEntryId " +
           "AND s.status = 'PENDING'")
    boolean existsPendingSuggestionForGLEntry(
        @Param("reconciliation") BankReconciliation reconciliation,
        @Param("glEntryId") Long glEntryId
    );
}
