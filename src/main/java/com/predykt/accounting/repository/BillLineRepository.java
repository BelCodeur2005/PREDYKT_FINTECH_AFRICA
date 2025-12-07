package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.Bill;
import com.predykt.accounting.domain.entity.BillLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour les lignes de factures fournisseurs (BillLine)
 */
@Repository
public interface BillLineRepository extends JpaRepository<BillLine, Long> {

    /**
     * Trouver toutes les lignes d'une facture fournisseur, triées par numéro de ligne
     */
    List<BillLine> findByBillOrderByLineNumberAsc(Bill bill);

    /**
     * Trouver une ligne spécifique d'une facture
     */
    Optional<BillLine> findByBillAndLineNumber(Bill bill, Integer lineNumber);

    /**
     * Compter le nombre de lignes d'une facture
     */
    long countByBill(Bill bill);

    /**
     * Supprimer toutes les lignes d'une facture
     */
    void deleteByBill(Bill bill);

    /**
     * Calculer le total HT de toutes les lignes d'une facture
     */
    @Query("SELECT COALESCE(SUM(bl.totalHt), 0) FROM BillLine bl WHERE bl.bill = :bill")
    java.math.BigDecimal sumTotalHtByBill(@Param("bill") Bill bill);

    /**
     * Calculer le total TVA de toutes les lignes d'une facture
     */
    @Query("SELECT COALESCE(SUM(bl.vatAmount), 0) FROM BillLine bl WHERE bl.bill = :bill")
    java.math.BigDecimal sumVatAmountByBill(@Param("bill") Bill bill);

    /**
     * Calculer le total TTC de toutes les lignes d'une facture
     */
    @Query("SELECT COALESCE(SUM(bl.totalTtc), 0) FROM BillLine bl WHERE bl.bill = :bill")
    java.math.BigDecimal sumTotalTtcByBill(@Param("bill") Bill bill);

    /**
     * Trouver le prochain numéro de ligne disponible pour une facture
     */
    @Query("SELECT COALESCE(MAX(bl.lineNumber), 0) + 1 FROM BillLine bl WHERE bl.bill = :bill")
    Integer getNextLineNumber(@Param("bill") Bill bill);
}
