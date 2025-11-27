package com.predykt.accounting.domain.enums;

/**
 * Banques supportées en Afrique Centrale et de l'Ouest (CEMAC + UEMOA)
 * avec leurs formats de relevés bancaires supportés
 */
public enum BankProvider {
    // CEMAC (Cameroun, Gabon, Congo, RCA, Tchad, Guinée Équatoriale)
    AFRILAND_FIRST_BANK("Afriland First Bank", "CEMAC", "MT940, CSV personnalisé"),
    BICEC("BICEC", "CEMAC", "MT940, CSV générique"),
    ECOBANK_CEMAC("Ecobank", "CEMAC", "OFX, CSV personnalisé"),
    SCB_CAMEROUN("Société Commerciale de Banque", "CEMAC", "CSV générique"),
    SGBC("Société Générale de Banques au Cameroun", "CEMAC", "CAMT.053, MT940, CSV personnalisé"),
    UBA_CAMEROUN("United Bank for Africa", "CEMAC", "OFX, CSV personnalisé"),

    // UEMOA (Côte d'Ivoire, Sénégal, Bénin, Burkina Faso, Mali, Niger, Togo, Guinée-Bissau)
    BOA("Bank of Africa", "UEMOA", "OFX, MT940, CSV générique"),
    CORIS_BANK("Coris Bank International", "UEMOA", "CSV générique"),
    ECOBANK_UEMOA("Ecobank", "UEMOA", "OFX, CSV personnalisé"),
    NSIA_BANQUE("NSIA Banque", "UEMOA", "CSV générique"),
    ORABANK("Orabank", "UEMOA", "OFX, CSV générique"),
    BRIDGE_BANK("Bridge Bank Group", "UEMOA", "CSV générique"),

    // Panafricaine
    UBA_GROUP("United Bank for Africa", "PANAFRICAIN", "OFX, CSV personnalisé"),
    STANDARD_BANK("Standard Bank", "PANAFRICAIN", "OFX, MT940, CSV générique"),

    // Format générique
    GENERIC("Format CSV Générique", "GENERIC", "CSV, OFX, QIF, MT940, CAMT.053");

    private final String displayName;
    private final String zone;
    private final String supportedFormats;

    BankProvider(String displayName, String zone, String supportedFormats) {
        this.displayName = displayName;
        this.zone = zone;
        this.supportedFormats = supportedFormats;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getZone() {
        return zone;
    }

    public String getSupportedFormats() {
        return supportedFormats;
    }

    /**
     * Vérifie si cette banque supporte le format OFX
     */
    public boolean supportsOfx() {
        return supportedFormats.contains("OFX");
    }

    /**
     * Vérifie si cette banque supporte le format MT940
     */
    public boolean supportsMt940() {
        return supportedFormats.contains("MT940");
    }

    /**
     * Vérifie si cette banque supporte le format CAMT.053
     */
    public boolean supportsCamt053() {
        return supportedFormats.contains("CAMT.053");
    }
}
