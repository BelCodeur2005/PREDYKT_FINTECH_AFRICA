package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.ChartOfAccounts;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.GeneralLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface GeneralLedgerRepository extends JpaRepository<GeneralLedger, Long> {
    
    // Recherche par entreprise et période
    List<GeneralLedger> findByCompanyAndEntryDateBetween(
        Company company, 
        LocalDate startDate, 
        LocalDate endDate
    );
    
    Page<GeneralLedger> findByCompanyAndEntryDateBetween(
        Company company, 
        LocalDate startDate, 
        LocalDate endDate, 
        Pageable pageable
    );
    
    // Recherche par compte
    List<GeneralLedger> findByAccountAndEntryDateBetween(
        ChartOfAccounts account, 
        LocalDate startDate, 
        LocalDate endDate
    );
    
    // Écritures non verrouillées
    @Query("SELECT g FROM GeneralLedger g WHERE g.company = :company " +
           "AND g.isLocked = false ORDER BY g.entryDate DESC")
    List<GeneralLedger> findUnlockedEntries(@Param("company") Company company);
    
    // Calcul du solde d'un compte
    @Query("SELECT COALESCE(SUM(g.debitAmount), 0) - COALESCE(SUM(g.creditAmount), 0) " +
           "FROM GeneralLedger g WHERE g.account = :account " +
           "AND g.entryDate <= :asOfDate")
    BigDecimal calculateAccountBalance(@Param("account") ChartOfAccounts account, 
                                       @Param("asOfDate") LocalDate asOfDate);
    
    // Solde d'un compte sur une période
    @Query("SELECT COALESCE(SUM(g.debitAmount), 0) - COALESCE(SUM(g.creditAmount), 0) " +
           "FROM GeneralLedger g WHERE g.account = :account " +
           "AND g.entryDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateAccountBalanceBetween(@Param("account") ChartOfAccounts account,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);
    
    // Total débits et crédits pour validation
    @Query("SELECT SUM(g.debitAmount) FROM GeneralLedger g " +
           "WHERE g.company = :company AND g.reference = :reference")
    BigDecimal sumDebitsByReference(@Param("company") Company company, 
                                    @Param("reference") String reference);
    
    @Query("SELECT SUM(g.creditAmount) FROM GeneralLedger g " +
           "WHERE g.company = :company AND g.reference = :reference")
    BigDecimal sumCreditsByReference(@Param("company") Company company, 
                                     @Param("reference") String reference);
    
    // Balance de vérification
    @Query("SELECT g.account.accountNumber as accountNumber, " +
           "g.account.accountName as accountName, " +
           "SUM(g.debitAmount) as totalDebit, " +
           "SUM(g.creditAmount) as totalCredit " +
           "FROM GeneralLedger g " +
           "WHERE g.company = :company " +
           "AND g.entryDate BETWEEN :startDate AND :endDate " +
           "GROUP BY g.account.accountNumber, g.account.accountName " +
           "ORDER BY g.account.accountNumber")
    List<Object[]> getTrialBalance(@Param("company") Company company,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);
    
    // Recherche par journal
    List<GeneralLedger> findByCompanyAndJournalCodeAndEntryDateBetween(
        Company company,
        String journalCode,
        LocalDate startDate,
        LocalDate endDate
    );
}