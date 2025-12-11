package com.predykt.accounting.controller;

import com.predykt.accounting.dto.request.DepositApplyRequest;
import com.predykt.accounting.dto.request.DepositCreateRequest;
import com.predykt.accounting.dto.request.DepositUpdateRequest;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.DepositResponse;
import com.predykt.accounting.service.DepositService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 🔵 CONTRÔLEUR REST: Gestion des Acomptes (OHADA Compte 4191)
 *
 * Endpoints pour la gestion complète des acomptes clients:
 * - Création avec génération automatique de reçu (RA-YYYY-NNNNNN)
 * - Consultation (par ID, numéro, client)
 * - Imputation sur facture finale
 * - Annulation d'imputation (correction d'erreurs)
 * - Recherche avancée multi-critères
 * - Statistiques et reporting
 *
 * Conformité OHADA:
 * - SYSCOHADA Articles 276-279: Compte 4191 obligatoire
 * - CGI Cameroun Article 128: TVA exigible sur encaissement
 * - Génération automatique d'écritures comptables
 *
 * @author PREDYKT System Optimizer
 * @since Phase 3 - Conformité OHADA Avancée
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/deposits")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Acomptes Clients", description = "Gestion des acomptes reçus (OHADA Compte 4191) - Conforme SYSCOHADA")
public class DepositController {

    private final DepositService depositService;

    // ==================== CRÉATION ====================

