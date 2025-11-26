package com.predykt.accounting.repository;

import com.predykt.accounting.config.TenantContextHolder;
import com.predykt.accounting.domain.entity.TenantMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité TenantMetadata
 */
@Repository
public interface TenantMetadataRepository extends JpaRepository<TenantMetadata, Long> {

    /**
     * Trouve un tenant par son ID
     */
    Optional<TenantMetadata> findByTenantId(String tenantId);

    /**
     * Trouve tous les tenants par mode
     */
    List<TenantMetadata> findByTenantMode(TenantContextHolder.TenantMode tenantMode);

    /**
     * Trouve tous les tenants actifs
     */
    List<TenantMetadata> findByIsActiveTrue();

    /**
     * Trouve tous les tenants actifs par mode
     */
    List<TenantMetadata> findByTenantModeAndIsActiveTrue(TenantContextHolder.TenantMode tenantMode);

    /**
     * Trouve le tenant associé à une entreprise (MODE DEDICATED)
     */
    Optional<TenantMetadata> findByCompanyId(Long companyId);

    /**
     * Trouve le tenant associé à un cabinet (MODE CABINET)
     */
    Optional<TenantMetadata> findByCabinetId(Long cabinetId);

    /**
     * Trouve un tenant par sous-domaine
     */
    Optional<TenantMetadata> findBySubdomain(String subdomain);

    /**
     * Trouve un tenant par domaine personnalisé
     */
    Optional<TenantMetadata> findByCustomDomain(String customDomain);

    /**
     * Vérifie si un tenant ID existe
     */
    boolean existsByTenantId(String tenantId);

    /**
     * Vérifie si un sous-domaine existe
     */
    boolean existsBySubdomain(String subdomain);

    /**
     * Vérifie si un domaine personnalisé existe
     */
    boolean existsByCustomDomain(String customDomain);

    /**
     * Compte le nombre de tenants par mode
     */
    @Query("SELECT COUNT(tm) FROM TenantMetadata tm WHERE tm.tenantMode = :mode")
    Long countByTenantMode(@Param("mode") TenantContextHolder.TenantMode mode);

    /**
     * Compte le nombre de tenants actifs
     */
    Long countByIsActiveTrue();

    /**
     * Trouve tous les tenants DEDICATED actifs
     */
    @Query("SELECT tm FROM TenantMetadata tm " +
           "WHERE tm.tenantMode = 'DEDICATED' " +
           "AND tm.isActive = true " +
           "AND tm.company IS NOT NULL")
    List<TenantMetadata> findActiveDedicatedTenants();

    /**
     * Trouve tous les tenants CABINET actifs
     */
    @Query("SELECT tm FROM TenantMetadata tm " +
           "WHERE tm.tenantMode = 'CABINET' " +
           "AND tm.isActive = true " +
           "AND tm.cabinet IS NOT NULL")
    List<TenantMetadata> findActiveCabinetTenants();
}
