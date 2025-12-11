package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.Customer;
import com.predykt.accounting.domain.entity.Deposit;
import com.predykt.accounting.domain.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour les acomptes (Deposits).
 * Conforme OHADA - Compte 4191 "Clients - Avances et acomptes".
 *
 * Requêtes optimisées avec index sur:
 * - company_id (multi-tenant isolation OBLIGATOIRE)
 * - customer_id, invoice_id, payment_id
 * - deposit_date, is_applied
 * - Index composite: (company_id, customer_id, deposit_date)
 * - Index partiel: (company_id, is_applied) WHERE is_applied = FALSE
 *
 * @author PREDYKT System Optimizer
 * @since Phase 3 - Conformité OHADA Avancée
 */
@Repository
public interface DepositRepository extends JpaRepository<Deposit, Long> {

    // ==================== RECHERCHE PAR IDENTIFIANTS ====================

    /**
     * Recherche un acompte par numéro de reçu (unique).
     * Ex: RA-2025-000001
     */
    Optional<Deposit> findByDepositNumber(String depositNumber);

    /**
     * Recherche un acompte par numéro et société (multi-tenant safe).
     */
    Optional<Deposit> findByDepositNumberAndCompany(String depositNumber, Company company);

    /**
     * Vérifie si un numéro de reçu existe déjà pour une société.
     */
    boolean existsByDepositNumberAndCompany(String depositNumber, Company company);

    // ==================== RECHERCHE PAR SOCIÉTÉ (MULTI-TENANT) ====================

    /**
     * Liste tous les acomptes d'une société, triés par date décroissante.
     * PAGINATION RECOMMANDÉE pour grandes bases.
     */
    Page<Deposit> findByCompanyOrderByDepositDateDesc(Company company, Pageable pageable);

    /**
     * Liste tous les acomptes d'une société (sans pagination).
     * ATTENTION: Peut être lent si beaucoup d'acomptes.
     */
    List<Deposit> findByCompanyOrderByDepositDateDesc(Company company);

    /**
     * Compte le nombre total d'acomptes pour une société.
     */
    long countByCompany(Company company);

    // ==================== RECHERCHE PAR CLIENT ====================

    /**
     * Liste tous les acomptes d'un client, triés par date décroissante.
     */
    List<Deposit> findByCompanyAndCustomerOrderByDepositDateDesc(Company company, Customer customer);

    /**
     * Liste les acomptes NON IMPUTÉS d'un client (disponibles pour imputation).
     * Requête OPTIMISÉE avec index partiel (company_id, is_applied) WHERE is_applied = FALSE.
     */
    List<Deposit> findByCompanyAndCustomerAndIsAppliedFalseOrderByDepositDateDesc(Company company, Customer customer);

    /**
     * Compte le nombre d'acomptes non imputés pour un client.
     */
    long countByCompanyAndCustomerAndIsAppliedFalse(Company company, Customer customer);

    /**
     * Calcule le total des acomptes disponibles (non imputés) pour un client.
     */
    @Query("SELECT COALESCE(SUM(d.amountTtc), 0) FROM Deposit d " +
           "WHERE d.company = :company AND d.customer = :customer AND d.isApplied = FALSE")
    BigDecimal sumAvailableDepositsByCustomer(@Param("company") Company company,
                                              @Param("customer") Customer customer);

    // ==================== RECHERCHE PAR FACTURE ====================

    /**
     * Liste les acomptes imputés sur une facture spécifique.
     * Normalement 0..N acomptes par facture.
     */
    List<Deposit> findByInvoice(Invoice invoice);

    /**
     * Calcule le total des acomptes imputés sur une facture.
     */
    @Query("SELECT COALESCE(SUM(d.amountTtc), 0) FROM Deposit d " +
           "WHERE d.invoice = :invoice AND d.isApplied = TRUE")
    BigDecimal sumAppliedDepositsByInvoice(@Param("invoice") Invoice invoice);

    // ==================== RECHERCHE PAR ÉTAT ====================

    /**
     * Liste tous les acomptes NON IMPUTÉS d'une société (disponibles).
     * REQUÊTE OPTIMISÉE avec index partiel.
     */
    List<Deposit> findByCompanyAndIsAppliedFalseOrderByDepositDateDesc(Company company);

    /**
     * Liste tous les acomptes IMPUTÉS d'une société.
     */
    List<Deposit> findByCompanyAndIsAppliedTrueOrderByDepositDateDesc(Company company);

    /**
     * Compte les acomptes non imputés pour une société.
     */
    long countByCompanyAndIsAppliedFalse(Company company);

    /**
     * Calcule le total des acomptes disponibles (non imputés) pour une société.
     */
    @Query("SELECT COALESCE(SUM(d.amountTtc), 0) FROM Deposit d " +
           "WHERE d.company = :company AND d.isApplied = FALSE")
    BigDecimal sumAvailableDepositsByCompany(@Param("company") Company company);

    // ==================== RECHERCHE PAR PÉRIODE ====================

