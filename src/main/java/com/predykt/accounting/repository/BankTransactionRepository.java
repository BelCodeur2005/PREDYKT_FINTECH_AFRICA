package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.BankTransaction;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.enums.TransactionCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {
    
    // Transactions non réconciliées (pour le rapprochement bancaire)
    List<BankTransaction> findByCompanyAndIsReconciledFalse(Company company);
    
    Page<BankTransaction> findByCompanyAndIsReconciledFalse(Company company, Pageable pageable);
    
    // Transactions par période
    List<BankTransaction> findByCompanyAndTransactionDateBetween(
        Company company,
        LocalDate startDate,
        LocalDate endDate
    );
    
    // Transactions par catégorie
    List<BankTransaction> findByCompanyAndCategory(Company company, TransactionCategory category);
    
    // Recherche par référence bancaire
    Optional<BankTransaction> findByCompanyAndBankReference(Company company, String bankReference);
    
    // Flux de trésorerie
    @Query("SELECT COALESCE(SUM(b.amount), 0) FROM BankTransaction b " +
           "WHERE b.company = :company " +
           "AND b.transactionDate BETWEEN :startDate AND :endDate " +
           "AND b.amount > 0")
    BigDecimal calculateTotalInflows(@Param("company") Company company,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COALESCE(SUM(ABS(b.amount)), 0) FROM BankTransaction b " +
           "WHERE b.company = :company " +
           "AND b.transactionDate BETWEEN :startDate AND :endDate " +
           "AND b.amount < 0")
    BigDecimal calculateTotalOutflows(@Param("company") Company company,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);
    
    // Solde bancaire cumulé
    @Query("SELECT COALESCE(SUM(b.amount), 0) FROM BankTransaction b " +
           "WHERE b.company = :company AND b.transactionDate <= :asOfDate")
    BigDecimal calculateBankBalance(@Param("company") Company company,
                                    @Param("asOfDate") LocalDate asOfDate);
    
    // Statistiques par catégorie
    @Query("SELECT b.category, COUNT(b), COALESCE(SUM(b.amount), 0) " +
           "FROM BankTransaction b " +
           "WHERE b.company = :company " +
           "AND b.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY b.category " +
           "ORDER BY SUM(b.amount) DESC")
    List<Object[]> getCategoryStatistics(@Param("company") Company company,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);
    
    boolean existsByCompanyAndBankReference(Company company, String bankReference);
}