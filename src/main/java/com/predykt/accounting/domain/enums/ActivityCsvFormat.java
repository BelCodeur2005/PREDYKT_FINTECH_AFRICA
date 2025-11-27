package com.predykt.accounting.domain.enums;

/**
 * Formats CSV d'activités supportés
 */
public enum ActivityCsvFormat {
    /**
     * Format générique PREDYKT
     * Format: date de saisie;Activitées;description;Montant Brut;Type;Années
     */
    GENERIC("Format Générique PREDYKT", "csv"),

    /**
     * Export SAP
     * Format: Posting Date|Document Type|GL Account|Amount|Description
     */
    SAP_EXPORT("Export SAP", "csv,txt"),

    /**
     * Export QuickBooks
     * Format: Date,Type,No.,Name,Memo,Account,Amount,Currency
     */
    QUICKBOOKS("Export QuickBooks", "csv"),

    /**
     * Export Sage
     * Format: Date;Journal;Compte;Libellé;Débit;Crédit
     */
    SAGE("Export Sage", "csv"),

    /**
     * Format Excel personnalisé
     */
    EXCEL_CUSTOM("Excel Personnalisé", "xlsx,xls"),

    /**
     * Via template configuré (CustomActivityCsvParser)
     */
    CUSTOM_TEMPLATE("Template Personnalisé", "csv,xlsx");

    private final String displayName;
    private final String supportedExtensions;

    ActivityCsvFormat(String displayName, String supportedExtensions) {
        this.displayName = displayName;
        this.supportedExtensions = supportedExtensions;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSupportedExtensions() {
        return supportedExtensions;
    }

    /**
     * Détecte le format à partir du nom de fichier
     */
    public static ActivityCsvFormat detectFromFileName(String fileName) {
        if (fileName == null) {
            return GENERIC;
        }

        String lowerName = fileName.toLowerCase();

        if (lowerName.contains("sap") || lowerName.contains("export_sap")) {
            return SAP_EXPORT;
        }
        if (lowerName.contains("quickbooks") || lowerName.contains("qb_")) {
            return QUICKBOOKS;
        }
        if (lowerName.contains("sage")) {
            return SAGE;
        }
        if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
            return EXCEL_CUSTOM;
        }

        return GENERIC;
    }
}
