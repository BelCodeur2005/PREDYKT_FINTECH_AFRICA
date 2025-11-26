package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.CabinetInvoice;
import com.predykt.accounting.domain.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité CabinetInvoice (MODE CABINET)
 */
@Repository
public interface CabinetInvoiceRepository extends JpaRepository<CabinetInvoice, Long> {

    /**
     * Trouve une facture par son numéro
     */
    Optional<CabinetInvoice> findByInvoiceNumber(String invoiceNumber);

    /**
     * Trouve toutes les factures d'un cabinet
     */
    @Query("SELECT ci FROM CabinetInvoice ci WHERE ci.cabinet.id = :cabinetId ORDER BY ci.invoiceDate DESC")
    List<CabinetInvoice> findByCabinetId(@Param("cabinetId") Long cabinetId);

    /**
     * Trouve les factures d'un cabinet par statut
     */
    @Query("SELECT ci FROM CabinetInvoice ci " +
           "WHERE ci.cabinet.id = :cabinetId AND ci.status = :status " +
           "ORDER BY ci.invoiceDate DESC")
    List<CabinetInvoice> findByCabinetIdAndStatus(
        @Param("cabinetId") Long cabinetId,
        @Param("status") InvoiceStatus status
    );

    /**
     * Trouve les factures en retard d'un cabinet
     */
    @Query("SELECT ci FROM CabinetInvoice ci " +
           "WHERE ci.cabinet.id = :cabinetId " +
           "AND ci.status = 'PENDING' " +
           "AND ci.dueDate < :today " +
           "ORDER BY ci.dueDate ASC")
    List<CabinetInvoice> findOverdueInvoicesByCabinetId(
        @Param("cabinetId") Long cabinetId,
        @Param("today") LocalDate today
    );

    /**
     * Trouve les factures d'un cabinet pour une période
     */
    @Query("SELECT ci FROM CabinetInvoice ci " +
           "WHERE ci.cabinet.id = :cabinetId " +
           "AND ci.invoiceDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ci.invoiceDate DESC")
    List<CabinetInvoice> findByCabinetIdAndPeriod(
        @Param("cabinetId") Long cabinetId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Calcule le chiffre d'affaires total d'un cabinet
     */
    @Query("SELECT COALESCE(SUM(ci.amountTtc), 0) FROM CabinetInvoice ci " +
           "WHERE ci.cabinet.id = :cabinetId AND ci.status = 'PAID'")
    BigDecimal calculateTotalRevenueByCabinetId(@Param("cabinetId") Long cabinetId);

    /**
     * Calcule le CA d'un cabinet pour une période
     */
    @Query("SELECT COALESCE(SUM(ci.amountTtc), 0) FROM CabinetInvoice ci " +
           "WHERE ci.cabinet.id = :cabinetId " +
           "AND ci.status = 'PAID' " +
           "AND ci.invoiceDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateRevenueByCabinetIdAndPeriod(
        @Param("cabinetId") Long cabinetId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Calcule le montant des impayés d'un cabinet
     */
    @Query("SELECT COALESCE(SUM(ci.amountTtc), 0) FROM CabinetInvoice ci " +
           "WHERE ci.cabinet.id = :cabinetId AND ci.status = 'PENDING'")
    BigDecimal calculateOutstandingAmountByCabinetId(@Param("cabinetId") Long cabinetId);

    /**
     * Calcule le montant des factures en retard d'un cabinet
     */
    @Query("SELECT COALESCE(SUM(ci.amountTtc), 0) FROM CabinetInvoice ci " +
           "WHERE ci.cabinet.id = :cabinetId " +
           "AND ci.status = 'PENDING' " +
           "AND ci.dueDate < :today")
    BigDecimal calculateOverdueAmountByCabinetId(
        @Param("cabinetId") Long cabinetId,
        @Param("today") LocalDate today
    );

    /**
     * Compte le nombre de factures d'un cabinet par statut
     */
    @Query("SELECT COUNT(ci) FROM CabinetInvoice ci " +
           "WHERE ci.cabinet.id = :cabinetId AND ci.status = :status")
    Long countByCabinetIdAndStatus(
        @Param("cabinetId") Long cabinetId,
        @Param("status") InvoiceStatus status
    );

    /**
     * Vérifie si un numéro de facture existe
     */
    boolean existsByInvoiceNumber(String invoiceNumber);

    /**
     * Génère le prochain numéro de facture pour un cabinet
     */
    @Query("SELECT MAX(ci.invoiceNumber) FROM CabinetInvoice ci " +
           "WHERE ci.cabinet.id = :cabinetId " +
           "AND ci.invoiceNumber LIKE :prefix%")
    Optional<String> findLastInvoiceNumberByCabinetIdAndPrefix(
        @Param("cabinetId") Long cabinetId,
        @Param("prefix") String prefix
    );
}
