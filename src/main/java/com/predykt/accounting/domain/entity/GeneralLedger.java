package com.predykt.accounting.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "general_ledger", indexes = {
    @Index(name = "idx_gl_company_date", columnList = "company_id, entry_date"),
    @Index(name = "idx_gl_account", columnList = "account_id"),
    @Index(name = "idx_gl_reference", columnList = "reference"),
    @Index(name = "idx_gl_locked", columnList = "is_locked")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneralLedger extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @NotNull(message = "L'entreprise est obligatoire")
    private Company company;
    
    @Column(name = "entry_date", nullable = false)
    @NotNull(message = "La date d'écriture est obligatoire")
    private LocalDate entryDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @NotNull(message = "Le compte est obligatoire")
    private ChartOfAccounts account;
    
    @Column(name = "debit_amount", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal debitAmount = BigDecimal.ZERO;
    
    @Column(name = "credit_amount", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal creditAmount = BigDecimal.ZERO;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(length = 100)
    private String reference;  // N° facture, bordereau, etc.
    
    @Column(name = "journal_code", length = 10)
    private String journalCode;  // VE (Vente), AC (Achat), BQ (Banque), OD (Opérations Diverses)
    
    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private Boolean isLocked = false;  // Immuabilité après clôture
    
    @Column(name = "period", length = 7)
    private String period;  // Format YYYY-MM pour reporting
    
    @Column(name = "fiscal_year", length = 4)
    private String fiscalYear;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_transaction_id")
    private BankTransaction bankTransaction;  // Lien avec transaction bancaire si réconciliée

    /**
     * PLAN TIERS - Références optionnelles vers les entités Customer/Supplier
     * Si NULL, le système utilise la description pour identifier le tiers
     * IMPORTANT: Ces champs sont OPTIONNELS pour rétrocompatibilité
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;  // Optionnel - Référence vers le client (pour comptes 411xxx)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;  // Optionnel - Référence vers le fournisseur (pour comptes 401xxx)

    /**
     * Retourne le nom du tiers (client ou fournisseur) associé à cette écriture
     * Ordre de priorité:
     * 1. Nom du customer si présent
     * 2. Nom du supplier si présent
     * 3. Description de l'écriture (fallback)
     * 4. Numéro de compte (dernier recours)
     */
    public String getTiersName() {
        if (customer != null) {
            return customer.getName();
        }
        if (supplier != null) {
            return supplier.getName();
        }
        if (description != null && !description.trim().isEmpty()) {
            return description;
        }
        return account != null ? account.getAccountNumber() : "N/A";
    }

    /**
     * Validation métier : une écriture doit avoir soit un débit, soit un crédit
     */
    @PrePersist
    @PreUpdate
    private void validate() {
        if (debitAmount.compareTo(BigDecimal.ZERO) == 0 && 
            creditAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException("Une écriture doit avoir un montant au débit ou au crédit");
        }
        
        if (debitAmount.compareTo(BigDecimal.ZERO) > 0 && 
            creditAmount.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Une écriture ne peut avoir à la fois un débit et un crédit");
        }
        
        // Calculer période et exercice
        if (entryDate != null) {
            this.period = entryDate.getYear() + "-" + String.format("%02d", entryDate.getMonthValue());
            this.fiscalYear = String.valueOf(entryDate.getYear());
        }
    }
}