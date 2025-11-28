package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.CashFlowProjection;
import com.predykt.accounting.domain.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CashFlowProjectionRepository extends JpaRepository<CashFlowProjection, Long> {

    // Recherche par entreprise
    List<CashFlowProjection> findByCompany(Company company);

    List<CashFlowProjection> findByCompanyOrderByProjectionDateDesc(Company company);

    // Recherche par horizon
    List<CashFlowProjection> findByCompanyAndProjectionHorizon(Company company, Integer projectionHorizon);

    // Recherche par date
    List<CashFlowProjection> findByCompanyAndProjectionDate(Company company, LocalDate projectionDate);

    List<CashFlowProjection> findByCompanyAndProjectionDateBetween(
        Company company,
        LocalDate startDate,
        LocalDate endDate
    );

    // Dernière projection par horizon
    @Query("SELECT cfp FROM CashFlowProjection cfp WHERE cfp.company = :company " +
           "AND cfp.projectionHorizon = :horizon " +
           "ORDER BY cfp.projectionDate DESC, cfp.createdAt DESC")
    List<CashFlowProjection> findLatestByCompanyAndHorizon(
        @Param("company") Company company,
        @Param("horizon") Integer horizon
    );

    Optional<CashFlowProjection> findFirstByCompanyAndProjectionHorizonOrderByProjectionDateDesc(
        Company company,
        Integer projectionHorizon
    );

    // Projections avec trésorerie négative
    @Query("SELECT cfp FROM CashFlowProjection cfp WHERE cfp.company = :company " +
           "AND cfp.projectedBalance < 0 " +
           "ORDER BY cfp.projectionDate ASC")
    List<CashFlowProjection> findNegativeCashFlowProjections(@Param("company") Company company);

    // Projections par niveau de confiance
    @Query("SELECT cfp FROM CashFlowProjection cfp WHERE cfp.company = :company " +
           "AND cfp.confidenceScore >= :minConfidence " +
           "ORDER BY cfp.projectionDate DESC")
    List<CashFlowProjection> findByCompanyAndMinConfidence(
        @Param("company") Company company,
        @Param("minConfidence") BigDecimal minConfidence
    );

    // Projections faible confiance
    @Query("SELECT cfp FROM CashFlowProjection cfp WHERE cfp.company = :company " +
           "AND cfp.confidenceScore < :threshold " +
           "ORDER BY cfp.projectionDate DESC")
    List<CashFlowProjection> findLowConfidenceProjections(
        @Param("company") Company company,
        @Param("threshold") BigDecimal threshold
    );

    // Statistiques
    @Query("SELECT AVG(cfp.confidenceScore) FROM CashFlowProjection cfp " +
           "WHERE cfp.company = :company AND cfp.projectionHorizon = :horizon")
    BigDecimal averageConfidenceByHorizon(
        @Param("company") Company company,
        @Param("horizon") Integer horizon
    );

    @Query("SELECT AVG(cfp.projectedBalance) FROM CashFlowProjection cfp " +
           "WHERE cfp.company = :company AND cfp.projectionDate BETWEEN :startDate AND :endDate")
    BigDecimal averageProjectedBalanceForPeriod(
        @Param("company") Company company,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    // Compter projections
    long countByCompany(Company company);

    long countByCompanyAndProjectionHorizon(Company company, Integer projectionHorizon);

    // Supprimer anciennes projections
    @Query("DELETE FROM CashFlowProjection cfp WHERE cfp.company = :company " +
           "AND cfp.projectionDate < :cutoffDate")
    void deleteOldProjectionsByCompany(
        @Param("company") Company company,
        @Param("cutoffDate") LocalDate cutoffDate
    );
}
