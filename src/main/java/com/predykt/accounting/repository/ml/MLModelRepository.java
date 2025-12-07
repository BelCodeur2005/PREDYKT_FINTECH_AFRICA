package com.predykt.accounting.repository.ml;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.ml.MLModel;
import com.predykt.accounting.domain.enums.MLModelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour MLModel
 * Gère les modèles ML (versioning, déploiement)
 *
 * @author PREDYKT ML Team
 */
@Repository
public interface MLModelRepository extends JpaRepository<MLModel, Long> {

    /**
     * Trouve le modèle actif pour une entreprise
     */
    Optional<MLModel> findByCompanyAndIsActiveTrue(Company company);

    /**
     * Liste tous les modèles d'une entreprise par date
     */
    List<MLModel> findByCompanyOrderByCreatedAtDesc(Company company);

    /**
     * Trouve les modèles par statut
     */
    List<MLModel> findByCompanyAndStatus(Company company, MLModelStatus status);

    /**
     * Historique des versions d'un modèle
     */
    @Query("SELECT m FROM MLModel m WHERE m.company = :company " +
           "AND m.modelName = :modelName " +
           "ORDER BY m.createdAt DESC")
    List<MLModel> findVersionHistory(
        @Param("company") Company company,
        @Param("modelName") String modelName
    );

    /**
     * Tous les modèles actifs (toutes entreprises)
     */
    @Query("SELECT m FROM MLModel m WHERE m.isActive = true " +
           "AND m.status = 'DEPLOYED' " +
           "ORDER BY m.accuracy DESC")
    List<MLModel> findAllActiveModels();

    /**
     * Meilleur modèle disponible pour une entreprise
     */
    @Query("SELECT m FROM MLModel m WHERE m.company = :company " +
           "AND m.status IN ('TRAINED', 'DEPLOYED') " +
           "ORDER BY m.accuracy DESC, m.f1Score DESC")
    List<MLModel> findBestModels(@Param("company") Company company);
}