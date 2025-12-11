package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.Deposit;
import com.predykt.accounting.domain.entity.DepositApplication;
import com.predykt.accounting.domain.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des imputations partielles des acomptes (DepositApplication).
 *
 * Ce repository fait partie de la Phase 2 - Imputation Partielle.
 * Il permet de gérer les cas où un acompte est fractionné sur plusieurs factures.
 *
 * Exemples d'utilisation:
 * - Rechercher toutes les imputations d'un acompte
 * - Rechercher toutes les imputations sur une facture
 * - Calculer le total imputé pour un acompte
 * - Statistiques d'utilisation des acomptes
 *
 * @author PREDYKT Accounting Team
 * @version 2.0 (Phase 2)
 * @since 2025-12-11
 */
@Repository
public interface DepositApplicationRepository extends JpaRepository<DepositApplication, Long> {

    // =====================================================================
    // Recherches par entités
    // =====================================================================

    /**
     * Trouve toutes les imputations d'un acompte donné
     *
     * @param deposit L'acompte source
     * @return Liste des imputations
     */
    List<DepositApplication> findByDeposit(Deposit deposit);

    /**
     * Trouve toutes les imputations d'un acompte donné (avec pagination)
     *
     * @param deposit L'acompte source
     * @param pageable Pagination
     * @return Page d'imputations
     */
    Page<DepositApplication> findByDeposit(Deposit deposit, Pageable pageable);

    /**
     * Trouve toutes les imputations sur une facture donnée
     *
     * @param invoice La facture
     * @return Liste des imputations
     */
    List<DepositApplication> findByInvoice(Invoice invoice);

    /**
     * Trouve toutes les imputations sur une facture donnée (avec pagination)
     *
     * @param invoice La facture
     * @param pageable Pagination
     * @return Page d'imputations
     */
    Page<DepositApplication> findByInvoice(Invoice invoice, Pageable pageable);

    /**
     * Trouve toutes les imputations d'une entreprise
     *
     * @param company L'entreprise (multi-tenant)
     * @return Liste des imputations
     */
    List<DepositApplication> findByCompany(Company company);

    /**
     * Trouve toutes les imputations d'une entreprise (avec pagination)
     *
     * @param company L'entreprise (multi-tenant)
     * @param pageable Pagination
     * @return Page d'imputations
     */
    Page<DepositApplication> findByCompany(Company company, Pageable pageable);

    // =====================================================================
    // Recherches par critères
    // =====================================================================

    /**
     * Trouve toutes les imputations d'un acompte pour une entreprise donnée
     *
     * @param company L'entreprise
     * @param deposit L'acompte
     * @return Liste des imputations
     */
    List<DepositApplication> findByCompanyAndDeposit(Company company, Deposit deposit);

    /**
     * Trouve toutes les imputations sur une facture pour une entreprise donnée
     *
     * @param company L'entreprise
     * @param invoice La facture
     * @return Liste des imputations
     */
    List<DepositApplication> findByCompanyAndInvoice(Company company, Invoice invoice);

    /**
     * Trouve toutes les imputations effectuées par un utilisateur
     *
     * @param company L'entreprise
     * @param appliedBy L'utilisateur
     * @return Liste des imputations
     */
    List<DepositApplication> findByCompanyAndAppliedBy(Company company, String appliedBy);

    /**
     * Trouve toutes les imputations dans une période donnée
     *
     * @param company L'entreprise
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Liste des imputations
     */
    List<DepositApplication> findByCompanyAndAppliedAtBetween(
        Company company,
        LocalDateTime startDate,
        LocalDateTime endDate
    );

    /**
     * Trouve toutes les imputations dans une période donnée (avec pagination)
     *
     * @param company L'entreprise
     * @param startDate Date de début
     * @param endDate Date de fin
     * @param pageable Pagination
     * @return Page d'imputations
     */
    Page<DepositApplication> findByCompanyAndAppliedAtBetween(
        Company company,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Pageable pageable
    );

