package com.predykt.accounting.controller;

import com.predykt.accounting.domain.entity.ActivityMappingRule;
import com.predykt.accounting.dto.request.activity.ActivityMappingRuleRequest;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.activity.ActivityMappingRuleResponse;
import com.predykt.accounting.service.ActivityMappingService;
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
 * Controller pour gérer les règles de mapping activité → compte OHADA
 */
@RestController
@RequestMapping("/companies/{companyId}/activity-mappings")
@RequiredArgsConstructor
@Tag(name = "Mapping d'Activités", description = "Gestion des règles de mapping activité → compte OHADA")
public class ActivityMappingController {

    private final ActivityMappingService mappingService;

    @GetMapping
    @Operation(summary = "Lister les règles de mapping",
               description = "Récupère toutes les règles de mapping d'une entreprise")
    public ResponseEntity<ApiResponse<List<ActivityMappingRuleResponse>>> getAllRules(
            @PathVariable Long companyId) {

        List<ActivityMappingRule> rules = mappingService.getAllRules(companyId);

        List<ActivityMappingRuleResponse> responses = rules.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping
    @Operation(summary = "Créer une règle de mapping",
               description = "Crée une nouvelle règle de mapping personnalisée")
    public ResponseEntity<ApiResponse<ActivityMappingRuleResponse>> createRule(
            @PathVariable Long companyId,
            @Valid @RequestBody ActivityMappingRuleRequest request) {

        ActivityMappingRule rule = mappingService.saveRule(companyId, request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(toResponse(rule), "Règle de mapping créée avec succès"));
    }

    @PutMapping("/{ruleId}")
    @Operation(summary = "Modifier une règle de mapping",
               description = "Met à jour une règle de mapping existante")
    public ResponseEntity<ApiResponse<ActivityMappingRuleResponse>> updateRule(
            @PathVariable Long companyId,
            @PathVariable Long ruleId,
            @Valid @RequestBody ActivityMappingRuleRequest request) {

        ActivityMappingRule rule = mappingService.updateRule(ruleId, request);

        return ResponseEntity.ok(ApiResponse.success(toResponse(rule), "Règle mise à jour"));
    }

    @DeleteMapping("/{ruleId}")
    @Operation(summary = "Supprimer une règle de mapping")
    public ResponseEntity<ApiResponse<Void>> deleteRule(
            @PathVariable Long companyId,
            @PathVariable Long ruleId) {

        mappingService.deleteRule(ruleId);

        return ResponseEntity.ok(ApiResponse.success(null, "Règle supprimée"));
    }

    @PostMapping("/init")
    @Operation(summary = "Initialiser les mappings par défaut",
               description = "Copie les règles de mapping OHADA par défaut pour cette entreprise")
    public ResponseEntity<ApiResponse<Void>> initializeDefaultMappings(
            @PathVariable Long companyId) {

        mappingService.initializeDefaultMappings(companyId);

        return ResponseEntity.ok(ApiResponse.success(null, "Mappings par défaut initialisés"));
    }

    @GetMapping("/test")
    @Operation(summary = "Tester un mapping",
               description = "Teste le mapping pour un nom d'activité donné")
    public ResponseEntity<ApiResponse<ActivityMappingService.MappingResult>> testMapping(
            @PathVariable Long companyId,
            @RequestParam String activityName) {

        ActivityMappingService.MappingResult result = mappingService.findAccountForActivity(companyId, activityName);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private ActivityMappingRuleResponse toResponse(ActivityMappingRule rule) {
        return ActivityMappingRuleResponse.builder()
            .id(rule.getId())
            .activityKeyword(rule.getActivityKeyword())
            .accountNumber(rule.getAccountNumber())
            .journalCode(rule.getJournalCode())
            .matchType(rule.getMatchType())
            .caseSensitive(rule.getCaseSensitive())
            .priority(rule.getPriority())
            .confidenceScore(rule.getConfidenceScore())
            .isActive(rule.getIsActive())
            .usageCount(rule.getUsageCount())
            .lastUsedAt(rule.getLastUsedAt())
            .createdAt(rule.getCreatedAt())
            .build();
    }
}
