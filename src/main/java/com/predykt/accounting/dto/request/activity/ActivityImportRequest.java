package com.predykt.accounting.dto.request.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Requête pour importer des activités
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityImportRequest {

    /**
     * ID du template à utiliser (optionnel, si null utilise le template par défaut)
     */
    private Long templateId;

    /**
     * Format du fichier (optionnel, détection automatique si null)
     */
    private String format;

    /**
     * Mode prévisualisation (ne sauvegarde pas les données)
     */
    @Builder.Default
    private Boolean preview = false;

    /**
     * Créer les règles de mapping manquantes automatiquement
     */
    @Builder.Default
    private Boolean autoLearnMappings = false;
}
