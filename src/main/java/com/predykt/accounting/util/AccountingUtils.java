package com.predykt.accounting.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Utilitaires comptables généraux
 */
public class AccountingUtils {
    
    private AccountingUtils() {
        // Classe utilitaire - constructeur privé
    }
    
    /**
     * Arrondit un montant à 2 décimales (standard comptable)
     */
    public static BigDecimal roundAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Vérifie si deux montants sont égaux (tolérance de 0.01)
     */
    public static boolean amountsEqual(BigDecimal amount1, BigDecimal amount2) {
        if (amount1 == null && amount2 == null) {
            return true;
        }
        if (amount1 == null || amount2 == null) {
            return false;
        }
        
        BigDecimal diff = amount1.subtract(amount2).abs();
        return diff.compareTo(new BigDecimal("0.01")) <= 0;
    }
    
    /**
     * Calcule un pourcentage avec précision
     */
    public static BigDecimal calculatePercentage(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (part == null) {
            return BigDecimal.ZERO;
        }
        
        return part.divide(total, 4, RoundingMode.HALF_UP)
                   .multiply(BigDecimal.valueOf(100))
                   .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calcule la somme d'une liste de montants
     */
    public static BigDecimal sum(BigDecimal... amounts) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal amount : amounts) {
            if (amount != null) {
                total = total.add(amount);
            }
        }
        return total;
    }
    
    /**
     * Formate un montant pour affichage (avec séparateurs de milliers)
     */
    public static String formatAmount(BigDecimal amount, String currencyCode) {
        if (amount == null) {
            return "0.00";
        }
        
        java.text.NumberFormat formatter = java.text.NumberFormat.getCurrencyInstance(
            java.util.Locale.FRANCE
        );
        formatter.setCurrency(java.util.Currency.getInstance(currencyCode));
        
        return formatter.format(amount);
    }
    
    /**
     * Détermine le début de l'exercice fiscal
     */
    public static LocalDate getFiscalYearStart(LocalDate date, String fiscalYearStart) {
        // fiscalYearStart au format "MM-DD" (ex: "01-01")
        String[] parts = fiscalYearStart.split("-");
        int month = Integer.parseInt(parts[0]);
        int day = Integer.parseInt(parts[1]);
        
        LocalDate fiscalStart = LocalDate.of(date.getYear(), month, day);
        
        // Si la date est avant le début de l'exercice, prendre l'année précédente
        if (date.isBefore(fiscalStart)) {
            fiscalStart = fiscalStart.minusYears(1);
        }
        
        return fiscalStart;
    }
    
    /**
     * Détermine la fin de l'exercice fiscal
     */
    public static LocalDate getFiscalYearEnd(LocalDate date, String fiscalYearEnd) {
        // fiscalYearEnd au format "MM-DD" (ex: "12-31")
        String[] parts = fiscalYearEnd.split("-");
        int month = Integer.parseInt(parts[0]);
        int day = Integer.parseInt(parts[1]);
        
        LocalDate fiscalEnd = LocalDate.of(date.getYear(), month, day);
        
        // Si la date est après la fin de l'exercice, prendre l'année suivante
        if (date.isAfter(fiscalEnd)) {
            fiscalEnd = fiscalEnd.plusYears(1);
        }
        
        return fiscalEnd;
    }
    
    /**
     * Obtient le premier jour du mois
     */
    public static LocalDate getFirstDayOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.firstDayOfMonth());
    }
    
    /**
     * Obtient le dernier jour du mois
     */
    public static LocalDate getLastDayOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfMonth());
    }
    
    /**
     * Calcule le nombre de jours entre deux dates
     */
    public static long daysBetween(LocalDate startDate, LocalDate endDate) {
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
    }
    
    /**
     * Vérifie si un montant est positif
     */
    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Vérifie si un montant est négatif
     */
    public static boolean isNegative(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) < 0;
    }
    
    /**
     * Obtient la valeur absolue d'un montant
     */
    public static BigDecimal abs(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.abs();
    }
    
    /**
     * Inverse le signe d'un montant
     */
    public static BigDecimal negate(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.negate();
    }
    
    /**
     * Génère une référence unique pour une écriture
     */
    public static String generateEntryReference(String journalCode, LocalDate date) {
        return String.format("%s-%s-%06d", 
            journalCode, 
            date.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")),
            (int)(Math.random() * 1000000)
        );
    }
    
    /**
     * Valide un numéro de compte OHADA
     */
    public static boolean isValidOHADAAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isEmpty()) {
            return false;
        }
        
        // Compte OHADA: commence par 1-9, longueur 1-7
        return accountNumber.matches("^[1-9]\\d{0,6}$");
    }
    
    /**
     * Extrait la classe du compte OHADA (premier chiffre)
     */
    public static int getAccountClass(String accountNumber) {
        if (accountNumber == null || accountNumber.isEmpty()) {
            throw new IllegalArgumentException("Numéro de compte invalide");
        }
        
        return Character.getNumericValue(accountNumber.charAt(0));
    }
    
    /**
     * Détermine si un compte est de type Actif (classes 2, 3, 4, 5)
     */
    public static boolean isAssetAccount(String accountNumber) {
        int classe = getAccountClass(accountNumber);
        return classe >= 2 && classe <= 5;
    }
    
    /**
     * Détermine si un compte est de type Passif (classe 1)
     */
    public static boolean isLiabilityAccount(String accountNumber) {
        return getAccountClass(accountNumber) == 1;
    }
    
    /**
     * Détermine si un compte est de type Charge (classe 6)
     */
    public static boolean isExpenseAccount(String accountNumber) {
        return getAccountClass(accountNumber) == 6;
    }
    
    /**
     * Détermine si un compte est de type Produit (classe 7)
     */
    public static boolean isRevenueAccount(String accountNumber) {
        return getAccountClass(accountNumber) == 7;
    }
}