package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    
    Optional<Company> findByEmail(String email);
    
    Optional<Company> findByTaxId(String taxId);
    
    List<Company> findByCountry(String country);
    
    List<Company> findByIsActiveTrue();
    
    @Query("SELECT c FROM Company c WHERE c.isActive = true AND c.country = :country")
    List<Company> findActiveCompaniesByCountry(@Param("country") String country);
    
    boolean existsByEmail(String email);
    
    boolean existsByTaxId(String taxId);
}