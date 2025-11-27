package com.predykt.accounting.dto.response.activity;

import com.predykt.accounting.domain.enums.ImportFileFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateResponse {
    private Long id;
    private String templateName;
    private String description;
    private ImportFileFormat fileFormat;
    private Character separator;
    private String encoding;
    private Boolean hasHeader;
    private Boolean isDefault;
    private Boolean isActive;
    private Integer usageCount;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;

    // Configuration détaillée (optionnel selon contexte)
    private Map<String, Object> columnMapping;
    private Map<String, Object> validationRules;
    private Map<String, Object> transformations;
}
