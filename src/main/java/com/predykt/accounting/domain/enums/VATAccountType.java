package com.predykt.accounting.domain.enums;

import lombok.Getter;

/**
 * Types de comptes TVA selon le plan comptable OHADA SYSCOHADA
 * Classe 44 - État et collectivités publiques
 */
@Getter
public enum VATAccountType {

    /**
     * 4431 - TVA facturée sur ventes de marchandises
     */
    VAT_COLLECTED_SALES("4431", "TVA facturée sur ventes", true, "TVA collectée sur ventes de marchandises"),

    /**
     * 4432 - TVA facturée sur prestations de services
     */
    VAT_COLLECTED_SERVICES("4432", "TVA facturée sur prestations de services", true, "TVA collectée sur services rendus"),

    /**
     * 4433 - TVA facturée sur travaux
     */
    VAT_COLLECTED_WORKS("4433", "TVA facturée sur travaux", true, "TVA collectée sur travaux réalisés"),

    /**
     * 4434 - TVA facturée sur production livrée à soi-même
     */
    VAT_COLLECTED_SELF_DELIVERY("4434", "TVA sur production livrée à soi-même", true, "TVA sur immobilisations produites par l'entreprise"),

    /**
     * 4441 - État, TVA due ou crédit de TVA
     */
    VAT_DUE("4441", "TVA due ou crédit de TVA", false, "Solde net de TVA à payer ou crédit de TVA"),

    /**
     * 4451 - TVA récupérable sur immobilisations
     */
    VAT_RECOVERABLE_FIXED_ASSETS("4451", "TVA récupérable sur immobilisations", true, "TVA déductible sur acquisitions d'immobilisations"),

    /**
     * 4452 - TVA récupérable sur achats de marchandises
     */
    VAT_RECOVERABLE_PURCHASES("4452", "TVA récupérable sur achats", true, "TVA déductible sur achats de marchandises et matières"),

    /**
     * 4453 - TVA récupérable sur transport
     */
    VAT_RECOVERABLE_TRANSPORT("4453", "TVA récupérable sur transport", true, "TVA déductible sur frais de transport"),

    /**
     * 4454 - TVA récupérable sur services extérieurs et autres charges
     */
    VAT_RECOVERABLE_SERVICES("4454", "TVA récupérable sur services extérieurs", true, "TVA déductible sur prestations de services"),

    /**
     * 4455 - TVA récupérable sur factures non parvenues
     */
    VAT_RECOVERABLE_UNINVOICED("4455", "TVA récupérable sur factures non parvenues", true, "TVA sur achats comptabilisés mais non facturés"),

    /**
     * 4456 - TVA transférée par d'autres entreprises
     */
    VAT_TRANSFERRED("4456", "TVA transférée par d'autres entreprises", true, "TVA transférée dans le cadre de groupes fiscaux");

    private final String accountNumber;
    private final String accountName;
    private final boolean isRecoverable;  // true = déductible, false = solde
    private final String description;

    VATAccountType(String accountNumber, String accountName, boolean isRecoverable, String description) {
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.isRecoverable = isRecoverable;
        this.description = description;
    }

    /**
     * Détermine si ce compte est un compte de TVA collectée (crédit)
     */
    public boolean isVATCollected() {
        return accountNumber.startsWith("443");
    }

    /**
     * Détermine si ce compte est un compte de TVA déductible (débit)
     */
    public boolean isVATDeductible() {
        return accountNumber.startsWith("445");
    }

    /**
     * Détermine si ce compte est le compte de TVA due/crédit
     */
    public boolean isVATDue() {
        return accountNumber.equals("4441");
    }

    /**
     * Trouve le type de compte par numéro de compte
     */
    public static VATAccountType fromAccountNumber(String accountNumber) {
        for (VATAccountType type : values()) {
            if (type.getAccountNumber().equals(accountNumber)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Retourne tous les comptes de TVA collectée
     */
    public static VATAccountType[] getCollectedAccounts() {
        return new VATAccountType[]{
            VAT_COLLECTED_SALES,
            VAT_COLLECTED_SERVICES,
            VAT_COLLECTED_WORKS,
            VAT_COLLECTED_SELF_DELIVERY
        };
    }

    /**
     * Retourne tous les comptes de TVA déductible
     */
    public static VATAccountType[] getDeductibleAccounts() {
        return new VATAccountType[]{
            VAT_RECOVERABLE_FIXED_ASSETS,
            VAT_RECOVERABLE_PURCHASES,
            VAT_RECOVERABLE_TRANSPORT,
            VAT_RECOVERABLE_SERVICES,
            VAT_RECOVERABLE_UNINVOICED,
            VAT_TRANSFERRED
        };
    }
}
