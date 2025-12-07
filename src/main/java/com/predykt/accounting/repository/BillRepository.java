package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.Bill;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.Supplier;
import com.predykt.accounting.domain.enums.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour les factures fournisseurs (Bill)
 */
@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    // Recherches de base
    Optional<Bill> findByCompanyAndBillNumber(Company company, String billNumber);

    List<Bill> findByCompanyOrderByIssueDateDesc(Company company);

    List<Bill> findByCompanyAndSupplierOrderByIssueDateDesc(Company company, Supplier supplier);

    List<Bill> findByCompanyAndStatusOrderByIssueDateDesc(Company company, BillStatus status);

    // Factures en retard (overdue)
    @Query("SELECT b FROM Bill b WHERE b.company = :company " +
           "AND b.status IN ('ISSUED', 'PARTIAL_PAID') " +
           "AND b.dueDate < :asOfDate " +
           "ORDER BY b.dueDate ASC")
    List<Bill> findOverdueBills(@Param("company") Company company,
                                 @Param("asOfDate") LocalDate asOfDate);

    // Factures par période
    List<Bill> findByCompanyAndIssueDateBetweenOrderByIssueDateDesc(
        Company company, LocalDate startDate, LocalDate endDate);

    // Factures non réglées
    @Query("SELECT b FROM Bill b WHERE b.company = :company " +
           "AND b.status IN ('ISSUED', 'PARTIAL_PAID') " +
           "AND b.amountDue > 0 " +
           "ORDER BY b.dueDate ASC")
    List<Bill> findUnpaidBills(@Param("company") Company company);

    // Factures non lettrées
    List<Bill> findByCompanyAndIsReconciledFalseOrderByIssueDateDesc(Company company);

    // Statistiques
    @Query("SELECT COUNT(b) FROM Bill b WHERE b.company = :company AND b.status = :status")
    Long countByCompanyAndStatus(@Param("company") Company company, @Param("status") BillStatus status);

    @Query("SELECT SUM(b.totalTtc) FROM Bill b WHERE b.company = :company AND b.status = :status")
    java.math.BigDecimal sumTotalTtcByCompanyAndStatus(
        @Param("company") Company company, @Param("status") BillStatus status);

    @Query("SELECT SUM(b.amountDue) FROM Bill b WHERE b.company = :company " +
           "AND b.status IN ('ISSUED', 'PARTIAL_PAID')")
    java.math.BigDecimal sumOutstandingAmount(@Param("company") Company company);

    // Statistiques AIR
    @Query("SELECT SUM(b.airAmount) FROM Bill b WHERE b.company = :company " +
           "AND b.issueDate BETWEEN :startDate AND :endDate")
    java.math.BigDecimal sumAirAmountByPeriod(
        @Param("company") Company company,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}
