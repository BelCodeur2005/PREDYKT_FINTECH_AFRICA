package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.ChartOfAccounts;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChartOfAccountsRepository extends JpaRepository<ChartOfAccounts, Long> {
    
    Optional<ChartOfAccounts> findByCompanyAndAccountNumber(Company company, String accountNumber);
    
    List<ChartOfAccounts> findByCompanyAndIsActiveTrue(Company company);
    
    List<ChartOfAccounts> findByCompanyAndAccountType(Company company, AccountType accountType);
    
    @Query("SELECT c FROM ChartOfAccounts c WHERE c.company = :company " +
           "AND c.accountNumber LIKE :prefix% AND c.isActive = true " +
           "ORDER BY c.accountNumber")
    List<ChartOfAccounts> findByAccountNumberPrefix(@Param("company") Company company, 
                                                     @Param("prefix") String prefix);
    
    @Query("SELECT c FROM ChartOfAccounts c WHERE c.company = :company " +
           "AND c.parentAccount IS NULL AND c.isActive = true " +
           "ORDER BY c.accountNumber")
    List<ChartOfAccounts> findRootAccounts(@Param("company") Company company);
    
    @Query("SELECT c FROM ChartOfAccounts c WHERE c.parentAccount = :parent " +
           "AND c.isActive = true ORDER BY c.accountNumber")
    List<ChartOfAccounts> findSubAccounts(@Param("parent") ChartOfAccounts parent);
    
    boolean existsByCompanyAndAccountNumber(Company company, String accountNumber);
    
    @Query("SELECT COUNT(c) FROM ChartOfAccounts c WHERE c.company = :company")
    long countByCompany(@Param("company") Company company);
}