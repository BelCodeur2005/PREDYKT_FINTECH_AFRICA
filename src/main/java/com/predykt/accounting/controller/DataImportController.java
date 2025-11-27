// ============================================
// DataImportController.java
// ============================================
package com.predykt.accounting.controller;

import com.predykt.accounting.dto.request.activity.ActivityImportRequest;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.ImportResultResponse;
import com.predykt.accounting.dto.response.activity.PreviewResponse;
import com.predykt.accounting.service.ActivityImportService;
import com.predykt.accounting.service.CsvImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/companies/{companyId}/import")
@RequiredArgsConstructor
@Tag(name = "Import de Données", description = "Import d'activités comptables (CSV, Excel, formats standards)")
public class DataImportController {

    private final CsvImportService csvImportService;  // Ancien service (legacy)
    private final ActivityImportService activityImportService;  // Nouveau service

    /**
     * LEGACY: Ancien endpoint CSV simple (conservé pour compatibilité)
     */
    @PostMapping(value = "/activities-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "[LEGACY] Importer un fichier CSV d'activités",
               description = "⚠️ Ancien endpoint. Utilisez /activities pour plus de flexibilité")
    public ResponseEntity<ApiResponse<ImportResultResponse>> importActivitiesCsv(
            @PathVariable Long companyId,
            @RequestParam("file") MultipartFile file) {

        ImportResultResponse result = csvImportService.importActivitiesCsv(companyId, file);

        return ResponseEntity
            .status(result.getErrorCount() > 0 ? HttpStatus.PARTIAL_CONTENT : HttpStatus.CREATED)
            .body(ApiResponse.success(result, result.getMessage()));
    }

    /**
     * NOUVEAU: Import d'activités avec templates et formats multiples
     */
    @PostMapping(value = "/activities", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importer des activités (nouveau système flexible)",
               description = "Importe des activités avec support de templates personnalisés, formats multiples (CSV, Excel, SAP, etc.) " +
                           "et mapping automatique vers comptes OHADA. " +
                           "Formats supportés: CSV générique, SAP, QuickBooks, Template personnalisé")
    public ResponseEntity<ApiResponse<ImportResultResponse>> importActivities(
            @PathVariable Long companyId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) String format) {

        ActivityImportRequest request = ActivityImportRequest.builder()
            .templateId(templateId)
            .format(format)
            .preview(false)
            .build();

        ImportResultResponse result = activityImportService.importActivities(companyId, file, request);

        return ResponseEntity
            .status(result.getErrorCount() > 0 ? HttpStatus.PARTIAL_CONTENT : HttpStatus.CREATED)
            .body(ApiResponse.success(result, result.getMessage()));
    }

    /**
     * Prévisualisation d'import sans sauvegarde
     */
    @PostMapping(value = "/activities/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Prévisualiser un import d'activités",
               description = "Analyse le fichier et montre un aperçu des activités détectées, " +
                           "du mapping appliqué et des erreurs potentielles SANS créer les écritures. " +
                           "Permet de valider avant l'import définitif.")
    public ResponseEntity<ApiResponse<PreviewResponse>> previewImport(
            @PathVariable Long companyId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) String format) {

        ActivityImportRequest request = ActivityImportRequest.builder()
            .templateId(templateId)
            .format(format)
            .preview(true)
            .build();

        PreviewResponse preview = activityImportService.previewImport(companyId, file, request);

        return ResponseEntity.ok(ApiResponse.success(preview, "Prévisualisation générée"));
    }
}
