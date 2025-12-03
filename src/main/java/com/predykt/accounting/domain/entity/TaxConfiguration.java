package com.predykt.accounting.domain.entity;

import com.predykt.accounting.domain.enums.TaxType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * Configuration des taxes par entreprise
 * Permet de personnaliser les taux et paramètres fiscaux
 */
@Entity
@Table(name = "tax_configurations", indexes = {
    @Index(name = "idx_tax_config_company", columnList = "company_id"),
    @Index(name = "idx_tax_config_type", columnList = "tax_type"),
    @Index(name = "idx_tax_config_active", columnList = "is_active")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_tax_config_company_type", columnNames = {"company_id", "tax_type"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxConfiguration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @NotNull(message = "Le type de taxe est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false, length = 50)
    private TaxType taxType;

    @NotBlank(message = "Le nom de la taxe est obligatoire")
    @Column(name = "tax_name", nullable = false, length = 100)
    private String taxName;

    @NotNull(message = "Le taux de taxe est obligatoire")
    @DecimalMin(value = "0.0", message = "Le taux de taxe doit être positif")
    @DecimalMax(value = "100.0", message = "Le taux de taxe ne peut pas dépasser 100%")
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    @NotBlank(message = "Le compte OHADA est obligatoire")
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Calcul automatique lors des imports/transactions
     */
    @Column(name = "is_automatic")
    @Builder.Default
    private Boolean isAutomatic = true;

    /**
     * S'applique aux ventes (ex: TVA collectée, Acompte IS)
     */
    @Column(name = "apply_to_sales")
    @Builder.Default
    private Boolean applyToSales = false;

    /**
     * S'applique aux achats (ex: TVA déductible, AIR, IRPP Loyer)
     */
    @Column(name = "apply_to_purchases")
    @Builder.Default
    private Boolean applyToPurchases = false;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Référence légale (ex: "CGI Art. 127")
     */
    @Column(name = "legal_reference", length = 200)
    private String legalReference;

    /**
     * Jour du mois pour paiement (ex: 15 pour "15 du mois suivant")
     */
    @Column(name = "due_day")
    private Integer dueDay;

    /**
     * Initialise une configuration avec les valeurs par défaut de l'enum
     */
    public static TaxConfiguration createDefault(Company company, TaxType taxType) {
        return TaxConfiguration.builder()
            .company(company)
            .taxType(taxType)
            .taxName(taxType.getDisplayName())
            .taxRate(taxType.getDefaultRate())
            .accountNumber(taxType.getDefaultAccountNumber())
            .legalReference(taxType.getLegalReference())
            .dueDay(taxType.getDueDay())
            .applyToSales(taxType.isApplyToSales())
            .applyToPurchases(taxType.isApplyToPurchases())
            .isActive(true)
            .isAutomatic(true)
            .build();
    }

    /**
     * Vérifie si la taxe est applicable pour les ventes
     */
    public boolean isApplicableToSales() {
        return isActive && applyToSales;
    }

    /**
     * Vérifie si la taxe est applicable pour les achats
     */
    public boolean isApplicableToPurchases() {
        return isActive && applyToPurchases;
    }

    /**
     * Vérifie si le calcul est automatique et actif
     */
    public boolean shouldAutoCalculate() {
        return isActive && isAutomatic;
    }

    /**
     * Calcule le montant de taxe à partir d'un montant HT
     */
    public BigDecimal calculateTaxAmount(BigDecimal amountExcludingTax) {
        if (amountExcludingTax == null || taxRate == null) {
            return BigDecimal.ZERO;
        }
        return amountExcludingTax
            .multiply(taxRate)
            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Calcule le montant TTC à partir d'un montant HT
     */
    public BigDecimal calculateAmountIncludingTax(BigDecimal amountExcludingTax) {
        if (amountExcludingTax == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal taxAmount = calculateTaxAmount(amountExcludingTax);
        return amountExcludingTax.add(taxAmount);
    }
}
