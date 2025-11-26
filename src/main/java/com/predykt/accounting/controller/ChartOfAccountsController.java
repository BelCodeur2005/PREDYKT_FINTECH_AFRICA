package com.predykt.accounting.controller;

import com.predykt.accounting.domain.entity.ChartOfAccounts;
import com.predykt.accounting.domain.enums.AccountType;
import com.predykt.accounting.dto.request.AccountCreateRequest;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.service.ChartOfAccountsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur Plan Comptable (Chart of Accounts)
 */
@RestController
@RequestMapping("/companies/{companyId}/charts")
@RequiredArgsConstructor
@Tag(name = "Plan Comptable", description = "Gestion du plan comptable OHADA")
public class ChartOfAccountsController {
    
    private final ChartOfAccountsService chartService;
    
    @PostMapping("/initialize")
    @PreAuthorize("hasAuthority('COMPANY_WRITE')")
    @Operation(summary = "Initialiser le plan comptable OHADA",
               description = "Charge le plan comptable OHADA par défaut pour l'entreprise")
    public ResponseEntity<ApiResponse<Void>> initializeChartOfAccounts(
            @PathVariable Long companyId) {
        
        chartService.initializeDefaultChartOfAccounts(companyId);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                null,
                "Plan comptable OHADA initialisé avec succès"
            ));
    }
    
    @GetMapping
    @PreAuthorize("hasAuthority('COMPANY_READ')")
    @Operation(summary = "Lister tous les comptes actifs",
               description = "Récupère la liste complète des comptes comptables actifs")
    public ResponseEntity<ApiResponse<List<ChartOfAccounts>>> getAllActiveAccounts(
            @PathVariable Long companyId) {
        
        List<ChartOfAccounts> accounts = chartService.getActiveAccounts(companyId);
        
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }
    
    @GetMapping("/{accountNumber}")
    @PreAuthorize("hasAuthority('COMPANY_READ')")
    @Operation(summary = "Obtenir un compte par son numéro",
               description = "Récupère les détails d'un compte comptable spécifique")
    public ResponseEntity<ApiResponse<ChartOfAccounts>> getAccountByNumber(
            @PathVariable Long companyId,
            @Parameter(description = "Numéro de compte OHADA (ex: 701)") 
            @PathVariable String accountNumber) {
        
        ChartOfAccounts account = chartService.getAccountByNumber(companyId, accountNumber);
        
        return ResponseEntity.ok(ApiResponse.success(account));
    }
    
    @GetMapping("/type/{accountType}")
    @PreAuthorize("hasAuthority('COMPANY_READ')")
    @Operation(summary = "Lister les comptes par type",
               description = "Récupère tous les comptes d'un type spécifique (ACTIF, PASSIF, CHARGES, PRODUITS)")
    public ResponseEntity<ApiResponse<List<ChartOfAccounts>>> getAccountsByType(
            @PathVariable Long companyId,
            @PathVariable AccountType accountType) {
        
        List<ChartOfAccounts> accounts = chartService.getAccountsByType(companyId, accountType);
        
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }
    
    @PostMapping
    @PreAuthorize("hasAuthority('COMPANY_WRITE')")
    @Operation(summary = "Créer un compte personnalisé",
               description = "Ajoute un nouveau compte comptable personnalisé au plan")
    public ResponseEntity<ApiResponse<ChartOfAccounts>> createCustomAccount(
            @PathVariable Long companyId,
            @Valid @RequestBody ChartOfAccounts request) {
        
        ChartOfAccounts account = chartService.createCustomAccount(companyId, request);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                account,
                "Compte créé avec succès"
            ));
    }
    
    @PutMapping("/{accountId}/activate")
    @PreAuthorize("hasAuthority('COMPANY_WRITE')")
    @Operation(summary = "Activer un compte",
               description = "Réactive un compte désactivé")
    public ResponseEntity<ApiResponse<Void>> activateAccount(
            @PathVariable Long companyId,
            @PathVariable Long accountId) {
        
        chartService.activateAccount(companyId, accountId);
        
        return ResponseEntity.ok(ApiResponse.success(
            null,
            "Compte activé"
        ));
    }
    
    @PutMapping("/{accountId}/deactivate")
    @PreAuthorize("hasAuthority('COMPANY_WRITE')")
    @Operation(summary = "Désactiver un compte",
               description = "Désactive un compte (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(
            @PathVariable Long companyId,
            @PathVariable Long accountId) {
        
        chartService.desactivateAccount(companyId, accountId);
        
        return ResponseEntity.ok(ApiResponse.success(
            null,
            "Compte désactivé"
        ));
    }
    
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('COMPANY_READ')")
    @Operation(summary = "Rechercher des comptes",
               description = "Recherche des comptes par numéro ou libellé")
    public ResponseEntity<ApiResponse<List<ChartOfAccounts>>> searchAccounts(
            @PathVariable Long companyId,
            @RequestParam(required = false) String query) {
        
        List<ChartOfAccounts> accounts = chartService.searchAccounts(companyId, query);
        
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }
}