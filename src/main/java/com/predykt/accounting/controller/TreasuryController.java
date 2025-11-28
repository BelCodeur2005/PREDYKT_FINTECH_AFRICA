package com.predykt.accounting.controller;

import com.predykt.accounting.domain.entity.CashFlowProjection;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.service.TreasuryProjectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/companies/{companyId}/treasury")
@RequiredArgsConstructor
@Tag(name = "Tresorerie", description = "Gestion de la tresorerie et projections")
public class TreasuryController {

    private final TreasuryProjectionService treasuryService;

    @PostMapping("/projections")
    @Operation(summary = "Creer une projection manuelle",
               description = "Cree une projection de tresorerie manuelle")
    public ResponseEntity<ApiResponse<CashFlowProjection>> createProjection(
            @PathVariable Long companyId,
            @RequestBody CashFlowProjection projection) {

        // Associer l'entreprise
        projection.getCompany().setId(companyId);

        CashFlowProjection created = treasuryService.createProjection(projection);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(created));
    }

    @PostMapping("/projections/auto-generate")
    @Operation(summary = "Generer projections automatiques",
               description = "Genere automatiquement les projections J+30, J+60, J+90 basees sur l'historique")
    public ResponseEntity<ApiResponse<List<CashFlowProjection>>> generateAutomaticProjections(
            @PathVariable Long companyId) {

        List<CashFlowProjection> projections = treasuryService.generateAutomaticProjections(companyId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(projections));
    }

    @GetMapping("/projections")
    @Operation(summary = "Lister les projections",
               description = "Recupere toutes les projections de tresorerie")
    public ResponseEntity<ApiResponse<List<CashFlowProjection>>> getProjections(
            @PathVariable Long companyId) {

        List<CashFlowProjection> projections = treasuryService.getCompanyProjections(companyId);
        return ResponseEntity.ok(ApiResponse.success(projections));
    }

    @GetMapping("/projections/latest")
    @Operation(summary = "Dernieres projections",
               description = "Recupere les dernieres projections par horizon (J+30, J+60, J+90)")
    public ResponseEntity<ApiResponse<Map<String, CashFlowProjection>>> getLatestProjections(
            @PathVariable Long companyId) {

        Map<String, CashFlowProjection> latest = treasuryService.getLatestProjections(companyId);
        return ResponseEntity.ok(ApiResponse.success(latest));
    }

    @GetMapping("/projections/horizon/{horizon}")
    @Operation(summary = "Projections par horizon",
               description = "Recupere les projections pour un horizon specifique (30, 60 ou 90 jours)")
    public ResponseEntity<ApiResponse<List<CashFlowProjection>>> getProjectionsByHorizon(
            @PathVariable Long companyId,
            @PathVariable Integer horizon) {

        List<CashFlowProjection> projections = treasuryService.getProjectionsByHorizon(companyId, horizon);
        return ResponseEntity.ok(ApiResponse.success(projections));
    }

    @GetMapping("/alerts/negative-cash")
    @Operation(summary = "Alertes tresorerie negative",
               description = "Recupere les projections indiquant une tresorerie negative")
    public ResponseEntity<ApiResponse<List<CashFlowProjection>>> getNegativeCashAlerts(
            @PathVariable Long companyId) {

        List<CashFlowProjection> alerts = treasuryService.getNegativeCashFlowAlerts(companyId);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @DeleteMapping("/projections/{projectionId}")
    @Operation(summary = "Supprimer une projection",
               description = "Supprime une projection de tresorerie")
    public ResponseEntity<ApiResponse<Void>> deleteProjection(
            @PathVariable Long companyId,
            @PathVariable Long projectionId) {

        treasuryService.deleteProjection(projectionId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/projections/cleanup")
    @Operation(summary = "Nettoyer anciennes projections",
               description = "Supprime les projections de plus de 6 mois")
    public ResponseEntity<ApiResponse<Map<String, String>>> cleanupOldProjections(
            @PathVariable Long companyId) {

        treasuryService.cleanupOldProjections(companyId);
        Map<String, String> result = Map.of(
            "message", "Anciennes projections supprimees avec succes"
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
