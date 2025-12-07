package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.Invoice;
import com.predykt.accounting.domain.entity.InvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour les lignes de factures clients (InvoiceLine)
 */
@Repository
public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {

    /**
     * Trouver toutes les lignes d'une facture, triées par numéro de ligne
     */
    List<InvoiceLine> findByInvoiceOrderByLineNumberAsc(Invoice invoice);

    /**
     * Trouver une ligne spécifique d'une facture
     */
    Optional<InvoiceLine> findByInvoiceAndLineNumber(Invoice invoice, Integer lineNumber);

    /**
     * Compter le nombre de lignes d'une facture
     */
    long countByInvoice(Invoice invoice);

    /**
     * Supprimer toutes les lignes d'une facture
     */
    void deleteByInvoice(Invoice invoice);

    /**
     * Calculer le total HT de toutes les lignes d'une facture
     */
    @Query("SELECT COALESCE(SUM(il.totalHt), 0) FROM InvoiceLine il WHERE il.invoice = :invoice")
    java.math.BigDecimal sumTotalHtByInvoice(@Param("invoice") Invoice invoice);

    /**
     * Calculer le total TVA de toutes les lignes d'une facture
     */
    @Query("SELECT COALESCE(SUM(il.vatAmount), 0) FROM InvoiceLine il WHERE il.invoice = :invoice")
    java.math.BigDecimal sumVatAmountByInvoice(@Param("invoice") Invoice invoice);

    /**
     * Calculer le total TTC de toutes les lignes d'une facture
     */
    @Query("SELECT COALESCE(SUM(il.totalTtc), 0) FROM InvoiceLine il WHERE il.invoice = :invoice")
    java.math.BigDecimal sumTotalTtcByInvoice(@Param("invoice") Invoice invoice);

    /**
     * Trouver le prochain numéro de ligne disponible pour une facture
     */
    @Query("SELECT COALESCE(MAX(il.lineNumber), 0) + 1 FROM InvoiceLine il WHERE il.invoice = :invoice")
    Integer getNextLineNumber(@Param("invoice") Invoice invoice);
}