    // =====================================================================
    // Requêtes d'agrégation
    // =====================================================================

    /**
     * Calcule le montant total imputé pour un acompte donné
     *
     * @param deposit L'acompte
     * @return Montant total imputé
     */
    @Query("SELECT COALESCE(SUM(da.amountTtc), 0) FROM DepositApplication da " +
           "WHERE da.deposit = :deposit")
    BigDecimal sumAmountByDeposit(@Param("deposit") Deposit deposit);

    /**
     * Calcule le montant total des acomptes imputés sur une facture
     *
     * @param invoice La facture
     * @return Montant total des acomptes
     */
    @Query("SELECT COALESCE(SUM(da.amountTtc), 0) FROM DepositApplication da " +
           "WHERE da.invoice = :invoice")
    BigDecimal sumAmountByInvoice(@Param("invoice") Invoice invoice);

    /**
     * Compte le nombre d'imputations pour un acompte
     *
     * @param deposit L'acompte
     * @return Nombre d'imputations
     */
    long countByDeposit(Deposit deposit);

    /**
     * Compte le nombre d'imputations sur une facture
     *
     * @param invoice La facture
     * @return Nombre d'imputations
     */
    long countByInvoice(Invoice invoice);

    /**
     * Compte le nombre d'imputations dans une période
     *
     * @param company L'entreprise
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Nombre d'imputations
     */
    long countByCompanyAndAppliedAtBetween(
        Company company,
        LocalDateTime startDate,
        LocalDateTime endDate
    );

    // =====================================================================
    // Statistiques avancées
    // =====================================================================

    /**
     * Calcule le montant total des imputations pour une entreprise
     *
     * @param company L'entreprise
     * @return Montant total
     */
    @Query("SELECT COALESCE(SUM(da.amountTtc), 0) FROM DepositApplication da " +
           "WHERE da.company = :company")
    BigDecimal sumTotalAmountByCompany(@Param("company") Company company);

