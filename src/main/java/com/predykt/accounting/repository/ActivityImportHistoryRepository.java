package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.ActivityImportHistory;
import com.predykt.accounting.domain.entity.ActivityImportTemplate;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.enums.ImportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityImportHistoryRepository extends JpaRepository<ActivityImportHistory, Long> {

    /**
     * Trouve l'historique d'une entreprise, trié par date décroissante
     */
    List<ActivityImportHistory> findByCompanyOrderByCreatedAtDesc(Company company);

    /**
     * Trouve l'historique d'une entreprise pour un template donné
     */
    List<ActivityImportHistory> findByCompanyAndTemplateOrderByCreatedAtDesc(Company company, ActivityImportTemplate template);

    /**
     * Trouve l'historique d'une entreprise par statut
     */
    List<ActivityImportHistory> findByCompanyAndStatus(Company company, ImportStatus status);

    /**
     * Trouve l'historique récent (X derniers jours)
     */
    @Query("SELECT h FROM ActivityImportHistory h WHERE h.company = :company AND h.createdAt >= :since ORDER BY h.createdAt DESC")
    List<ActivityImportHistory> findRecentHistory(@Param("company") Company company, @Param("since") LocalDateTime since);

    /**
     * Compte le nombre total d'imports réussis d'une entreprise
     */
    long countByCompanyAndStatus(Company company, ImportStatus status);

    /**
     * Calcule le nombre total de lignes importées pour une entreprise
     */
    @Query("SELECT COALESCE(SUM(h.successCount), 0) FROM ActivityImportHistory h WHERE h.company = :company AND h.status = 'COMPLETED'")
    Long countTotalImportedRows(@Param("company") Company company);

    /**
     * Supprime tout l'historique d'une entreprise
     */
    void deleteByCompany(Company company);
}
