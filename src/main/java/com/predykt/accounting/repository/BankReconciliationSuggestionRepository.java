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
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // ========== NOUVEAUX ENDPOINTS : MÉTRIQUES DU DASHBOARD ==========

    /**
     * Trouve toutes les suggestions d'une entreprise sur une période
     * (pour les métriques et analyses)
     */
    @Query("SELECT s FROM BankReconciliationSuggestion s " +
           "JOIN s.reconciliation r " +
           "WHERE r.company.id = :companyId " +
           "AND s.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY s.createdAt DESC")
    List<BankReconciliationSuggestion> findByCompanyAndDateRange(
        @Param("companyId") Long companyId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Trouve les suggestions par entreprise, période et statut
     */
    @Query("SELECT s FROM BankReconciliationSuggestion s " +
           "JOIN s.reconciliation r " +
           "WHERE r.company.id = :companyId " +
           "AND s.createdAt BETWEEN :startDate AND :endDate " +
           "AND s.status = :status " +
           "ORDER BY s.createdAt DESC")
    List<BankReconciliationSuggestion> findByCompanyAndDateRangeAndStatus(
        @Param("companyId") Long companyId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("status") SuggestionStatus status
    );

    /**
     * Compte les suggestions par statut pour une entreprise sur une période
     */
    @Query("SELECT s.status, COUNT(s) FROM BankReconciliationSuggestion s " +
           "JOIN s.reconciliation r " +
           "WHERE r.company.id = :companyId " +
           "AND s.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY s.status")
    List<Object[]> countByStatusForCompanyAndPeriod(
        @Param("companyId") Long companyId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Calcule le score de confiance moyen pour une entreprise sur une période
     */
    @Query("SELECT AVG(s.confidenceScore) FROM BankReconciliationSuggestion s " +
           "JOIN s.reconciliation r " +
           "WHERE r.company.id = :companyId " +
           "AND s.createdAt BETWEEN :startDate AND :endDate")
    Optional<BigDecimal> calculateAverageConfidenceScoreForPeriod(
        @Param("companyId") Long companyId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Trouve les suggestions rejetées avec raison pour une entreprise (analyse)
     */
    @Query("SELECT s FROM BankReconciliationSuggestion s " +
           "JOIN s.reconciliation r " +
           "WHERE r.company.id = :companyId " +
           "AND s.createdAt BETWEEN :startDate AND :endDate " +
           "AND s.status = 'REJECTED' " +
           "AND s.rejectionReason IS NOT NULL " +
           "ORDER BY s.updatedAt DESC")
    List<BankReconciliationSuggestion> findRejectedWithReasonForPeriod(
        @Param("companyId") Long companyId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Groupe les rejets par raison avec comptage
     * (Top raisons de rejet)
     */
    @Query("SELECT s.rejectionReason, COUNT(s) FROM BankReconciliationSuggestion s " +
           "JOIN s.reconciliation r " +
           "WHERE r.company.id = :companyId " +
           "AND s.createdAt BETWEEN :startDate AND :endDate " +
           "AND s.status = 'REJECTED' " +
           "AND s.rejectionReason IS NOT NULL " +
           "GROUP BY s.rejectionReason " +
           "ORDER BY COUNT(s) DESC")
    List<Object[]> groupRejectionsByReason(
        @Param("companyId") Long companyId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Groupe les suggestions par type avec statistiques
     * (Type de transaction, nombre généré, appliqué, rejeté)
     */
    @Query("SELECT s.suggestedItemType, s.status, COUNT(s), AVG(s.confidenceScore) " +
           "FROM BankReconciliationSuggestion s " +
           "JOIN s.reconciliation r " +
           "WHERE r.company.id = :companyId " +
           "AND s.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY s.suggestedItemType, s.status " +
           "ORDER BY s.suggestedItemType, s.status")
    List<Object[]> groupByTypeAndStatus(
        @Param("companyId") Long companyId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Trouve les suggestions par niveau de confiance
     * (Distribution par tranches : EXCELLENT, GOOD, FAIR, LOW)
     */
    @Query("SELECT " +
           "CASE " +
           "  WHEN s.confidenceScore >= 95 THEN 'EXCELLENT' " +
           "  WHEN s.confidenceScore >= 80 THEN 'GOOD' " +
           "  WHEN s.confidenceScore >= 70 THEN 'FAIR' " +
           "  ELSE 'LOW' " +
           "END as confidenceLevel, " +
           "s.status, " +
           "COUNT(s), " +
           "AVG(s.confidenceScore) " +
           "FROM BankReconciliationSuggestion s " +
           "JOIN s.reconciliation r " +
           "WHERE r.company.id = :companyId " +
           "AND s.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY " +
           "CASE " +
           "  WHEN s.confidenceScore >= 95 THEN 'EXCELLENT' " +
           "  WHEN s.confidenceScore >= 80 THEN 'GOOD' " +
           "  WHEN s.confidenceScore >= 70 THEN 'FAIR' " +
           "  ELSE 'LOW' " +
           "END, s.status " +
           "ORDER BY " +
           "CASE " +
           "  WHEN s.confidenceScore >= 95 THEN 1 " +
           "  WHEN s.confidenceScore >= 80 THEN 2 " +
           "  WHEN s.confidenceScore >= 70 THEN 3 " +
           "  ELSE 4 " +
           "END")
    List<Object[]> groupByConfidenceLevelAndStatus(
        @Param("companyId") Long companyId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Calcule le nombre de transactions bancaires analysées
     * (compte les bank transactions distinctes dans les suggestions)
     */
    @Query("SELECT COUNT(DISTINCT bt.id) FROM BankReconciliationSuggestion s " +
           "JOIN s.reconciliation r " +
           "JOIN s.bankTransactions bt " +
           "WHERE r.company.id = :companyId " +
           "AND s.createdAt BETWEEN :startDate AND :endDate")
    long countDistinctBankTransactionsAnalyzed(
        @Param("companyId") Long companyId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Calcule le nombre d'écritures GL analysées
     * (compte les GL entries distinctes dans les suggestions)
     */
    @Query("SELECT COUNT(DISTINCT gl.id) FROM BankReconciliationSuggestion s " +
           "JOIN s.reconciliation r " +
           "JOIN s.glEntries gl " +
           "WHERE r.company.id = :companyId " +
           "AND s.createdAt BETWEEN :startDate AND :endDate")
    long countDistinctGLEntriesAnalyzed(
        @Param("companyId") Long companyId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Trouve les suggestions par jour (pour time series)
     */
    @Query("SELECT CAST(s.createdAt AS date), s.status, COUNT(s) " +
           "FROM BankReconciliationSuggestion s " +
           "JOIN s.reconciliation r " +
           "WHERE r.company.id = :companyId " +
           "AND s.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY CAST(s.createdAt AS date), s.status " +
           "ORDER BY CAST(s.createdAt AS date)")
    List<Object[]> groupByDateAndStatus(
        @Param("companyId") Long companyId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Compte les analyses (rapprochements) effectuées sur la période
     */
    @Query("SELECT COUNT(DISTINCT s.reconciliation.id) FROM BankReconciliationSuggestion s " +
           "JOIN s.reconciliation r " +
           "WHERE r.company.id = :companyId " +
           "AND s.createdAt BETWEEN :startDate AND :endDate")
    long countDistinctReconciliationsWithSuggestions(
        @Param("companyId") Long companyId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
