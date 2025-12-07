package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.*;
import com.predykt.accounting.domain.enums.PaymentStatus;
import com.predykt.accounting.domain.enums.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour les paiements
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Recherches de base
    Optional<Payment> findByCompanyAndPaymentNumber(Company company, String paymentNumber);

    List<Payment> findByCompanyOrderByPaymentDateDesc(Company company);

    List<Payment> findByCompanyAndPaymentTypeOrderByPaymentDateDesc(Company company, PaymentType paymentType);

    // Paiements par facture
    List<Payment> findByInvoiceOrderByPaymentDateDesc(Invoice invoice);

    List<Payment> findByBillOrderByPaymentDateDesc(Bill bill);

    // Paiements par client/fournisseur
    List<Payment> findByCustomerOrderByPaymentDateDesc(Customer customer);

    List<Payment> findBySupplierOrderByPaymentDateDesc(Supplier supplier);

    // Paiements par période
    List<Payment> findByCompanyAndPaymentDateBetweenOrderByPaymentDateDesc(
        Company company, LocalDate startDate, LocalDate endDate);

    // Paiements non lettrés
    List<Payment> findByCompanyAndIsReconciledFalseOrderByPaymentDateDesc(Company company);

    // Paiements par statut
    List<Payment> findByCompanyAndStatusOrderByPaymentDateDesc(Company company, PaymentStatus status);

    // Statistiques
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.company = :company " +
           "AND p.paymentType = :paymentType " +
           "AND p.status = 'COMPLETED' " +
           "AND p.paymentDate BETWEEN :startDate AND :endDate")
    java.math.BigDecimal sumAmountByTypeAndPeriod(
        @Param("company") Company company,
        @Param("paymentType") PaymentType paymentType,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.company = :company " +
           "AND p.status = :status")
    Long countByCompanyAndStatus(@Param("company") Company company, @Param("status") PaymentStatus status);
}
