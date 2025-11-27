package com.predykt.accounting.controller;

import com.predykt.accounting.domain.entity.ActivityImportTemplate;
import com.predykt.accounting.dto.request.activity.TemplateConfigurationRequest;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.activity.TemplateResponse;
import com.predykt.accounting.service.ActivityTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller pour gérer les templates d'import personnalisés
 */
@RestController
@RequestMapping("/companies/{companyId}/activity-templates")
@RequiredArgsConstructor
@Tag(name = "Templates d'Import", description = "Gestion des templates d'import personnalisés")
public class ActivityTemplateController {

    private final ActivityTemplateService templateService;

    @GetMapping
    @Operation(summary = "Lister les templates",
               description = "Récupère tous les templates d'import actifs d'une entreprise")
    public ResponseEntity<ApiResponse<List<TemplateResponse>>> getAllTemplates(
            @PathVariable Long companyId,
            @RequestParam(required = false, defaultValue = "true") Boolean activeOnly) {

        List<ActivityImportTemplate> templates = activeOnly
            ? templateService.getActiveTemplates(companyId)
            : templateService.getAllTemplates(companyId);

        List<TemplateResponse> responses = templates.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{templateId}")
    @Operation(summary = "Détails d'un template",
               description = "Récupère les détails complets d'un template")
    public ResponseEntity<ApiResponse<TemplateResponse>> getTemplate(
            @PathVariable Long companyId,
            @PathVariable Long templateId) {

        ActivityImportTemplate template = templateService.getTemplate(templateId);

        return ResponseEntity.ok(ApiResponse.success(toResponseWithDetails(template)));
    }

    @PostMapping
    @Operation(summary = "Créer un template",
               description = "Crée un nouveau template d'import personnalisé")
    public ResponseEntity<ApiResponse<TemplateResponse>> createTemplate(
            @PathVariable Long companyId,
            @Valid @RequestBody TemplateConfigurationRequest request) {

        ActivityImportTemplate template = templateService.createTemplate(companyId, request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(toResponseWithDetails(template), "Template créé avec succès"));
    }

    @PutMapping("/{templateId}")
    @Operation(summary = "Modifier un template",
               description = "Met à jour un template existant")
    public ResponseEntity<ApiResponse<TemplateResponse>> updateTemplate(
            @PathVariable Long companyId,
            @PathVariable Long templateId,
            @Valid @RequestBody TemplateConfigurationRequest request) {

        ActivityImportTemplate template = templateService.updateTemplate(templateId, request);

        return ResponseEntity.ok(ApiResponse.success(toResponseWithDetails(template), "Template mis à jour"));
    }

    @DeleteMapping("/{templateId}")
    @Operation(summary = "Supprimer un template")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @PathVariable Long companyId,
            @PathVariable Long templateId) {

        templateService.deleteTemplate(templateId);

        return ResponseEntity.ok(ApiResponse.success(null, "Template supprimé"));
    }

    @PostMapping("/{templateId}/set-default")
    @Operation(summary = "Définir comme template par défaut",
               description = "Marque ce template comme template par défaut pour l'entreprise")
    public ResponseEntity<ApiResponse<TemplateResponse>> setAsDefault(
            @PathVariable Long companyId,
            @PathVariable Long templateId) {

        ActivityImportTemplate template = templateService.setAsDefault(templateId);

        return ResponseEntity.ok(ApiResponse.success(toResponse(template), "Template défini par défaut"));
    }

    @GetMapping("/default")
    @Operation(summary = "Récupérer le template par défaut",
               description = "Récupère le template par défaut de l'entreprise s'il existe")
    public ResponseEntity<ApiResponse<TemplateResponse>> getDefaultTemplate(
            @PathVariable Long companyId) {

        return templateService.getDefaultTemplate(companyId)
            .map(template -> ResponseEntity.ok(ApiResponse.success(toResponseWithDetails(template))))
            .orElse(ResponseEntity.ok(ApiResponse.success(null, "Aucun template par défaut")));
    }

    private TemplateResponse toResponse(ActivityImportTemplate template) {
        return TemplateResponse.builder()
            .id(template.getId())
            .templateName(template.getTemplateName())
            .description(template.getDescription())
            .fileFormat(template.getFileFormat())
            .separator(template.getSeparator())
            .encoding(template.getEncoding())
            .hasHeader(template.getHasHeader())
            .isDefault(template.getIsDefault())
            .isActive(template.getIsActive())
            .usageCount(template.getUsageCount())
            .lastUsedAt(template.getLastUsedAt())
            .createdAt(template.getCreatedAt())
            .build();
    }

    private TemplateResponse toResponseWithDetails(ActivityImportTemplate template) {
        TemplateResponse response = toResponse(template);
        response.setColumnMapping(template.getColumnMapping());
        response.setValidationRules(template.getValidationRules());
        response.setTransformations(template.getTransformations());
        return response;
    }
}
