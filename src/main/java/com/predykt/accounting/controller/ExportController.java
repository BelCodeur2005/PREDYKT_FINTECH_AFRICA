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
}
