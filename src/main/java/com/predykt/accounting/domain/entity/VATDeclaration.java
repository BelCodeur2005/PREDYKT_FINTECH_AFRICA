package com.predykt.accounting.domain.entity;

import com.predykt.accounting.domain.enums.VATDeclarationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Déclaration de TVA (CA3 mensuel / CA12 annuel)
 * Conforme au système fiscal camerounais
 */
@Entity
@Table(name = "vat_declarations", indexes = {
    @Index(name = "idx_vat_decl_company", columnList = "company_id"),
    @Index(name = "idx_vat_decl_period", columnList = "fiscal_period"),
    @Index(name = "idx_vat_decl_status", columnList = "status")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VATDeclaration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @NotNull(message = "Le type de déclaration est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(name = "declaration_type", nullable = false, length = 30)
    private VATDeclarationType declarationType;  // CA3_MONTHLY, CA12_ANNUAL

    /**
     * Période fiscale au format YYYY-MM (ex: 2024-11 pour novembre 2024)
     */
    @NotNull(message = "La période fiscale est obligatoire")
    @Column(name = "fiscal_period", nullable = false, length = 7)
    private String fiscalPeriod;

    @NotNull(message = "La date de début est obligatoire")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "La date de fin est obligatoire")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    // ============================================
    // SECTION 1: TVA COLLECTÉE
    // ============================================

    /**
     * TVA collectée sur ventes de marchandises (Compte 4431)
     */
    @Column(name = "vat_collected_sales", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal vatCollectedSales = BigDecimal.ZERO;

    /**
     * TVA collectée sur prestations de services (Compte 4432)
     */
    @Column(name = "vat_collected_services", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal vatCollectedServices = BigDecimal.ZERO;

    /**
     * TVA collectée sur travaux (Compte 4433)
     */
    @Column(name = "vat_collected_works", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal vatCollectedWorks = BigDecimal.ZERO;

    /**
     * TOTAL TVA collectée (Somme des comptes 443x)
     */
    @Column(name = "total_vat_collected", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalVatCollected = BigDecimal.ZERO;

    // ============================================
    // SECTION 2: TVA DÉDUCTIBLE
    // ============================================

    /**
     * TVA déductible sur immobilisations (Compte 4451)
     */
    @Column(name = "vat_deductible_fixed_assets", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal vatDeductibleFixedAssets = BigDecimal.ZERO;

    /**
     * TVA déductible sur achats (Compte 4452)
     */
    @Column(name = "vat_deductible_purchases", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal vatDeductiblePurchases = BigDecimal.ZERO;

    /**
     * TVA déductible sur transport (Compte 4453)
     */
    @Column(name = "vat_deductible_transport", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal vatDeductibleTransport = BigDecimal.ZERO;

    /**
     * TVA déductible sur services extérieurs (Compte 4454)
     */
    @Column(name = "vat_deductible_services", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal vatDeductibleServices = BigDecimal.ZERO;

    /**
     * TOTAL TVA déductible (Somme des comptes 445x)
     */
    @Column(name = "total_vat_deductible", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalVatDeductible = BigDecimal.ZERO;

    // ============================================
    // SECTION 3: SOLDE
    // ============================================

    /**
     * Crédit de TVA du mois précédent reporté
     */
    @Column(name = "previous_vat_credit", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal previousVatCredit = BigDecimal.ZERO;

    /**
     * TVA à payer = TVA collectée - TVA déductible - Crédit précédent
     * Si positif: TVA à payer (Compte 4441 crédit)
     * Si négatif: Crédit de TVA (Compte 4441 débit)
     */
    @Column(name = "vat_to_pay", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal vatToPay = BigDecimal.ZERO;

    /**
     * Crédit de TVA à reporter sur le mois suivant
     */
    @Column(name = "vat_credit_to_carry_forward", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal vatCreditToCarryForward = BigDecimal.ZERO;

    // ============================================
    // STATUT ET MÉTADONNÉES
    // ============================================

    /**
     * Statut de la déclaration:
     * - DRAFT: Brouillon
     * - VALIDATED: Validée (prête à soumettre)
     * - SUBMITTED: Soumise à l'administration fiscale
     * - PAID: Payée
     */
    @Column(length = 20)
    @Builder.Default
    private String status = "DRAFT";

    /**
     * Date de soumission à l'administration fiscale
     */
    @Column(name = "submission_date")
    private LocalDate submissionDate;

    /**
     * Date de paiement effectif de la TVA
     */
    @Column(name = "payment_date")
    private LocalDate paymentDate;

    /**
     * Numéro de référence de la déclaration (fourni par l'administration)
     */
    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Calcule le total de TVA collectée
     */
    public BigDecimal calculateTotalVatCollected() {
        return vatCollectedSales
            .add(vatCollectedServices)
            .add(vatCollectedWorks);
    }

    /**
     * Calcule le total de TVA déductible
     */
    public BigDecimal calculateTotalVatDeductible() {
        return vatDeductibleFixedAssets
            .add(vatDeductiblePurchases)
            .add(vatDeductibleTransport)
            .add(vatDeductibleServices);
    }

    /**
     * Calcule la TVA à payer (peut être négatif = crédit de TVA)
     */
    public BigDecimal calculateVatToPay() {
        BigDecimal netVat = totalVatCollected
            .subtract(totalVatDeductible)
            .subtract(previousVatCredit);

        return netVat;
    }

    /**
     * Recalcule tous les totaux
     */
    public void recalculateTotals() {
        this.totalVatCollected = calculateTotalVatCollected();
        this.totalVatDeductible = calculateTotalVatDeductible();
        this.vatToPay = calculateVatToPay();

        // Si le solde est négatif, c'est un crédit à reporter
        if (vatToPay.compareTo(BigDecimal.ZERO) < 0) {
            this.vatCreditToCarryForward = vatToPay.negate();
            this.vatToPay = BigDecimal.ZERO;
        } else {
            this.vatCreditToCarryForward = BigDecimal.ZERO;
        }
    }

    /**
     * Valide la déclaration (changement de statut)
     */
    public void validate() {
        if (!"DRAFT".equals(status)) {
            throw new IllegalStateException("Seules les déclarations en brouillon peuvent être validées");
        }
        recalculateTotals();
        this.status = "VALIDATED";
    }

    /**
     * Marque la déclaration comme soumise
     */
    public void markAsSubmitted(String referenceNumber) {
        if (!"VALIDATED".equals(status)) {
            throw new IllegalStateException("Seules les déclarations validées peuvent être soumises");
        }
        this.status = "SUBMITTED";
        this.submissionDate = LocalDate.now();
        this.referenceNumber = referenceNumber;
    }

    /**
     * Marque la déclaration comme payée
     */
    public void markAsPaid() {
        if (!"SUBMITTED".equals(status)) {
            throw new IllegalStateException("Seules les déclarations soumises peuvent être marquées comme payées");
        }
        this.status = "PAID";
        this.paymentDate = LocalDate.now();
    }

    /**
     * Vérifie si la déclaration est modifiable
     */
    public boolean isEditable() {
        return "DRAFT".equals(status);
    }

    /**
     * Vérifie si c'est un crédit de TVA
     */
    public boolean hasVatCredit() {
        return vatCreditToCarryForward.compareTo(BigDecimal.ZERO) > 0;
    }
}
