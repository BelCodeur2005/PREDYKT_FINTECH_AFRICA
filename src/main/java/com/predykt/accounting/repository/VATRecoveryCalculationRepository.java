package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.VATRecoveryCalculation;
import com.predykt.accounting.domain.enums.VATRecoverableCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Repository pour les calculs de TVA récupérable
 */
@Repository
public interface VATRecoveryCalculationRepository extends JpaRepository<VATRecoveryCalculation, Long> {

    /**
     * Trouve tous les calculs pour une entreprise
     */
    List<VATRecoveryCalculation> findByCompanyOrderByCalculationDateDesc(Company company);

    /**
     * Trouve les calculs pour une entreprise et une année
     */
    List<VATRecoveryCalculation> findByCompanyAndFiscalYearOrderByCalculationDateDesc(
        Company company,
        Integer fiscalYear
    );

    /**
     * Trouve les calculs pour une entreprise entre deux dates
     */
    @Query("SELECT v FROM VATRecoveryCalculation v WHERE v.company = :company AND v.calculationDate BETWEEN :startDate AND :endDate ORDER BY v.calculationDate DESC")
    List<VATRecoveryCalculation> findByCompanyAndDateRange(
        @Param("company") Company company,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Trouve les calculs liés à une écriture comptable
     */
    @Query("SELECT v FROM VATRecoveryCalculation v WHERE v.generalLedger.id = :generalLedgerId")
    List<VATRecoveryCalculation> findByGeneralLedgerId(@Param("generalLedgerId") Long generalLedgerId);

    /**
     * Trouve les calculs par catégorie
     */
    List<VATRecoveryCalculation> findByCompanyAndRecoveryCategoryOrderByCalculationDateDesc(
        Company company,
        VATRecoverableCategory category
    );

    /**
     * Calcule le total de TVA récupérable pour une entreprise/année
     */
    @Query("SELECT COALESCE(SUM(v.recoverableVat), 0) FROM VATRecoveryCalculation v WHERE v.company.id = :companyId AND v.fiscalYear = :year")
    BigDecimal sumRecoverableVatByCompanyAndYear(@Param("companyId") Long companyId, @Param("year") Integer year);

    /**
     * Calcule le total de TVA non récupérable pour une entreprise/année
     */
    @Query("SELECT COALESCE(SUM(v.nonRecoverableVat), 0) FROM VATRecoveryCalculation v WHERE v.company.id = :companyId AND v.fiscalYear = :year")
    BigDecimal sumNonRecoverableVatByCompanyAndYear(@Param("companyId") Long companyId, @Param("year") Integer year);

    /**
     * Statistiques de récupération par catégorie
     */
    @Query("""
        SELECT v.recoveryCategory as category,
               COUNT(v) as count,
               SUM(v.vatAmount) as totalVat,
               SUM(v.recoverableVat) as totalRecoverable,
               SUM(v.nonRecoverableVat) as totalNonRecoverable
        FROM VATRecoveryCalculation v
        WHERE v.company.id = :companyId AND v.fiscalYear = :year
        GROUP BY v.recoveryCategory
        """)
    List<Map<String, Object>> getStatisticsByCategory(@Param("companyId") Long companyId, @Param("year") Integer year);

    /**
     * Trouve les calculs avec impact prorata significatif (> seuil)
     */
    @Query("""
        SELECT v FROM VATRecoveryCalculation v
        WHERE v.company = :company
        AND v.fiscalYear = :year
        AND v.prorata IS NOT NULL
        AND (v.recoverableByNature - v.recoverableWithProrata) > :threshold
        ORDER BY (v.recoverableByNature - v.recoverableWithProrata) DESC
        """)
    List<VATRecoveryCalculation> findWithSignificantProrataImpact(
        @Param("company") Company company,
        @Param("year") Integer year,
        @Param("threshold") BigDecimal threshold
    );

    /**
     * Trouve les calculs non récupérables (0%)
     */
    @Query("SELECT v FROM VATRecoveryCalculation v WHERE v.company = :company AND v.fiscalYear = :year AND v.recoverableVat = 0 ORDER BY v.vatAmount DESC")
    List<VATRecoveryCalculation> findFullyNonRecoverable(@Param("company") Company company, @Param("year") Integer year);

    /**
     * Trouve les calculs totalement récupérables (100%)
     */
    @Query("SELECT v FROM VATRecoveryCalculation v WHERE v.company = :company AND v.fiscalYear = :year AND v.recoverableVat = v.vatAmount ORDER BY v.vatAmount DESC")
    List<VATRecoveryCalculation> findFullyRecoverable(@Param("company") Company company, @Param("year") Integer year);

    /**
     * Compte les calculs par entreprise et année
     */
    long countByCompanyAndFiscalYear(Company company, Integer fiscalYear);

    /**
     * Calcule le taux de récupération moyen pour une entreprise/année
     */
    @Query("""
        SELECT
            CASE
                WHEN SUM(v.vatAmount) > 0
                THEN (SUM(v.recoverableVat) / SUM(v.vatAmount)) * 100
                ELSE 0
            END
        FROM VATRecoveryCalculation v
        WHERE v.company.id = :companyId AND v.fiscalYear = :year
        """)
    BigDecimal calculateAverageRecoveryRate(@Param("companyId") Long companyId, @Param("year") Integer year);

    /**
     * Trouve les calculs avec faible confiance (< seuil)
     */
    @Query("SELECT v FROM VATRecoveryCalculation v WHERE v.company = :company AND v.fiscalYear = :year AND v.detectionConfidence < :threshold ORDER BY v.detectionConfidence ASC")
    List<VATRecoveryCalculation> findWithLowConfidence(
        @Param("company") Company company,
        @Param("year") Integer year,
        @Param("threshold") Integer threshold
    );

    /**
     * Supprime les calculs pour une écriture comptable
     */
    void deleteByGeneralLedgerId(Long generalLedgerId);
}
