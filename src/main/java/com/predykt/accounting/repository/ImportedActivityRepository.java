package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.ImportedActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository pour l'entité ImportedActivity
 */
@Repository
public interface ImportedActivityRepository extends JpaRepository<ImportedActivity, Long> {

    /**
     * Trouve toutes les activités importées d'une entreprise
     */
    @Query("SELECT ia FROM ImportedActivity ia WHERE ia.company.id = :companyId ORDER BY ia.activityDate DESC")
    List<ImportedActivity> findByCompanyId(@Param("companyId") Long companyId);

    /**
     * Trouve les activités importées par statut
     */
    @Query("SELECT ia FROM ImportedActivity ia " +
           "WHERE ia.company.id = :companyId AND ia.importStatus = :status " +
           "ORDER BY ia.importedAt DESC")
    List<ImportedActivity> findByCompanyIdAndStatus(
        @Param("companyId") Long companyId,
        @Param("status") String status
    );

    /**
     * Trouve les activités en attente de traitement
     */
    @Query("SELECT ia FROM ImportedActivity ia " +
           "WHERE ia.company.id = :companyId AND ia.importStatus = 'PENDING' " +
           "ORDER BY ia.activityDate ASC")
    List<ImportedActivity> findPendingByCompanyId(@Param("companyId") Long companyId);

    /**
     * Trouve les activités mappées avec faible confiance
     */
    @Query("SELECT ia FROM ImportedActivity ia " +
           "WHERE ia.company.id = :companyId " +
           "AND ia.importStatus = 'MAPPED' " +
           "AND ia.confidenceScore < :threshold " +
           "ORDER BY ia.confidenceScore ASC")
    List<ImportedActivity> findLowConfidenceMappingsByCompanyId(
        @Param("companyId") Long companyId,
        @Param("threshold") BigDecimal threshold
    );

    /**
     * Trouve les activités pour une période
     */
    @Query("SELECT ia FROM ImportedActivity ia " +
           "WHERE ia.company.id = :companyId " +
           "AND ia.activityDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ia.activityDate DESC")
    List<ImportedActivity> findByCompanyIdAndPeriod(
        @Param("companyId") Long companyId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Compte les activités par statut
     */
    @Query("SELECT COUNT(ia) FROM ImportedActivity ia " +
           "WHERE ia.company.id = :companyId AND ia.importStatus = :status")
    Long countByCompanyIdAndStatus(
        @Param("companyId") Long companyId,
        @Param("status") String status
    );

    /**
     * Calcule le montant total des activités importées
     */
    @Query("SELECT COALESCE(SUM(ia.amount), 0) FROM ImportedActivity ia " +
           "WHERE ia.company.id = :companyId AND ia.importStatus = 'PROCESSED'")
    BigDecimal calculateTotalProcessedAmount(@Param("companyId") Long companyId);

    /**
     * Trouve les activités importées par catégorie
     */
    @Query("SELECT ia FROM ImportedActivity ia " +
           "WHERE ia.company.id = :companyId AND ia.category = :category " +
           "ORDER BY ia.activityDate DESC")
    List<ImportedActivity> findByCompanyIdAndCategory(
        @Param("companyId") Long companyId,
        @Param("category") String category
    );

    /**
     * Trouve les activités importées par utilisateur
     */
    @Query("SELECT ia FROM ImportedActivity ia " +
           "WHERE ia.company.id = :companyId AND ia.importedBy = :importedBy " +
           "ORDER BY ia.importedAt DESC")
    List<ImportedActivity> findByCompanyIdAndImportedBy(
        @Param("companyId") Long companyId,
        @Param("importedBy") String importedBy
    );

    /**
     * Statistiques d'import par statut
     */
    @Query("SELECT ia.importStatus, COUNT(ia), SUM(ia.amount) " +
           "FROM ImportedActivity ia " +
           "WHERE ia.company.id = :companyId " +
           "GROUP BY ia.importStatus")
    List<Object[]> getImportStatisticsByCompanyId(@Param("companyId") Long companyId);

    /**
     * Supprime les activités rejetées plus anciennes que N jours
     */
    @Query("DELETE FROM ImportedActivity ia " +
           "WHERE ia.importStatus = 'REJECTED' " +
           "AND ia.processedAt < :cutoffDate")
    void deleteOldRejectedActivities(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
}
