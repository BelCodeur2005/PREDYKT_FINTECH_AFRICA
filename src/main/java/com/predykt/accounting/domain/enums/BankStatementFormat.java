package com.predykt.accounting.domain.enums;

/**
 * Formats de relevés bancaires supportés
 */
public enum BankStatementFormat {
    /**
     * Open Financial Exchange - Format XML standard international
     * Extensions: .ofx, .qfx
     * Banques: Ecobank, UBA, BOA, Standard Bank, Orabank
     */
    OFX("Open Financial Exchange", new String[]{".ofx", ".qfx"}, "application/x-ofx"),

    /**
     * SWIFT MT940 - Format texte SWIFT pour relevés bancaires
     * Extensions: .mt940, .sta, .txt
     * Banques: SGBC, BICEC, Afriland First Bank (transactions internationales)
     */
    MT940("SWIFT MT940", new String[]{".mt940", ".sta", ".txt"}, "text/plain"),

    /**
     * CAMT.053 ISO 20022 - Format XML européen moderne
     * Extensions: .xml, .camt
     * Banques: Société Générale groupe, SGBC
     */
    CAMT_053("CAMT.053 (ISO 20022)", new String[]{".xml", ".camt"}, "application/xml"),

    /**
     * Quicken Interchange Format - Format texte ancien mais simple
     * Extensions: .qif
     * Banques: Support générique
     */
    QIF("Quicken Interchange Format", new String[]{".qif"}, "text/plain"),

    /**
     * CSV Générique - Format CSV standard avec colonnes communes
     * Extensions: .csv
     * Format: Date, Description, Montant, Référence
     */
    CSV_GENERIC("CSV Générique", new String[]{".csv"}, "text/csv"),

    /**
     * CSV spécifique Afriland First Bank
     * Format propriétaire de la banque
     */
    CSV_AFRILAND("CSV Afriland First Bank", new String[]{".csv"}, "text/csv"),

    /**
     * CSV spécifique Ecobank
     * Format propriétaire de la banque
     */
    CSV_ECOBANK("CSV Ecobank", new String[]{".csv"}, "text/csv"),

    /**
     * CSV spécifique UBA
     * Format propriétaire de la banque
     */
    CSV_UBA("CSV UBA", new String[]{".csv"}, "text/csv"),

    /**
     * CSV spécifique SGBC
     * Format propriétaire de la banque
     */
    CSV_SGBC("CSV SGBC", new String[]{".csv"}, "text/csv");

    private final String displayName;
    private final String[] fileExtensions;
    private final String mimeType;

    BankStatementFormat(String displayName, String[] fileExtensions, String mimeType) {
        this.displayName = displayName;
        this.fileExtensions = fileExtensions;
        this.mimeType = mimeType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String[] getFileExtensions() {
        return fileExtensions;
    }

    public String getMimeType() {
        return mimeType;
    }

    /**
     * Détecte le format à partir de l'extension du fichier
     */
    public static BankStatementFormat fromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return CSV_GENERIC;
        }

        String lowerFileName = fileName.toLowerCase();

        // Vérifier les extensions spécifiques
        if (lowerFileName.endsWith(".ofx") || lowerFileName.endsWith(".qfx")) {
            return OFX;
        }
        if (lowerFileName.endsWith(".mt940") || lowerFileName.endsWith(".sta")) {
            return MT940;
        }
        if (lowerFileName.endsWith(".qif")) {
            return QIF;
        }
        if (lowerFileName.endsWith(".xml") || lowerFileName.endsWith(".camt")) {
            return CAMT_053;
        }
        if (lowerFileName.endsWith(".csv")) {
            return CSV_GENERIC; // Par défaut, on peut détecter le type spécifique après
        }

        return CSV_GENERIC;
    }

    /**
     * Détecte le format CSV spécifique à partir du provider
     */
    public static BankStatementFormat fromBankProvider(BankProvider provider, String fileName) {
        if (!fileName.toLowerCase().endsWith(".csv")) {
            return fromFileName(fileName);
        }

        return switch (provider) {
            case AFRILAND_FIRST_BANK -> CSV_AFRILAND;
            case ECOBANK_CEMAC, ECOBANK_UEMOA -> CSV_ECOBANK;
            case UBA_CAMEROUN, UBA_GROUP -> CSV_UBA;
            case SGBC -> CSV_SGBC;
            default -> CSV_GENERIC;
        };
    }

    /**
     * Vérifie si le format est un format XML
     */
    public boolean isXmlFormat() {
        return this == OFX || this == CAMT_053;
    }

    /**
     * Vérifie si le format est un format CSV
     */
    public boolean isCsvFormat() {
        return this == CSV_GENERIC || this == CSV_AFRILAND ||
               this == CSV_ECOBANK || this == CSV_UBA || this == CSV_SGBC;
    }
}
