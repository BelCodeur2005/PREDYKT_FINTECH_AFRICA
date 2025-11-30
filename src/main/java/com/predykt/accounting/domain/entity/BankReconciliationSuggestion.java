package com.predykt.accounting.domain.entity;

import com.predykt.accounting.domain.enums.PendingItemType;
import com.predykt.accounting.domain.enums.SuggestionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Suggestion de matching automatique persistée en base de données
 * Permet de stocker les résultats de l'algorithme de matching
 * et de tracer les décisions du comptable (acceptation/rejet)
 */
@Entity
@Table(name = "bank_reconciliation_suggestions", indexes = {
    @Index(name = "idx_suggestion_reconciliation", columnList = "reconciliation_id"),
    @Index(name = "idx_suggestion_status", columnList = "status"),
    @Index(name = "idx_suggestion_confidence", columnList = "confidence_score")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankReconciliationSuggestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_id", nullable = false)
    private BankReconciliation reconciliation;

    /**
     * Type d'opération en suspens suggérée
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "suggested_item_type", nullable = false, length = 50)
    private PendingItemType suggestedItemType;

    /**
     * Score de confiance (0-100)
     */
    @NotNull
    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    /**
     * Niveau de confiance (EXCELLENT, GOOD, FAIR, LOW)
     */
    @Column(name = "confidence_level", length = 20)
    private String confidenceLevel;

    /**
     * Statut de la suggestion
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SuggestionStatus status = SuggestionStatus.PENDING;

    /**
     * Support du matching multiple (N-à-1 ou 1-à-N)
     * Une suggestion peut concerner plusieurs transactions bancaires
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "suggestion_bank_transactions",
        joinColumns = @JoinColumn(name = "suggestion_id"),
        inverseJoinColumns = @JoinColumn(name = "bank_transaction_id")
    )
    @Builder.Default
    private List<BankTransaction> bankTransactions = new ArrayList<>();

    /**
     * Support du matching multiple (N-à-1 ou 1-à-N)
     * Une suggestion peut concerner plusieurs écritures comptables
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "suggestion_gl_entries",
        joinColumns = @JoinColumn(name = "suggestion_id"),
        inverseJoinColumns = @JoinColumn(name = "gl_entry_id")
    )
    @Builder.Default
    private List<GeneralLedger> glEntries = new ArrayList<>();

    /**
     * Montant total suggéré (somme si matching multiple)
     */
    @NotNull
    @Column(name = "suggested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal suggestedAmount;

    /**
     * Description de la suggestion
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Tiers concerné
     */
    @Column(name = "third_party", length = 255)
    private String thirdParty;

    /**
     * Date de la transaction
     */
    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    /**
     * Raison du matching
     */
    @Column(name = "matching_reason", columnDefinition = "TEXT")
    private String matchingReason;

    /**
     * Indique si révision manuelle nécessaire
     */
    @Column(name = "requires_manual_review")
    @Builder.Default
    private boolean requiresManualReview = false;

    /**
     * Raison du rejet (si status = REJECTED)
     */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /**
     * Utilisateur qui a traité la suggestion
     */
    @Column(name = "processed_by", length = 100)
    private String processedBy;

    /**
     * Type de matching (SINGLE, MANY_TO_ONE, ONE_TO_MANY)
     */
    @Column(name = "match_type", length = 20)
    @Builder.Default
    private String matchType = "SINGLE";

    /**
     * Métadonnées additionnelles (JSON)
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    // === Méthodes utilitaires ===

    /**
     * Applique la suggestion et change son statut
     */
    public void apply(String appliedBy) {
        if (this.status != SuggestionStatus.PENDING) {
            throw new IllegalStateException("Seules les suggestions en attente peuvent être appliquées");
        }
        this.status = SuggestionStatus.APPLIED;
        this.processedBy = appliedBy;
    }

    /**
     * Rejette la suggestion avec une raison
     */
    public void reject(String rejectedBy, String reason) {
        if (this.status != SuggestionStatus.PENDING) {
            throw new IllegalStateException("Seules les suggestions en attente peuvent être rejetées");
        }
        this.status = SuggestionStatus.REJECTED;
        this.processedBy = rejectedBy;
        this.rejectionReason = reason;
    }

    /**
     * Ajoute une transaction bancaire au matching
     */
    public void addBankTransaction(BankTransaction transaction) {
        this.bankTransactions.add(transaction);
        updateMatchType();
    }

    /**
     * Ajoute une écriture GL au matching
     */
    public void addGlEntry(GeneralLedger entry) {
        this.glEntries.add(entry);
        updateMatchType();
    }

    /**
     * Met à jour automatiquement le type de matching
     */
    private void updateMatchType() {
        int btCount = bankTransactions.size();
        int glCount = glEntries.size();

        if (btCount == 1 && glCount == 1) {
            this.matchType = "SINGLE";
        } else if (btCount > 1 && glCount == 1) {
            this.matchType = "MANY_TO_ONE";
        } else if (btCount == 1 && glCount > 1) {
            this.matchType = "ONE_TO_MANY";
        } else if (btCount > 1 && glCount > 1) {
            this.matchType = "MANY_TO_MANY";
        }
    }

    /**
     * Calcule le montant total des transactions bancaires
     */
    public BigDecimal getTotalBankTransactionAmount() {
        return bankTransactions.stream()
            .map(BankTransaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcule le montant total des écritures GL
     */
    public BigDecimal getTotalGlEntryAmount() {
        return glEntries.stream()
            .map(gl -> gl.getDebitAmount().subtract(gl.getCreditAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
