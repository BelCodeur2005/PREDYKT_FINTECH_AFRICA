package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Trouver tous les clients d'une entreprise
     */
    List<Customer> findByCompanyOrderByNameAsc(Company company);

    /**
     * Trouver les clients actifs d'une entreprise
     */
    List<Customer> findByCompanyAndIsActiveTrueOrderByNameAsc(Company company);

    /**
     * Trouver un client par nom dans une entreprise (insensible à la casse)
     */
    Optional<Customer> findByCompanyAndNameIgnoreCase(Company company, String name);

    /**
     * Trouver un client par NIU dans une entreprise
     */
    Optional<Customer> findByCompanyAndNiuNumber(Company company, String niuNumber);

    /**
     * Rechercher des clients par nom (recherche partielle)
     */
    @Query("SELECT c FROM Customer c WHERE c.company = :company AND LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY c.name ASC")
    List<Customer> searchByName(@Param("company") Company company, @Param("searchTerm") String searchTerm);

    /**
     * Compter les clients actifs d'une entreprise
     */
    long countByCompanyAndIsActiveTrue(Company company);

    /**
     * Trouver les clients par type
     */
    List<Customer> findByCompanyAndCustomerTypeOrderByNameAsc(Company company, String customerType);

    /**
     * Trouver les clients avec NIU
     */
    List<Customer> findByCompanyAndHasNiuTrueOrderByNameAsc(Company company);

    /**
     * Vérifier si un client existe par nom dans une entreprise
     */
    boolean existsByCompanyAndNameIgnoreCase(Company company, String name);
}
