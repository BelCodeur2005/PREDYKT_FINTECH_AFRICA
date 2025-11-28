package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.Budget;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.ChartOfAccounts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    // Recherche par entreprise
    List<Budget> findByCompany(Company company);

    List<Budget> findByCompanyOrderByPeriodStartDesc(Company company);

    // Recherche par année fiscale
    List<Budget> findByCompanyAndFiscalYear(Company company, String fiscalYear);

    // Recherche par type de budget
    List<Budget> findByCompanyAndBudgetType(Company company, Budget.BudgetType budgetType);

    // Recherche par compte
    List<Budget> findByCompanyAndAccount(Company company, ChartOfAccounts account);

    // Recherche par période
    List<Budget> findByCompanyAndPeriodStartBetween(Company company, LocalDate startDate, LocalDate endDate);

    @Query("SELECT b FROM Budget b WHERE b.company = :company AND b.periodStart <= :date AND b.periodEnd >= :date")
    List<Budget> findByCompanyAndDate(@Param("company") Company company, @Param("date") LocalDate date);

    // Recherche budgets dépassés
    @Query("SELECT b FROM Budget b WHERE b.company = :company AND b.variance > 0 AND b.isLocked = false")
    List<Budget> findOverBudgetByCompany(@Param("company") Company company);

    // Recherche budgets avec variance significative (> seuil)
    @Query("SELECT b FROM Budget b WHERE b.company = :company AND ABS(b.variancePct) > :threshold")
    List<Budget> findBudgetsWithHighVariance(@Param("company") Company company, @Param("threshold") BigDecimal threshold);

    // Vérifier existence
    boolean existsByCompanyAndAccountAndPeriodStartAndPeriodEnd(
        Company company,
        ChartOfAccounts account,
        LocalDate periodStart,
        LocalDate periodEnd
    );

    // Statistiques budgétaires
    @Query("SELECT SUM(b.budgetedAmount) FROM Budget b WHERE b.company = :company AND b.fiscalYear = :fiscalYear")
    BigDecimal sumBudgetedAmountByCompanyAndYear(@Param("company") Company company, @Param("fiscalYear") String fiscalYear);

    @Query("SELECT SUM(b.actualAmount) FROM Budget b WHERE b.company = :company AND b.fiscalYear = :fiscalYear")
    BigDecimal sumActualAmountByCompanyAndYear(@Param("company") Company company, @Param("fiscalYear") String fiscalYear);

    @Query("SELECT SUM(b.variance) FROM Budget b WHERE b.company = :company AND b.fiscalYear = :fiscalYear")
    BigDecimal sumVarianceByCompanyAndYear(@Param("company") Company company, @Param("fiscalYear") String fiscalYear);

    // Compter budgets verrouillés
    long countByCompanyAndIsLocked(Company company, Boolean isLocked);
}
