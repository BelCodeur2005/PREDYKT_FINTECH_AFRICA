package com.predykt.accounting.controller;

import com.predykt.accounting.domain.enums.AssetCategory;
import com.predykt.accounting.dto.request.FixedAssetCreateRequest;
import com.predykt.accounting.dto.request.FixedAssetDisposalRequest;
import com.predykt.accounting.dto.request.FixedAssetUpdateRequest;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.DepreciationScheduleResponse;
import com.predykt.accounting.dto.response.FixedAssetResponse;
import com.predykt.accounting.service.DepreciationService;
import com.predykt.accounting.service.FixedAssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des immobilisations et des amortissements
 * Conforme OHADA et fiscalité camerounaise (CGI)
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/fixed-assets")
@RequiredArgsConstructor
@Tag(name = "Immobilisations et Amortissements", description = "Gestion du patrimoine immobilisé et calcul des amortissements")
public class FixedAssetController {

    private final DepreciationService depreciationService;
    private final FixedAssetService fixedAssetService;

    // ============================================
    // ENDPOINTS CRUD - GESTION DES IMMOBILISATIONS
    // ============================================

    @PostMapping
    @Operation(
        summary = "Créer une immobilisation",
        description = "Enregistre une nouvelle immobilisation dans le patrimoine de l'entreprise. " +
                      "Génère automatiquement le taux d'amortissement et le coût total. " +
                      "Conforme OHADA et CGI Cameroun."
    )
    public ResponseEntity<ApiResponse<FixedAssetResponse>> createFixedAsset(
            @PathVariable Long companyId,
            @Valid @RequestBody FixedAssetCreateRequest request) {

        FixedAssetResponse response = fixedAssetService.createFixedAsset(companyId, request);

        String message = String.format(
            "Immobilisation créée: %s - Catégorie: %s - Valeur: %s FCFA",
            response.getAssetNumber(),
            response.getCategoryName(),
            response.getTotalCost().setScale(0, java.math.RoundingMode.HALF_UP)
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, message));
    }

    @GetMapping
    @Operation(
        summary = "Lister les immobilisations",
        description = "Récupère toutes les immobilisations d'une entreprise avec filtres optionnels. " +
                      "Inclut les calculs en temps réel (VNC, amortissements cumulés, âge, statut)."
    )
    public ResponseEntity<ApiResponse<List<FixedAssetResponse>>> getCompanyAssets(
            @PathVariable Long companyId,
            @Parameter(description = "Filtrer par catégorie (BUILDING, EQUIPMENT, VEHICLE, etc.)")
            @RequestParam(required = false) AssetCategory category,
            @Parameter(description = "Filtrer par statut actif (true) ou inactif (false)")
            @RequestParam(required = false) Boolean isActive,
            @Parameter(description = "Filtrer par localisation")
            @RequestParam(required = false) String location,
            @Parameter(description = "Filtrer par département")
            @RequestParam(required = false) String department) {

        List<FixedAssetResponse> assets = fixedAssetService.getCompanyAssets(
            companyId, category, isActive, location, department);

        String message = String.format("%d immobilisation(s) trouvée(s)", assets.size());

        return ResponseEntity.ok(ApiResponse.success(assets, message));
    }

    @GetMapping("/{assetId}")
    @Operation(
        summary = "Détail d'une immobilisation",
        description = "Récupère les détails complets d'une immobilisation par son ID. " +
                      "Inclut VNC actuelle, amortissements, plus/moins-value si cédée."
    )
    public ResponseEntity<ApiResponse<FixedAssetResponse>> getAssetById(
            @PathVariable Long companyId,
            @PathVariable Long assetId) {

        FixedAssetResponse response = fixedAssetService.getAssetById(companyId, assetId);

        String message = String.format(
            "Immobilisation %s - VNC: %s FCFA - Statut: %s",
            response.getAssetNumber(),
            response.getCurrentNetBookValue().setScale(0, java.math.RoundingMode.HALF_UP),
            response.getStatusLabel()
        );

        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @GetMapping("/number/{assetNumber}")
    @Operation(
        summary = "Rechercher par numéro",
        description = "Récupère une immobilisation par son numéro unique (ex: IMM-2024-001)"
    )
    public ResponseEntity<ApiResponse<FixedAssetResponse>> getAssetByNumber(
            @PathVariable Long companyId,
            @PathVariable String assetNumber) {

        FixedAssetResponse response = fixedAssetService.getAssetByNumber(companyId, assetNumber);

        return ResponseEntity.ok(ApiResponse.success(response,
            "Immobilisation trouvée: " + assetNumber));
    }

    @PutMapping("/{assetId}")
    @Operation(
        summary = "Modifier une immobilisation",
        description = "Met à jour les informations d'une immobilisation (nom, localisation, responsable, etc.). " +
                      "Les données comptables critiques (coût, catégorie, méthode) ne peuvent être modifiées."
    )
    public ResponseEntity<ApiResponse<FixedAssetResponse>> updateFixedAsset(
            @PathVariable Long companyId,
            @PathVariable Long assetId,
            @Valid @RequestBody FixedAssetUpdateRequest request) {

        FixedAssetResponse response = fixedAssetService.updateFixedAsset(companyId, assetId, request);

        return ResponseEntity.ok(ApiResponse.success(response,
            "Immobilisation mise à jour: " + response.getAssetNumber()));
    }

    @DeleteMapping("/{assetId}")
    @Operation(
        summary = "Supprimer une immobilisation",
        description = "Suppression logique (soft delete) - L'immobilisation est marquée comme inactive. " +
                      "Pour une sortie définitive (cession), utilisez l'endpoint /dispose."
    )
    public ResponseEntity<ApiResponse<Void>> deleteFixedAsset(
            @PathVariable Long companyId,
            @PathVariable Long assetId) {

        fixedAssetService.deleteFixedAsset(companyId, assetId);

        return ResponseEntity.ok(ApiResponse.success(null,
            "Immobilisation supprimée (soft delete)"));
    }

    @PostMapping("/{assetId}/dispose")
    @Operation(
        summary = "Céder une immobilisation",
        description = "Enregistre la cession d'une immobilisation (vente, mise au rebut, don, destruction). " +
                      "Calcule automatiquement la plus-value ou moins-value. " +
                      "Conforme OHADA - Génère les écritures de cession (compte 654 et 754)."
    )
    public ResponseEntity<ApiResponse<FixedAssetResponse>> disposeAsset(
            @PathVariable Long companyId,
            @PathVariable Long assetId,
            @Valid @RequestBody FixedAssetDisposalRequest request) {

        FixedAssetResponse response = fixedAssetService.disposeAsset(companyId, assetId, request);

        String gainLossLabel = response.getDisposalGainLoss().compareTo(java.math.BigDecimal.ZERO) >= 0
            ? "Plus-value" : "Moins-value";

        String message = String.format(
            "Immobilisation cédée: %s - %s: %s FCFA",
            response.getAssetNumber(),
            gainLossLabel,
            response.getDisposalGainLoss().abs().setScale(0, java.math.RoundingMode.HALF_UP)
        );

        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    // ============================================
    // RAPPORTS ET ANALYSES
    // ============================================

    @GetMapping("/depreciation-schedule")
    @Operation(
        summary = "Tableau d'amortissements",
        description = "Génère le tableau d'amortissements complet pour un exercice fiscal donné. " +
                      "Inclut toutes les immobilisations (actives et cédées), les dotations aux amortissements, " +
                      "les valeurs nettes comptables (VNC), et l'analyse des mouvements. " +
                      "Conforme OHADA et fiscalité camerounaise (CGI)."
    )
    public ResponseEntity<ApiResponse<DepreciationScheduleResponse>> getDepreciationSchedule(
            @PathVariable Long companyId,
            @RequestParam Integer fiscalYear) {

        DepreciationScheduleResponse schedule = depreciationService.generateDepreciationSchedule(
            companyId, fiscalYear);

        String message = String.format(
            "Tableau d'amortissements généré: %d immobilisation(s) - Dotation %s FCFA - VNC totale %s FCFA",
            schedule.getSummary().getTotalAssetCount(),
            schedule.getSummary().getTotalCurrentDepreciation().setScale(0, java.math.RoundingMode.HALF_UP),
            schedule.getSummary().getTotalNetBookValue().setScale(0, java.math.RoundingMode.HALF_UP)
        );

        return ResponseEntity.ok(ApiResponse.success(schedule, message));
    }

    @GetMapping("/next-number")
    @Operation(
        summary = "Générer le prochain numéro",
        description = "Génère le prochain numéro d'immobilisation disponible pour l'année fiscale. " +
                      "Format: IMM-YYYY-NNN (ex: IMM-2024-001)"
    )
    public ResponseEntity<ApiResponse<String>> getNextAssetNumber(
            @PathVariable Long companyId,
            @RequestParam(required = false) Integer fiscalYear) {

        String nextNumber = fixedAssetService.generateNextAssetNumber(companyId, fiscalYear);

        return ResponseEntity.ok(ApiResponse.success(nextNumber,
            "Prochain numéro d'immobilisation disponible"));
    }

    @PostMapping("/generate-monthly-depreciation")
    @Operation(
        summary = "Générer les dotations mensuelles",
        description = "Génère automatiquement les écritures de dotations aux amortissements pour toutes les " +
                      "immobilisations actives. Crée les écritures au journal (comptes 681x et 28x). " +
                      "À utiliser en fin de mois ou via job planifié. Conforme OHADA."
    )
    public ResponseEntity<ApiResponse<Void>> generateMonthlyDepreciation(
            @PathVariable Long companyId,
            @RequestParam Integer year,
            @RequestParam Integer month) {

        fixedAssetService.generateMonthlyDepreciationEntries(companyId, year, month);

        String message = String.format(
            "Dotations aux amortissements générées pour %02d/%d",
            month, year
        );

        return ResponseEntity.ok(ApiResponse.success(null, message));
    }
}
