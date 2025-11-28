package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.BankReconciliation;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.enums.ReconciliationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankReconciliationRepository extends JpaRepository<BankReconciliation, Long> {

    // Recherche par entreprise
    List<BankReconciliation> findByCompanyOrderByReconciliationDateDesc(Company company);

    // Recherche par compte bancaire
    List<BankReconciliation> findByCompanyAndBankAccountNumberOrderByReconciliationDateDesc(
        Company company,
        String bankAccountNumber
    );

    // Recherche par période
    List<BankReconciliation> findByCompanyAndReconciliationDateBetweenOrderByReconciliationDateDesc(
        Company company,
        LocalDate startDate,
        LocalDate endDate
    );

    // Recherche par statut
    List<BankReconciliation> findByCompanyAndStatusOrderByReconciliationDateDesc(
        Company company,
        ReconciliationStatus status
    );

    // Dernier rapprochement pour un compte
    Optional<BankReconciliation> findFirstByCompanyAndBankAccountNumberOrderByReconciliationDateDesc(
        Company company,
        String bankAccountNumber
    );

    // Vérifier si un rapprochement existe déjà pour une date et un compte
    boolean existsByCompanyAndBankAccountNumberAndReconciliationDate(
        Company company,
        String bankAccountNumber,
        LocalDate reconciliationDate
    );

    // Rapprochements non équilibrés
    @Query("SELECT br FROM BankReconciliation br WHERE br.company = :company " +
           "AND br.isBalanced = false " +
           "ORDER BY br.reconciliationDate DESC")
    List<BankReconciliation> findUnbalancedReconciliations(@Param("company") Company company);

    // Rapprochements en attente de validation
    @Query("SELECT br FROM BankReconciliation br WHERE br.company = :company " +
           "AND br.status IN :statuses " +
           "ORDER BY br.reconciliationDate DESC")
    List<BankReconciliation> findByCompanyAndStatusIn(
        @Param("company") Company company,
        @Param("statuses") List<ReconciliationStatus> statuses
    );

    // Statistiques par période
    @Query("SELECT COUNT(br) FROM BankReconciliation br " +
           "WHERE br.company = :company " +
           "AND br.reconciliationDate BETWEEN :startDate AND :endDate")
    long countByCompanyAndPeriod(
        @Param("company") Company company,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    // Nombre de rapprochements approuvés
    @Query("SELECT COUNT(br) FROM BankReconciliation br " +
           "WHERE br.company = :company " +
           "AND br.status = 'APPROVED' " +
           "AND br.reconciliationDate BETWEEN :startDate AND :endDate")
    long countApprovedByCompanyAndPeriod(
        @Param("company") Company company,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    // Rapprochements par préparateur
    @Query("SELECT br FROM BankReconciliation br WHERE br.company = :company " +
           "AND br.preparedBy = :preparedBy " +
           "ORDER BY br.reconciliationDate DESC")
    List<BankReconciliation> findByCompanyAndPreparedBy(
        @Param("company") Company company,
        @Param("preparedBy") String preparedBy
    );

    // Compter rapprochements par statut
    long countByCompanyAndStatus(Company company, ReconciliationStatus status);
}
