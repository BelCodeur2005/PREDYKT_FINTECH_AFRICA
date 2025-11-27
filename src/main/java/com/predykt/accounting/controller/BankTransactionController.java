package com.predykt.accounting.controller;

import com.predykt.accounting.domain.entity.BankTransaction;
import com.predykt.accounting.domain.enums.BankProvider;
import com.predykt.accounting.domain.enums.BankStatementFormat;
import com.predykt.accounting.dto.request.TransactionImportRequest;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.TransactionResponse;
import com.predykt.accounting.mapper.TransactionMapper;
import com.predykt.accounting.service.BankTransactionService;
import com.predykt.accounting.service.parser.BankStatementParserFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/companies/{companyId}/bank-transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions Bancaires", description = "Gestion des transactions bancaires")
public class BankTransactionController {

    private final BankTransactionService transactionService;
    private final TransactionMapper transactionMapper;
    private final BankStatementParserFactory parserFactory;
    
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importer des transactions bancaires",
               description = "Importe des transactions depuis un fichier (OFX, MT940, CAMT.053, QIF, CSV). " +
                           "Le format est détecté automatiquement à partir de l'extension du fichier. " +
                           "Vous pouvez spécifier la banque émettrice pour améliorer la précision du parsing.")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> importTransactions(
            @PathVariable Long companyId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String bankProvider) {

        List<BankTransaction> transactions = transactionService.importTransactions(
            companyId, file, bankProvider
        );

        List<TransactionResponse> responses = transactions.stream()
            .map(transactionMapper::toResponse)
            .toList();

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                responses,
                String.format("%d transactions importées avec succès", transactions.size())
            ));
    }
    
    @GetMapping
    @Operation(summary = "Lister les transactions bancaires",
               description = "Récupère les transactions bancaires d'une entreprise sur une période")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactions(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<BankTransaction> transactions = transactionService.getTransactionsByDateRange(
            companyId, startDate, endDate
        );
        
        List<TransactionResponse> responses = transactions.stream()
            .map(transactionMapper::toResponse)
            .toList();
        
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    @GetMapping("/unreconciled")
    @Operation(summary = "Transactions non réconciliées",
               description = "Récupère les transactions bancaires non encore réconciliées avec le GL")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getUnreconciledTransactions(
            @PathVariable Long companyId) {
        
        List<BankTransaction> transactions = transactionService.getUnreconciledTransactions(companyId);
        
        List<TransactionResponse> responses = transactions.stream()
            .map(transactionMapper::toResponse)
            .toList();
        
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    @PostMapping("/{transactionId}/reconcile")
    @Operation(summary = "Réconcilier une transaction",
               description = "Associe une transaction bancaire à une écriture comptable")
    public ResponseEntity<ApiResponse<Void>> reconcileTransaction(
            @PathVariable Long companyId,
            @PathVariable Long transactionId,
            @RequestParam Long glEntryId) {

        transactionService.reconcileTransaction(companyId, transactionId, glEntryId);

        return ResponseEntity.ok(ApiResponse.success(
            null,
            "Transaction réconciliée avec succès"
        ));
    }

    @GetMapping("/supported-formats")
    @Operation(summary = "Lister les formats supportés",
               description = "Retourne la liste des formats de relevés bancaires supportés par banque")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSupportedFormats(
            @PathVariable Long companyId,
            @RequestParam(required = false) String bankProvider) {

        Map<String, Object> result = new HashMap<>();

        if (bankProvider != null && !bankProvider.isEmpty()) {
            // Formats pour une banque spécifique
            try {
                BankProvider provider = BankProvider.valueOf(bankProvider.toUpperCase());
                List<BankStatementFormat> formats = parserFactory.getSupportedFormats(provider);

                result.put("bank", provider.getDisplayName());
                result.put("zone", provider.getZone());
                result.put("formats", formats.stream()
                    .map(f -> Map.of(
                        "name", f.name(),
                        "displayName", f.getDisplayName(),
                        "extensions", f.getFileExtensions()
                    ))
                    .collect(Collectors.toList()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Banque inconnue: " + bankProvider, "UNKNOWN_BANK"));
            }
        } else {
            // Tous les formats et banques
            Map<String, List<String>> bankFormats = new HashMap<>();
            for (BankProvider provider : BankProvider.values()) {
                List<BankStatementFormat> formats = parserFactory.getSupportedFormats(provider);
                bankFormats.put(
                    provider.name(),
                    formats.stream().map(BankStatementFormat::getDisplayName).collect(Collectors.toList())
                );
            }
            result.put("banks", bankFormats);

            // Liste des formats standards
            result.put("standardFormats", Arrays.asList(
                Map.of("name", "OFX", "description", "Open Financial Exchange (XML)", "extensions", ".ofx, .qfx"),
                Map.of("name", "MT940", "description", "SWIFT MT940 (Texte)", "extensions", ".mt940, .sta, .txt"),
                Map.of("name", "CAMT.053", "description", "ISO 20022 (XML)", "extensions", ".xml, .camt"),
                Map.of("name", "QIF", "description", "Quicken Interchange Format (Texte)", "extensions", ".qif"),
                Map.of("name", "CSV", "description", "CSV générique ou spécifique banque", "extensions", ".csv")
            ));
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}