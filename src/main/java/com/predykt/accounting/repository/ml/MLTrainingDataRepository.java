package com.predykt.accounting.repository.ml;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.ml.MLTrainingData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository pour MLTrainingData
 * Gère l'accès aux données d'entra înement ML
 *
 * @author PREDYKT ML Team
 */
@Repository
public interface MLTrainingDataRepository extends JpaRepository<MLTrainingData, Long> {

    List<MLTrainingData> findByCompanyOrderByCreatedAtDesc(Company company);

    @Query("SELECT COUNT(t) FROM MLTrainingData t WHERE t.company = :company")
    long countByCompany(@Param("company") Company company);

    @Query("SELECT t FROM MLTrainingData t WHERE t.company = :company " +
           "AND t.createdAt >= :since " +
           "ORDER BY t.createdAt DESC")
    List<MLTrainingData> findRecentByCompany(
        @Param("company") Company company,
        @Param("since") LocalDateTime since
    );

    @Query("SELECT t FROM MLTrainingData t WHERE t.company = :company " +
           "ORDER BY t.createdAt DESC")
    Page<MLTrainingData> findByCompanyPaginated(
        @Param("company") Company company,
        Pageable pageable
    );

    /**
     * Récupère données d'entraînement utilisables (avec label)
     */
    @Query("SELECT t FROM MLTrainingData t WHERE t.company = :company " +
           "AND t.wasAccepted IS NOT NULL " +
           "ORDER BY t.createdAt DESC")
    List<MLTrainingData> findUsableTrainingData(@Param("company") Company company);

    /**
     * Compte les nouvelles données depuis un timestamp
     */
    @Query("SELECT COUNT(t) FROM MLTrainingData t WHERE t.company = :company " +
           "AND t.createdAt >= :since")
    long countNewSamples(@Param("company") Company company, @Param("since") LocalDateTime since);
}