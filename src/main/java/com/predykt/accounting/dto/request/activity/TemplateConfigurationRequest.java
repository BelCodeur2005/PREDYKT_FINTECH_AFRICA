package com.predykt.accounting.dto.request.activity;

import com.predykt.accounting.domain.enums.ImportFileFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Requête pour créer/modifier un template d'import personnalisé
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateConfigurationRequest {

    @NotBlank(message = "Le nom du template est obligatoire")
    private String templateName;

    private String description;

    @Builder.Default
    private ImportFileFormat fileFormat = ImportFileFormat.CSV;

    @Builder.Default
    private Character separator = ';';

    @Builder.Default
    private String encoding = "UTF-8";

    @Builder.Default
    private Boolean hasHeader = true;

    @Builder.Default
    private Integer skipRows = 0;

    // Configuration Excel
    private String worksheetName;
    private Integer startRow;
    private Integer endRow;

    // Mapping des colonnes (JSON)
    @NotNull(message = "Le mapping des colonnes est obligatoire")
    private Map<String, Object> columnMapping;

    // Règles de validation (optionnel)
    private Map<String, Object> validationRules;

    // Transformations (optionnel)
    private Map<String, Object> transformations;

    @Builder.Default
    private Boolean isDefault = false;

    @Builder.Default
    private Boolean isActive = true;
}
