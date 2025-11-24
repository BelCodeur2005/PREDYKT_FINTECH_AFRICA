package com.predykt.accounting.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utilitaires de gestion des devises
 */
public class CurrencyUtils {
    
    private CurrencyUtils() {
        // Classe utilitaire - constructeur privé
    }
    
    // Cache des taux de change (à actualiser périodiquement via API externe)
    private static final Map<String, BigDecimal> EXCHANGE_RATES = new HashMap<>();
    
    static {
        // Taux par rapport à l'EUR (à actualiser via API)
        EXCHANGE_RATES.put("XAF", new BigDecimal("655.957")); // Franc CFA
        EXCHANGE_RATES.put("EUR", BigDecimal.ONE);
        EXCHANGE_RATES.put("USD", new BigDecimal("1.10"));
        EXCHANGE_RATES.put("GBP", new BigDecimal("0.87"));
    }
    
    /**
     * Convertit un montant d'une devise à une autre
     */
    public static BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }
        
        // Convertir en EUR (devise de référence)
        BigDecimal amountInEUR = amount.divide(
            EXCHANGE_RATES.getOrDefault(fromCurrency, BigDecimal.ONE),
            4,
            RoundingMode.HALF_UP
        );
        
        // Convertir en devise cible
        BigDecimal result = amountInEUR.multiply(
            EXCHANGE_RATES.getOrDefault(toCurrency, BigDecimal.ONE)
        );
        
        return result.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Formate un montant avec le symbole de la devise
     */
    public static String format(BigDecimal amount, String currencyCode) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        
        try {
            Currency currency = Currency.getInstance(currencyCode);
            NumberFormat formatter = NumberFormat.getCurrencyInstance(getLocaleForCurrency(currencyCode));
            formatter.setCurrency(currency);
            
            return formatter.format(amount);
        } catch (IllegalArgumentException e) {
            // Devise non reconnue, formater sans symbole
            return String.format("%,.2f %s", amount, currencyCode);
        }
    }
    
    /**
     * Formate un montant sans symbole de devise
     */
    public static String formatWithoutSymbol(BigDecimal amount, String currencyCode) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        
        NumberFormat formatter = NumberFormat.getNumberInstance(getLocaleForCurrency(currencyCode));
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        
        return formatter.format(amount);
    }
    
    /**
     * Obtient le symbole d'une devise
     */
    public static String getSymbol(String currencyCode) {
        try {
            Currency currency = Currency.getInstance(currencyCode);
            return currency.getSymbol(getLocaleForCurrency(currencyCode));
        } catch (IllegalArgumentException e) {
            return currencyCode;
        }
    }
    
    /**
     * Obtient le nombre de décimales par défaut pour une devise
     */
    public static int getDefaultFractionDigits(String currencyCode) {
        try {
            Currency currency = Currency.getInstance(currencyCode);
            return currency.getDefaultFractionDigits();
        } catch (IllegalArgumentException e) {
            return 2; // Par défaut
        }
    }
    
    /**
     * Arrondit un montant selon les règles de la devise
     */
    public static BigDecimal round(BigDecimal amount, String currencyCode) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        
        int scale = getDefaultFractionDigits(currencyCode);
        return amount.setScale(scale, RoundingMode.HALF_UP);
    }
    
    /**
     * Vérifie si une devise est valide
     */
    public static boolean isValidCurrency(String currencyCode) {
        try {
            Currency.getInstance(currencyCode);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Obtient la locale appropriée pour une devise
     */
    private static Locale getLocaleForCurrency(String currencyCode) {
        return switch (currencyCode) {
            case "XAF", "XOF" -> Locale.FRANCE; // Franc CFA
            case "EUR" -> Locale.FRANCE;
            case "USD" -> Locale.US;
            case "GBP" -> Locale.UK;
            default -> Locale.getDefault();
        };
    }
    
    /**
     * Obtient le taux de change actuel entre deux devises
     */
    public static BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }
        
        BigDecimal fromRate = EXCHANGE_RATES.getOrDefault(fromCurrency, BigDecimal.ONE);
        BigDecimal toRate = EXCHANGE_RATES.getOrDefault(toCurrency, BigDecimal.ONE);
        
        return toRate.divide(fromRate, 6, RoundingMode.HALF_UP);
    }
    
    /**
     * Met à jour le taux de change d'une devise
     * (À appeler périodiquement via API externe)
     */
    public static void updateExchangeRate(String currencyCode, BigDecimal rateToEUR) {
        EXCHANGE_RATES.put(currencyCode, rateToEUR);
    }
    
    /**
     * Formate un montant en lettres (pour chèques, factures)
     */
    public static String toWords(BigDecimal amount, String currencyCode) {
        // Implémentation simplifiée pour XAF/EUR
        // À compléter selon les besoins
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "Zéro";
        }
        
        long integerPart = amount.longValue();
        long decimalPart = amount.remainder(BigDecimal.ONE)
                                  .multiply(BigDecimal.valueOf(100))
                                  .longValue();
        
        String words = convertToWords(integerPart);
        
        if (decimalPart > 0) {
            words += " et " + decimalPart + "/100";
        }
        
        // Ajouter le nom de la devise
        words += " " + getCurrencyName(currencyCode);
        
        return words;
    }
    
    /**
     * Convertit un nombre en mots (français)
     */
    private static String convertToWords(long number) {
        if (number == 0) return "zéro";
        
        String[] units = {"", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf"};
        String[] teens = {"dix", "onze", "douze", "treize", "quatorze", "quinze", "seize", 
                          "dix-sept", "dix-huit", "dix-neuf"};
        String[] tens = {"", "", "vingt", "trente", "quarante", "cinquante", 
                         "soixante", "soixante-dix", "quatre-vingt", "quatre-vingt-dix"};
        
        if (number < 10) return units[(int)number];
        if (number < 20) return teens[(int)number - 10];
        if (number < 100) {
            int ten = (int)(number / 10);
            int unit = (int)(number % 10);
            return tens[ten] + (unit > 0 ? "-" + units[unit] : "");
        }
        
        // Simplification pour nombres > 100
        return String.valueOf(number);
    }
    
    /**
     * Obtient le nom complet de la devise
     */
    private static String getCurrencyName(String currencyCode) {
        return switch (currencyCode) {
            case "XAF" -> "Francs CFA";
            case "EUR" -> "Euros";
            case "USD" -> "Dollars";
            case "GBP" -> "Livres Sterling";
            default -> currencyCode;
        };
    }
}