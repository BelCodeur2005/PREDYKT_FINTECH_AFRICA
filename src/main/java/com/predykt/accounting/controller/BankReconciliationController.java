package com.predykt.accounting.controller;

import com.predykt.accounting.domain.entity.*;
import com.predykt.accounting.domain.enums.SuggestionStatus;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.AutoMatchResultDTO;
import com.predykt.accounting.dto.response.MatchSuggestionDTO;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.repository.BankReconciliationSuggestionRepository;
import com.predykt.accounting.repository.BankTransactionRepository;
import com.predykt.accounting.repository.GeneralLedgerRepository;
import com.predykt.accounting.service.BankReconciliationService;
import com.predykt.accounting.service.BankReconciliationMatchingService;
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
    private final BankReconciliationMatchingService matchingService;
    private final BankReconciliationSuggestionRepository suggestionRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final GeneralLedgerRepository generalLedgerRepository;

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

    // ========== NOUVEAUX ENDPOINTS : MATCHING AUTOMATIQUE INTELLIGENT ==========

    @PostMapping("/{reconciliationId}/auto-match")
    @Operation(summary = "🤖 Matching automatique intelligent",
               description = "Lance l'analyse automatique pour identifier les correspondances entre " +
                   "les transactions bancaires et les écritures comptables. " +
                   "Retourne des suggestions avec score de confiance. " +
                   "Le comptable peut ensuite valider, rejeter ou modifier les suggestions.")
    public ResponseEntity<ApiResponse<AutoMatchResultDTO>> performAutoMatching(
            @PathVariable Long companyId,
            @PathVariable Long reconciliationId) {

        AutoMatchResultDTO result = matchingService.performAutoMatching(reconciliationId);

        String message = String.format(
            "✅ Analyse terminée : %d suggestions générées (confiance moyenne: %.1f%%)",
            result.getSuggestions().size(),
            result.getStatistics().getOverallConfidenceScore()
        );

        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    @PostMapping("/{reconciliationId}/suggestions/apply")
    @Operation(summary = "✓ Appliquer les suggestions sélectionnées",
               description = "Applique les suggestions de matching validées par le comptable. " +
                   "Les opérations en suspens correspondantes sont automatiquement créées. " +
                   "CORRIGÉ: Charge maintenant les vraies entités depuis la BDD.")
    public ResponseEntity<ApiResponse<BankReconciliation>> applySuggestions(
            @PathVariable Long companyId,
            @PathVariable Long reconciliationId,
            @RequestBody ApplySuggestionsRequest request) {

        BankReconciliation reconciliation = reconciliationService.getReconciliationById(reconciliationId);

        int appliedCount = 0;
        for (Long suggestionId : request.getSuggestionIds()) {
            // ✅ CORRECTION: Charger la suggestion depuis la BDD
            BankReconciliationSuggestion suggestion = suggestionRepository
                .findByIdAndReconciliation(suggestionId, reconciliation)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Suggestion non trouvée: " + suggestionId));

            // Vérifier que la suggestion est en attente
            if (suggestion.getStatus() != SuggestionStatus.PENDING) {
                continue; // Ignorer les suggestions déjà traitées
            }

            // Créer l'opération en suspens
            BankReconciliationItem item = BankReconciliationItem.builder()
                .itemType(suggestion.getSuggestedItemType())
                .transactionDate(suggestion.getTransactionDate())
                .amount(suggestion.getSuggestedAmount())
                .description(suggestion.getDescription())
                .thirdParty(suggestion.getThirdParty())
                .build();

            // ✅ CORRECTION: Charger les vraies entités depuis la BDD
            if (!suggestion.getBankTransactions().isEmpty()) {
                BankTransaction bt = suggestion.getBankTransactions().get(0);
                item.setBankTransaction(bt);
                item.setReference(bt.getBankReference());

                // Marquer la transaction comme réconciliée
                bt.setIsReconciled(true);
                bankTransactionRepository.save(bt);
            }

            if (!suggestion.getGlEntries().isEmpty()) {
                GeneralLedger gl = suggestion.getGlEntries().get(0);
                item.setGlEntry(gl);
                if (item.getReference() == null) {
                    item.setReference(gl.getReference());
                }

                // Lier l'écriture au rapprochement
                generaltr LedgerRepository.save(gl);
            }

            reconciliation = reconciliationService.addPendingItem(reconciliationId, item);

            // Marquer la suggestion comme appliquée
            suggestion.apply("SYSTEM"); // TODO: Récupérer l'utilisateur connecté
            suggestionRepository.save(suggestion);

            appliedCount++;
        }

        return ResponseEntity.ok(ApiResponse.success(
            reconciliation,
            String.format("✅ %d/%d suggestions appliquées avec succès",
                appliedCount, request.getSuggestionIds().size())
        ));
    }

    @PostMapping("/{reconciliationId}/suggestions/{suggestionId}/apply")
    @Operation(summary = "✓ Appliquer une suggestion spécifique",
               description = "Applique une seule suggestion de matching. " +
                   "Crée automatiquement l'opération en suspens correspondante. " +
                   "CORRIGÉ: Charge maintenant la suggestion depuis la BDD.")
    public ResponseEntity<ApiResponse<BankReconciliation>> applySingleSuggestion(
            @PathVariable Long companyId,
            @PathVariable Long reconciliationId,
            @PathVariable Long suggestionId) {

        BankReconciliation reconciliation = reconciliationService.getReconciliationById(reconciliationId);

        // ✅ CORRECTION: Charger la suggestion depuis la BDD
        BankReconciliationSuggestion suggestion = suggestionRepository
            .findByIdAndReconciliation(suggestionId, reconciliation)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Suggestion non trouvée: " + suggestionId));

        // Vérifier que la suggestion est en attente
        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new IllegalStateException("La suggestion a déjà été traitée: " + suggestion.getStatus());
        }

        // Créer l'opération en suspens
        BankReconciliationItem item = BankReconciliationItem.builder()
            .itemType(suggestion.getSuggestedItemType())
            .transactionDate(suggestion.getTransactionDate())
            .amount(suggestion.getSuggestedAmount())
            .description(suggestion.getDescription())
            .thirdParty(suggestion.getThirdParty())
            .build();

        // ✅ CORRECTION: Charger les vraies entités depuis la BDD
        if (!suggestion.getBankTransactions().isEmpty()) {
            BankTransaction bt = suggestion.getBankTransactions().get(0);
            item.setBankTransaction(bt);
            item.setReference(bt.getBankReference());

            bt.setIsReconciled(true);
            bankTransactionRepository.save(bt);
        }

        if (!suggestion.getGlEntries().isEmpty()) {
            GeneralLedger gl = suggestion.getGlEntries().get(0);
            item.setGlEntry(gl);
            if (item.getReference() == null) {
                item.setReference(gl.getReference());
            }
            generalLedgerRepository.save(gl);
        }

        BankReconciliation updated = reconciliationService.addPendingItem(reconciliationId, item);

        // Marquer la suggestion comme appliquée
        suggestion.apply("SYSTEM"); // TODO: Récupérer l'utilisateur connecté
        suggestionRepository.save(suggestion);

        return ResponseEntity.ok(ApiResponse.success(
            updated,
            String.format("✅ Suggestion appliquée : %s (confiance: %.0f%%)",
                suggestion.getSuggestedItemType().getDisplayName(),
                suggestion.getConfidenceScore())
        ));
    }

    @PostMapping("/{reconciliationId}/suggestions/{suggestionId}/reject")
    @Operation(summary = "✗ Rejeter une suggestion",
               description = "Rejette une suggestion de matching. " +
                   "Le comptable devra analyser et saisir manuellement si nécessaire. " +
                   "AMÉLIORÉ: Persiste maintenant le rejet avec traçabilité pour améliorer l'algorithme.")
    public ResponseEntity<ApiResponse<Void>> rejectSuggestion(
            @PathVariable Long companyId,
            @PathVariable Long reconciliationId,
            @PathVariable Long suggestionId,
            @RequestParam(required = false) String reason) {

        BankReconciliation reconciliation = reconciliationService.getReconciliationById(reconciliationId);

        // ✅ AMÉLIORATION: Charger et persister le rejet
        BankReconciliationSuggestion suggestion = suggestionRepository
            .findByIdAndReconciliation(suggestionId, reconciliation)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Suggestion non trouvée: " + suggestionId));

        // Vérifier que la suggestion est en attente
        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new IllegalStateException("La suggestion a déjà été traitée: " + suggestion.getStatus());
        }

        // Marquer comme rejetée avec la raison
        suggestion.reject("SYSTEM", reason); // TODO: Récupérer l'utilisateur connecté
        suggestionRepository.save(suggestion);

        log.info("📊 Suggestion #{} rejetée - Raison: {} - Type: {} - Confiance: {}%",
            suggestionId,
            reason != null ? reason : "Non spécifiée",
            suggestion.getSuggestedItemType(),
            suggestion.getConfidenceScore());

        String message = reason != null ?
            String.format("✗ Suggestion rejetée : %s", reason) :
            "✗ Suggestion rejetée par le comptable";

        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    // DTO pour la requête d'application de suggestions
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ApplySuggestionsRequest {
        private List<Long> suggestionIds; // IDs des suggestions à appliquer
    }
}
