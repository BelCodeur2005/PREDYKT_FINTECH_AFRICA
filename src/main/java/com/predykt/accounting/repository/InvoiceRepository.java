package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.Customer;
import com.predykt.accounting.domain.entity.Invoice;
import com.predykt.accounting.domain.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour les factures clients (Invoice)
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // Recherches de base
    Optional<Invoice> findByCompanyAndInvoiceNumber(Company company, String invoiceNumber);

    List<Invoice> findByCompanyOrderByIssueDateDesc(Company company);

    List<Invoice> findByCompanyAndCustomerOrderByIssueDateDesc(Company company, Customer customer);

    List<Invoice> findByCompanyAndStatusOrderByIssueDateDesc(Company company, InvoiceStatus status);

    // Factures en retard (overdue)
    @Query("SELECT i FROM Invoice i WHERE i.company = :company " +
           "AND i.status IN ('ISSUED', 'PARTIAL_PAID') " +
           "AND i.dueDate < :asOfDate " +
           "ORDER BY i.dueDate ASC")
    List<Invoice> findOverdueInvoices(@Param("company") Company company,
                                       @Param("asOfDate") LocalDate asOfDate);

    // Factures par période
    List<Invoice> findByCompanyAndIssueDateBetweenOrderByIssueDateDesc(
        Company company, LocalDate startDate, LocalDate endDate);

    // Factures non réglées
    @Query("SELECT i FROM Invoice i WHERE i.company = :company " +
           "AND i.status IN ('ISSUED', 'PARTIAL_PAID') " +
           "AND i.amountDue > 0 " +
           "ORDER BY i.dueDate ASC")
    List<Invoice> findUnpaidInvoices(@Param("company") Company company);

    // Factures non lettrées
    List<Invoice> findByCompanyAndIsReconciledFalseOrderByIssueDateDesc(Company company);

    // Statistiques
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.company = :company AND i.status = :status")
    Long countByCompanyAndStatus(@Param("company") Company company, @Param("status") InvoiceStatus status);

    @Query("SELECT SUM(i.totalTtc) FROM Invoice i WHERE i.company = :company AND i.status = :status")
    java.math.BigDecimal sumTotalTtcByCompanyAndStatus(
        @Param("company") Company company, @Param("status") InvoiceStatus status);

    @Query("SELECT SUM(i.amountDue) FROM Invoice i WHERE i.company = :company " +
           "AND i.status IN ('ISSUED', 'PARTIAL_PAID')")
    java.math.BigDecimal sumOutstandingAmount(@Param("company") Company company);
}
