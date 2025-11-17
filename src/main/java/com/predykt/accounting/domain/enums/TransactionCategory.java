package com.predykt.accounting.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransactionCategory {
    // Produits
    VENTES_MARCHANDISES("Ventes de marchandises", "701"),
    VENTES_SERVICES("Prestations de services", "706"),
    PRODUITS_FINANCIERS("Produits financiers", "77"),
    
    // Charges
    ACHATS_MARCHANDISES("Achats de marchandises", "601"),
    ACHATS_FOURNITURES("Achats de fournitures", "605"),
    SALAIRES("Salaires et charges sociales", "66"),
    LOYERS("Loyers", "622"),
    ENERGIE("Eau, électricité, téléphone", "624"),
    ENTRETIEN("Entretien et réparations", "625"),
    ASSURANCES("Primes d'assurances", "616"),
    PUBLICITE("Publicité", "627"),
    TRANSPORT("Frais de transport", "628"),
    HONORAIRES("Honoraires et frais d'actes", "632"),
    IMPOTS_TAXES("Impôts et taxes", "64"),
    CHARGES_FINANCIERES("Charges financières", "67"),
    
    // Trésorerie
    VIREMENT_INTERNE("Virement interne", "58"),
    APPORT_CAPITAL("Apport en capital", "101"),
    PRET_RECU("Emprunt reçu", "16"),
    REMBOURSEMENT_PRET("Remboursement d'emprunt", "16"),
    
    // Non catégorisé
    AUTRES("Autres opérations", "00"),
    NON_CATEGORISE("Non catégorisé", "99");
    
    private final String label;
    private final String defaultAccountNumber;
}