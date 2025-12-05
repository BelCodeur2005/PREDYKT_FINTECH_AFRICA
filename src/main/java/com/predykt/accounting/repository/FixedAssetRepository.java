package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.FixedAsset;
import com.predykt.accounting.domain.enums.AssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des immobilisations (actifs fixes)
 * Conforme OHADA et multi-tenant
 */
@Repository
public interface FixedAssetRepository extends JpaRepository<FixedAsset, Long> {

    // ========== RECHERCHE PAR ENTREPRISE ==========

    /**
     * Trouver toutes les immobilisations d'une entreprise
     */
    List<FixedAsset> findByCompany(Company company);

    /**
     * Trouver toutes les immobilisations actives d'une entreprise
     */
    List<FixedAsset> findByCompanyAndIsActiveTrue(Company company);

    /**
     * Trouver toutes les immobilisations cédées d'une entreprise
     */
    List<FixedAsset> findByCompanyAndIsActiveFalse(Company company);

    // ========== RECHERCHE PAR NUMÉRO ==========

    /**
     * Trouver une immobilisation par son numéro unique
     */
    Optional<FixedAsset> findByCompanyAndAssetNumber(Company company, String assetNumber);

    /**
     * Vérifier si un numéro d'immobilisation existe déjà
     */
    boolean existsByCompanyAndAssetNumber(Company company, String assetNumber);

    // ========== RECHERCHE PAR CATÉGORIE ==========

    /**
     * Trouver les immobilisations par catégorie
     */
    List<FixedAsset> findByCompanyAndCategory(Company company, AssetCategory category);

    /**
     * Trouver les immobilisations actives par catégorie
     */
    List<FixedAsset> findByCompanyAndCategoryAndIsActiveTrue(Company company, AssetCategory category);

    // ========== RECHERCHE PAR COMPTE COMPTABLE ==========

    /**
     * Trouver les immobilisations par compte OHADA
     */
    List<FixedAsset> findByCompanyAndAccountNumber(Company company, String accountNumber);

    /**
     * Trouver les immobilisations par préfixe de compte (ex: "24" pour tous les matériels)
     */
    List<FixedAsset> findByCompanyAndAccountNumberStartingWith(Company company, String accountPrefix);

    // ========== RECHERCHE PAR DATE ==========

    /**
     * Trouver les immobilisations acquises entre deux dates
     */
    List<FixedAsset> findByCompanyAndAcquisitionDateBetween(
        Company company, LocalDate startDate, LocalDate endDate);

