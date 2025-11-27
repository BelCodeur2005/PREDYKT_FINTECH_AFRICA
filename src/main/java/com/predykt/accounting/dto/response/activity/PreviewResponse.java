package com.predykt.accounting.dto.response.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Réponse de prévisualisation d'import
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewResponse {
    private String fileName;
    private Integer totalRows;
    private Integer validRows;
    private Integer invalidRows;
    private List<ActivityPreviewRow> rows;          // Échantillon (max 50)
    private List<String> globalWarnings;
    private Map<String, Integer> accountDistribution;  // Compte OHADA → nombre de lignes
    private Map<String, Integer> confidenceDistribution; // HIGH/MEDIUM/LOW → count
}
