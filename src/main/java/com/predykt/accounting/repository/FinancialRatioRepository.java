// ============================================
// FinancialRatioRepository.java
// ============================================
package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.FinancialRatio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialRatioRepository extends JpaRepository<FinancialRatio, Long> {
    
    Optional<FinancialRatio> findByCompanyAndFiscalYear(Company company, String fiscalYear);
    
    List<FinancialRatio> findByCompanyOrderByFiscalYearDesc(Company company);
    
    List<FinancialRatio> findByFiscalYear(String fiscalYear);
    
    boolean existsByCompanyAndFiscalYear(Company company, String fiscalYear);
}