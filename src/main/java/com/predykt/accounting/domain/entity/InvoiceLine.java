package com.predykt.accounting.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ligne de facture client (Invoice Line)
 * Détail des produits/services facturés
 */
@Entity
@Table(name = "invoice_lines", uniqueConstraints = {
    @UniqueConstraint(name = "uk_invoice_line_number", columnNames = {"invoice_id", "line_number"})
})
@Data
@EqualsAndHashCode(exclude = {"invoice"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @NotNull(message = "La facture est obligatoire")
    private Invoice invoice;

    // ==================== Détails produit/service ====================

    @NotNull(message = "Le numéro de ligne est obligatoire")
    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;  // 1, 2, 3...

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
    private String unit = "Unité";  // kg, m², unité, heure, etc.

    @NotNull
    @Positive(message = "Le prix unitaire doit être positif")
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;  // Remise %

    // ==================== Montants ====================

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;  // Sous-total avant remise

    @Column(name = "discount_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_ht", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalHt = BigDecimal.ZERO;  // Total HT ligne (après remise)

    // ==================== TVA ====================

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal vatRate = new BigDecimal("19.25");  // Taux TVA Cameroun

    @Column(name = "vat_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "total_ttc", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalTtc = BigDecimal.ZERO;  // Total TTC ligne

    // ==================== Compte comptable ====================

    @Column(name = "account_number", length = 20)
    private String accountNumber;  // Compte de produit (701, 706, 707...)

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
