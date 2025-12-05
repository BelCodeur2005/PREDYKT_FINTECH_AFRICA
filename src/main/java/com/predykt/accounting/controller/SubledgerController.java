package com.predykt.accounting.controller;

import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.SubledgerResponse;
import com.predykt.accounting.service.SubledgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller pour les Grands Livres Auxiliaires
 * Conforme OHADA - Clients (411x) et Fournisseurs (401x)
 */
@RestController
@RequestMapping("/companies/{companyId}/subledgers")
@RequiredArgsConstructor
@Tag(name = "Grands Livres Auxiliaires", description = "Génération des grands livres auxiliaires clients et fournisseurs conformes OHADA")
public class SubledgerController {

    private final SubledgerService subledgerService;

    @GetMapping("/customers")
    @Operation(summary = "Grand livre auxiliaire CLIENTS",
               description = "Génère le grand livre auxiliaire de tous les clients (comptes 411x) avec détail des créances, " +
                   "échéances, analyse des retards et statistiques - OBLIGATOIRE OHADA")
    public ResponseEntity<ApiResponse<SubledgerResponse>> getCustomersSubledger(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        SubledgerResponse subledger = subledgerService.getCustomersSubledger(companyId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(subledger,
            String.format("Grand livre auxiliaire clients généré: %d clients - Solde total: %,.0f XAF",
                subledger.getNombreTiers(),
                subledger.getTotalSoldeCloture().doubleValue())));
    }

    @GetMapping("/suppliers")
    @Operation(summary = "Grand livre auxiliaire FOURNISSEURS",
               description = "Génère le grand livre auxiliaire de tous les fournisseurs (comptes 401x) avec détail des dettes, " +
                   "échéances, analyse des retards et statistiques - OBLIGATOIRE OHADA")
    public ResponseEntity<ApiResponse<SubledgerResponse>> getSuppliersSubledger(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        SubledgerResponse subledger = subledgerService.getSuppliersSubledger(companyId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(subledger,
            String.format("Grand livre auxiliaire fournisseurs généré: %d fournisseurs - Solde total: %,.0f XAF",
                subledger.getNombreTiers(),
                subledger.getTotalSoldeCloture().abs().doubleValue())));
    }

    @GetMapping("/customers/{accountNumber}")
    @Operation(summary = "Grand livre d'UN client",
               description = "Génère le grand livre auxiliaire détaillé pour un client spécifique (compte 411xxx)")
    public ResponseEntity<ApiResponse<SubledgerResponse>> getCustomerSubledger(
            @PathVariable Long companyId,
            @PathVariable String accountNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        SubledgerResponse subledger = subledgerService.getCustomerSubledger(companyId, accountNumber, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(subledger,
            String.format("Grand livre client %s généré: %d écritures",
                accountNumber, subledger.getNombreEcritures())));
    }

    @GetMapping("/suppliers/{accountNumber}")
    @Operation(summary = "Grand livre d'UN fournisseur",
               description = "Génère le grand livre auxiliaire détaillé pour un fournisseur spécifique (compte 401xxx)")
    public ResponseEntity<ApiResponse<SubledgerResponse>> getSupplierSubledger(
            @PathVariable Long companyId,
            @PathVariable String accountNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        SubledgerResponse subledger = subledgerService.getSupplierSubledger(companyId, accountNumber, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(subledger,
            String.format("Grand livre fournisseur %s généré: %d écritures",
                accountNumber, subledger.getNombreEcritures())));
    }
}
