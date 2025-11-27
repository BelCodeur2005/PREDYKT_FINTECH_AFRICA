package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.ActivityMappingRule;
import com.predykt.accounting.domain.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityMappingRuleRepository extends JpaRepository<ActivityMappingRule, Long> {

    /**
     * Trouve toutes les règles actives d'une entreprise, triées par priorité décroissante
     */
    List<ActivityMappingRule> findByCompanyAndIsActiveTrueOrderByPriorityDesc(Company company);

    /**
     * Trouve toutes les règles d'une entreprise (actives et inactives)
     */
    List<ActivityMappingRule> findByCompanyOrderByPriorityDesc(Company company);

    /**
     * Trouve une règle par company et keyword exact
     */
    Optional<ActivityMappingRule> findByCompanyAndActivityKeyword(Company company, String activityKeyword);

    /**
     * Vérifie si une règle existe pour un keyword donné
     */
    boolean existsByCompanyAndActivityKeyword(Company company, String activityKeyword);

    /**
     * Compte le nombre de règles actives d'une entreprise
     */
    long countByCompanyAndIsActiveTrue(Company company);

    /**
     * Trouve les règles les plus utilisées d'une entreprise
     */
    @Query("SELECT r FROM ActivityMappingRule r WHERE r.company = :company AND r.isActive = true ORDER BY r.usageCount DESC")
    List<ActivityMappingRule> findMostUsedRulesByCompany(@Param("company") Company company);

    /**
     * Supprime toutes les règles d'une entreprise
     */
    void deleteByCompany(Company company);
}
