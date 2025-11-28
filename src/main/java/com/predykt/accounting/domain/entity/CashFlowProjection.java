package com.predykt.accounting.domain.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Entité représentant une projection de trésorerie
 * Prévisions de flux de trésorerie à J+30, J+60, J+90
 */
@Entity
@Table(name = "cash_flow_projections", indexes = {
    @Index(name = "idx_projection_company_date", columnList = "company_id, projection_date"),
    @Index(name = "idx_projection_horizon", columnList = "projection_horizon")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashFlowProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "L'entreprise est obligatoire")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @NotNull(message = "La date de projection est obligatoire")
    @Column(name = "projection_date", nullable = false)
    private LocalDate projectionDate;

    @NotNull(message = "L'horizon de projection est obligatoire")
    @Column(name = "projection_horizon", nullable = false)
    private Integer projectionHorizon; // J+30, J+60, J+90

    // Soldes
    @NotNull(message = "Le solde d'ouverture est obligatoire")
    @Column(name = "opening_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal openingBalance;

    @NotNull(message = "Le solde projeté est obligatoire")
    @Column(name = "projected_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal projectedBalance;

    // Flux entrants prévus
    @Column(name = "projected_inflows", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal projectedInflows = BigDecimal.ZERO;

    @Column(name = "receivables_collection", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal receivablesCollection = BigDecimal.ZERO;

    @Column(name = "other_income", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal otherIncome = BigDecimal.ZERO;

    // Flux sortants prévus
    @Column(name = "projected_outflows", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal projectedOutflows = BigDecimal.ZERO;

    @Column(name = "payables_payment", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal payablesPayment = BigDecimal.ZERO;

    @Column(name = "payroll_payment", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal payrollPayment = BigDecimal.ZERO;

    @Column(name = "tax_payment", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal taxPayment = BigDecimal.ZERO;

    @Column(name = "other_expenses", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal otherExpenses = BigDecimal.ZERO;

    // Métadonnées ML (pour phase future avec prédictions IA)
    @Column(name = "model_used", length = 50)
    private String modelUsed; // ARIMA, Prophet, Linear Regression, etc.

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore; // Score de confiance 0-100

    @Type(JsonBinaryType.class)
    @Column(name = "prediction_details", columnDefinition = "jsonb")
    private Map<String, Object> predictionDetails; // Détails du modèle ML (format JSON)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDate createdAt = LocalDate.now();

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Horizons de projection standards
     */
    public static final int HORIZON_30_DAYS = 30;
    public static final int HORIZON_60_DAYS = 60;
    public static final int HORIZON_90_DAYS = 90;

    /**
     * Calcule les flux totaux entrants
     */
    public BigDecimal calculateTotalInflows() {
        return receivablesCollection
            .add(otherIncome)
            .add(projectedInflows);
    }

    /**
     * Calcule les flux totaux sortants
     */
    public BigDecimal calculateTotalOutflows() {
        return payablesPayment
            .add(payrollPayment)
            .add(taxPayment)
            .add(otherExpenses)
            .add(projectedOutflows);
    }

    /**
     * Calcule le flux net (inflows - outflows)
     */
    public BigDecimal calculateNetCashFlow() {
        return calculateTotalInflows().subtract(calculateTotalOutflows());
    }

    /**
     * Vérifie si la projection indique une trésorerie négative
     */
    public boolean isNegativeCashFlow() {
        return projectedBalance.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Calcule le ratio de couverture (opening / outflows)
     * Indique combien de temps la trésorerie actuelle peut couvrir les dépenses
     */
    public BigDecimal getCoverageRatio() {
        BigDecimal totalOutflows = calculateTotalOutflows();
        if (totalOutflows.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return openingBalance.divide(totalOutflows, 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Vérifie si c'est une projection haute confiance (>= 80%)
     */
    public boolean isHighConfidence() {
        return confidenceScore != null && confidenceScore.compareTo(BigDecimal.valueOf(80)) >= 0;
    }

    /**
     * Retourne le niveau de confiance sous forme de texte
     */
    public String getConfidenceLevel() {
        if (confidenceScore == null) {
            return "UNKNOWN";
        }
        if (confidenceScore.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return "HIGH";
        } else if (confidenceScore.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * Retourne la date cible de la projection (date + horizon)
     */
    public LocalDate getTargetDate() {
        return projectionDate.plusDays(projectionHorizon);
    }
}
