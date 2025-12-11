package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour une imputation partielle d'acompte.
 *
 * Contient toutes les informations sur une imputation partielle,
 * incluant les montants, les dates, et les références aux entités liées.
 *
 * @author PREDYKT Accounting Team
 * @version 2.0 (Phase 2)
 * @since 2025-12-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositApplicationResponse {

    /**
     * ID technique de l'imputation
     */
    private Long id;

    /**
     * ID de l'acompte source
     */
    private Long depositId;

    /**
     * Numéro du reçu d'acompte (ex: RA-2025-000001)
     */
    private String depositNumber;

    /**
     * ID de la facture destination
     */
    private Long invoiceId;

    /**
     * Numéro de la facture (ex: FV-2025-0045)
     */
    private String invoiceNumber;

    /**
     * ID de l'entreprise (multi-tenant)
     */
    private Long companyId;

    /**
     * Montant HT de cette imputation partielle
     */
    private BigDecimal amountHt;

    /**
     * Taux de TVA appliqué (en %)
     */
    private BigDecimal vatRate;

    /**
     * Montant de TVA
     */
    private BigDecimal vatAmount;

    /**
     * Montant TTC de cette imputation
     */
    private BigDecimal amountTtc;

    /**
     * Date/heure de l'imputation
     */
    private LocalDateTime appliedAt;

    /**
     * Utilisateur ayant effectué l'imputation
     */
    private String appliedBy;

    /**
     * ID de l'écriture comptable générée
     */
    private Long journalEntryId;

    /**
     * Notes sur l'imputation
     */
    private String notes;

    /**
     * Pourcentage de l'acompte total que représente cette imputation
     */
    private BigDecimal percentageOfDeposit;

    /**
     * Pourcentage de la facture que représente cette imputation
     */
    private BigDecimal percentageOfInvoice;

    /**
     * Description formatée de l'imputation
     */
    private String description;

    /**
     * Date de création
     */
    private LocalDateTime createdAt;

    /**
     * Date de dernière modification
     */
    private LocalDateTime updatedAt;
}
