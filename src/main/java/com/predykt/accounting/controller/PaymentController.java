package com.predykt.accounting.controller;

import com.predykt.accounting.domain.enums.PaymentType;
import com.predykt.accounting.dto.request.PaymentCreateRequest;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.PaymentResponse;
import com.predykt.accounting.service.PaymentService;
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
 * Contrôleur REST pour la gestion des paiements
 * Conforme OHADA avec lettrage automatique
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Paiements", description = "Gestion des paiements clients et fournisseurs avec lettrage automatique")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/customer")
    @Operation(summary = "Enregistrer un paiement client (encaissement)",
               description = "Enregistre un paiement reçu d'un client. " +
                           "Mise à jour automatique de la facture et génération écriture comptable. " +
                           "Lettrage automatique si paiement total.")
    public ResponseEntity<ApiResponse<PaymentResponse>> recordCustomerPayment(
            @PathVariable Long companyId,
            @Valid @RequestBody PaymentCreateRequest request) {

        log.info("POST /api/v1/companies/{}/payments/customer - Encaissement client", companyId);
        PaymentResponse response = paymentService.recordCustomerPayment(companyId, request);

        String message = response.getIsReconciled()
            ? "Paiement enregistré et lettré automatiquement"
            : "Paiement enregistré (paiement partiel)";

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, message));
    }

    @PostMapping("/supplier")
    @Operation(summary = "Enregistrer un paiement fournisseur (décaissement)",
               description = "Enregistre un paiement effectué à un fournisseur. " +
                           "Mise à jour automatique de la facture et génération écriture comptable. " +
                           "Lettrage automatique si paiement total.")
    public ResponseEntity<ApiResponse<PaymentResponse>> recordSupplierPayment(
            @PathVariable Long companyId,
            @Valid @RequestBody PaymentCreateRequest request) {

        log.info("POST /api/v1/companies/{}/payments/supplier - Décaissement fournisseur", companyId);
        PaymentResponse response = paymentService.recordSupplierPayment(companyId, request);

        String message = response.getIsReconciled()
            ? "Paiement enregistré et lettré automatiquement"
            : "Paiement enregistré (paiement partiel)";

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, message));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Obtenir un paiement par ID",
               description = "Récupère les détails complets d'un paiement")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @PathVariable Long companyId,
            @PathVariable Long paymentId) {

        log.info("GET /api/v1/companies/{}/payments/{}", companyId, paymentId);
        PaymentResponse response = paymentService.getPayment(companyId, paymentId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Lister tous les paiements",
               description = "Récupère tous les paiements d'une entreprise. " +
                           "Paramètre 'type' optionnel : CUSTOMER_PAYMENT ou SUPPLIER_PAYMENT")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getAllPayments(
            @PathVariable Long companyId,
            @RequestParam(required = false) PaymentType type) {

        log.info("GET /api/v1/companies/{}/payments?type={}", companyId, type);
        List<PaymentResponse> responses = paymentService.getAllPayments(companyId, type);

        return ResponseEntity.ok(ApiResponse.success(responses,
            String.format("%d paiement(s) trouvé(s)", responses.size())));
    }

    @PostMapping("/{paymentId}/cancel")
    @Operation(summary = "Annuler un paiement",
               description = "Annule un paiement non lettré. Remet le montant sur la facture. " +
                           "Impossible d'annuler un paiement déjà lettré.")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelPayment(
            @PathVariable Long companyId,
            @PathVariable Long paymentId) {

        log.info("POST /api/v1/companies/{}/payments/{}/cancel", companyId, paymentId);
        PaymentResponse response = paymentService.cancelPayment(companyId, paymentId);

        return ResponseEntity.ok(ApiResponse.success(response,
            "Paiement annulé. Montant remis sur la facture."));
    }
}
