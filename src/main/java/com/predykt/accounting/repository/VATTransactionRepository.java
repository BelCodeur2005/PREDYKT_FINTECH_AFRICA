package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.VATTransaction;
import com.predykt.accounting.domain.enums.VATRecoverableCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository pour les transactions de TVA avec gestion de la récupérabilité
 */
@Repository
public interface VATTransactionRepository extends JpaRepository<VATTransaction, Long> {

    /**
     * Trouve toutes les transactions d'une entreprise
     */
    List<VATTransaction> findByCompany(Company company);

    /**
     * Trouve les transactions pour une période donnée
     */
    @Query("SELECT vt FROM VATTransaction vt WHERE vt.company = :company AND vt.transactionDate BETWEEN :startDate AND :endDate ORDER BY vt.transactionDate")
    List<VATTransaction> findByCompanyAndPeriod(
        @Param("company") Company company,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Trouve les transactions avec TVA non récupérable
     */
    @Query("SELECT vt FROM VATTransaction vt WHERE vt.company = :company AND vt.nonRecoverableVatAmount > 0 ORDER BY vt.transactionDate DESC")
    List<VATTransaction> findNonRecoverableTransactions(@Param("company") Company company);

    /**
     * Trouve les transactions par catégorie de récupérabilité
     */
    List<VATTransaction> findByCompanyAndRecoverableCategory(
        Company company,
        VATRecoverableCategory category
    );

    /**
     * Somme de la TVA récupérable pour une période
     */
    @Query("SELECT SUM(vt.recoverableVatAmount) FROM VATTransaction vt " +
           "WHERE vt.company = :company " +
           "AND vt.transactionDate BETWEEN :startDate AND :endDate " +
           "AND vt.vatAccountType IN :vatAccountTypes")
    BigDecimal sumRecoverableVatByPeriodAndAccounts(
        @Param("company") Company company,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("vatAccountTypes") List<String> vatAccountTypes
    );

    /**
     * Somme de la TVA non récupérable pour une période
     */
    @Query("SELECT SUM(vt.nonRecoverableVatAmount) FROM VATTransaction vt " +
           "WHERE vt.company = :company " +
           "AND vt.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumNonRecoverableVatByPeriod(
        @Param("company") Company company,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Somme de la TVA récupérable par type de compte pour une période
     */
    @Query("SELECT SUM(vt.recoverableVatAmount) FROM VATTransaction vt " +
           "WHERE vt.company = :company " +
           "AND vt.transactionDate BETWEEN :startDate AND :endDate " +
           "AND vt.vatAccountType = :vatAccountType")
    BigDecimal sumRecoverableVatByAccountType(
        @Param("company") Company company,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("vatAccountType") String vatAccountType
    );

    /**
     * Statistiques de TVA non récupérable par catégorie
     */
    @Query("SELECT vt.recoverableCategory, SUM(vt.nonRecoverableVatAmount), COUNT(vt) " +
           "FROM VATTransaction vt " +
           "WHERE vt.company = :company " +
           "AND vt.transactionDate BETWEEN :startDate AND :endDate " +
           "AND vt.nonRecoverableVatAmount > 0 " +
           "GROUP BY vt.recoverableCategory")
    List<Object[]> getNonRecoverableVatStatistics(
        @Param("company") Company company,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Compte les transactions avec alertes
     */
    @Query("SELECT COUNT(vt) FROM VATTransaction vt WHERE vt.company = :company AND vt.hasAlert = true")
    Long countTransactionsWithAlerts(@Param("company") Company company);

    /**
     * Trouve les transactions avec alertes
     */
    @Query("SELECT vt FROM VATTransaction vt WHERE vt.company = :company AND vt.hasAlert = true ORDER BY vt.transactionDate DESC")
    List<VATTransaction> findTransactionsWithAlerts(@Param("company") Company company);

    /**
     * Somme totale de la TVA collectée pour une période
     */
    @Query("SELECT SUM(vt.vatAmount) FROM VATTransaction vt " +
           "WHERE vt.company = :company " +
           "AND vt.transactionType = 'SALE' " +
           "AND vt.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumVatCollected(
        @Param("company") Company company,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Somme totale de la TVA déductible (récupérable uniquement) pour une période
     */
    @Query("SELECT SUM(vt.recoverableVatAmount) FROM VATTransaction vt " +
           "WHERE vt.company = :company " +
           "AND vt.transactionType = 'PURCHASE' " +
           "AND vt.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumRecoverableVatDeductible(
        @Param("company") Company company,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
