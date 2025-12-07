package com.predykt.accounting.controller;

import com.predykt.accounting.dto.request.CustomerCreateRequest;
import com.predykt.accounting.dto.request.CustomerUpdateRequest;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.CustomerResponse;
import com.predykt.accounting.service.CustomerService;
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
 * Contrôleur REST pour la gestion des clients (Plan Tiers)
 * IMPORTANT: Cette fonctionnalité est OPTIONNELLE
 * Le système comptable fonctionne sans clients définis (utilise les descriptions)
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/customers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Plan Tiers - Clients", description = "Gestion du plan tiers - clients (optionnel)")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @Operation(summary = "Créer un nouveau client",
               description = "Crée un client dans le plan tiers. Optionnel - le système fonctionne sans cette fonctionnalité.")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @PathVariable Long companyId,
            @Valid @RequestBody CustomerCreateRequest request) {

        log.info("POST /api/v1/companies/{}/customers - Création d'un client", companyId);
        CustomerResponse response = customerService.createCustomer(companyId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Client créé avec succès"));
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Obtenir un client par ID")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(
            @PathVariable Long companyId,
            @PathVariable Long customerId) {

        log.info("GET /api/v1/companies/{}/customers/{}", companyId, customerId);
        CustomerResponse response = customerService.getCustomer(companyId, customerId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Lister tous les clients",
               description = "Récupère la liste des clients de l'entreprise. Paramètre activeOnly pour filtrer uniquement les actifs.")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAllCustomers(
            @PathVariable Long companyId,
            @RequestParam(required = false) Boolean activeOnly) {

        log.info("GET /api/v1/companies/{}/customers?activeOnly={}", companyId, activeOnly);
        List<CustomerResponse> responses = customerService.getAllCustomers(companyId, activeOnly);

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/search")
    @Operation(summary = "Rechercher des clients par nom",
               description = "Recherche partielle insensible à la casse dans les noms de clients")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> searchCustomers(
            @PathVariable Long companyId,
            @RequestParam String q) {

        log.info("GET /api/v1/companies/{}/customers/search?q={}", companyId, q);
        List<CustomerResponse> responses = customerService.searchCustomers(companyId, q);

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/{customerId}")
    @Operation(summary = "Mettre à jour un client")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable Long companyId,
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerUpdateRequest request) {

        log.info("PUT /api/v1/companies/{}/customers/{}", companyId, customerId);
        CustomerResponse response = customerService.updateCustomer(companyId, customerId, request);

        return ResponseEntity.ok(ApiResponse.success(response, "Client mis à jour avec succès"));
    }

    @PatchMapping("/{customerId}/deactivate")
    @Operation(summary = "Désactiver un client",
               description = "Soft delete - le client reste en base mais est marqué inactif")
    public ResponseEntity<ApiResponse<Void>> deactivateCustomer(
            @PathVariable Long companyId,
            @PathVariable Long customerId) {

        log.info("PATCH /api/v1/companies/{}/customers/{}/deactivate", companyId, customerId);
        customerService.deactivateCustomer(companyId, customerId);

        return ResponseEntity.ok(ApiResponse.success(null, "Client désactivé avec succès"));
    }

    @PatchMapping("/{customerId}/reactivate")
    @Operation(summary = "Réactiver un client")
    public ResponseEntity<ApiResponse<Void>> reactivateCustomer(
            @PathVariable Long companyId,
            @PathVariable Long customerId) {

        log.info("PATCH /api/v1/companies/{}/customers/{}/reactivate", companyId, customerId);
        customerService.reactivateCustomer(companyId, customerId);

        return ResponseEntity.ok(ApiResponse.success(null, "Client réactivé avec succès"));
    }

    @DeleteMapping("/{customerId}")
    @Operation(summary = "Supprimer définitivement un client",
               description = "ATTENTION: Suppression définitive. À utiliser avec précaution si des écritures sont liées.")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(
            @PathVariable Long companyId,
            @PathVariable Long customerId) {

        log.warn("DELETE /api/v1/companies/{}/customers/{} - Suppression définitive", companyId, customerId);
        customerService.deleteCustomer(companyId, customerId);

        return ResponseEntity.ok(ApiResponse.success(null, "Client supprimé définitivement"));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Obtenir les statistiques des clients",
               description = "Retourne le nombre total de clients, clients avec NIU, par type, etc.")
    public ResponseEntity<ApiResponse<CustomerService.CustomerStatistics>> getCustomerStatistics(
            @PathVariable Long companyId) {

        log.info("GET /api/v1/companies/{}/customers/statistics", companyId);
        CustomerService.CustomerStatistics stats = customerService.getCustomerStatistics(companyId);

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
