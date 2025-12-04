package com.predykt.accounting.controller;

import com.predykt.accounting.domain.entity.VATProrata;
import com.predykt.accounting.dto.request.VATProrataCreateRequest;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.VATProrataResponse;
import com.predykt.accounting.mapper.VATProrataMapper;
import com.predykt.accounting.service.VATProratService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Contrôleur REST pour la gestion du prorata de TVA
 *
 * Le prorata de TVA permet de calculer la part de TVA récupérable pour les entreprises
 * ayant des activités mixtes (taxables + exonérées).
 *
 * Conforme au CGI Cameroun Art. 133
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "VAT Prorata", description = "API de gestion du prorata de TVA")
public class VATProrataController {

    private final VATProratService prorataService;
    private final VATProrataMapper prorataMapper;

    /**
     * Crée ou met à jour un prorata de TVA manuellement
     *
     * @param companyId ID de l'entreprise
     * @param request Données du prorata
     * @return Prorata créé ou mis à jour
     */
    @PostMapping("/companies/{companyId}/vat-prorata")
    @Operation(summary = "Créer/Mettre à jour un prorata de TVA",
            description = "Crée ou met à jour le prorata de TVA pour une entreprise et une année fiscale donnée")
    public ResponseEntity<ApiResponse<VATProrataResponse>> createOrUpdateProrata(
            @Parameter(description = "ID de l'entreprise") @PathVariable Long companyId,
            @Valid @RequestBody VATProrataCreateRequest request) {

        log.info("📊 [API] Création/Mise à jour prorata pour entreprise {} année {}",
                companyId, request.getFiscalYear());

        VATProrata prorata = prorataService.createOrUpdateProrata(
                companyId,
                request.getFiscalYear(),
                request.getTaxableTurnover(),
                request.getExemptTurnover(),
                request.getProrataType(),
                request.getNotes(),
                "API_USER" // TODO: Récupérer l'utilisateur connecté via JWT
        );

        VATProrataResponse response = prorataMapper.toResponse(prorata);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response,
                        "Prorata " + prorata.getProrataType().getLabel() + " créé/mis à jour avec succès: " +
                                prorata.getProrataPercentage() + "% récupérable"));
    }

    /**
     * Crée un prorata provisoire basé sur l'année N-1
     *
     * @param companyId ID de l'entreprise
     * @param fiscalYear Année fiscale
     * @return Prorata provisoire créé
     */
    @PostMapping("/companies/{companyId}/vat-prorata/provisional/{fiscalYear}")
    @Operation(summary = "Créer un prorata provisoire",
            description = "Crée automatiquement un prorata provisoire basé sur les données de l'année N-1")
    public ResponseEntity<ApiResponse<VATProrataResponse>> createProvisionalProrata(
            @Parameter(description = "ID de l'entreprise") @PathVariable Long companyId,
            @Parameter(description = "Année fiscale (N)") @PathVariable Integer fiscalYear) {

        log.info("⏳ [API] Création prorata provisoire pour entreprise {} année {}", companyId, fiscalYear);

        VATProrata prorata = prorataService.createProvisionalProrata(
                companyId,
                fiscalYear,
                "API_USER" // TODO: Récupérer l'utilisateur connecté via JWT
        );

        VATProrataResponse response = prorataMapper.toResponse(prorata);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response,
                        "Prorata provisoire créé: " + prorata.getProrataPercentage() +
                                "% (basé sur année " + (fiscalYear - 1) + ")"));
    }

    /**
     * Convertit un prorata provisoire en définitif
     *
     * @param companyId ID de l'entreprise
     * @param fiscalYear Année fiscale
     * @param request Données définitives du CA
     * @return Prorata définitif
     */
    @PostMapping("/companies/{companyId}/vat-prorata/{fiscalYear}/convert-definitive")
    @Operation(summary = "Convertir en prorata définitif",
            description = "Convertit un prorata provisoire en définitif avec régularisation si nécessaire")
    public ResponseEntity<ApiResponse<VATProrataResponse>> convertToDefinitive(
            @Parameter(description = "ID de l'entreprise") @PathVariable Long companyId,
            @Parameter(description = "Année fiscale") @PathVariable Integer fiscalYear,
            @Valid @RequestBody VATProrataCreateRequest request) {

        log.info("✅ [API] Conversion en prorata définitif pour entreprise {} année {}",
                companyId, fiscalYear);

        VATProrata prorata = prorataService.convertToDefinitive(
                companyId,
                fiscalYear,
                request.getTaxableTurnover(),
                request.getExemptTurnover(),
                "API_USER" // TODO: Récupérer l'utilisateur connecté via JWT
        );

        VATProrataResponse response = prorataMapper.toResponse(prorata);

        String message = "Prorata définitif: " + prorata.getProrataPercentage() + "% récupérable";

        // Vérifier si régularisation nécessaire
        BigDecimal gap = prorata.getTaxableTurnover()
                .divide(prorata.getTotalTurnover(), 4, java.math.RoundingMode.HALF_UP)
                .subtract(prorata.getProrataRate())
                .abs();

        if (gap.compareTo(new BigDecimal("0.10")) > 0) {
            message += " ⚠️ RÉGULARISATION EFFECTUÉE (écart > 10%)";
        }

        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    /**
     * Récupère le prorata actif pour une entreprise et une année
     *
     * @param companyId ID de l'entreprise
     * @param fiscalYear Année fiscale
     * @return Prorata actif ou 404
     */
    @GetMapping("/companies/{companyId}/vat-prorata/{fiscalYear}")
    @Operation(summary = "Récupérer le prorata actif",
            description = "Récupère le prorata actif pour une entreprise et une année fiscale donnée")
    public ResponseEntity<ApiResponse<VATProrataResponse>> getActiveProrata(
            @Parameter(description = "ID de l'entreprise") @PathVariable Long companyId,
            @Parameter(description = "Année fiscale") @PathVariable Integer fiscalYear) {

        log.debug("🔍 [API] Recherche prorata pour entreprise {} année {}", companyId, fiscalYear);

        Optional<VATProrata> prorataOpt = prorataService.getActiveProrata(companyId, fiscalYear);

        if (prorataOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(null,
                    "Aucun prorata défini pour l'année " + fiscalYear +
                            " → 100% activités taxables (TVA entièrement récupérable)"));
        }

        VATProrata prorata = prorataOpt.get();
        VATProrataResponse response = prorataMapper.toResponse(prorata);

        return ResponseEntity.ok(ApiResponse.success(response,
                "Prorata " + prorata.getProrataType().getLabel() + ": " +
                        prorata.getProrataPercentage() + "% récupérable"));
    }

    /**
     * Liste tous les prorata d'une entreprise (historique)
     *
     * @param companyId ID de l'entreprise
     * @return Liste des prorata
     */
    @GetMapping("/companies/{companyId}/vat-prorata")
    @Operation(summary = "Lister tous les prorata d'une entreprise",
            description = "Récupère l'historique de tous les prorata de TVA d'une entreprise")
    public ResponseEntity<ApiResponse<List<VATProrataResponse>>> getAllProrata(
            @Parameter(description = "ID de l'entreprise") @PathVariable Long companyId) {

        log.debug("📋 [API] Liste des prorata pour entreprise {}", companyId);

        List<VATProrata> proratas = prorataService.getAllProrata(companyId);

        List<VATProrataResponse> responses = proratas.stream()
                .map(prorataMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses,
                responses.size() + " prorata(s) trouvé(s)"));
    }

    /**
     * Applique le prorata à un montant de TVA (simulation)
     *
     * @param companyId ID de l'entreprise
     * @param fiscalYear Année fiscale
     * @param vatAmount Montant de TVA
     * @return Montant récupérable après prorata
     */
    @GetMapping("/companies/{companyId}/vat-prorata/{fiscalYear}/apply")
    @Operation(summary = "Simuler l'application du prorata",
            description = "Calcule le montant de TVA récupérable après application du prorata")
    public ResponseEntity<ApiResponse<BigDecimal>> applyProrata(
            @Parameter(description = "ID de l'entreprise") @PathVariable Long companyId,
            @Parameter(description = "Année fiscale") @PathVariable Integer fiscalYear,
            @Parameter(description = "Montant de TVA (FCFA)") @RequestParam BigDecimal vatAmount) {

        log.debug("🧮 [API] Simulation prorata pour {} FCFA (entreprise {} année {})",
                vatAmount, companyId, fiscalYear);

        BigDecimal recoverable = prorataService.applyProrata(companyId, fiscalYear, vatAmount);

        return ResponseEntity.ok(ApiResponse.success(recoverable,
                String.format("TVA récupérable: %s FCFA sur %s FCFA (%.2f%%)",
                        recoverable,
                        vatAmount,
                        recoverable.multiply(new BigDecimal("100"))
                                .divide(vatAmount, 2, java.math.RoundingMode.HALF_UP))));
    }

    /**
     * Verrouille un prorata (après clôture fiscale)
     *
     * @param prorataId ID du prorata
     * @return Prorata verrouillé
     */
    @PostMapping("/vat-prorata/{prorataId}/lock")
    @Operation(summary = "Verrouiller un prorata",
            description = "Verrouille un prorata pour empêcher toute modification (clôture fiscale)")
    public ResponseEntity<ApiResponse<VATProrataResponse>> lockProrata(
            @Parameter(description = "ID du prorata") @PathVariable Long prorataId) {

        log.info("🔒 [API] Verrouillage du prorata {}", prorataId);

        VATProrata prorata = prorataService.lockProrata(prorataId, "API_USER");

        VATProrataResponse response = prorataMapper.toResponse(prorata);

        return ResponseEntity.ok(ApiResponse.success(response,
                "Prorata verrouillé avec succès - Aucune modification ne sera possible"));
    }

    /**
     * Supprime un prorata (si non verrouillé)
     *
     * @param prorataId ID du prorata
     * @return Message de confirmation
     */
    @DeleteMapping("/vat-prorata/{prorataId}")
    @Operation(summary = "Supprimer un prorata",
            description = "Supprime un prorata (uniquement si non verrouillé)")
    public ResponseEntity<ApiResponse<Void>> deleteProrata(
            @Parameter(description = "ID du prorata") @PathVariable Long prorataId) {

        log.info("🗑️ [API] Suppression du prorata {}", prorataId);

        prorataService.deleteProrata(prorataId);

        return ResponseEntity.ok(ApiResponse.success(null, "Prorata supprimé avec succès"));
    }

    /**
     * Vérifie si un prorata existe pour une entreprise/année
     *
     * @param companyId ID de l'entreprise
     * @param fiscalYear Année fiscale
     * @return true si prorata existe, false sinon
     */
    @GetMapping("/companies/{companyId}/vat-prorata/{fiscalYear}/exists")
    @Operation(summary = "Vérifier l'existence d'un prorata",
            description = "Vérifie si un prorata actif existe pour une entreprise et une année fiscale")
    public ResponseEntity<ApiResponse<Boolean>> hasProrataForYear(
            @Parameter(description = "ID de l'entreprise") @PathVariable Long companyId,
            @Parameter(description = "Année fiscale") @PathVariable Integer fiscalYear) {

        boolean exists = prorataService.hasProrataForYear(companyId, fiscalYear);

        return ResponseEntity.ok(ApiResponse.success(exists,
                exists ? "Un prorata existe pour l'année " + fiscalYear
                        : "Aucun prorata pour l'année " + fiscalYear + " → 100% récupérable"));
    }
}
