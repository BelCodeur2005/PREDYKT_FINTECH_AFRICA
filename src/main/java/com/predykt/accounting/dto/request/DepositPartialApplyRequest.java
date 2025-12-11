package com.predykt.accounting.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour la requête d'imputation partielle d'un acompte sur une facture.
 *
 * Phase 2 - Imputation Partielle:
 * Permet de fractionner un acompte sur plusieurs factures en spécifiant
 * le montant exact à imputer.
 *
 * Exemple d'utilisation:
 * POST /api/v1/companies/1/deposits/5/apply-partial
 * {
 *   "invoiceId": 123,
 *   "amountToApply": 100000.00,
 *   "notes": "Premier acompte partiel sur commande #CMD-001"
 * }
 *
 * @author PREDYKT Accounting Team
 * @version 2.0 (Phase 2)
 * @since 2025-12-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositPartialApplyRequest {

    /**
     * ID de la facture sur laquelle imputer l'acompte
     */
    @NotNull(message = "L'ID de la facture est obligatoire")
    private Long invoiceId;

    /**
     * Montant TTC à imputer sur cette facture
     * Doit être:
     * - Strictement positif
     * - <= au montant restant disponible de l'acompte
     * - <= au montant restant dû sur la facture
     */
    @NotNull(message = "Le montant à imputer est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant à imputer doit être strictement positif")
    private BigDecimal amountToApply;

    /**
     * Notes optionnelles sur cette imputation partielle
     * Exemple: "Acompte 1/3 sur commande CMD-001"
     */
    private String notes;
}