    /**
     * Liste les acomptes reçus entre deux dates (pour déclaration TVA).
     * IMPORTANT pour CGI Cameroun: TVA exigible sur encaissement.
     */
    List<Deposit> findByCompanyAndDepositDateBetweenOrderByDepositDateDesc(
        Company company,
        LocalDate startDate,
        LocalDate endDate
    );

    /**
     * Calcule le total HT des acomptes reçus sur une période (base TVA).
     */
    @Query("SELECT COALESCE(SUM(d.amountHt), 0) FROM Deposit d " +
           "WHERE d.company = :company " +
           "AND d.depositDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountHtByPeriod(@Param("company") Company company,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    /**
     * Calcule le total de TVA collectée sur acomptes pour une période (déclaration TVA).
     */
    @Query("SELECT COALESCE(SUM(d.vatAmount), 0) FROM Deposit d " +
           "WHERE d.company = :company " +
           "AND d.depositDate BETWEEN :startDate AND :endDate")
    BigDecimal sumVatAmountByPeriod(@Param("company") Company company,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    /**
     * Calcule le total TTC des acomptes reçus sur une période (flux de trésorerie).
     */
    @Query("SELECT COALESCE(SUM(d.amountTtc), 0) FROM Deposit d " +
           "WHERE d.company = :company " +
           "AND d.depositDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountTtcByPeriod(@Param("company") Company company,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    // ==================== RECHERCHE AVANCÉE ====================

    /**
     * Recherche d'acomptes par référence de commande client.
     */
    List<Deposit> findByCompanyAndCustomerOrderReferenceContainingIgnoreCaseOrderByDepositDateDesc(
        Company company,
        String orderReference
    );

    /**
     * Recherche d'acomptes par description (texte libre).
     */
    List<Deposit> findByCompanyAndDescriptionContainingIgnoreCaseOrderByDepositDateDesc(
        Company company,
        String description
    );

    /**
     * Recherche multi-critères avec JPQL pour filtres complexes.
     * Utilisé par DepositService.searchDeposits() pour recherche avancée.
     */
    @Query("SELECT d FROM Deposit d " +
           "WHERE d.company = :company " +
           "AND (:customer IS NULL OR d.customer = :customer) " +
           "AND (:isApplied IS NULL OR d.isApplied = :isApplied) " +
           "AND (:startDate IS NULL OR d.depositDate >= :startDate) " +
           "AND (:endDate IS NULL OR d.depositDate <= :endDate) " +
           "AND (:minAmount IS NULL OR d.amountTtc >= :minAmount) " +
           "AND (:maxAmount IS NULL OR d.amountTtc <= :maxAmount) " +
           "ORDER BY d.depositDate DESC")
    Page<Deposit> searchDeposits(@Param("company") Company company,
                                 @Param("customer") Customer customer,
                                 @Param("isApplied") Boolean isApplied,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate,
                                 @Param("minAmount") BigDecimal minAmount,
                                 @Param("maxAmount") BigDecimal maxAmount,
                                 Pageable pageable);

    // ==================== STATISTIQUES ====================

    /**
     * Statistiques agrégées pour le dashboard.
     * Retourne: [count, sumAmountHt, sumVatAmount, sumAmountTtc]
     */
    @Query("SELECT " +
           "COUNT(d), " +
           "COALESCE(SUM(d.amountHt), 0), " +
           "COALESCE(SUM(d.vatAmount), 0), " +
           "COALESCE(SUM(d.amountTtc), 0) " +
           "FROM Deposit d " +
           "WHERE d.company = :company " +
           "AND (:isApplied IS NULL OR d.isApplied = :isApplied)")
    Object[] getDepositStatistics(@Param("company") Company company,
                                  @Param("isApplied") Boolean isApplied);

    /**
     * Statistiques mensuelles pour graphiques.
     * Retourne: [year, month, count, sumAmountTtc]
     */
    @Query("SELECT " +
           "YEAR(d.depositDate), " +
           "MONTH(d.depositDate), " +
           "COUNT(d), " +
           "COALESCE(SUM(d.amountTtc), 0) " +
           "FROM Deposit d " +
           "WHERE d.company = :company " +
           "AND d.depositDate BETWEEN :startDate AND :endDate " +
           "GROUP BY YEAR(d.depositDate), MONTH(d.depositDate) " +
           "ORDER BY YEAR(d.depositDate), MONTH(d.depositDate)")
    List<Object[]> getMonthlyDepositStatistics(@Param("company") Company company,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    // ==================== TOP CLIENTS ====================

    /**
     * Top clients par volume d'acomptes.
     * Retourne: [customerId, customerName, count, sumAmountTtc]
     */
    @Query("SELECT " +
           "d.customer.id, " +
           "d.customer.name, " +
           "COUNT(d), " +
           "COALESCE(SUM(d.amountTtc), 0) " +
           "FROM Deposit d " +
           "WHERE d.company = :company " +
           "AND d.customer IS NOT NULL " +
           "AND d.depositDate BETWEEN :startDate AND :endDate " +
           "GROUP BY d.customer.id, d.customer.name " +
           "ORDER BY SUM(d.amountTtc) DESC")
    List<Object[]> getTopCustomersByDepositVolume(@Param("company") Company company,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate,
                                                   Pageable pageable);
}
