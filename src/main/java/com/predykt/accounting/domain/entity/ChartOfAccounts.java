package com.predykt.accounting.domain.entity;

import com.predykt.accounting.domain.enums.AccountType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "chart_of_accounts", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "account_number"}),
       indexes = {
           @Index(name = "idx_coa_company_number", columnList = "company_id, account_number"),
           @Index(name = "idx_coa_type", columnList = "account_type")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChartOfAccounts extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    
    @NotBlank(message = "Le numéro de compte est obligatoire")
    @Pattern(regexp = "^[1-9]\\d{0,6}$", message = "Format numéro de compte invalide (OHADA: 1-9999999)")
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;
    
    @NotBlank(message = "Le libellé du compte est obligatoire")
    @Column(name = "account_name", nullable = false, length = 200)
    private String accountName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 50)
    private AccountType accountType;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_account_id")
    private ChartOfAccounts parentAccount;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "is_reconcilable")
    @Builder.Default
    private Boolean isReconcilable = false;  // Pour comptes bancaires/clients/fournisseurs
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "opening_balance", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal openingBalance = BigDecimal.ZERO;
    
    @Column(name = "account_level")
    private Integer accountLevel;  // 1 = Classe, 2 = Compte principal, 3+ = Sous-comptes
    
    /**
     * Calcule le niveau du compte basé sur le numéro
     */
    @PrePersist
    @PreUpdate
    private void calculateAccountLevel() {
        if (accountNumber != null) {
            this.accountLevel = accountNumber.length();
        }
    }
}