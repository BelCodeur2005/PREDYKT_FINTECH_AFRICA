package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.VATProrata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour les prorata de TVA
 */
@Repository
public interface VATProrataRepository extends JpaRepository<VATProrata, Long> {

    /**
     * Trouve le prorata actif pour une entreprise et une année
     */
    @Query("SELECT p FROM VATProrata p WHERE p.company = :company AND p.fiscalYear = :year AND p.isActive = true")
    Optional<VATProrata> findActiveByCompanyAndYear(@Param("company") Company company, @Param("year") Integer year);

    /**
     * Trouve le prorata actif pour une entreprise et une année (par ID)
     */
    @Query("SELECT p FROM VATProrata p WHERE p.company.id = :companyId AND p.fiscalYear = :year AND p.isActive = true")
    Optional<VATProrata> findActiveByCompanyIdAndYear(@Param("companyId") Long companyId, @Param("year") Integer year);

    /**
     * Trouve tous les prorata (actifs et inactifs) pour une entreprise
     */
    List<VATProrata> findByCompanyOrderByFiscalYearDesc(Company company);

    /**
     * Trouve tous les prorata actifs pour une entreprise
     */
    List<VATProrata> findByCompanyAndIsActiveTrueOrderByFiscalYearDesc(Company company);

    /**
     * Trouve les prorata non verrouillés
     */
    List<VATProrata> findByCompanyAndIsLockedFalseOrderByFiscalYearDesc(Company company);

    /**
     * Vérifie si un prorata actif existe pour une entreprise/année
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM VATProrata p WHERE p.company.id = :companyId AND p.fiscalYear = :year AND p.isActive = true")
    boolean existsActiveByCompanyIdAndYear(@Param("companyId") Long companyId, @Param("year") Integer year);

    /**
     * Trouve les prorata provisoires (qui nécessitent conversion en définitif)
     */
    @Query("SELECT p FROM VATProrata p WHERE p.company = :company AND p.prorataType = 'PROVISIONAL' AND p.isActive = true ORDER BY p.fiscalYear DESC")
    List<VATProrata> findProvisionalByCompany(@Param("company") Company company);

    /**
     * Trouve les prorata nécessitant régularisation (écart > seuil)
     * Prorata provisoire dont le taux diffère de plus de 10% par rapport au taux définitif calculé
     */
    @Query("""
        SELECT p FROM VATProrata p
        WHERE p.company = :company
        AND p.prorataType = 'PROVISIONAL'
        AND p.isActive = true
        AND ABS(p.prorataRate - :definitiveRate) > 0.10
        """)
    List<VATProrata> findNeedingRegularization(
        @Param("company") Company company,
        @Param("definitiveRate") java.math.BigDecimal definitiveRate
    );

    /**
     * Compte les prorata par entreprise
     */
    long countByCompany(Company company);

    /**
     * Trouve le prorata le plus récent pour une entreprise
     */
    Optional<VATProrata> findFirstByCompanyOrderByFiscalYearDesc(Company company);

    /**
     * Désactive tous les prorata actifs pour une année (avant d'en créer un nouveau)
     */
    @Query("UPDATE VATProrata p SET p.isActive = false WHERE p.company.id = :companyId AND p.fiscalYear = :year AND p.isActive = true")
    void deactivateAllForCompanyAndYear(@Param("companyId") Long companyId, @Param("year") Integer year);
}
