package com.predykt.accounting.domain.entity;

import com.predykt.accounting.domain.enums.TransactionCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "bank_transactions", indexes = {
    @Index(name = "idx_bank_company_date", columnList = "company_id, transaction_date"),
    @Index(name = "idx_bank_reconciled", columnList = "is_reconciled"),
    @Index(name = "idx_bank_category", columnList = "category")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTransaction extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @NotNull
    private Company company;
    
    @Column(name = "transaction_date", nullable = false)
    @NotNull(message = "La date de transaction est obligatoire")
    private LocalDate transactionDate;
    
    @Column(name = "value_date")
    private LocalDate valueDate;  // Date de valeur (différente de la date transaction)
    
    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    @NotNull(message = "Le montant est obligatoire")
    private BigDecimal amount;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "bank_reference", length = 100)
    private String bankReference;  // Référence banque unique
    
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private TransactionCategory category;  // Catégorie IA (Python assignera)
    
    @Column(name = "category_confidence", precision = 5, scale = 2)
    private BigDecimal categoryConfidence;  // Score de confiance IA (0-100)
    
    @Column(name = "is_reconciled", nullable = false)
    @Builder.Default
    private Boolean isReconciled = false;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_entry_id")
    private GeneralLedger glEntry;  // Écriture comptable associée après réconciliation
    
    @Column(name = "third_party_name", length = 200)
    private String thirdPartyName;  // Nom du client/fournisseur extrait de la description
    
    @Column(name = "imported_at")
    private LocalDate importedAt;
    
    @Column(name = "import_source", length = 50)
    private String importSource;  // CSV, API, OFX, MT940
}