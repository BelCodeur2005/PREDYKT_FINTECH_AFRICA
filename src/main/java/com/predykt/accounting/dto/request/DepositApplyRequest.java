package com.predykt.accounting.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour l'imputation d'un acompte sur une facture finale.
 *
 * Processus OHADA:
 * 1. Vérification: acompte non imputé, client correspond, montant <= total facture
 * 2. Imputation: acompte.invoice = facture, acompte.isApplied = true
 * 3. Écriture comptable: Débit 4191 Avances / Crédit 411 Clients
 * 4. Mise à jour facture: invoice.amountPaid += deposit.amountTtc
 *
 * @author PREDYKT System Optimizer
 * @since Phase 3 - Conformité OHADA Avancée
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositApplyRequest {

    @NotNull(message = "L'ID de la facture est obligatoire")
    private Long invoiceId;
}
