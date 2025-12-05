package com.predykt.accounting.controller;

import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.AuxiliaryJournalResponse;
import com.predykt.accounting.service.AuxiliaryJournalsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller pour les journaux auxiliaires OHADA
 *
 * Les 6 journaux obligatoires:
 * - VE: Journal des Ventes
 * - AC: Journal des Achats
 * - BQ: Journal de Banque
 * - CA: Journal de Caisse
 * - OD: Journal des Opérations Diverses
 * - AN: Journal à Nouveaux
 */
@RestController
@RequestMapping("/companies/{companyId}/journals")
@RequiredArgsConstructor
@Tag(name = "Journaux Auxiliaires", description = "Génération des journaux auxiliaires OHADA")
public class AuxiliaryJournalsController {

    private final AuxiliaryJournalsService journalsService;

    @GetMapping("/sales")
    @Operation(summary = "Journal des Ventes (VE)",
               description = "Génère le journal des ventes avec toutes les factures clients et la TVA collectée - OBLIGATOIRE OHADA")
    public ResponseEntity<ApiResponse<AuxiliaryJournalResponse>> getSalesJournal(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        AuxiliaryJournalResponse journal = journalsService.getSalesJournal(companyId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(journal,
            String.format("Journal des ventes généré: %d écritures - Total TTC: %,.0f XAF",
                journal.getNumberOfEntries(),
                journal.getStatistics() != null ? journal.getStatistics().getTotalSalesTTC().doubleValue() : 0)));
    }

    @GetMapping("/purchases")
    @Operation(summary = "Journal des Achats (AC)",
               description = "Génère le journal des achats avec toutes les factures fournisseurs et la TVA déductible - OBLIGATOIRE OHADA")
    public ResponseEntity<ApiResponse<AuxiliaryJournalResponse>> getPurchasesJournal(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        AuxiliaryJournalResponse journal = journalsService.getPurchasesJournal(companyId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(journal,
            String.format("Journal des achats généré: %d écritures - Total TTC: %,.0f XAF",
                journal.getNumberOfEntries(),
                journal.getStatistics() != null ? journal.getStatistics().getTotalPurchasesTTC().doubleValue() : 0)));
    }

    @GetMapping("/bank")
    @Operation(summary = "Journal de Banque (BQ)",
               description = "Génère le journal de banque avec tous les mouvements bancaires - OBLIGATOIRE OHADA")
    public ResponseEntity<ApiResponse<AuxiliaryJournalResponse>> getBankJournal(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        AuxiliaryJournalResponse journal = journalsService.getBankJournal(companyId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(journal,
            String.format("Journal de banque généré: %d mouvements - Flux net: %,.0f XAF",
                journal.getNumberOfEntries(),
                journal.getStatistics() != null ? journal.getStatistics().getNetCashFlow().doubleValue() : 0)));
    }

    @GetMapping("/cash")
    @Operation(summary = "Journal de Caisse (CA)",
               description = "Génère le journal de caisse avec tous les mouvements d'espèces - OBLIGATOIRE OHADA")
    public ResponseEntity<ApiResponse<AuxiliaryJournalResponse>> getCashJournal(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        AuxiliaryJournalResponse journal = journalsService.getCashJournal(companyId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(journal,
            String.format("Journal de caisse généré: %d mouvements - Flux net: %,.0f XAF",
                journal.getNumberOfEntries(),
                journal.getStatistics() != null ? journal.getStatistics().getNetCashMovement().doubleValue() : 0)));
    }

    @GetMapping("/general")
    @Operation(summary = "Journal des Opérations Diverses (OD)",
               description = "Génère le journal des opérations diverses (provisions, corrections, régularisations) - OBLIGATOIRE OHADA")
    public ResponseEntity<ApiResponse<AuxiliaryJournalResponse>> getGeneralJournal(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        AuxiliaryJournalResponse journal = journalsService.getGeneralJournal(companyId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(journal,
            String.format("Journal des opérations diverses généré: %d écritures", journal.getNumberOfEntries())));
    }

    @GetMapping("/opening")
    @Operation(summary = "Journal à Nouveaux (AN)",
               description = "Génère le journal à nouveaux (écritures d'ouverture d'exercice) - OBLIGATOIRE OHADA")
    public ResponseEntity<ApiResponse<AuxiliaryJournalResponse>> getOpeningJournal(
            @PathVariable Long companyId,
            @RequestParam Integer fiscalYear) {

        AuxiliaryJournalResponse journal = journalsService.getOpeningJournal(companyId, fiscalYear);

        return ResponseEntity.ok(ApiResponse.success(journal,
            String.format("Journal à nouveaux %d généré: %d écritures d'ouverture",
                fiscalYear, journal.getNumberOfEntries())));
    }
}
