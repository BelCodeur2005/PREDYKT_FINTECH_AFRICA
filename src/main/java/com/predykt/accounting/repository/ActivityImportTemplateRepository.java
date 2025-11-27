package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.ActivityImportTemplate;
import com.predykt.accounting.domain.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityImportTemplateRepository extends JpaRepository<ActivityImportTemplate, Long> {
    /**
     * Trouve tous les templates actifs d'une entreprise
     */
    List<ActivityImportTemplate> findByCompanyAndIsActiveTrue(Company company);

    /**
     * Trouve tous les templates d'une entreprise
     */
    List<ActivityImportTemplate> findByCompany(Company company);

    /**
     * Trouve le template par défaut d'une entreprise
     */
    Optional<ActivityImportTemplate> findByCompanyAndIsDefaultTrue(Company company);

    /**
     * Trouve un template par nom
     */
    Optional<ActivityImportTemplate> findByCompanyAndTemplateName(Company company, String templateName);

    /**
     * Vérifie si un template avec ce nom existe
     */
    boolean existsByCompanyAndTemplateName(Company company, String templateName);

    /**
     * Compte le nombre de templates actifs d'une entreprise
     */
    long countByCompanyAndIsActiveTrue(Company company);

    /**
     * Trouve les templates les plus utilisés
     */
    @Query("SELECT t FROM ActivityImportTemplate t WHERE t.company = :company AND t.isActive = true ORDER BY t.usageCount DESC")
    List<ActivityImportTemplate> findMostUsedTemplatesByCompany(@Param("company") Company company);

    /**
     * Supprime tous les templates d'une entreprise
     */
    void deleteByCompany(Company company);
}
