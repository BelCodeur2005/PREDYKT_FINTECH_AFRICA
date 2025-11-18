// ============================================
// FinancialRatio.java
// ============================================
package com.predykt.accounting.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_ratios", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "fiscal_year", "period_end"}),
       indexes = {
           @Index(name = "idx_ratios_company_year", columnList = "company_id, fiscal_year"),
           @Index(name = "idx_ratios_period", columnList = "period_start, period_end")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialRatio extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    
    @Column(name = "fiscal_year", nullable = false, length = 4)
    private String fiscalYear;
    
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;
    
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;
    
    // === RENTABILITÉ ===
    @Column(name = "gross_margin_pct", precision = 5, scale = 2)
    private BigDecimal grossMarginPct;
    
    @Column(name = "net_margin_pct", precision = 5, scale = 2)
    private BigDecimal netMarginPct;
    
    @Column(name = "roa_pct", precision = 5, scale = 2)
    private BigDecimal roaPct;
    
    @Column(name = "roe_pct", precision = 5, scale = 2)
    private BigDecimal roePct;
    
    // === LIQUIDITÉ ===
    @Column(name = "current_ratio", precision = 5, scale = 2)
    private BigDecimal currentRatio;
    
    @Column(name = "quick_ratio", precision = 5, scale = 2)
    private BigDecimal quickRatio;
    
    @Column(name = "cash_ratio", precision = 5, scale = 2)
    private BigDecimal cashRatio;
    
    // === SOLVABILITÉ ===
    @Column(name = "debt_ratio_pct", precision = 5, scale = 2)
    private BigDecimal debtRatioPct;
    
    @Column(name = "debt_to_equity", precision = 5, scale = 2)
    private BigDecimal debtToEquity;
    
    @Column(name = "interest_coverage", precision = 5, scale = 2)
    private BigDecimal interestCoverage;
    
    // === ACTIVITÉ ===
    @Column(name = "asset_turnover", precision = 5, scale = 2)
    private BigDecimal assetTurnover;
    
    @Column(name = "inventory_turnover", precision = 5, scale = 2)
    private BigDecimal inventoryTurnover;
    
    @Column(name = "receivables_turnover", precision = 5, scale = 2)
    private BigDecimal receivablesTurnover;
    
    // === DÉLAIS MOYENS ===
    @Column(name = "dso_days")
    private Integer dsoDays;
    
    @Column(name = "dio_days")
    private Integer dioDays;
    
    @Column(name = "dpo_days")
    private Integer dpoDays;
    
    @Column(name = "cash_conversion_cycle")
    private Integer cashConversionCycle;
    
    // === DONNÉES BRUTES ===
    @Column(name = "total_revenue", precision = 15, scale = 2)
    private BigDecimal totalRevenue;
    
    @Column(name = "total_expenses", precision = 15, scale = 2)
    private BigDecimal totalExpenses;
    
    @Column(name = "net_income", precision = 15, scale = 2)
    private BigDecimal netIncome;
    
    @Column(name = "total_assets", precision = 15, scale = 2)
    private BigDecimal totalAssets;
    
    @Column(name = "total_equity", precision = 15, scale = 2)
    private BigDecimal totalEquity;
    
    @Column(name = "total_debt", precision = 15, scale = 2)
    private BigDecimal totalDebt;
    
    @Column(name = "working_capital", precision = 15, scale = 2)
    private BigDecimal workingCapital;
    
    @Column(name = "calculated_at", nullable = false)
    @Builder.Default
    private LocalDateTime calculatedAt = LocalDateTime.now();
}
