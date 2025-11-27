package com.predykt.accounting.dto.response.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Ligne de prévisualisation d'activité
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityPreviewRow {
    private Integer rowNumber;
    private LocalDate date;
    private String activity;
    private String description;
    private BigDecimal amount;
    private String type;

    // Détection automatique
    private String detectedAccount;
    private String accountName;
    private String journalCode;
    private String confidence;        // HIGH, MEDIUM, LOW

    // Validation
    private Boolean isValid;
    private List<String> warnings;
    private List<String> errors;
}
