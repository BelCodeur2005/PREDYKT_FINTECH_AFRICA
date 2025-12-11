package com.predykt.accounting.controller;

import com.predykt.accounting.domain.enums.InvoiceStatus;
import com.predykt.accounting.dto.request.InvoiceCreateRequest;
import com.predykt.accounting.dto.request.InvoiceUpdateRequest;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.InvoicePaymentSummaryResponse;
import com.predykt.accounting.dto.response.InvoiceResponse;
import com.predykt.accounting.service.InvoiceService;
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
 * Contrôleur REST pour la gestion des factures clients (Invoices)
 * Conforme OHADA avec génération automatique écritures comptables
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/invoices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Facturation Client", description = "Gestion des factures clients (Invoices) - Conforme OHADA")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    @Operation(summary = "Créer une facture client",
               description = "Crée une nouvelle facture en statut DRAFT. Les totaux HT/TVA/TTC sont calculés automatiquement.")
    public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoice(
            @PathVariable Long companyId,
            @Valid @RequestBody InvoiceCreateRequest request) {

        log.info("POST /api/v1/companies/{}/invoices - Création facture client", companyId);
        InvoiceResponse response = invoiceService.createInvoice(companyId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Facture créée avec succès en mode DRAFT"));
    }

    @GetMapping("/{invoiceId}")
    @Operation(summary = "Obtenir une facture par ID",
               description = "Récupère les détails complets d'une facture, incluant toutes les lignes")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoice(
            @PathVariable Long companyId,
            @PathVariable Long invoiceId) {

        log.info("GET /api/v1/companies/{}/invoices/{}", companyId, invoiceId);
        InvoiceResponse response = invoiceService.getInvoice(companyId, invoiceId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{invoiceId}/payments")
    @Operation(summary = "Obtenir l'historique des paiements d'une facture (Option B - OHADA)",
               description = "Récupère l'historique complet des paiements fractionnés d'une facture avec statistiques détaillées. " +
                           "Conforme OHADA : Chaque paiement a été enregistré séparément à sa date effective, " +
                           "créant une écriture comptable distincte (DÉBIT Banque / CRÉDIT Client). " +
                           "\n\nExemple pratique :" +
                           "\nFacture 200 000 XAF" +
                           "\n• Paiement 1 : 15/03 - 100 000 XAF (50%)" +
                           "\n• Paiement 2 : 30/03 - 100 000 XAF (50%)" +
                           "\nTotal : 200 000 XAF (100%) - PAYÉE" +
                           "\n\nLe résumé inclut :" +
                           "\n- Liste de tous les paiements avec détails complets" +
                           "\n- Statistiques : montant total payé, pourcentage, nombre de paiements" +
                           "\n- Statut : en cours, partiellement payée, totalement payée" +
                           "\n- Indicateurs de retard si applicable")
    public ResponseEntity<ApiResponse<InvoicePaymentSummaryResponse>> getInvoicePayments(
            @PathVariable Long companyId,
            @PathVariable Long invoiceId) {

        log.info("GET /api/v1/companies/{}/invoices/{}/payments - Récupération historique paiements", companyId, invoiceId);
        InvoicePaymentSummaryResponse response = invoiceService.getInvoicePaymentSummary(companyId, invoiceId);

        String message = String.format("%d paiement(s) enregistré(s) - %.2f%% payé (%s / %s XAF)",
            response.getPaymentCount(),
            response.getPaymentPercentage(),
            response.getAmountPaid(),
            response.getTotalTtc());

        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @GetMapping
    @Operation(summary = "Lister toutes les factures",
               description = "Récupère toutes les factures d'une entreprise. Paramètre 'status' optionnel pour filtrer.")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getAllInvoices(
            @PathVariable Long companyId,
            @RequestParam(required = false) InvoiceStatus status) {

        log.info("GET /api/v1/companies/{}/invoices?status={}", companyId, status);
        List<InvoiceResponse> responses = invoiceService.getAllInvoices(companyId, status);

        return ResponseEntity.ok(ApiResponse.success(responses,
            String.format("%d facture(s) trouvée(s)", responses.size())));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Lister les factures d'un client",
               description = "Récupère toutes les factures d'un client spécifique")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getInvoicesByCustomer(
            @PathVariable Long companyId,
            @PathVariable Long customerId) {

        log.info("GET /api/v1/companies/{}/invoices/customer/{}", companyId, customerId);
        List<InvoiceResponse> responses = invoiceService.getInvoicesByCustomer(companyId, customerId);

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Obtenir les factures en retard (Balance âgée)",
               description = "Liste toutes les factures échues non payées, avec catégorisation par ancienneté")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getOverdueInvoices(
            @PathVariable Long companyId) {

        log.info("GET /api/v1/companies/{}/invoices/overdue", companyId);
        List<InvoiceResponse> responses = invoiceService.getOverdueInvoices(companyId);

        return ResponseEntity.ok(ApiResponse.success(responses,
            String.format("%d facture(s) en retard", responses.size())));
    }

    @PutMapping("/{invoiceId}")
    @Operation(summary = "Mettre à jour une facture",
               description = "Modifie une facture en statut DRAFT uniquement. Les factures validées ne peuvent pas être modifiées.")
    public ResponseEntity<ApiResponse<InvoiceResponse>> updateInvoice(
            @PathVariable Long companyId,
            @PathVariable Long invoiceId,
            @Valid @RequestBody InvoiceUpdateRequest request) {

        log.info("PUT /api/v1/companies/{}/invoices/{}", companyId, invoiceId);
        InvoiceResponse response = invoiceService.updateInvoice(companyId, invoiceId, request);

        return ResponseEntity.ok(ApiResponse.success(response, "Facture mise à jour avec succès"));
    }

    @PostMapping("/{invoiceId}/validate")
    @Operation(summary = "VALIDER une facture (IRRÉVERSIBLE)",
               description = "Valide la facture et génère automatiquement l'écriture comptable OHADA. " +
                           "Action IRRÉVERSIBLE : statut passe de DRAFT à ISSUED. " +
                           "Écriture générée : DÉBIT Client (411) / CRÉDIT Ventes (701) + TVA collectée (4431)")
    public ResponseEntity<ApiResponse<InvoiceResponse>> validateInvoice(
            @PathVariable Long companyId,
            @PathVariable Long invoiceId) {

        log.info("POST /api/v1/companies/{}/invoices/{}/validate - VALIDATION FACTURE", companyId, invoiceId);
        InvoiceResponse response = invoiceService.validateInvoice(companyId, invoiceId);

        return ResponseEntity.ok(ApiResponse.success(response,
            "Facture validée avec succès. Écriture comptable générée automatiquement."));
    }

    @PostMapping("/{invoiceId}/cancel")
    @Operation(summary = "Annuler une facture",
               description = "Annule une facture (statut CANCELLED). Impossible si déjà payée partiellement ou totalement.")
    public ResponseEntity<ApiResponse<InvoiceResponse>> cancelInvoice(
            @PathVariable Long companyId,
            @PathVariable Long invoiceId) {

        log.info("POST /api/v1/companies/{}/invoices/{}/cancel", companyId, invoiceId);
        InvoiceResponse response = invoiceService.cancelInvoice(companyId, invoiceId);

        return ResponseEntity.ok(ApiResponse.success(response, "Facture annulée avec succès"));
    }

    @DeleteMapping("/{invoiceId}")
    @Operation(summary = "Supprimer définitivement une facture",
               description = "ATTENTION : Suppression définitive. Possible uniquement pour les factures en DRAFT.")
    public ResponseEntity<ApiResponse<Void>> deleteInvoice(
            @PathVariable Long companyId,
            @PathVariable Long invoiceId) {

        log.warn("DELETE /api/v1/companies/{}/invoices/{} - Suppression définitive", companyId, invoiceId);
        invoiceService.deleteInvoice(companyId, invoiceId);

        return ResponseEntity.ok(ApiResponse.success(null, "Facture supprimée définitivement"));
    }
}
