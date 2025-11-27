package com.predykt.accounting.domain.enums;

/**
 * Formats de fichiers supportés pour l'import d'activités
 */
public enum ImportFileFormat {
    CSV("CSV", ".csv", "text/csv"),
    EXCEL("Microsoft Excel", ".xlsx,.xls", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    JSON("JSON", ".json", "application/json");

    private final String displayName;
    private final String extensions;
    private final String mimeType;

    ImportFileFormat(String displayName, String extensions, String mimeType) {
        this.displayName = displayName;
        this.extensions = extensions;
        this.mimeType = mimeType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getExtensions() {
        return extensions;
    }

    public String getMimeType() {
        return mimeType;
    }

    public static ImportFileFormat fromFileName(String fileName) {
        if (fileName == null) {
            return CSV;
        }
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
            return EXCEL;
        }
        if (lowerName.endsWith(".json")) {
            return JSON;
        }
        return CSV;
    }
}
