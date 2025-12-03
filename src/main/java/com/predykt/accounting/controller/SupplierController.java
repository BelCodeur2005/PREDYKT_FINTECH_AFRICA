package com.predykt.accounting.controller;

import com.predykt.accounting.domain.entity.Supplier;
import com.predykt.accounting.dto.request.SupplierCreateRequest;
import com.predykt.accounting.dto.request.SupplierUpdateRequest;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.SupplierResponse;
import com.predykt.accounting.mapper.SupplierMapper;
import com.predykt.accounting.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller pour la gestion des fournisseurs
 * Gère le NIU (Numéro d'Identifiant Unique) pour le calcul de l'AIR
 */
@RestController
@RequestMapping("/companies/{companyId}/suppliers")
@RequiredArgsConstructor
@Tag(name = "Fournisseurs", description = "Gestion des fournisseurs et du NIU pour calcul AIR")
public class SupplierController {

    private final SupplierService supplierService;
    private final SupplierMapper supplierMapper;

    @PostMapping
    @Operation(summary = "Créer un fournisseur",
               description = "Crée un nouveau fournisseur avec ou sans NIU")
    public ResponseEntity<ApiResponse<SupplierResponse>> createSupplier(
            @PathVariable Long companyId,
            @Valid @RequestBody SupplierCreateRequest request) {

        Supplier supplier = supplierMapper.toEntity(request);
        Supplier created = supplierService.createSupplier(companyId, supplier);
        SupplierResponse response = supplierMapper.toResponse(created);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Liste des fournisseurs",
               description = "Récupère tous les fournisseurs de l'entreprise")
    public ResponseEntity<ApiResponse<List<SupplierResponse>>> getAllSuppliers(
            @PathVariable Long companyId,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {

        List<Supplier> suppliers = activeOnly
            ? supplierService.getActiveSuppliers(companyId)
            : supplierService.getAllSuppliers(companyId);

        List<SupplierResponse> response = suppliers.stream()
            .map(supplierMapper::toResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{supplierId}")
    @Operation(summary = "Détails d'un fournisseur",
               description = "Récupère les détails d'un fournisseur par ID")
    public ResponseEntity<ApiResponse<SupplierResponse>> getSupplier(
            @PathVariable Long companyId,
            @PathVariable Long supplierId) {

        Supplier supplier = supplierService.getSupplierById(supplierId);
        SupplierResponse response = supplierMapper.toResponse(supplier);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{supplierId}")
    @Operation(summary = "Mettre à jour un fournisseur",
               description = "Met à jour les informations d'un fournisseur (y compris le NIU)")
    public ResponseEntity<ApiResponse<SupplierResponse>> updateSupplier(
            @PathVariable Long companyId,
            @PathVariable Long supplierId,
            @Valid @RequestBody SupplierUpdateRequest request) {

        Supplier existing = supplierService.getSupplierById(supplierId);
        supplierMapper.updateEntityFromRequest(request, existing);
        Supplier updated = supplierService.updateSupplier(supplierId, existing);
        SupplierResponse response = supplierMapper.toResponse(updated);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{supplierId}")
    @Operation(summary = "Supprimer un fournisseur",
               description = "Désactive un fournisseur (suppression logique)")
    public ResponseEntity<ApiResponse<String>> deleteSupplier(
            @PathVariable Long companyId,
            @PathVariable Long supplierId) {

        supplierService.deleteSupplier(supplierId);

        return ResponseEntity.ok(ApiResponse.success(
            "Fournisseur désactivé avec succès"
        ));
    }

    @PutMapping("/{supplierId}/reactivate")
    @Operation(summary = "Réactiver un fournisseur",
               description = "Réactive un fournisseur précédemment désactivé")
    public ResponseEntity<ApiResponse<String>> reactivateSupplier(
            @PathVariable Long companyId,
            @PathVariable Long supplierId) {

        supplierService.reactivateSupplier(supplierId);

        return ResponseEntity.ok(ApiResponse.success(
            "Fournisseur réactivé avec succès"
        ));
    }

    @PutMapping("/{supplierId}/niu")
    @Operation(summary = "Mettre à jour le NIU",
               description = "Ajoute ou met à jour le NIU d'un fournisseur (impacte le taux AIR)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateNiu(
            @PathVariable Long companyId,
            @PathVariable Long supplierId,
            @RequestParam String niuNumber) {

        Supplier before = supplierService.getSupplierById(supplierId);
        boolean hadNiu = before.hasValidNiu();

        Supplier updated = supplierService.updateNiu(supplierId, niuNumber);

        Map<String, Object> result = new HashMap<>();
        result.put("supplierId", updated.getId());
        result.put("supplierName", updated.getName());
        result.put("niuNumber", updated.getNiuNumber());
        result.put("previousAirRate", hadNiu ? "2.2%" : "5.5%");
        result.put("newAirRate", updated.hasValidNiu() ? "2.2%" : "5.5%");

        String message;
        if (!hadNiu && updated.hasValidNiu()) {
            message = "✅ NIU ajouté avec succès - Taux AIR réduit de 5,5% à 2,2% (économie de 3,3%)";
        } else if (hadNiu && !updated.hasValidNiu()) {
            message = "⚠️ NIU supprimé - Taux AIR augmenté de 2,2% à 5,5% (surcoût de 3,3%)";
        } else {
            message = "NIU mis à jour avec succès";
        }

        result.put("message", message);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/without-niu")
    @Operation(summary = "Fournisseurs sans NIU",
               description = "Récupère la liste des fournisseurs sans NIU (alertes)")
    public ResponseEntity<ApiResponse<List<SupplierResponse>>> getSuppliersWithoutNiu(
            @PathVariable Long companyId) {

        List<Supplier> suppliers = supplierService.getSuppliersWithoutNiu(companyId);
        List<SupplierResponse> response = suppliers.stream()
            .map(supplierMapper::toResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/without-niu/count")
    @Operation(summary = "Nombre de fournisseurs sans NIU",
               description = "Compte les fournisseurs sans NIU")
    public ResponseEntity<ApiResponse<Map<String, Object>>> countSuppliersWithoutNiu(
            @PathVariable Long companyId) {

        Long count = supplierService.countSuppliersWithoutNiu(companyId);

        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("severity", count > 0 ? "HIGH" : "NONE");
        result.put("message", count > 0
            ? String.format("⚠️ %d fournisseur(s) sans NIU - Pénalité AIR 5,5%% appliquée", count)
            : "✅ Tous les fournisseurs ont un NIU");

        // Estimation du surcoût potentiel (exemple simplifié)
        if (count > 0) {
            result.put("warning", "Chaque transaction avec ces fournisseurs entraîne une pénalité de 3,3% supplémentaire (5,5% - 2,2%)");
            result.put("actionRequired", "Demander le NIU aux fournisseurs et mettre à jour leurs fiches");
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/rent-suppliers")
    @Operation(summary = "Fournisseurs loueurs",
               description = "Récupère les fournisseurs de type RENT (soumis à IRPP Loyer 15%)")
    public ResponseEntity<ApiResponse<List<SupplierResponse>>> getRentSuppliers(
            @PathVariable Long companyId) {

        List<Supplier> suppliers = supplierService.getRentSuppliers(companyId);
        List<SupplierResponse> response = suppliers.stream()
            .map(supplierMapper::toResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @Operation(summary = "Rechercher un fournisseur",
               description = "Recherche un fournisseur par nom")
    public ResponseEntity<ApiResponse<SupplierResponse>> searchSupplierByName(
            @PathVariable Long companyId,
            @RequestParam String name) {

        Supplier supplier = supplierService.findByName(companyId, name);

        if (supplier == null) {
            return ResponseEntity.ok(ApiResponse.error(
                "Fournisseur non trouvé: " + name,
                "SUPPLIER_NOT_FOUND"
            ));
        }

        SupplierResponse response = supplierMapper.toResponse(supplier);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Statistiques fournisseurs",
               description = "Vue d'ensemble des fournisseurs de l'entreprise")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSupplierStatistics(
            @PathVariable Long companyId) {

        List<Supplier> allSuppliers = supplierService.getAllSuppliers(companyId);
        long activeCount = allSuppliers.stream().filter(Supplier::getIsActive).count();
        long inactiveCount = allSuppliers.size() - activeCount;
        long withNiu = allSuppliers.stream().filter(Supplier::hasValidNiu).count();
        long withoutNiu = allSuppliers.stream().filter(s -> !s.hasValidNiu()).count();
        long rentSuppliers = allSuppliers.stream()
            .filter(s -> "RENT".equalsIgnoreCase(s.getSupplierType()))
            .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSuppliers", allSuppliers.size());
        stats.put("activeSuppliers", activeCount);
        stats.put("inactiveSuppliers", inactiveCount);
        stats.put("suppliersWithNiu", withNiu);
        stats.put("suppliersWithoutNiu", withoutNiu);
        stats.put("niuComplianceRate", allSuppliers.isEmpty()
            ? 0.0
            : (double) withNiu / allSuppliers.size() * 100);
        stats.put("rentSuppliers", rentSuppliers);

        // Statut de conformité
        String complianceStatus;
        if (withoutNiu == 0) {
            complianceStatus = "EXCELLENT";
        } else if ((double) withNiu / allSuppliers.size() >= 0.8) {
            complianceStatus = "GOOD";
        } else if ((double) withNiu / allSuppliers.size() >= 0.5) {
            complianceStatus = "NEEDS_IMPROVEMENT";
        } else {
            complianceStatus = "CRITICAL";
        }
        stats.put("complianceStatus", complianceStatus);

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
