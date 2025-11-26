package com.predykt.accounting.domain.enums;

/**
 * Banques supportées en Afrique Centrale et de l'Ouest (CEMAC + UEMOA)
 */
public enum BankProvider {
    // CEMAC (Cameroun, Gabon, Congo, RCA, Tchad, Guinée Équatoriale)
    AFRILAND_FIRST_BANK("Afriland First Bank", "CEMAC"),
    BICEC("BICEC", "CEMAC"),
    ECOBANK_CEMAC("Ecobank", "CEMAC"),
    SCB_CAMEROUN("Société Commerciale de Banque", "CEMAC"),
    SGBC("Société Générale de Banques au Cameroun", "CEMAC"),
    UBA_CAMEROUN("United Bank for Africa", "CEMAC"),

    // UEMOA (Côte d'Ivoire, Sénégal, Bénin, Burkina Faso, Mali, Niger, Togo, Guinée-Bissau)
    BOA("Bank of Africa", "UEMOA"),
    CORIS_BANK("Coris Bank International", "UEMOA"),
    ECOBANK_UEMOA("Ecobank", "UEMOA"),
    NSIA_BANQUE("NSIA Banque", "UEMOA"),
    ORABANK("Orabank", "UEMOA"),
    BRIDGE_BANK("Bridge Bank Group", "UEMOA"),

    // Panafricaine
    UBA_GROUP("United Bank for Africa", "PANAFRICAIN"),
    STANDARD_BANK("Standard Bank", "PANAFRICAIN"),

    // Format générique
    GENERIC("Format CSV Générique", "GENERIC");

    private final String displayName;
    private final String zone;

    BankProvider(String displayName, String zone) {
        this.displayName = displayName;
        this.zone = zone;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getZone() {
        return zone;
    }
}
