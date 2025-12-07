package com.predykt.accounting.controller;

import com.predykt.accounting.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/companies/{companyId}/exports")
@RequiredArgsConstructor
@Tag(name = "Exports", description = "Export de rapports financiers en PDF et Excel")
public class ExportController {

    private final ExportService exportService;
    private final com.predykt.accounting.service.BankReconciliationService bankReconciliationService;

    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @GetMapping("/balance-sheet/pdf")
    @Operation(summary = "Exporter le bilan en PDF",
               description = "Génère et télécharge le bilan comptable au format PDF")
    public ResponseEntity<byte[]> exportBalanceSheetToPdf(
            @PathVariable Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        if (asOfDate == null) {
            asOfDate = LocalDate.now();
        }

        try {
            byte[] pdfData = exportService.exportBalanceSheetToPdf(companyId, asOfDate);

            String filename = String.format("bilan_%s_%s.pdf",
                companyId, asOfDate.format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfData.length);

            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/balance-sheet/excel")
    @Operation(summary = "Exporter le bilan en Excel",
               description = "Génère et télécharge le bilan comptable au format Excel (.xlsx)")
    public ResponseEntity<byte[]> exportBalanceSheetToExcel(
            @PathVariable Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        if (asOfDate == null) {
            asOfDate = LocalDate.now();
        }

        try {
            byte[] excelData = exportService.exportBalanceSheetToExcel(companyId, asOfDate);

            String filename = String.format("bilan_%s_%s.xlsx",
                companyId, asOfDate.format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            headers.set("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/income-statement/pdf")
    @Operation(summary = "Exporter le compte de résultat en PDF",
               description = "Génère et télécharge le compte de résultat au format PDF")
    public ResponseEntity<byte[]> exportIncomeStatementToPdf(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            byte[] pdfData = exportService.exportIncomeStatementToPdf(companyId, startDate, endDate);

            String filename = String.format("compte-resultat_%s_%s_%s.pdf",
                companyId,
                startDate.format(FILE_DATE_FORMATTER),
                endDate.format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfData.length);

            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/income-statement/excel")
    @Operation(summary = "Exporter le compte de résultat en Excel",
               description = "Génère et télécharge le compte de résultat au format Excel (.xlsx)")
    public ResponseEntity<byte[]> exportIncomeStatementToExcel(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            byte[] excelData = exportService.exportIncomeStatementToExcel(companyId, startDate, endDate);

            String filename = String.format("compte-resultat_%s_%s_%s.xlsx",
                companyId,
                startDate.format(FILE_DATE_FORMATTER),
                endDate.format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            headers.set("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/general-ledger/csv")
    @Operation(summary = "Exporter le grand livre en CSV",
               description = "Génère et télécharge le grand livre au format CSV")
    public ResponseEntity<byte[]> exportGeneralLedgerToCsv(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            byte[] csvData = exportService.exportGeneralLedgerToCsv(companyId, startDate, endDate);

            String filename = String.format("grand-livre_%s_%s_%s.csv",
                companyId,
                startDate.format(FILE_DATE_FORMATTER),
                endDate.format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(csvData.length);
            headers.set("Content-Type", "text/csv; charset=UTF-8");

            return new ResponseEntity<>(csvData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/general-ledger/excel")
    @Operation(summary = "Exporter le grand livre en Excel",
               description = "Génère et télécharge le grand livre au format Excel (.xlsx)")
    public ResponseEntity<byte[]> exportGeneralLedgerToExcel(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            byte[] excelData = exportService.exportGeneralLedgerToExcel(companyId, startDate, endDate);

            String filename = String.format("grand-livre_%s_%s_%s.xlsx",
                companyId,
                startDate.format(FILE_DATE_FORMATTER),
                endDate.format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            headers.set("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/bank-reconciliation/{reconciliationId}/pdf")
    @Operation(summary = "Exporter l'état de rapprochement en PDF",
               description = "Génère et télécharge l'état de rapprochement bancaire au format PDF conforme OHADA")
    public ResponseEntity<byte[]> exportBankReconciliationToPdf(
            @PathVariable Long companyId,
            @PathVariable Long reconciliationId) {

        try {
            com.predykt.accounting.domain.entity.BankReconciliation reconciliation =
                bankReconciliationService.getReconciliationById(reconciliationId);

            byte[] pdfData = exportService.exportBankReconciliationToPdf(reconciliation);

            String filename = String.format("rapprochement_%s_%s.pdf",
                reconciliation.getBankAccountNumber().replace(" ", "_"),
                reconciliation.getReconciliationDate().format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfData.length);

            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/bank-reconciliation/{reconciliationId}/excel")
    @Operation(summary = "Exporter l'état de rapprochement en Excel",
               description = "Génère et télécharge l'état de rapprochement bancaire au format Excel (.xlsx)")
    public ResponseEntity<byte[]> exportBankReconciliationToExcel(
            @PathVariable Long companyId,
            @PathVariable Long reconciliationId) {

        try {
            com.predykt.accounting.domain.entity.BankReconciliation reconciliation =
                bankReconciliationService.getReconciliationById(reconciliationId);

            byte[] excelData = exportService.exportBankReconciliationToExcel(reconciliation);

            String filename = String.format("rapprochement_%s_%s.xlsx",
                reconciliation.getBankAccountNumber().replace(" ", "_"),
                reconciliation.getReconciliationDate().format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            headers.set("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== BALANCE DE VÉRIFICATION ====================

    @GetMapping("/trial-balance/pdf")
    @Operation(summary = "Exporter la balance de vérification en PDF",
               description = "Génère et télécharge la balance de vérification au format PDF conforme OHADA")
    public ResponseEntity<byte[]> exportTrialBalanceToPdf(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            byte[] pdfData = exportService.exportTrialBalanceToPdf(companyId, startDate, endDate);

            String filename = String.format("balance-verification_%s_%s_%s.pdf",
                companyId,
                startDate.format(FILE_DATE_FORMATTER),
                endDate.format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfData.length);

            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/trial-balance/excel")
    @Operation(summary = "Exporter la balance de vérification en Excel",
               description = "Génère et télécharge la balance de vérification au format Excel (.xlsx)")
    public ResponseEntity<byte[]> exportTrialBalanceToExcel(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            byte[] excelData = exportService.exportTrialBalanceToExcel(companyId, startDate, endDate);

            String filename = String.format("balance-verification_%s_%s_%s.xlsx",
                companyId,
                startDate.format(FILE_DATE_FORMATTER),
                endDate.format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            headers.set("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/general-ledger/pdf")
    @Operation(summary = "Exporter le grand livre en PDF",
               description = "Génère et télécharge le grand livre complet au format PDF")
    public ResponseEntity<byte[]> exportGeneralLedgerToPdf(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            byte[] pdfData = exportService.exportGeneralLedgerToPdf(companyId, startDate, endDate);

            String filename = String.format("grand-livre_%s_%s_%s.pdf",
                companyId,
                startDate.format(FILE_DATE_FORMATTER),
                endDate.format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfData.length);

            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/ratios/excel")
    @Operation(summary = "Exporter l'historique des ratios financiers en Excel",
               description = "Génère et télécharge l'historique complet des ratios financiers au format Excel (.xlsx)")
    public ResponseEntity<byte[]> exportRatiosHistoryToExcel(@PathVariable Long companyId) {

        try {
            byte[] excelData = exportService.exportRatiosHistoryToExcel(companyId);

            String filename = String.format("historique-ratios_%s_%s.xlsx",
                companyId,
                LocalDate.now().format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            headers.set("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== TAFIRE (PRIORITÉ 2) ====================

    @GetMapping("/tafire/pdf")
    @Operation(summary = "Exporter le TAFIRE en PDF",
               description = "Génère et télécharge le Tableau Financier des Ressources et Emplois (TAFIRE) au format PDF - Conforme OHADA")
    public ResponseEntity<byte[]> exportTAFIREToPdf(
            @PathVariable Long companyId,
            @RequestParam Integer fiscalYear) {

        try {
            byte[] pdfData = exportService.exportTAFIREToPdf(companyId, fiscalYear);

            String filename = String.format("tafire_%s_%d.pdf", companyId, fiscalYear);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfData.length);

            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/tafire/excel")
    @Operation(summary = "Exporter le TAFIRE en Excel",
               description = "Génère et télécharge le Tableau Financier des Ressources et Emplois (TAFIRE) au format Excel (.xlsx)")
    public ResponseEntity<byte[]> exportTAFIREToExcel(
            @PathVariable Long companyId,
            @RequestParam Integer fiscalYear) {

        try {
            byte[] excelData = exportService.exportTAFIREToExcel(companyId, fiscalYear);

            String filename = String.format("tafire_%s_%d.xlsx", companyId, fiscalYear);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            headers.set("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== JOURNAUX AUXILIAIRES (PRIORITÉ 2) ====================

    @GetMapping("/journals/sales/pdf")
    @Operation(summary = "Exporter le journal des ventes (VE) en PDF",
               description = "Génère et télécharge le journal des ventes au format PDF - OHADA")
    public ResponseEntity<byte[]> exportSalesJournalToPdf(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            byte[] pdfData = exportService.exportSalesJournalToPdf(companyId, startDate, endDate);

            String filename = String.format("journal-ventes_%s_%s_%s.pdf",
                companyId,
                startDate.format(FILE_DATE_FORMATTER),
                endDate.format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfData.length);

            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/journals/sales/excel")
    @Operation(summary = "Exporter le journal des ventes (VE) en Excel",
               description = "Génère et télécharge le journal des ventes au format Excel (.xlsx)")
    public ResponseEntity<byte[]> exportSalesJournalToExcel(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            byte[] excelData = exportService.exportSalesJournalToExcel(companyId, startDate, endDate);

            String filename = String.format("journal-ventes_%s_%s_%s.xlsx",
                companyId,
                startDate.format(FILE_DATE_FORMATTER),
                endDate.format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            headers.set("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/journals/purchases/pdf")
    @Operation(summary = "Exporter le journal des achats (AC) en PDF",
               description = "Génère et télécharge le journal des achats au format PDF - OHADA")
    public ResponseEntity<byte[]> exportPurchasesJournalToPdf(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            byte[] pdfData = exportService.exportPurchasesJournalToPdf(companyId, startDate, endDate);

            String filename = String.format("journal-achats_%s_%s_%s.pdf",
                companyId,
                startDate.format(FILE_DATE_FORMATTER),
                endDate.format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfData.length);

            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/journals/purchases/excel")
    @Operation(summary = "Exporter le journal des achats (AC) en Excel",
               description = "Génère et télécharge le journal des achats au format Excel (.xlsx)")
    public ResponseEntity<byte[]> exportPurchasesJournalToExcel(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            byte[] excelData = exportService.exportPurchasesJournalToExcel(companyId, startDate, endDate);

            String filename = String.format("journal-achats_%s_%s_%s.xlsx",
                companyId,
                startDate.format(FILE_DATE_FORMATTER),
                endDate.format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            headers.set("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/journals/bank/pdf")
    @Operation(summary = "Exporter le journal de banque (BQ) en PDF",
               description = "Génère et télécharge le journal de banque au format PDF - OHADA")
    public ResponseEntity<byte[]> exportBankJournalToPdf(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            byte[] pdfData = exportService.exportBankJournalToPdf(companyId, startDate, endDate);

            String filename = String.format("journal-banque_%s_%s_%s.pdf",
                companyId,
                startDate.format(FILE_DATE_FORMATTER),
                endDate.format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfData.length);

            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/journals/bank/excel")
    @Operation(summary = "Exporter le journal de banque (BQ) en Excel",
               description = "Génère et télécharge le journal de banque au format Excel (.xlsx)")
    public ResponseEntity<byte[]> exportBankJournalToExcel(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            byte[] excelData = exportService.exportBankJournalToExcel(companyId, startDate, endDate);

            String filename = String.format("journal-banque_%s_%s_%s.xlsx",
                companyId,
                startDate.format(FILE_DATE_FORMATTER),
                endDate.format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            headers.set("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/journals/cash/pdf")
    @Operation(summary = "Exporter le journal de caisse (CA) en PDF",
               description = "Génère et télécharge le journal de caisse au format PDF - OHADA")
    public ResponseEntity<byte[]> exportCashJournalToPdf(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            byte[] pdfData = exportService.exportCashJournalToPdf(companyId, startDate, endDate);

            String filename = String.format("journal-caisse_%s_%s_%s.pdf",
                companyId,
                startDate.format(FILE_DATE_FORMATTER),
                endDate.format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfData.length);

            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/journals/cash/excel")
    @Operation(summary = "Exporter le journal de caisse (CA) en Excel",
               description = "Génère et télécharge le journal de caisse au format Excel (.xlsx)")
    public ResponseEntity<byte[]> exportCashJournalToExcel(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            byte[] excelData = exportService.exportCashJournalToExcel(companyId, startDate, endDate);

            String filename = String.format("journal-caisse_%s_%s_%s.xlsx",
                companyId,
                startDate.format(FILE_DATE_FORMATTER),
                endDate.format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            headers.set("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/journals/general/pdf")
    @Operation(summary = "Exporter le journal des opérations diverses (OD) en PDF",
               description = "Génère et télécharge le journal OD au format PDF - OHADA")
    public ResponseEntity<byte[]> exportGeneralJournalPdfToPdf(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            byte[] pdfData = exportService.exportGeneralJournalToPdf(companyId, startDate, endDate);

            String filename = String.format("journal-od_%s_%s_%s.pdf",
                companyId,
                startDate.format(FILE_DATE_FORMATTER),
                endDate.format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfData.length);

            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/journals/general/excel")
    @Operation(summary = "Exporter le journal des opérations diverses (OD) en Excel",
               description = "Génère et télécharge le journal OD au format Excel (.xlsx)")
    public ResponseEntity<byte[]> exportGeneralJournalExcelToExcel(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            byte[] excelData = exportService.exportGeneralJournalToExcel(companyId, startDate, endDate);

            String filename = String.format("journal-od_%s_%s_%s.xlsx",
                companyId,
                startDate.format(FILE_DATE_FORMATTER),
                endDate.format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            headers.set("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/journals/opening/pdf")
    @Operation(summary = "Exporter le journal à nouveaux (AN) en PDF",
               description = "Génère et télécharge le journal à nouveaux au format PDF - OHADA")
    public ResponseEntity<byte[]> exportOpeningJournalToPdf(
            @PathVariable Long companyId,
            @RequestParam Integer fiscalYear) {

        try {
            byte[] pdfData = exportService.exportOpeningJournalToPdf(companyId, fiscalYear);

            String filename = String.format("journal-an_%s_%d.pdf", companyId, fiscalYear);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfData.length);

            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/journals/opening/excel")
    @Operation(summary = "Exporter le journal à nouveaux (AN) en Excel",
               description = "Génère et télécharge le journal à nouveaux au format Excel (.xlsx)")
    public ResponseEntity<byte[]> exportOpeningJournalToExcel(
            @PathVariable Long companyId,
            @RequestParam Integer fiscalYear) {

        try {
            byte[] excelData = exportService.exportOpeningJournalToExcel(companyId, fiscalYear);

            String filename = String.format("journal-an_%s_%d.xlsx", companyId, fiscalYear);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            headers.set("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================================
    // EXPORTS NOTES ANNEXES
    // ============================================================

    @GetMapping("/notes-annexes/pdf")
    @Operation(summary = "Exporter les Notes Annexes en PDF",
               description = "Génère et télécharge les 12 notes annexes OHADA au format PDF")
    public ResponseEntity<byte[]> exportNotesAnnexesToPdf(
            @PathVariable Long companyId,
            @RequestParam Integer fiscalYear) {

        try {
            byte[] pdfData = exportService.exportNotesAnnexesToPdf(companyId, fiscalYear);

            String filename = String.format("notes-annexes_%s_%d.pdf", companyId, fiscalYear);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfData.length);

            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/notes-annexes/excel")
    @Operation(summary = "Exporter les Notes Annexes en Excel",
               description = "Génère et télécharge les 12 notes annexes OHADA au format Excel")
    public ResponseEntity<byte[]> exportNotesAnnexesToExcel(
            @PathVariable Long companyId,
            @RequestParam Integer fiscalYear) {

        try {
            byte[] excelData = exportService.exportNotesAnnexesToExcel(companyId, fiscalYear);

            String filename = String.format("notes-annexes_%s_%d.xlsx", companyId, fiscalYear);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            headers.set("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================================
    // EXPORTS GRANDS LIVRES AUXILIAIRES
    // ============================================================

    @GetMapping("/subledgers/customers/pdf")
    @Operation(summary = "Exporter le Grand Livre Auxiliaire Clients en PDF",
               description = "Génère et télécharge le grand livre auxiliaire clients au format PDF")
    public ResponseEntity<byte[]> exportCustomersSubledgerToPdf(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            byte[] pdfData = exportService.exportCustomersSubledgerToPdf(companyId, startDate, endDate);

            String filename = String.format("gl-auxiliaire-clients_%s_%s_%s.pdf",
                companyId, startDate.format(FILE_DATE_FORMATTER), endDate.format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfData.length);

            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/subledgers/customers/excel")
    @Operation(summary = "Exporter le Grand Livre Auxiliaire Clients en Excel",
               description = "Génère et télécharge le grand livre auxiliaire clients au format Excel")
    public ResponseEntity<byte[]> exportCustomersSubledgerToExcel(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            byte[] excelData = exportService.exportCustomersSubledgerToExcel(companyId, startDate, endDate);

            String filename = String.format("gl-auxiliaire-clients_%s_%s_%s.xlsx",
                companyId, startDate.format(FILE_DATE_FORMATTER), endDate.format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            headers.set("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/subledgers/suppliers/pdf")
    @Operation(summary = "Exporter le Grand Livre Auxiliaire Fournisseurs en PDF",
               description = "Génère et télécharge le grand livre auxiliaire fournisseurs au format PDF")
    public ResponseEntity<byte[]> exportSuppliersSubledgerToPdf(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            byte[] pdfData = exportService.exportSuppliersSubledgerToPdf(companyId, startDate, endDate);

            String filename = String.format("gl-auxiliaire-fournisseurs_%s_%s_%s.pdf",
                companyId, startDate.format(FILE_DATE_FORMATTER), endDate.format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfData.length);

            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/subledgers/suppliers/excel")
    @Operation(summary = "Exporter le Grand Livre Auxiliaire Fournisseurs en Excel",
               description = "Génère et télécharge le grand livre auxiliaire fournisseurs au format Excel")
    public ResponseEntity<byte[]> exportSuppliersSubledgerToExcel(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            byte[] excelData = exportService.exportSuppliersSubledgerToExcel(companyId, startDate, endDate);

            String filename = String.format("gl-auxiliaire-fournisseurs_%s_%s_%s.xlsx",
                companyId, startDate.format(FILE_DATE_FORMATTER), endDate.format(FILE_DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            headers.set("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
