package com.predykt.accounting.domain.entity;

import com.predykt.accounting.domain.enums.VATRecoverableCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Calcul détaillé de TVA récupérable avec traçabilité complète
 *
 * Cette entité enregistre chaque calcul de récupération de TVA en 2 étapes :
 * 1. Récupérabilité par NATURE de la dépense (VP, VU, représentation, etc.)
 * 2. Application du PRORATA (si activités mixtes)
 *
 * Permet audit trail complet et justification des déclarations CA3
 */
@Entity
@Table(
    name = "vat_recovery_calculation",
    indexes = {
        @Index(name = "idx_vat_calc_company", columnList = "company_id"),
        @Index(name = "idx_vat_calc_gl", columnList = "general_ledger_id"),
        @Index(name = "idx_vat_calc_year", columnList = "fiscal_year"),
        @Index(name = "idx_vat_calc_account", columnList = "account_number"),
        @Index(name = "idx_vat_calc_prorata", columnList = "prorata_id")
    }
)
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VATRecoveryCalculation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Entreprise concernée (MULTI-TENANT)
     */
    @NotNull(message = "L'entreprise est obligatoire")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /**
     * Référence à l'écriture comptable (optionnel)
     * Permet de lier le calcul à l'écriture si effectué lors de la saisie
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "general_ledger_id")
    private GeneralLedger generalLedger;

    /**
     * Numéro de compte OHADA (ex: 2441, 605, 622, 627)
     */
    @NotBlank(message = "Le numéro de compte est obligatoire")
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    /**
     * Description de la dépense (pour détection par règles)
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Montant HT de la dépense
     */
    @NotNull(message = "Le montant HT est obligatoire")
    @Min(value = 0, message = "Le montant HT doit être positif")
    @Column(name = "ht_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal htAmount;

    /**
     * Montant TVA total de la dépense
     */
    @NotNull(message = "Le montant TVA est obligatoire")
    @Min(value = 0, message = "Le montant TVA doit être positif")
    @Column(name = "vat_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal vatAmount;

    /**
     * Taux de TVA appliqué (19.25% au Cameroun)
     */
    @NotNull(message = "Le taux de TVA est obligatoire")
    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate;

    // ============================================
    // ÉTAPE 1: RÉCUPÉRABILITÉ PAR NATURE
    // ============================================

    /**
     * Catégorie de récupérabilité détectée par les règles
     */
    @NotNull(message = "La catégorie de récupérabilité est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(name = "recovery_category", nullable = false, length = 50)
    private VATRecoverableCategory recoveryCategory;

    /**
     * Taux de récupération par nature (0.0000 à 1.0000)
     * - 1.0000 = 100% récupérable (VU, équipement professionnel)
     * - 0.8000 = 80% récupérable (carburant VU)
     * - 0.0000 = Non récupérable (VP, représentation, luxe, personnel)
     */
    @NotNull(message = "Le taux de récupération par nature est obligatoire")
    @DecimalMin(value = "0.0000")
    @DecimalMax(value = "1.0000")
    @Column(name = "recovery_by_nature_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal recoveryByNatureRate;

    /**
     * TVA récupérable PAR NATURE (AVANT prorata)
     * = vatAmount × recoveryByNatureRate
     */
    @NotNull(message = "La TVA récupérable par nature est obligatoire")
    @Column(name = "recoverable_by_nature", nullable = false, precision = 20, scale = 2)
    private BigDecimal recoverableByNature;

    // ============================================
    // ÉTAPE 2: APPLICATION DU PRORATA
    // ============================================

    /**
     * Prorata appliqué (référence)
     * NULL si pas de prorata (activités 100% taxables)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prorata_id")
    private VATProrata prorata;

    /**
     * Taux du prorata appliqué (0.0000 à 1.0000)
     * NULL si pas de prorata
     * Ex: 0.8500 = 85% activités taxables
     */
    @DecimalMin(value = "0.0000")
    @DecimalMax(value = "1.0000")
    @Column(name = "prorata_rate", precision = 5, scale = 4)
    private BigDecimal prorataRate;

    /**
     * TVA récupérable APRÈS prorata
     * = recoverableByNature × prorataRate (si prorata appliqué)
     * = recoverableByNature (si pas de prorata)
     */
    @NotNull(message = "La TVA récupérable avec prorata est obligatoire")
    @Column(name = "recoverable_with_prorata", nullable = false, precision = 20, scale = 2)
    private BigDecimal recoverableWithProrata;

    // ============================================
    // RÉSULTAT FINAL
    // ============================================

    /**
     * TVA récupérable FINALE
     * = MIN(recoverableByNature, recoverableWithProrata)
     *
     * Cette valeur sera déclarée en CA3
     */
    @NotNull(message = "La TVA récupérable finale est obligatoire")
    @Column(name = "recoverable_vat", nullable = false, precision = 20, scale = 2)
    private BigDecimal recoverableVat;

    /**
     * TVA NON récupérable
     * = vatAmount - recoverableVat
     *
     * Cette portion passe en charge (compte 606) ou est incorporée à l'immobilisation
     */
    @NotNull(message = "La TVA non récupérable est obligatoire")
    @Column(name = "non_recoverable_vat", nullable = false, precision = 20, scale = 2)
    private BigDecimal nonRecoverableVat;

    // ============================================
    // MÉTADONNÉES DE DÉTECTION
    // ============================================

    /**
     * Règle de récupérabilité appliquée
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_rule_id")
    private RecoverabilityRule appliedRule;

    /**
     * Confiance de la détection (0-100)
     */
    @Min(0)
    @Max(100)
    @Column(name = "detection_confidence")
    private Integer detectionConfidence;

    /**
     * Raison de la détection (pour audit)
     */
    @Column(name = "detection_reason", columnDefinition = "TEXT")
    private String detectionReason;

    /**
     * Date du calcul
     */
    @Column(name = "calculation_date", nullable = false)
    @Builder.Default
    private LocalDateTime calculationDate = LocalDateTime.now();

    /**
     * Année fiscale
     */
    @NotNull(message = "L'année fiscale est obligatoire")
    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    /**
     * Calcule les montants récupérables
     */
    public void calculate() {
        // ÉTAPE 1: Récupérabilité par nature
        this.recoverableByNature = vatAmount
            .multiply(recoveryByNatureRate)
            .setScale(2, java.math.RoundingMode.HALF_UP);

        // ÉTAPE 2: Application du prorata
        if (prorata != null && prorataRate != null) {
            this.recoverableWithProrata = recoverableByNature
                .multiply(prorataRate)
                .setScale(0, java.math.RoundingMode.HALF_UP);
        } else {
            // Pas de prorata → récupération intégrale (selon nature)
            this.recoverableWithProrata = recoverableByNature;
        }

        // RÉSULTAT FINAL
        this.recoverableVat = recoverableWithProrata;
        this.nonRecoverableVat = vatAmount.subtract(recoverableVat);
    }

    /**
     * Vérifie si le prorata a eu un impact
     */
    public boolean hasProrataImpact() {
        return prorata != null &&
               recoverableByNature.compareTo(recoverableWithProrata) != 0;
    }

    /**
     * Retourne le montant de l'impact du prorata
     */
    public BigDecimal getProrataImpact() {
        if (!hasProrataImpact()) {
            return BigDecimal.ZERO;
        }
        return recoverableByNature.subtract(recoverableWithProrata);
    }

    /**
     * Vérifie si la dépense est totalement non récupérable
     */
    public boolean isFullyNonRecoverable() {
        return recoverableVat.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Vérifie si la dépense est totalement récupérable
     */
    public boolean isFullyRecoverable() {
        return recoverableVat.compareTo(vatAmount) == 0;
    }

    /**
     * Retourne le taux de récupération effectif (%)
     */
    public BigDecimal getEffectiveRecoveryRate() {
        if (vatAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return recoverableVat.divide(vatAmount, 4, java.math.RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }

    /**
     * Hook avant persist/update
     */
    @PrePersist
    @PreUpdate
    public void prePersist() {
        // Calculer les montants si null
        if (recoverableVat == null || nonRecoverableVat == null) {
            calculate();
        }

        // Date de calcul
        if (calculationDate == null) {
            calculationDate = LocalDateTime.now();
        }

        // Année fiscale par défaut
        if (fiscalYear == null) {
            fiscalYear = LocalDateTime.now().getYear();
        }
    }
}
