package com.predykt.accounting.controller;

import com.predykt.accounting.domain.entity.GeneralLedger;
import com.predykt.accounting.dto.request.JournalEntryRequest;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.service.GeneralLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/companies/{companyId}/journal-entries")
@RequiredArgsConstructor
@Tag(name = "Grand Livre", description = "Gestion des écritures comptables")
public class GeneralLedgerController {
    
    private final GeneralLedgerService glService;
    
    @PostMapping
    @Operation(summary = "Enregistrer une écriture comptable",
description = "Enregistre une nouvelle écriture comptable en respectant la partie double (Débit = Crédit)")
    public ResponseEntity<ApiResponse<List<GeneralLedger>>> createJournalEntry(
            @Parameter(description = "ID de l'entreprise") @PathVariable Long companyId,
            @Valid @RequestBody JournalEntryRequest request) {
        
        List<GeneralLedger> entries = glService.recordJournalEntry(companyId, request);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                entries,
                "Écriture enregistrée avec succès : " + entries.size() + " lignes"
            ));
    }
    
    @GetMapping("/accounts/{accountNumber}/balance")
    @Operation(summary = "Obtenir le solde d'un compte",
               description = "Calcule le solde d'un compte à une date donnée")
    public ResponseEntity<ApiResponse<BigDecimal>> getAccountBalance(
            @PathVariable Long companyId,
            @PathVariable String accountNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        
        BigDecimal balance = glService.getAccountBalance(companyId, accountNumber, asOfDate);
        
        return ResponseEntity.ok(ApiResponse.success(
            balance,
            String.format("Solde du compte %s au %s", accountNumber, asOfDate)
        ));
    }
    
    @GetMapping("/accounts/{accountNumber}/ledger")
    @Operation(summary = "Obtenir le grand livre d'un compte",
               description = "Récupère toutes les écritures d'un compte sur une période")
    public ResponseEntity<ApiResponse<List<GeneralLedger>>> getAccountLedger(
            @PathVariable Long companyId,
            @PathVariable String accountNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<GeneralLedger> entries = glService.getAccountLedger(
            companyId, accountNumber, startDate, endDate
        );
        
        return ResponseEntity.ok(ApiResponse.success(
            entries,
            String.format("%d écritures trouvées", entries.size())
        ));
    }
    
    @GetMapping("/trial-balance")
    @Operation(summary = "Balance de vérification",
               description = "Génère la balance de vérification pour une période donnée")
    public ResponseEntity<ApiResponse<List<GeneralLedgerService.TrialBalanceEntry>>> getTrialBalance(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<GeneralLedgerService.TrialBalanceEntry> trialBalance = 
            glService.getTrialBalance(companyId, startDate, endDate);
        
        return ResponseEntity.ok(ApiResponse.success(trialBalance));
    }
    
    @PostMapping("/lock-period")
    @Operation(summary = "Verrouiller une période comptable",
               description = "Verrouille les écritures d'une période (clôture)")
    public ResponseEntity<ApiResponse<Void>> lockPeriod(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        glService.lockPeriod(companyId, startDate, endDate);
        
        return ResponseEntity.ok(ApiResponse.success(
            null,
            "Période verrouillée avec succès"
        ));
    }
}