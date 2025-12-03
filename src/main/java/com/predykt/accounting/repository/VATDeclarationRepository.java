package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.VATDeclaration;
import com.predykt.accounting.domain.enums.VATDeclarationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour les déclarations de TVA
 */
@Repository
public interface VATDeclarationRepository extends JpaRepository<VATDeclaration, Long> {

    /**
     * Trouve toutes les déclarations d'une entreprise (triées par période)
     */
    List<VATDeclaration> findByCompanyOrderByFiscalPeriodDesc(Company company);

    /**
     * Trouve une déclaration par période et type
     */
    Optional<VATDeclaration> findByCompanyAndFiscalPeriodAndDeclarationType(
        Company company,
        String fiscalPeriod,
        VATDeclarationType declarationType
    );

    /**
     * Trouve les déclarations par statut
     */
    List<VATDeclaration> findByCompanyAndStatus(Company company, String status);

    /**
     * Trouve les déclarations non payées
     */
    @Query("SELECT vd FROM VATDeclaration vd WHERE vd.company = :company AND vd.status IN ('VALIDATED', 'SUBMITTED') ORDER BY vd.fiscalPeriod DESC")
    List<VATDeclaration> findUnpaidDeclarations(@Param("company") Company company);

    /**
     * Trouve les déclarations pour une période donnée
     */
    @Query("SELECT vd FROM VATDeclaration vd WHERE vd.company = :company AND vd.startDate >= :startDate AND vd.endDate <= :endDate ORDER BY vd.fiscalPeriod DESC")
    List<VATDeclaration> findByPeriod(
        @Param("company") Company company,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Compte les déclarations en attente
     */
    @Query("SELECT COUNT(vd) FROM VATDeclaration vd WHERE vd.company = :company AND vd.status = 'DRAFT'")
    Long countDraftDeclarations(@Param("company") Company company);
}
