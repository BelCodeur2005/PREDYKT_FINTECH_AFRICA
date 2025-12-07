package com.predykt.accounting.controller;

import com.predykt.accounting.domain.enums.BillStatus;
import com.predykt.accounting.dto.request.BillCreateRequest;
import com.predykt.accounting.dto.request.BillUpdateRequest;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.BillResponse;
import com.predykt.accounting.service.BillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des factures fournisseurs (Bills)
 * Conforme OHADA + Fiscalité Cameroun (AIR, IRPP Loyer)
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/bills")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Facturation Fournisseur", description = "Gestion des factures fournisseurs (Bills) - OHADA + AIR/IRPP")
public class BillController {

    private final BillService billService;

    @PostMapping
    @Operation(summary = "Créer une facture fournisseur",
               description = "Crée une nouvelle facture fournisseur en statut DRAFT. " +
                           "Calcul automatique : TVA déductible, AIR 2.2% (NIU) ou 5.5% (pas de NIU), IRPP Loyer 15%")
    public ResponseEntity<ApiResponse<BillResponse>> createBill(
            @PathVariable Long companyId,
            @Valid @RequestBody BillCreateRequest request) {

        log.info("POST /api/v1/companies/{}/bills - Création facture fournisseur", companyId);
        BillResponse response = billService.createBill(companyId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response,
                "Facture fournisseur créée avec succès (AIR: " + response.getAirRate() + "%)"));
    }

    @GetMapping("/{billId}")
    @Operation(summary = "Obtenir une facture fournisseur par ID",
               description = "Récupère les détails complets, incluant calcul AIR et IRPP Loyer")
    public ResponseEntity<ApiResponse<BillResponse>> getBill(
            @PathVariable Long companyId,
            @PathVariable Long billId) {

        log.info("GET /api/v1/companies/{}/bills/{}", companyId, billId);
        BillResponse response = billService.getBill(companyId, billId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Lister toutes les factures fournisseurs",
               description = "Récupère toutes les factures. Paramètre 'status' optionnel pour filtrer.")
    public ResponseEntity<ApiResponse<List<BillResponse>>> getAllBills(
            @PathVariable Long companyId,
            @RequestParam(required = false) BillStatus status) {

        log.info("GET /api/v1/companies/{}/bills?status={}", companyId, status);
        List<BillResponse> responses = billService.getAllBills(companyId, status);

        return ResponseEntity.ok(ApiResponse.success(responses,
            String.format("%d facture(s) fournisseur trouvée(s)", responses.size())));
    }

    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "Lister les factures d'un fournisseur",
               description = "Récupère toutes les factures d'un fournisseur spécifique")
    public ResponseEntity<ApiResponse<List<BillResponse>>> getBillsBySupplier(
            @PathVariable Long companyId,
            @PathVariable Long supplierId) {

        log.info("GET /api/v1/companies/{}/bills/supplier/{}", companyId, supplierId);
        List<BillResponse> responses = billService.getBillsBySupplier(companyId, supplierId);

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Obtenir les factures en retard",
               description = "Liste toutes les factures fournisseurs échues non payées")
    public ResponseEntity<ApiResponse<List<BillResponse>>> getOverdueBills(
            @PathVariable Long companyId) {

        log.info("GET /api/v1/companies/{}/bills/overdue", companyId);
        List<BillResponse> responses = billService.getOverdueBills(companyId);

        return ResponseEntity.ok(ApiResponse.success(responses,
            String.format("%d facture(s) fournisseur en retard", responses.size())));
    }

    @PutMapping("/{billId}")
    @Operation(summary = "Mettre à jour une facture fournisseur",
               description = "Modifie une facture en statut DRAFT uniquement. Recalcule AIR et IRPP.")
    public ResponseEntity<ApiResponse<BillResponse>> updateBill(
            @PathVariable Long companyId,
            @PathVariable Long billId,
            @Valid @RequestBody BillUpdateRequest request) {

        log.info("PUT /api/v1/companies/{}/bills/{}", companyId, billId);
        BillResponse response = billService.updateBill(companyId, billId, request);

        return ResponseEntity.ok(ApiResponse.success(response, "Facture fournisseur mise à jour"));
    }

    @PostMapping("/{billId}/validate")
    @Operation(summary = "VALIDER une facture fournisseur (IRRÉVERSIBLE)",
               description = "Valide la facture et génère l'écriture comptable OHADA. " +
                           "Action IRRÉVERSIBLE : statut DRAFT → ISSUED. " +
                           "Écriture : DÉBIT Achats (601) + TVA (4452) + AIR (4421) / CRÉDIT Fournisseur (401)")
    public ResponseEntity<ApiResponse<BillResponse>> validateBill(
            @PathVariable Long companyId,
            @PathVariable Long billId) {

        log.info("POST /api/v1/companies/{}/bills/{}/validate - VALIDATION", companyId, billId);
        BillResponse response = billService.validateBill(companyId, billId);

        return ResponseEntity.ok(ApiResponse.success(response,
            "Facture fournisseur validée. Écriture comptable générée (AIR: " + response.getAirRate() + "%)"));
    }

    @PostMapping("/{billId}/cancel")
    @Operation(summary = "Annuler une facture fournisseur",
               description = "Annule une facture (statut CANCELLED). Impossible si déjà payée.")
    public ResponseEntity<ApiResponse<BillResponse>> cancelBill(
            @PathVariable Long companyId,
            @PathVariable Long billId) {

        log.info("POST /api/v1/companies/{}/bills/{}/cancel", companyId, billId);
        BillResponse response = billService.cancelBill(companyId, billId);

        return ResponseEntity.ok(ApiResponse.success(response, "Facture fournisseur annulée"));
    }

    @DeleteMapping("/{billId}")
    @Operation(summary = "Supprimer définitivement une facture fournisseur",
               description = "ATTENTION : Suppression définitive. Possible uniquement pour DRAFT.")
    public ResponseEntity<ApiResponse<Void>> deleteBill(
            @PathVariable Long companyId,
            @PathVariable Long billId) {

        log.warn("DELETE /api/v1/companies/{}/bills/{} - Suppression définitive", companyId, billId);
        billService.deleteBill(companyId, billId);

        return ResponseEntity.ok(ApiResponse.success(null, "Facture fournisseur supprimée définitivement"));
    }
}
