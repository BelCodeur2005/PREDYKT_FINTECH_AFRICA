package com.predykt.accounting.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO pour l'import de transactions bancaires (format intermédiaire)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankTransactionImportDto {

    /**
     * Date de la transaction
     */
    private LocalDate transactionDate;

    /**
     * Date de valeur (peut être différente de la date transaction)
     */
    private LocalDate valueDate;

    /**
     * Montant de la transaction (positif = crédit, négatif = débit)
     */
    private BigDecimal amount;

    /**
     * Description/libellé de la transaction
     */
    private String description;

    /**
     * Référence unique de la banque
     */
    private String bankReference;

    /**
     * Nom du tiers (client/fournisseur)
     */
    private String thirdPartyName;

    /**
     * Solde après transaction (optionnel)
     */
    private BigDecimal balanceAfter;

    /**
     * Code devise (XAF, EUR, USD, etc.)
     */
    private String currency;

    /**
     * Informations supplémentaires (format libre)
     */
    private String additionalInfo;

    /**
     * Numéro de compte bancaire
     */
    private String accountNumber;
}