    @PostMapping
    @Operation(
        summary = "Créer un acompte",
        description = "Crée un nouvel acompte avec génération automatique du reçu (RA-YYYY-NNNNNN). " +
                     "La TVA (19.25%) est calculée automatiquement sur le montant HT selon CGI Cameroun Article 128 " +
                     "(TVA exigible sur encaissement). " +
                     "\n\nÉcriture comptable générée automatiquement:" +
                     "\nDÉBIT 512 Banque" +
                     "\n  CRÉDIT 4191 Clients - Avances et acomptes (HT)" +
                     "\n  CRÉDIT 4431 TVA collectée" +
                     "\n\nExemple: Acompte 100 000 XAF HT → TVA 19 250 XAF → Total 119 250 XAF TTC"
    )
    public ResponseEntity<ApiResponse<DepositResponse>> createDeposit(
        @PathVariable @Parameter(description = "ID de la société") Long companyId,
        @Valid @RequestBody DepositCreateRequest request
    ) {
        log.info("POST /api/v1/companies/{}/deposits - Création acompte {} XAF HT",
            companyId, request.getAmountHt());

        DepositResponse response = depositService.createDeposit(companyId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, String.format(
                "Acompte %s créé avec succès: %s XAF TTC (TVA %.2f%%)",
                response.getDepositNumber(),
                response.getAmountTtc(),
                response.getVatRate()
            )));
    }

    // ==================== CONSULTATION ====================

    @GetMapping("/{depositId}")
    @Operation(
        summary = "Obtenir un acompte par ID",
        description = "Récupère les détails complets d'un acompte, incluant son statut d'imputation " +
                     "et le montant disponible pour imputation"
    )
    public ResponseEntity<ApiResponse<DepositResponse>> getDeposit(
        @PathVariable Long companyId,
        @PathVariable @Parameter(description = "ID de l'acompte") Long depositId
    ) {
        log.info("GET /api/v1/companies/{}/deposits/{}", companyId, depositId);

        DepositResponse response = depositService.getDeposit(companyId, depositId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/number/{depositNumber}")
    @Operation(
        summary = "Obtenir un acompte par numéro",
        description = "Récupère un acompte par son numéro de reçu (ex: RA-2025-000001)"
    )
    public ResponseEntity<ApiResponse<DepositResponse>> getDepositByNumber(
        @PathVariable Long companyId,
        @PathVariable @Parameter(description = "Numéro du reçu d'acompte (ex: RA-2025-000001)") String depositNumber
    ) {
        log.info("GET /api/v1/companies/{}/deposits/number/{}", companyId, depositNumber);

        DepositResponse response = depositService.getDepositByNumber(companyId, depositNumber);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(
        summary = "Lister tous les acomptes",
        description = "Liste tous les acomptes d'une société avec pagination. " +
                     "Triés par date de réception décroissante par défaut."
    )
    public ResponseEntity<ApiResponse<Page<DepositResponse>>> getAllDeposits(
        @PathVariable Long companyId,
        @PageableDefault(size = 20, sort = "depositDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("GET /api/v1/companies/{}/deposits - Liste acomptes (page {}, size {})",
            companyId, pageable.getPageNumber(), pageable.getPageSize());

        Page<DepositResponse> response = depositService.getAllDeposits(companyId, pageable);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/customer/{customerId}/available")
    @Operation(
        summary = "Lister les acomptes disponibles d'un client",
        description = "Liste tous les acomptes NON IMPUTÉS d'un client, disponibles pour imputation sur une facture. " +
                     "Utile lors de la facturation pour voir les acomptes existants à imputer."
    )
    public ResponseEntity<ApiResponse<List<DepositResponse>>> getAvailableDepositsForCustomer(
        @PathVariable Long companyId,
        @PathVariable @Parameter(description = "ID du client") Long customerId
    ) {
        log.info("GET /api/v1/companies/{}/deposits/customer/{}/available", companyId, customerId);

        List<DepositResponse> response = depositService.getAvailableDepositsForCustomer(companyId, customerId);

        return ResponseEntity.ok(ApiResponse.success(response, String.format(
            "%d acompte(s) disponible(s) pour ce client", response.size()
        )));
    }

    @GetMapping("/search")
    @Operation(
        summary = "Recherche avancée multi-critères",
        description = "Recherche d'acomptes avec filtres multiples: client, statut, période, montant. " +
                     "Tous les paramètres sont optionnels. Pagination supportée."
    )
    public ResponseEntity<ApiResponse<Page<DepositResponse>>> searchDeposits(
        @PathVariable Long companyId,
        @RequestParam(required = false) @Parameter(description = "ID du client") Long customerId,
        @RequestParam(required = false) @Parameter(description = "Statut: true = imputé, false = disponible") Boolean isApplied,
        @RequestParam(required = false) @Parameter(description = "Date de début (YYYY-MM-DD)") LocalDate startDate,
        @RequestParam(required = false) @Parameter(description = "Date de fin (YYYY-MM-DD)") LocalDate endDate,
        @RequestParam(required = false) @Parameter(description = "Montant minimum (TTC)") BigDecimal minAmount,
        @RequestParam(required = false) @Parameter(description = "Montant maximum (TTC)") BigDecimal maxAmount,
        @PageableDefault(size = 20, sort = "depositDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("GET /api/v1/companies/{}/deposits/search - Recherche multi-critères", companyId);

        Page<DepositResponse> response = depositService.searchDeposits(
            companyId, customerId, isApplied, startDate, endDate, minAmount, maxAmount, pageable
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== MODIFICATION ====================

    @PutMapping("/{depositId}")
    @Operation(
        summary = "Modifier un acompte",
        description = "Modifie les informations d'un acompte NON IMPUTÉ. " +
                     "\n\nIMPORTANT: Les montants (amountHt, vatRate) ne peuvent PAS être modifiés pour garantir " +
                     "l'intégrité comptable. Si modification des montants nécessaire: annuler l'acompte et en créer un nouveau." +
                     "\n\nChamps modifiables:" +
                     "\n- Date de réception" +
                     "\n- Client" +
                     "\n- Description" +
                     "\n- Référence commande client" +
                     "\n- Notes internes"
    )
    public ResponseEntity<ApiResponse<DepositResponse>> updateDeposit(
        @PathVariable Long companyId,
        @PathVariable @Parameter(description = "ID de l'acompte") Long depositId,
        @Valid @RequestBody DepositUpdateRequest request
    ) {
        log.info("PUT /api/v1/companies/{}/deposits/{} - Modification acompte", companyId, depositId);

        DepositResponse response = depositService.updateDeposit(companyId, depositId, request);

        return ResponseEntity.ok(ApiResponse.success(response, "Acompte modifié avec succès"));
    }

    // ==================== IMPUTATION ====================

    @PostMapping("/{depositId}/apply")
    @Operation(
        summary = "Imputer un acompte sur une facture",
        description = "Impute un acompte disponible sur une facture finale. " +
                     "\n\nProcessus OHADA:" +
                     "\n1. Validation: acompte non imputé, client correspond, montant ≤ total facture" +
                     "\n2. Imputation: acompte.isApplied = true, lien avec facture" +
                     "\n3. Mise à jour facture: amountPaid += deposit.amountTtc" +
                     "\n4. Écriture comptable générée:" +
                     "\n   DÉBIT 4191 Clients - Avances (HT)" +
                     "\n   DÉBIT 4431 TVA collectée" +
                     "\n     CRÉDIT 411 Clients (TTC)" +
                     "\n\nExemple:" +
                     "\nFacture 500 000 XAF TTC" +
                     "\nAcompte imputé 119 250 XAF TTC" +
                     "\nSolde restant dû: 380 750 XAF"
    )
    public ResponseEntity<ApiResponse<DepositResponse>> applyDepositToInvoice(
        @PathVariable Long companyId,
        @PathVariable @Parameter(description = "ID de l'acompte") Long depositId,
        @Valid @RequestBody DepositApplyRequest request
    ) {
        log.info("POST /api/v1/companies/{}/deposits/{}/apply - Imputation sur facture {}",
            companyId, depositId, request.getInvoiceId());

        DepositResponse response = depositService.applyDepositToInvoice(companyId, depositId, request);

        return ResponseEntity.ok(ApiResponse.success(response, String.format(
            "Acompte %s (%s XAF) imputé avec succès sur la facture",
            response.getDepositNumber(),
            response.getAmountTtc()
        )));
    }

    @PostMapping("/{depositId}/unapply")
    @Operation(
        summary = "Annuler l'imputation d'un acompte",
        description = "Annule l'imputation d'un acompte (correction d'erreur). " +
                     "\n\nL'acompte redevient disponible pour imputation sur une autre facture. " +
                     "Le montant payé de la facture est recalculé automatiquement." +
                     "\n\n⚠️ ATTENTION: Opération de correction, à utiliser avec précaution."
    )
    public ResponseEntity<ApiResponse<DepositResponse>> unapplyDeposit(
        @PathVariable Long companyId,
        @PathVariable @Parameter(description = "ID de l'acompte") Long depositId
    ) {
        log.warn("POST /api/v1/companies/{}/deposits/{}/unapply - Annulation imputation", companyId, depositId);

        DepositResponse response = depositService.unapplyDeposit(companyId, depositId);

        return ResponseEntity.ok(ApiResponse.success(response, "Imputation annulée avec succès"));
    }

    // ==================== STATISTIQUES ====================

    @GetMapping("/statistics/available-total")
    @Operation(
        summary = "Total des acomptes disponibles",
        description = "Calcule le montant total (TTC) des acomptes NON IMPUTÉS pour une société. " +
                     "Représente les fonds reçus des clients en attente d'imputation sur facture."
    )
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalAvailableDeposits(
        @PathVariable Long companyId
    ) {
        log.info("GET /api/v1/companies/{}/deposits/statistics/available-total", companyId);

        BigDecimal total = depositService.getTotalAvailableDeposits(companyId);

        return ResponseEntity.ok(ApiResponse.success(total, String.format(
            "Total des acomptes disponibles: %s XAF TTC", total
        )));
    }

    @GetMapping("/statistics/customer/{customerId}/available-total")
    @Operation(
        summary = "Total des acomptes disponibles d'un client",
        description = "Calcule le montant total (TTC) des acomptes NON IMPUTÉS pour un client spécifique. " +
                     "Utile lors de la facturation pour calculer le solde disponible."
    )
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalAvailableDepositsForCustomer(
        @PathVariable Long companyId,
        @PathVariable @Parameter(description = "ID du client") Long customerId
    ) {
        log.info("GET /api/v1/companies/{}/deposits/statistics/customer/{}/available-total",
            companyId, customerId);

        BigDecimal total = depositService.getTotalAvailableDepositsForCustomer(companyId, customerId);

        return ResponseEntity.ok(ApiResponse.success(total, String.format(
            "Total des acomptes disponibles pour ce client: %s XAF TTC", total
        )));
    }
}
