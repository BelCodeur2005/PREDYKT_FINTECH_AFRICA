package com.predykt.accounting.domain.enums;

/**
 * Types d'opérations en suspens dans un rapprochement bancaire
 * Conforme OHADA - explications des écarts entre solde banque et livre
 */
public enum PendingItemType {
    // Opérations affectant le solde banque (à ajouter au solde relevé)
    CHEQUE_ISSUED_NOT_CASHED("Chèques émis non encaissés", "BANK", true),
    DEPOSIT_IN_TRANSIT("Dépôts/virements en cours", "BANK", false),
    BANK_ERROR("Erreur bancaire", "BANK", true),

    // Opérations affectant le solde livre (à ajouter au solde comptable)
    CREDIT_NOT_RECORDED("Virements reçus non comptabilisés", "BOOK", true),
    DEBIT_NOT_RECORDED("Prélèvements non comptabilisés", "BOOK", false),
    BANK_FEES_NOT_RECORDED("Frais bancaires non enregistrés", "BOOK", false),
    INTEREST_NOT_RECORDED("Intérêts non enregistrés", "BOOK", true),
    DIRECT_DEBIT_NOT_RECORDED("Prélèvements automatiques non comptabilisés", "BOOK", false),
    BANK_CHARGES_NOT_RECORDED("Agios non enregistrés", "BOOK", false),

    // Opérations diverses
    UNCATEGORIZED("Non catégorisé", "OTHER", true);

    private final String displayName;
    private final String side; // BANK (ajuste solde banque) ou BOOK (ajuste solde livre)
    private final boolean isAddition; // true = ajouter, false = soustraire

    PendingItemType(String displayName, String side, boolean isAddition) {
        this.displayName = displayName;
        this.side = side;
        this.isAddition = isAddition;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSide() {
        return side;
    }

    public boolean isAddition() {
        return isAddition;
    }

    /**
     * Retourne true si cette opération ajuste le solde bancaire
     */
    public boolean affectsBankBalance() {
        return "BANK".equals(side);
    }

    /**
     * Retourne true si cette opération ajuste le solde livre
     */
    public boolean affectsBookBalance() {
        return "BOOK".equals(side);
    }
}
