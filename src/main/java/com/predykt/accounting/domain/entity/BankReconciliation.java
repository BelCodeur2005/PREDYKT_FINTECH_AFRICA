package com.predykt.accounting.domain.entity;

import com.predykt.accounting.domain.enums.ReconciliationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * État de rapprochement bancaire conforme OHADA
 * Réconcilie le solde du relevé bancaire avec le solde comptable (compte 52X)
 */
@Entity
@Table(name = "bank_reconciliations", indexes = {
    @Index(name = "idx_recon_company_date", columnList = "company_id, reconciliation_date"),
    @Index(name = "idx_recon_status", columnList = "status"),
    @Index(name = "idx_recon_account", columnList = "bank_account_number")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_recon_company_account_date",
                      columnNames = {"company_id", "bank_account_number", "reconciliation_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankReconciliation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "L'entreprise est obligatoire")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @NotNull(message = "La date de rapprochement est obligatoire")
    @Column(name = "reconciliation_date", nullable = false)
    private LocalDate reconciliationDate;

    @NotNull(message = "La période est obligatoire")
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @NotNull(message = "La période est obligatoire")
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    // Compte bancaire (OHADA: compte 52X)
    @NotNull(message = "Le numéro de compte bancaire est obligatoire")
    @Column(name = "bank_account_number", nullable = false, length = 50)
    private String bankAccountNumber;

    @Column(name = "bank_name", length = 200)
    private String bankName;

    // === SECTION A: SOLDE SELON RELEVÉ BANCAIRE ===
    @NotNull(message = "Le solde bancaire est obligatoire")
    @Column(name = "bank_statement_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal bankStatementBalance;

    @Column(name = "statement_reference", length = 100)
    private String statementReference;

    // === SECTION B: AJUSTEMENTS AU SOLDE BANCAIRE ===
    @Column(name = "cheques_issued_not_cashed", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal chequesIssuedNotCashed = BigDecimal.ZERO;

    @Column(name = "deposits_in_transit", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal depositsInTransit = BigDecimal.ZERO;

    @Column(name = "bank_errors", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal bankErrors = BigDecimal.ZERO;

    // Solde bancaire rectifié = solde relevé + ajustements
    @Column(name = "adjusted_bank_balance", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal adjustedBankBalance = BigDecimal.ZERO;

    // === SECTION C: SOLDE SELON LIVRE (COMPTABILITÉ) ===
    @NotNull(message = "Le solde comptable est obligatoire")
    @Column(name = "book_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal bookBalance;

    @Column(name = "gl_account_number", length = 20)
    private String glAccountNumber; // Compte 521, 522, etc.

    // === SECTION D: AJUSTEMENTS AU SOLDE LIVRE ===
    @Column(name = "credits_not_recorded", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal creditsNotRecorded = BigDecimal.ZERO;

    @Column(name = "debits_not_recorded", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal debitsNotRecorded = BigDecimal.ZERO;

    @Column(name = "bank_fees_not_recorded", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal bankFeesNotRecorded = BigDecimal.ZERO;

    @Column(name = "book_errors", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal bookErrors = BigDecimal.ZERO;

    // Solde livre rectifié = solde comptable + ajustements
    @Column(name = "adjusted_book_balance", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal adjustedBookBalance = BigDecimal.ZERO;

    // === ÉCART ===
    // Écart = Solde banque rectifié - Solde livre rectifié
    // DOIT ÊTRE = 0 pour que le rapprochement soit correct
    @Column(name = "difference", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal difference = BigDecimal.ZERO;

    @Column(name = "is_balanced")
    @Builder.Default
    private Boolean isBalanced = false;

    // === OPÉRATIONS EN SUSPENS (détail) ===
    @OneToMany(mappedBy = "bankReconciliation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BankReconciliationItem> pendingItems = new ArrayList<>();

    // === WORKFLOW ET VALIDATION ===
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ReconciliationStatus status = ReconciliationStatus.DRAFT;

    @Column(name = "prepared_by", length = 100)
    private String preparedBy;

    @Column(name = "prepared_at")
    private LocalDateTime preparedAt;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Calcule le solde bancaire rectifié
     */
    public void calculateAdjustedBankBalance() {
        this.adjustedBankBalance = bankStatementBalance
            .add(chequesIssuedNotCashed)
            .subtract(depositsInTransit)
            .add(bankErrors);
    }

    /**
     * Calcule le solde livre rectifié
     */
    public void calculateAdjustedBookBalance() {
        this.adjustedBookBalance = bookBalance
            .add(creditsNotRecorded)
            .subtract(debitsNotRecorded)
            .subtract(bankFeesNotRecorded)
            .add(bookErrors);
    }

    /**
     * Calcule l'écart et vérifie si le rapprochement est équilibré
     */
    public void calculateDifference() {
        calculateAdjustedBankBalance();
        calculateAdjustedBookBalance();

        this.difference = adjustedBankBalance.subtract(adjustedBookBalance);
        this.isBalanced = difference.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Ajoute un élément en suspens et recalcule les totaux
     */
    public void addPendingItem(BankReconciliationItem item) {
        pendingItems.add(item);
        item.setBankReconciliation(this);
        recalculateTotalsFromItems();
    }

    /**
     * Recalcule les totaux à partir des opérations en suspens
     */
    public void recalculateTotalsFromItems() {
        // Réinitialiser les totaux
        chequesIssuedNotCashed = BigDecimal.ZERO;
        depositsInTransit = BigDecimal.ZERO;
        bankErrors = BigDecimal.ZERO;
        creditsNotRecorded = BigDecimal.ZERO;
        debitsNotRecorded = BigDecimal.ZERO;
        bankFeesNotRecorded = BigDecimal.ZERO;
        bookErrors = BigDecimal.ZERO;

        // Recalculer depuis les items
        for (BankReconciliationItem item : pendingItems) {
            switch (item.getItemType()) {
                case CHEQUE_ISSUED_NOT_CASHED:
                    chequesIssuedNotCashed = chequesIssuedNotCashed.add(item.getAmount());
                    break;
                case DEPOSIT_IN_TRANSIT:
                    depositsInTransit = depositsInTransit.add(item.getAmount());
                    break;
                case BANK_ERROR:
                    bankErrors = bankErrors.add(item.getAmount());
                    break;
                case CREDIT_NOT_RECORDED:
                    creditsNotRecorded = creditsNotRecorded.add(item.getAmount());
                    break;
                case DEBIT_NOT_RECORDED:
                    debitsNotRecorded = debitsNotRecorded.add(item.getAmount());
                    break;
                case BANK_FEES_NOT_RECORDED:
                    bankFeesNotRecorded = bankFeesNotRecorded.add(item.getAmount());
                    break;
                case INTEREST_NOT_RECORDED:
                case DIRECT_DEBIT_NOT_RECORDED:
                case BANK_CHARGES_NOT_RECORDED:
                    // Ces types sont ajoutés aux debits non enregistrés
                    debitsNotRecorded = debitsNotRecorded.add(item.getAmount());
                    break;
            }
        }

        calculateDifference();
    }

    /**
     * Valide que le rapprochement peut être soumis pour révision
     */
    public boolean canSubmitForReview() {
        return status == ReconciliationStatus.DRAFT && isBalanced;
    }

    /**
     * Soumet le rapprochement pour révision
     */
    public void submitForReview(String preparedBy) {
        if (!canSubmitForReview()) {
            throw new IllegalStateException("Le rapprochement doit être équilibré avant soumission");
        }
        this.status = ReconciliationStatus.PENDING_REVIEW;
        this.preparedBy = preparedBy;
        this.preparedAt = LocalDateTime.now();
    }

    /**
     * Approuve le rapprochement
     */
    public void approve(String approvedBy) {
        if (status != ReconciliationStatus.REVIEWED && status != ReconciliationStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Seul un rapprochement révisé peut être approuvé");
        }
        this.status = ReconciliationStatus.APPROVED;
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * Rejette le rapprochement
     */
    public void reject(String rejectedBy, String reason) {
        if (status == ReconciliationStatus.APPROVED || status == ReconciliationStatus.ARCHIVED) {
            throw new IllegalStateException("Un rapprochement approuvé ou archivé ne peut pas être rejeté");
        }
        this.status = ReconciliationStatus.REJECTED;
        this.rejectionReason = reason;
        this.reviewedBy = rejectedBy;
        this.reviewedAt = LocalDateTime.now();
    }
}
