package com.predykt.accounting.controller;

import com.predykt.accounting.domain.entity.BankReconciliation;
import com.predykt.accounting.domain.entity.BankReconciliationItem;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.service.BankReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/companies/{companyId}/bank-reconciliations")
@RequiredArgsConstructor
@Tag(name = "Rapprochements Bancaires", description = "Gestion des rapprochements bancaires OHADA")
public class BankReconciliationController {

    private final BankReconciliationService reconciliationService;

    @PostMapping
    @Operation(summary = "Créer un rapprochement bancaire",
               description = "Crée un nouvel état de rapprochement bancaire conforme OHADA")
    public ResponseEntity<ApiResponse<BankReconciliation>> createReconciliation(
            @PathVariable Long companyId,
            @RequestBody BankReconciliation reconciliation) {

        // Associer l'entreprise
        reconciliation.getCompany().setId(companyId);

        BankReconciliation created = reconciliationService.createReconciliation(reconciliation);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(created, "Rapprochement bancaire créé avec succès"));
    }

    @GetMapping
    @Operation(summary = "Lister les rapprochements",
               description = "Récupère tous les rapprochements bancaires d'une entreprise")
    public ResponseEntity<ApiResponse<List<BankReconciliation>>> getReconciliations(
            @PathVariable Long companyId) {

        List<BankReconciliation> reconciliations = reconciliationService.getCompanyReconciliations(companyId);
        return ResponseEntity.ok(ApiResponse.success(reconciliations));
    }

    @GetMapping("/{reconciliationId}")
    @Operation(summary = "Récupérer un rapprochement",
               description = "Récupère un rapprochement bancaire par son ID")
    public ResponseEntity<ApiResponse<BankReconciliation>> getReconciliation(
            @PathVariable Long companyId,
            @PathVariable Long reconciliationId) {

        BankReconciliation reconciliation = reconciliationService.getReconciliationById(reconciliationId);
        return ResponseEntity.ok(ApiResponse.success(reconciliation));
    }

    @PutMapping("/{reconciliationId}")
    @Operation(summary = "Mettre à jour un rapprochement",
               description = "Met à jour un rapprochement bancaire existant")
    public ResponseEntity<ApiResponse<BankReconciliation>> updateReconciliation(
            @PathVariable Long companyId,
            @PathVariable Long reconciliationId,
            @RequestBody BankReconciliation reconciliation) {

        BankReconciliation updated = reconciliationService.updateReconciliation(
            reconciliationId, reconciliation);
        return ResponseEntity.ok(ApiResponse.success(updated, "Rapprochement mis à jour"));
    }

    @DeleteMapping("/{reconciliationId}")
    @Operation(summary = "Supprimer un rapprochement",
               description = "Supprime un rapprochement (uniquement si brouillon ou rejeté)")
    public ResponseEntity<ApiResponse<Void>> deleteReconciliation(
            @PathVariable Long companyId,
            @PathVariable Long reconciliationId) {

        reconciliationService.deleteReconciliation(reconciliationId);
        return ResponseEntity.ok(ApiResponse.success(null, "Rapprochement supprimé"));
    }

    @GetMapping("/by-account/{bankAccountNumber}")
    @Operation(summary = "Rapprochements par compte",
               description = "Récupère les rapprochements pour un compte bancaire spécifique")
    public ResponseEntity<ApiResponse<List<BankReconciliation>>> getReconciliationsByAccount(
            @PathVariable Long companyId,
            @PathVariable String bankAccountNumber) {

        List<BankReconciliation> reconciliations = reconciliationService.getReconciliationsByBankAccount(
            companyId, bankAccountNumber);
        return ResponseEntity.ok(ApiResponse.success(reconciliations));
    }

    @GetMapping("/unbalanced")
    @Operation(summary = "Rapprochements non équilibrés",
               description = "Récupère les rapprochements avec écart non nul")
    public ResponseEntity<ApiResponse<List<BankReconciliation>>> getUnbalancedReconciliations(
            @PathVariable Long companyId) {

        List<BankReconciliation> reconciliations = reconciliationService.getUnbalancedReconciliations(companyId);
        return ResponseEntity.ok(ApiResponse.success(reconciliations));
    }

    @GetMapping("/pending")
    @Operation(summary = "Rapprochements en attente",
               description = "Récupère les rapprochements en attente de validation")
    public ResponseEntity<ApiResponse<List<BankReconciliation>>> getPendingReconciliations(
            @PathVariable Long companyId) {

        List<BankReconciliation> reconciliations = reconciliationService.getPendingReconciliations(companyId);
        return ResponseEntity.ok(ApiResponse.success(reconciliations));
    }

    @PostMapping("/{reconciliationId}/items")
    @Operation(summary = "Ajouter une opération en suspens",
               description = "Ajoute une opération en suspens au rapprochement")
    public ResponseEntity<ApiResponse<BankReconciliation>> addPendingItem(
            @PathVariable Long companyId,
            @PathVariable Long reconciliationId,
            @RequestBody BankReconciliationItem item) {

        BankReconciliation updated = reconciliationService.addPendingItem(reconciliationId, item);
        return ResponseEntity.ok(ApiResponse.success(updated, "Opération ajoutée"));
    }

    @DeleteMapping("/{reconciliationId}/items/{itemId}")
    @Operation(summary = "Supprimer une opération en suspens",
               description = "Supprime une opération en suspens du rapprochement")
    public ResponseEntity<ApiResponse<BankReconciliation>> removePendingItem(
            @PathVariable Long companyId,
            @PathVariable Long reconciliationId,
            @PathVariable Long itemId) {

        BankReconciliation updated = reconciliationService.removePendingItem(reconciliationId, itemId);
        return ResponseEntity.ok(ApiResponse.success(updated, "Opération supprimée"));
    }

    @PostMapping("/{reconciliationId}/submit")
    @Operation(summary = "Soumettre pour révision",
               description = "Soumet le rapprochement pour révision et validation")
    public ResponseEntity<ApiResponse<BankReconciliation>> submitForReview(
            @PathVariable Long companyId,
            @PathVariable Long reconciliationId,
            @RequestParam String preparedBy) {

        BankReconciliation updated = reconciliationService.submitForReview(reconciliationId, preparedBy);
        return ResponseEntity.ok(ApiResponse.success(updated, "Rapprochement soumis pour révision"));
    }

    @PostMapping("/{reconciliationId}/approve")
    @Operation(summary = "Approuver le rapprochement",
               description = "Approuve le rapprochement bancaire")
    public ResponseEntity<ApiResponse<BankReconciliation>> approveReconciliation(
            @PathVariable Long companyId,
            @PathVariable Long reconciliationId,
            @RequestParam String approvedBy) {

        BankReconciliation updated = reconciliationService.approveReconciliation(reconciliationId, approvedBy);
        return ResponseEntity.ok(ApiResponse.success(updated, "Rapprochement approuvé"));
    }

    @PostMapping("/{reconciliationId}/reject")
    @Operation(summary = "Rejeter le rapprochement",
               description = "Rejette le rapprochement avec une raison")
    public ResponseEntity<ApiResponse<BankReconciliation>> rejectReconciliation(
            @PathVariable Long companyId,
            @PathVariable Long reconciliationId,
            @RequestParam String rejectedBy,
            @RequestParam String reason) {

        BankReconciliation updated = reconciliationService.rejectReconciliation(
            reconciliationId, rejectedBy, reason);
        return ResponseEntity.ok(ApiResponse.success(updated, "Rapprochement rejeté"));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Statistiques de rapprochement",
               description = "Génère des statistiques sur les rapprochements bancaires")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Map<String, Object> stats = reconciliationService.getReconciliationStatistics(
            companyId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
