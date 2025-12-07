package com.predykt.accounting.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ligne de facture fournisseur (Bill Line)
 * Détail des produits/services achetés
 */
@Entity
@Table(name = "bill_lines", uniqueConstraints = {
    @UniqueConstraint(name = "uk_bill_line_number", columnNames = {"bill_id", "line_number"})
})
@Data
@EqualsAndHashCode(exclude = {"bill"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    @NotNull(message = "La facture est obligatoire")
    private Bill bill;

    // ==================== Détails produit/service ====================

    @NotNull(message = "Le numéro de ligne est obligatoire")
    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "product_code", length = 50)
    private String productCode;

    @NotBlank(message = "La description est obligatoire")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    // ==================== Quantités et prix ====================

    @NotNull
    @Positive(message = "La quantité doit être positive")
    @Column(nullable = false, precision = 15, scale = 3)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(length = 20)
    @Builder.Default
    private String unit = "Unité";

    @NotNull
    @Positive(message = "Le prix unitaire doit être positif")
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    // ==================== Montants ====================

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_ht", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalHt = BigDecimal.ZERO;

    // ==================== TVA ====================

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal vatRate = new BigDecimal("19.25");

    @Column(name = "vat_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "total_ttc", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalTtc = BigDecimal.ZERO;

    // ==================== Compte comptable ====================

    @Column(name = "account_number", length = 20)
    private String accountNumber;  // Compte d'achat (601, 602, 605, 622, 625...)

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    // ==================== Méthodes métier ====================

    /**
     * Calcule tous les montants de la ligne
     */
    public void calculateAmounts() {
        // Sous-total = quantité × prix unitaire
        this.subtotal = this.quantity.multiply(this.unitPrice)
            .setScale(2, RoundingMode.HALF_UP);

        // Montant de remise
        if (this.discountPercentage != null && this.discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
            this.discountAmount = this.subtotal
                .multiply(this.discountPercentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else {
            this.discountAmount = BigDecimal.ZERO;
        }

        // Total HT = sous-total - remise
        this.totalHt = this.subtotal.subtract(this.discountAmount)
            .setScale(2, RoundingMode.HALF_UP);

        // Montant TVA
        if (this.vatRate != null && this.vatRate.compareTo(BigDecimal.ZERO) > 0) {
            this.vatAmount = this.totalHt
                .multiply(this.vatRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else {
            this.vatAmount = BigDecimal.ZERO;
        }

        // Total TTC = HT + TVA
        this.totalTtc = this.totalHt.add(this.vatAmount)
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcul automatique avant sauvegarde
     */
    @PrePersist
    @PreUpdate
    private void preSave() {
        calculateAmounts();
    }
}