    /**
     * Calcule le montant total des imputations sur une période
     *
     * @param company L'entreprise
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Montant total
     */
    @Query("SELECT COALESCE(SUM(da.amountTtc), 0) FROM DepositApplication da " +
           "WHERE da.company = :company " +
           "AND da.appliedAt BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByPeriod(
        @Param("company") Company company,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Calcule les statistiques mensuelles des imputations
     *
     * @param company L'entreprise
     * @param year L'année
     * @param month Le mois (1-12)
     * @return Statistiques (count, sum, avg)
     */
    @Query("SELECT " +
           "COUNT(da.id) as count, " +
           "COALESCE(SUM(da.amountTtc), 0) as total, " +
           "COALESCE(AVG(da.amountTtc), 0) as average " +
           "FROM DepositApplication da " +
           "WHERE da.company = :company " +
           "AND EXTRACT(YEAR FROM da.appliedAt) = :year " +
           "AND EXTRACT(MONTH FROM da.appliedAt) = :month")
    Object[] getMonthlyStatistics(
        @Param("company") Company company,
        @Param("year") int year,
        @Param("month") int month
    );

    /**
     * Trouve les factures avec le plus d'acomptes imputés
     *
     * @param company L'entreprise
     * @param pageable Pagination (limite)
     * @return Liste des factures avec nombre d'imputations
     */
    @Query("SELECT da.invoice, COUNT(da.id) as count " +
           "FROM DepositApplication da " +
           "WHERE da.company = :company " +
           "GROUP BY da.invoice " +
           "ORDER BY count DESC")
    Page<Object[]> findInvoicesWithMostApplications(@Param("company") Company company, Pageable pageable);

    /**
     * Trouve les acomptes les plus utilisés (nombreuses imputations)
     *
     * @param company L'entreprise
     * @param pageable Pagination (limite)
     * @return Liste des acomptes avec nombre d'imputations
     */
    @Query("SELECT da.deposit, COUNT(da.id) as count " +
           "FROM DepositApplication da " +
           "WHERE da.company = :company " +
           "GROUP BY da.deposit " +
           "ORDER BY count DESC")
    Page<Object[]> findDepositsWithMostApplications(@Param("company") Company company, Pageable pageable);

    /**
     * Calcule la moyenne du délai entre réception acompte et imputation
     *
     * @param company L'entreprise
     * @return Délai moyen en jours
     */
    @Query("SELECT AVG(DATEDIFF(da.appliedAt, d.depositDate)) " +
           "FROM DepositApplication da " +
           "JOIN da.deposit d " +
           "WHERE da.company = :company")
    Double getAverageApplicationDelayInDays(@Param("company") Company company);

    // =====================================================================
    // Recherches spécifiques
    // =====================================================================

    /**
     * Vérifie si un acompte a été imputé sur une facture spécifique
     *
     * @param deposit L'acompte
     * @param invoice La facture
     * @return true si une imputation existe
     */
    boolean existsByDepositAndInvoice(Deposit deposit, Invoice invoice);

    /**
     * Trouve la première imputation (chronologique) d'un acompte
     *
     * @param deposit L'acompte
     * @return Première imputation
     */
    Optional<DepositApplication> findFirstByDepositOrderByAppliedAtAsc(Deposit deposit);

    /**
     * Trouve la dernière imputation (chronologique) d'un acompte
     *
     * @param deposit L'acompte
     * @return Dernière imputation
     */
    Optional<DepositApplication> findFirstByDepositOrderByAppliedAtDesc(Deposit deposit);

    /**
     * Trouve toutes les imputations d'un acompte ordonnées par date
     *
     * @param deposit L'acompte
     * @return Liste ordonnée des imputations
     */
    List<DepositApplication> findByDepositOrderByAppliedAtAsc(Deposit deposit);

    /**
     * Trouve toutes les imputations sur une facture ordonnées par date
     *
     * @param invoice La facture
     * @return Liste ordonnée des imputations
     */
    List<DepositApplication> findByInvoiceOrderByAppliedAtAsc(Invoice invoice);

    /**
     * Trouve les imputations les plus récentes pour une entreprise
     *
     * @param company L'entreprise
     * @param pageable Pagination (limite)
     * @return Page d'imputations récentes
     */
    Page<DepositApplication> findByCompanyOrderByAppliedAtDesc(Company company, Pageable pageable);

    /**
     * Recherche par notes (contient texte)
     *
     * @param company L'entreprise
     * @param searchText Texte à rechercher
     * @param pageable Pagination
     * @return Page d'imputations correspondantes
     */
    @Query("SELECT da FROM DepositApplication da " +
           "WHERE da.company = :company " +
           "AND LOWER(da.notes) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<DepositApplication> searchByNotes(
        @Param("company") Company company,
        @Param("searchText") String searchText,
        Pageable pageable
    );

    /**
     * Trouve les imputations avec un montant supérieur à un seuil
     *
     * @param company L'entreprise
     * @param threshold Montant seuil
     * @param pageable Pagination
     * @return Page d'imputations importantes
     */
    Page<DepositApplication> findByCompanyAndAmountTtcGreaterThanEqual(
        Company company,
        BigDecimal threshold,
        Pageable pageable
    );

    // =====================================================================
    // Requêtes de nettoyage/maintenance
    // =====================================================================

    /**
     * Supprime toutes les imputations d'un acompte
     * (utilisé lors de l'annulation complète d'un acompte)
     *
     * @param deposit L'acompte
     */
    void deleteByDeposit(Deposit deposit);

    /**
     * Supprime toutes les imputations sur une facture
     * (utilisé lors de l'annulation d'une facture)
     *
     * @param invoice La facture
     */
    void deleteByInvoice(Invoice invoice);
}
