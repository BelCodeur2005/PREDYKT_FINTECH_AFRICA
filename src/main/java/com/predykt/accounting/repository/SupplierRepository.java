package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour les fournisseurs
 */
@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    /**
     * Trouve tous les fournisseurs d'une entreprise
     */
    List<Supplier> findByCompany(Company company);

    /**
     * Trouve les fournisseurs actifs d'une entreprise
     */
    List<Supplier> findByCompanyAndIsActiveTrue(Company company);

    /**
     * Trouve un fournisseur par nom et entreprise
     */
    Optional<Supplier> findByCompanyAndName(Company company, String name);

    /**
     * Trouve un fournisseur par NIU
     */
    Optional<Supplier> findByNiuNumber(String niuNumber);

    /**
     * Trouve les fournisseurs SANS NIU (pour alertes AIR)
     */
    @Query("SELECT s FROM Supplier s WHERE s.company = :company AND s.hasNiu = false AND s.isActive = true")
    List<Supplier> findSuppliersWithoutNiu(@Param("company") Company company);

    /**
     * Trouve les fournisseurs de type "RENT" (loueurs)
     */
    @Query("SELECT s FROM Supplier s WHERE s.company = :company AND s.supplierType = 'RENT' AND s.isActive = true")
    List<Supplier> findRentSuppliers(@Param("company") Company company);

    /**
     * Compte les fournisseurs sans NIU
     */
    @Query("SELECT COUNT(s) FROM Supplier s WHERE s.company = :company AND s.hasNiu = false AND s.isActive = true")
    Long countSuppliersWithoutNiu(@Param("company") Company company);

    /**
     * Vérifie si un fournisseur existe par nom
     */
    boolean existsByCompanyAndName(Company company, String name);

    /**
     * Rechercher des fournisseurs par nom (recherche partielle)
     */
    @Query("SELECT s FROM Supplier s WHERE s.company = :company AND LOWER(s.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY s.name ASC")
    List<Supplier> searchByName(@Param("company") Company company, @Param("searchTerm") String searchTerm);

    /**
     * Trouver les fournisseurs triés par nom
     */
    List<Supplier> findByCompanyOrderByNameAsc(Company company);

    /**
     * Trouver les fournisseurs actifs triés par nom
     */
    List<Supplier> findByCompanyAndIsActiveTrueOrderByNameAsc(Company company);
}
