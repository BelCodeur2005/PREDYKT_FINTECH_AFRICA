package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.TaxConfiguration;
import com.predykt.accounting.domain.enums.TaxType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour la configuration des taxes
 */
@Repository
public interface TaxConfigurationRepository extends JpaRepository<TaxConfiguration, Long> {

    /**
     * Trouve toutes les configurations fiscales d'une entreprise
     */
    List<TaxConfiguration> findByCompany(Company company);

    /**
     * Trouve les configurations actives d'une entreprise
     */
    List<TaxConfiguration> findByCompanyAndIsActiveTrue(Company company);

    /**
     * Trouve une configuration par type de taxe
     */
    Optional<TaxConfiguration> findByCompanyAndTaxType(Company company, TaxType taxType);

    /**
     * Trouve les taxes applicables aux ventes
     */
    @Query("SELECT tc FROM TaxConfiguration tc WHERE tc.company = :company AND tc.isActive = true AND tc.applyToSales = true")
    List<TaxConfiguration> findActiveSalesTaxes(@Param("company") Company company);

    /**
     * Trouve les taxes applicables aux achats
     */
    @Query("SELECT tc FROM TaxConfiguration tc WHERE tc.company = :company AND tc.isActive = true AND tc.applyToPurchases = true")
    List<TaxConfiguration> findActivePurchaseTaxes(@Param("company") Company company);

    /**
     * Trouve les taxes avec calcul automatique activé
     */
    @Query("SELECT tc FROM TaxConfiguration tc WHERE tc.company = :company AND tc.isActive = true AND tc.isAutomatic = true")
    List<TaxConfiguration> findAutomaticTaxes(@Param("company") Company company);

    /**
     * Vérifie si une entreprise a la TVA activée
     */
    @Query("SELECT CASE WHEN COUNT(tc) > 0 THEN true ELSE false END FROM TaxConfiguration tc WHERE tc.company = :company AND tc.taxType = 'VAT' AND tc.isActive = true")
    boolean hasVATEnabled(@Param("company") Company company);

    /**
     * Vérifie si une configuration existe pour ce type de taxe
     */
    boolean existsByCompanyAndTaxType(Company company, TaxType taxType);
}
