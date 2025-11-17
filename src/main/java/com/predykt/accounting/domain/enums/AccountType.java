package com.predykt.accounting.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccountType {
    ACTIF("Actif", "A"),
    ACTIF_IMMOBILISE("Actif Immobilisé", "AI"),
    ACTIF_CIRCULANT("Actif Circulant", "AC"),
    PASSIF("Passif", "P"),
    CAPITAUX_PROPRES("Capitaux Propres", "CP"),
    DETTES("Dettes", "D"),
    CHARGES("Charges", "CH"),
    CHARGES_EXPLOITATION("Charges d'Exploitation", "CE"),
    CHARGES_FINANCIERES("Charges Financières", "CF"),
    PRODUITS("Produits", "PR"),
    PRODUITS_EXPLOITATION("Produits d'Exploitation", "PE"),
    PRODUITS_FINANCIERS("Produits Financiers", "PF");
    
    private final String label;
    private final String code;
    
    /**
     * Détermine le type de compte OHADA basé sur le numéro
     */
    public static AccountType fromAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isEmpty()) {
            throw new IllegalArgumentException("Numéro de compte invalide");
        }
        
        char firstDigit = accountNumber.charAt(0);
        
        return switch (firstDigit) {
            case '1' -> CAPITAUX_PROPRES;
            case '2' -> ACTIF_IMMOBILISE;
            case '3' -> ACTIF_CIRCULANT;
            case '4' -> accountNumber.startsWith("40") ? DETTES : ACTIF_CIRCULANT;
            case '5' -> ACTIF_CIRCULANT;  // Trésorerie
            case '6' -> CHARGES;
            case '7' -> PRODUITS;
            case '8' -> CHARGES;  // Comptes spéciaux
            default -> throw new IllegalArgumentException("Classe de compte OHADA invalide: " + firstDigit);
        };
    }
    
    /**
     * Indique si le compte augmente au débit
     */
    public boolean isDebitNature() {
        return this == ACTIF || this == ACTIF_IMMOBILISE || 
               this == ACTIF_CIRCULANT || this == CHARGES ||
               this == CHARGES_EXPLOITATION || this == CHARGES_FINANCIERES;
    }
}