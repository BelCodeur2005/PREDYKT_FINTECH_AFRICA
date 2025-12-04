package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.RecoverabilityRule;
import com.predykt.accounting.domain.enums.VATRecoverableCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour les règles de récupérabilité
 */
@Repository
public interface RecoverabilityRuleRepository extends JpaRepository<RecoverabilityRule, Long> {

    /**
     * Trouve toutes les règles actives triées par priorité
     */
    List<RecoverabilityRule> findByIsActiveTrueOrderByPriorityAsc();

    /**
     * Trouve les règles applicables pour une entreprise (MULTI-TENANT)
     * Inclut les règles GLOBAL + règles spécifiques selon le mode
     *
     * Mode SHARED: GLOBAL + COMPANY (pour cette company)
     * Mode DEDICATED: GLOBAL + TENANT (pour ce tenant)
     * Mode CABINET: GLOBAL + CABINET (pour ce cabinet) + COMPANY (pour cette company)
     */
    @Query("""
        SELECT r FROM RecoverabilityRule r
        WHERE r.isActive = true
        AND (
            r.scopeType = 'GLOBAL'
            OR (r.scopeType = 'COMPANY' AND r.company.id = :companyId)
            OR (r.scopeType = 'TENANT' AND r.scopeId = :tenantId)
            OR (r.scopeType = 'CABINET' AND r.scopeId = :cabinetId)
        )
        ORDER BY r.priority ASC
        """)
    List<RecoverabilityRule> findApplicableRulesForContext(
        @Param("companyId") Long companyId,
        @Param("tenantId") String tenantId,
        @Param("cabinetId") String cabinetId
    );

    /**
     * Trouve les règles actives par type
     */
    List<RecoverabilityRule> findByIsActiveTrueAndRuleTypeOrderByPriorityAsc(String ruleType);

    /**
     * Trouve les règles actives par catégorie
     */
    List<RecoverabilityRule> findByIsActiveTrueAndCategoryOrderByPriorityAsc(VATRecoverableCategory category);

    /**
     * Trouve les règles qui nécessitent une révision
     */
    @Query("SELECT r FROM RecoverabilityRule r WHERE r.isActive = true AND (r.correctionCount >= 5 OR (r.matchCount >= 20 AND r.accuracyRate < 70)) ORDER BY r.accuracyRate ASC")
    List<RecoverabilityRule> findRulesNeedingReview();

    /**
     * Trouve les règles les plus performantes
     */
    @Query("SELECT r FROM RecoverabilityRule r WHERE r.isActive = true AND r.matchCount >= 10 ORDER BY r.accuracyRate DESC, r.matchCount DESC")
    List<RecoverabilityRule> findTopPerformingRules();

    /**
     * Compte les règles actives
     */
    Long countByIsActiveTrue();

    /**
     * Trouve les règles par pattern de compte
     */
    @Query("SELECT r FROM RecoverabilityRule r WHERE r.isActive = true AND (r.accountPattern IS NULL OR :accountNumber LIKE CONCAT(r.accountPattern, '%')) ORDER BY r.priority ASC")
    List<RecoverabilityRule> findApplicableRulesForAccount(@Param("accountNumber") String accountNumber);
}
