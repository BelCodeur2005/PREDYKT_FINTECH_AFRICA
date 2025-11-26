package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.TimeTracking;
import com.predykt.accounting.domain.enums.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository pour l'entité TimeTracking (MODE CABINET)
 */
@Repository
public interface TimeTrackingRepository extends JpaRepository<TimeTracking, Long> {

    /**
     * Trouve toutes les entrées de temps d'un utilisateur
     */
    @Query("SELECT tt FROM TimeTracking tt WHERE tt.user.id = :userId ORDER BY tt.taskDate DESC")
    List<TimeTracking> findByUserId(@Param("userId") Long userId);

    /**
     * Trouve toutes les entrées de temps pour une entreprise
     */
    @Query("SELECT tt FROM TimeTracking tt WHERE tt.company.id = :companyId ORDER BY tt.taskDate DESC")
    List<TimeTracking> findByCompanyId(@Param("companyId") Long companyId);

    /**
     * Trouve les entrées de temps d'un utilisateur pour une entreprise
     */
    @Query("SELECT tt FROM TimeTracking tt " +
           "WHERE tt.user.id = :userId AND tt.company.id = :companyId " +
           "ORDER BY tt.taskDate DESC")
    List<TimeTracking> findByUserIdAndCompanyId(
        @Param("userId") Long userId,
        @Param("companyId") Long companyId
    );

    /**
     * Trouve les entrées de temps pour une période
     */
    @Query("SELECT tt FROM TimeTracking tt " +
           "WHERE tt.taskDate BETWEEN :startDate AND :endDate " +
           "ORDER BY tt.taskDate DESC")
    List<TimeTracking> findByPeriod(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Trouve les entrées de temps d'un utilisateur pour une période
     */
    @Query("SELECT tt FROM TimeTracking tt " +
           "WHERE tt.user.id = :userId " +
           "AND tt.taskDate BETWEEN :startDate AND :endDate " +
           "ORDER BY tt.taskDate DESC")
    List<TimeTracking> findByUserIdAndPeriod(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Trouve les entrées de temps d'une entreprise pour une période
     */
    @Query("SELECT tt FROM TimeTracking tt " +
           "WHERE tt.company.id = :companyId " +
           "AND tt.taskDate BETWEEN :startDate AND :endDate " +
           "ORDER BY tt.taskDate DESC")
    List<TimeTracking> findByCompanyIdAndPeriod(
        @Param("companyId") Long companyId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Trouve les entrées facturables d'une entreprise
     */
    @Query("SELECT tt FROM TimeTracking tt " +
           "WHERE tt.company.id = :companyId " +
           "AND tt.isBillable = true " +
           "ORDER BY tt.taskDate DESC")
    List<TimeTracking> findBillableByCompanyId(@Param("companyId") Long companyId);

    /**
     * Calcule le total d'heures pour un utilisateur sur une période
     */
    @Query("SELECT COALESCE(SUM(tt.durationMinutes), 0) / 60.0 FROM TimeTracking tt " +
           "WHERE tt.user.id = :userId " +
           "AND tt.taskDate BETWEEN :startDate AND :endDate")
    Double calculateTotalHoursByUserIdAndPeriod(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Calcule le total d'heures pour une entreprise sur une période
     */
    @Query("SELECT COALESCE(SUM(tt.durationMinutes), 0) / 60.0 FROM TimeTracking tt " +
           "WHERE tt.company.id = :companyId " +
           "AND tt.taskDate BETWEEN :startDate AND :endDate")
    Double calculateTotalHoursByCompanyIdAndPeriod(
        @Param("companyId") Long companyId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Calcule le montant facturable pour une entreprise
     */
    @Query("SELECT COALESCE(SUM(tt.durationMinutes * tt.hourlyRate / 60.0), 0) " +
           "FROM TimeTracking tt " +
           "WHERE tt.company.id = :companyId " +
           "AND tt.isBillable = true " +
           "AND tt.hourlyRate IS NOT NULL")
    BigDecimal calculateBillableAmountByCompanyId(@Param("companyId") Long companyId);

    /**
     * Calcule le montant facturable pour une entreprise sur une période
     */
    @Query("SELECT COALESCE(SUM(tt.durationMinutes * tt.hourlyRate / 60.0), 0) " +
           "FROM TimeTracking tt " +
           "WHERE tt.company.id = :companyId " +
           "AND tt.isBillable = true " +
           "AND tt.hourlyRate IS NOT NULL " +
           "AND tt.taskDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateBillableAmountByCompanyIdAndPeriod(
        @Param("companyId") Long companyId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Trouve les entrées de temps par type de tâche
     */
    @Query("SELECT tt FROM TimeTracking tt " +
           "WHERE tt.company.id = :companyId " +
           "AND tt.taskType = :taskType " +
           "ORDER BY tt.taskDate DESC")
    List<TimeTracking> findByCompanyIdAndTaskType(
        @Param("companyId") Long companyId,
        @Param("taskType") TaskType taskType
    );

    /**
     * Statistiques par type de tâche pour une entreprise
     */
    @Query("SELECT tt.taskType, COUNT(tt), SUM(tt.durationMinutes) " +
           "FROM TimeTracking tt " +
           "WHERE tt.company.id = :companyId " +
           "GROUP BY tt.taskType")
    List<Object[]> getStatisticsByCompanyId(@Param("companyId") Long companyId);

    /**
     * Statistiques par utilisateur pour une entreprise
     */
    @Query("SELECT tt.user.id, tt.user.firstName, tt.user.lastName, " +
           "COUNT(tt), SUM(tt.durationMinutes) " +
           "FROM TimeTracking tt " +
           "WHERE tt.company.id = :companyId " +
           "GROUP BY tt.user.id, tt.user.firstName, tt.user.lastName")
    List<Object[]> getUserStatisticsByCompanyId(@Param("companyId") Long companyId);
}