    /**
     * Trouver les immobilisations cédées entre deux dates
     */
    @Query("SELECT fa FROM FixedAsset fa WHERE fa.company = :company " +
           "AND fa.disposalDate BETWEEN :startDate AND :endDate")
    List<FixedAsset> findDisposedBetween(
        @Param("company") Company company,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    // ========== RECHERCHE POUR AMORTISSEMENTS ==========

    /**
     * Trouver toutes les immobilisations amortissables actives
     * (exclu terrains et immobilisations financières)
     */
    @Query("SELECT fa FROM FixedAsset fa WHERE fa.company = :company " +
           "AND fa.isActive = true " +
           "AND fa.isFullyDepreciated = false " +
           "AND fa.category NOT IN ('LAND', 'FINANCIAL')")
    List<FixedAsset> findDepreciableAssets(@Param("company") Company company);

    /**
     * Trouver les immobilisations amortissables pour une année fiscale donnée
     * (acquises avant la fin de l'année et non encore cédées)
     */
    @Query("SELECT fa FROM FixedAsset fa WHERE fa.company = :company " +
           "AND fa.acquisitionDate <= :fiscalYearEnd " +
           "AND (fa.disposalDate IS NULL OR fa.disposalDate > :fiscalYearStart) " +
           "AND fa.category NOT IN ('LAND', 'FINANCIAL')")
    List<FixedAsset> findDepreciableForFiscalYear(
        @Param("company") Company company,
        @Param("fiscalYearStart") LocalDate fiscalYearStart,
        @Param("fiscalYearEnd") LocalDate fiscalYearEnd);

    /**
     * Trouver les immobilisations totalement amorties
     */
    List<FixedAsset> findByCompanyAndIsFullyDepreciatedTrue(Company company);

    // ========== RECHERCHE PAR LOCALISATION ==========

    /**
     * Trouver les immobilisations par lieu d'affectation
     */
    List<FixedAsset> findByCompanyAndLocation(Company company, String location);

    /**
     * Trouver les immobilisations par département
     */
    List<FixedAsset> findByCompanyAndDepartment(Company company, String department);

    /**
     * Trouver les immobilisations par responsable
     */
    List<FixedAsset> findByCompanyAndResponsiblePerson(Company company, String responsiblePerson);

    // ========== STATISTIQUES ==========

    /**
     * Compter les immobilisations actives par catégorie
     */
    @Query("SELECT fa.category, COUNT(fa) FROM FixedAsset fa " +
           "WHERE fa.company = :company AND fa.isActive = true " +
           "GROUP BY fa.category")
    List<Object[]> countByCategory(@Param("company") Company company);

    /**
     * Calculer la valeur totale des immobilisations actives
     */
    @Query("SELECT COALESCE(SUM(fa.totalCost), 0) FROM FixedAsset fa " +
           "WHERE fa.company = :company AND fa.isActive = true")
    java.math.BigDecimal getTotalAssetValue(@Param("company") Company company);

    /**
     * Calculer la valeur totale par catégorie
     */
    @Query("SELECT fa.category, COALESCE(SUM(fa.totalCost), 0) FROM FixedAsset fa " +
           "WHERE fa.company = :company AND fa.isActive = true " +
           "GROUP BY fa.category")
    List<Object[]> getTotalValueByCategory(@Param("company") Company company);

    // ========== RECHERCHE POUR RAPPORTS ==========

    /**
     * Trouver les immobilisations pour le tableau d'amortissements d'un exercice
     * Inclut les acquisitions et cessions de l'année
     */
    @Query("SELECT fa FROM FixedAsset fa WHERE fa.company = :company " +
           "AND (" +
           "  (fa.acquisitionDate <= :fiscalYearEnd AND fa.isActive = true) " +
           "  OR (fa.disposalDate BETWEEN :fiscalYearStart AND :fiscalYearEnd)" +
           ") " +
           "ORDER BY fa.category, fa.acquisitionDate")
    List<FixedAsset> findForDepreciationSchedule(
        @Param("company") Company company,
        @Param("fiscalYearStart") LocalDate fiscalYearStart,
        @Param("fiscalYearEnd") LocalDate fiscalYearEnd);

    /**
     * Trouver les nouvelles acquisitions d'un exercice
     */
    @Query("SELECT fa FROM FixedAsset fa WHERE fa.company = :company " +
           "AND fa.acquisitionDate BETWEEN :fiscalYearStart AND :fiscalYearEnd " +
           "ORDER BY fa.acquisitionDate")
    List<FixedAsset> findAcquisitionsDuringYear(
        @Param("company") Company company,
        @Param("fiscalYearStart") LocalDate fiscalYearStart,
        @Param("fiscalYearEnd") LocalDate fiscalYearEnd);

    /**
     * Trouver les cessions d'un exercice
     */
    @Query("SELECT fa FROM FixedAsset fa WHERE fa.company = :company " +
           "AND fa.disposalDate BETWEEN :fiscalYearStart AND :fiscalYearEnd " +
           "ORDER BY fa.disposalDate")
    List<FixedAsset> findDisposalsDuringYear(
        @Param("company") Company company,
        @Param("fiscalYearStart") LocalDate fiscalYearStart,
        @Param("fiscalYearEnd") LocalDate fiscalYearEnd);
}
