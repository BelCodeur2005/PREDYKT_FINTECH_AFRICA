package com.predykt.accounting.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Entité représentant un budget prévisionnel
 * Permet de suivre les écarts entre budget et réel
 */
@Entity
@Table(name = "budgets", indexes = {
    @Index(name = "idx_budget_company_year", columnList = "company_id, fiscal_year"),
    @Index(name = "idx_budget_account", columnList = "account_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_budget_period",
                      columnNames = {"company_id", "account_id", "period_start", "period_end"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "L'entreprise est obligatoire")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @NotBlank(message = "L'année fiscale est obligatoire")
    @Column(name = "fiscal_year", nullable = false, length = 4)
    private String fiscalYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private ChartOfAccounts account;

    @NotNull(message = "Le type de budget est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(name = "budget_type", nullable = false, length = 20)
    private BudgetType budgetType;

    @NotNull(message = "La date de début est obligatoire")
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @NotNull(message = "La date de fin est obligatoire")
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @NotNull(message = "Le montant budgété est obligatoire")
    @Column(name = "budgeted_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal budgetedAmount;

    @Column(name = "actual_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal actualAmount = BigDecimal.ZERO;

    @Column(name = "variance", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal variance = BigDecimal.ZERO;

    @Column(name = "variance_pct", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal variancePct = BigDecimal.ZERO;

    @Column(name = "is_locked")
    @Builder.Default
    private Boolean isLocked = false;

    /**
     * Type de budget
     */
    public enum BudgetType {
        ANNUAL,     // Budget annuel
        QUARTERLY,  // Budget trimestriel
        MONTHLY     // Budget mensuel
    }

    /**
     * Calcule et met à jour la variance entre budget et réel
     */
    public void calculateVariance() {
        if (budgetedAmount != null && actualAmount != null) {
            // Variance = Réel - Budget (positif si dépassement)
            this.variance = actualAmount.subtract(budgetedAmount);

            // Variance % = (Réel - Budget) / Budget * 100
            if (budgetedAmount.compareTo(BigDecimal.ZERO) != 0) {
                this.variancePct = variance
                    .divide(budgetedAmount.abs(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            } else {
                this.variancePct = BigDecimal.ZERO;
            }
        }
    }

    /**
     * Met à jour le montant réel et recalcule la variance
     */
    public void updateActualAmount(BigDecimal newActualAmount) {
        this.actualAmount = newActualAmount;
        calculateVariance();
    }

    /**
     * Vérifie si le budget est dépassé
     */
    public boolean isOverBudget() {
        return variance != null && variance.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Vérifie si le budget est sous-utilisé
     */
    public boolean isUnderBudget() {
        return variance != null && variance.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Calcule le taux d'exécution du budget
     */
    public BigDecimal getExecutionRate() {
        if (budgetedAmount == null || budgetedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return actualAmount
            .divide(budgetedAmount, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Verrouille le budget pour empêcher les modifications
     */
    public void lock() {
        this.isLocked = true;
    }

    /**
     * Déverrouille le budget
     */
    public void unlock() {
        this.isLocked = false;
    }
}
