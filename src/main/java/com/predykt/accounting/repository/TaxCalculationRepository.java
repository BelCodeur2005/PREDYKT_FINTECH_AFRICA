package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.Supplier;
import com.predykt.accounting.domain.entity.TaxCalculation;
import com.predykt.accounting.domain.enums.TaxType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository pour l'historique des calculs fiscaux
 */
@Repository
public interface TaxCalculationRepository extends JpaRepository<TaxCalculation, Long> {

    /**
     * Trouve tous les calculs d'une entreprise
     */
    List<TaxCalculation> findByCompany(Company company);

    /**
     * Trouve les calculs pour une période donnée
     */
    @Query("SELECT tc FROM TaxCalculation tc WHERE tc.company = :company AND tc.calculationDate BETWEEN :startDate AND :endDate")
    List<TaxCalculation> findByCompanyAndPeriod(
        @Param("company") Company company,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Trouve les calculs par type de taxe
     */
    List<TaxCalculation> findByCompanyAndTaxType(Company company, TaxType taxType);

    /**
     * Trouve les calculs avec alertes
     */
    @Query("SELECT tc FROM TaxCalculation tc WHERE tc.company = :company AND tc.hasAlert = true ORDER BY tc.calculationDate DESC")
    List<TaxCalculation> findCalculationsWithAlerts(@Param("company") Company company);

    /**
     * Trouve les calculs AIR sans NIU (alertes)
     */
    @Query("SELECT tc FROM TaxCalculation tc WHERE tc.company = :company AND tc.taxType = 'AIR_WITHOUT_NIU' AND tc.hasAlert = true ORDER BY tc.calculationDate DESC")
    List<TaxCalculation> findAIRWithoutNiuAlerts(@Param("company") Company company);

    /**
     * Somme des taxes par type pour une période
     */
    @Query("SELECT SUM(tc.taxAmount) FROM TaxCalculation tc WHERE tc.company = :company AND tc.taxType = :taxType AND tc.calculationDate BETWEEN :startDate AND :endDate")
    BigDecimal sumTaxAmountByTypeAndPeriod(
        @Param("company") Company company,
        @Param("taxType") TaxType taxType,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Trouve les calculs pour un fournisseur
     */
    List<TaxCalculation> findBySupplier(Supplier supplier);

    /**
     * Compte le nombre de calculs avec alertes
     */
    @Query("SELECT COUNT(tc) FROM TaxCalculation tc WHERE tc.company = :company AND tc.hasAlert = true")
    Long countAlertsForCompany(@Param("company") Company company);

    /**
     * Récupère le résumé mensuel des taxes
     */
    @Query("""
        SELECT tc.taxType as taxType,
               FUNCTION('TO_CHAR', tc.calculationDate, 'YYYY-MM') as month,
               SUM(tc.taxAmount) as totalAmount,
               COUNT(tc) as transactionCount
        FROM TaxCalculation tc
        WHERE tc.company = :company
          AND tc.calculationDate BETWEEN :startDate AND :endDate
        GROUP BY tc.taxType, FUNCTION('TO_CHAR', tc.calculationDate, 'YYYY-MM')
        ORDER BY FUNCTION('TO_CHAR', tc.calculationDate, 'YYYY-MM') DESC
    """)
    List<Object[]> getMonthlyTaxSummary(
        @Param("company") Company company,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Trouve les calculs non encore postés en comptabilité
     */
    @Query("SELECT tc FROM TaxCalculation tc WHERE tc.company = :company AND tc.status = 'CALCULATED' ORDER BY tc.calculationDate")
    List<TaxCalculation> findUnpostedCalculations(@Param("company") Company company);
}
