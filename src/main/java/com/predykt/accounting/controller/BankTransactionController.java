package com.predykt.accounting.controller;

import com.predykt.accounting.domain.entity.BankTransaction;
import com.predykt.accounting.dto.request.TransactionImportRequest;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.TransactionResponse;
import com.predykt.accounting.mapper.TransactionMapper;
import com.predykt.accounting.service.BankTransactionService;
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
import java.util.List;

@RestController
@RequestMapping("/companies/{companyId}/bank-transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions Bancaires", description = "Gestion des transactions bancaires")
public class BankTransactionController {
    
    private final BankTransactionService transactionService;
    private final TransactionMapper transactionMapper;
    
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importer des transactions bancaires",
               description = "Importe des transactions depuis un fichier CSV ou OFX")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> importTransactions(
            @PathVariable Long companyId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String format) {
        
        List<BankTransaction> transactions = transactionService.importTransactions(
            companyId, file, format
        );
        
        List<TransactionResponse> responses = transactions.stream()
            .map(transactionMapper::toResponse)
            .toList();
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                responses,
                String.format("%d transactions importées", transactions.size())
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
}