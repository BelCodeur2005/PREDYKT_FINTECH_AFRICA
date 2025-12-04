package com.predykt.accounting.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Prorata de TVA pour activités mixtes (taxables + exonérées)
 * Conforme au CGI Cameroun Art. 133
 *
 * Le prorata détermine le coefficient de récupération de TVA pour les entreprises
 * ayant des activités taxables ET des activités exonérées (exports, hors champ TVA).
 *
 * Formule : Prorata = (CA taxable ÷ CA total) × 100
 *
 * Exemple :
 * - CA taxable : 800 M FCFA
 * - CA exonéré : 200 M FCFA
 * - Prorata = 80% → TVA récup. = TVA × 80%
 */
@Entity
@Table(
    name = "vat_prorata",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_vat_prorata_company_year",
            columnNames = {"company_id", "fiscal_year", "is_active"}
        )
    },
    indexes = {
        @Index(name = "idx_vat_prorata_company", columnList = "company_id"),
        @Index(name = "idx_vat_prorata_year", columnList = "fiscal_year"),
        @Index(name = "idx_vat_prorata_active", columnList = "is_active")
    }
)
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VATProrata extends BaseEntity {

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
     * Année fiscale (ex: 2024)
     */
    @NotNull(message = "L'année fiscale est obligatoire")
    @Min(value = 2000, message = "Année fiscale minimum: 2000")
    @Max(value = 2100, message = "Année fiscale maximum: 2100")
    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    /**
     * Chiffre d'affaires taxable (ventes soumises à TVA)
     * Comptes 70x avec TVA collectée
     */
    @NotNull(message = "Le CA taxable est obligatoire")
    @Min(value = 0, message = "Le CA taxable doit être positif")
    @Column(name = "taxable_turnover", nullable = false, precision = 20, scale = 2)
    @Builder.Default
    private BigDecimal taxableTurnover = BigDecimal.ZERO;

    /**
     * Chiffre d'affaires exonéré (exports, hors champ TVA, exonérations)
     */
    @NotNull(message = "Le CA exonéré est obligatoire")
    @Min(value = 0, message = "Le CA exonéré doit être positif")
    @Column(name = "exempt_turnover", nullable = false, precision = 20, scale = 2)
    @Builder.Default
    private BigDecimal exemptTurnover = BigDecimal.ZERO;

    /**
     * Chiffre d'affaires total = taxable + exempt
     */
    @NotNull(message = "Le CA total est obligatoire")
    @Min(value = 0, message = "Le CA total doit être positif")
    @Column(name = "total_turnover", nullable = false, precision = 20, scale = 2)
    @Builder.Default
    private BigDecimal totalTurnover = BigDecimal.ZERO;

    /**
     * Prorata de TVA (coefficient de récupération)
     * Valeur entre 0.0000 et 1.0000
     * Ex: 0.8000 = 80% de récupération
     *
     * Calculé automatiquement : taxableTurnover / totalTurnover
     */
    @NotNull(message = "Le prorata est obligatoire")
    @DecimalMin(value = "0.0000", message = "Le prorata minimum est 0%")
    @DecimalMax(value = "1.0000", message = "Le prorata maximum est 100%")
    @Column(name = "prorata_rate", nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal prorataRate = BigDecimal.ONE;

    /**
     * Type de prorata
     * - PROVISIONAL : Prorata provisoire (basé sur année N-1)
     * - DEFINITIVE : Prorata définitif (basé sur CA réel année N)
     */
    @NotNull(message = "Le type de prorata est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(name = "prorata_type", nullable = false, length = 20)
    @Builder.Default
    private ProrataType prorataType = ProrataType.PROVISIONAL;

    /**
     * Prorata actif (un seul prorata actif par company/année)
     */
    @NotNull
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Prorata verrouillé (après clôture fiscale)
     * Un prorata verrouillé ne peut plus être modifié
     */
    @NotNull
    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private Boolean isLocked = false;

    /**
     * Date du calcul
     */
    @Column(name = "calculation_date")
    private LocalDateTime calculationDate;

    /**
     * Date et auteur du verrouillage
     */
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    /**
     * Notes (explications, contexte)
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Type de prorata
     */
    public enum ProrataType {
        /**
         * Prorata provisoire - Calculé en début d'année basé sur l'année N-1
         * Utilisé pour les déclarations mensuelles CA3
         */
        PROVISIONAL("Provisoire"),

        /**
         * Prorata définitif - Calculé en fin d'année basé sur le CA réel
         * Déclenche une régularisation si différent du provisoire
         */
        DEFINITIVE("Définitif");

        private final String displayName;

        ProrataType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Calcule le prorata à partir des CA
     */
    public void calculateProrataRate() {
        if (totalTurnover.compareTo(BigDecimal.ZERO) > 0) {
            this.prorataRate = taxableTurnover
                .divide(totalTurnover, 4, java.math.RoundingMode.HALF_UP);
        } else {
            this.prorataRate = BigDecimal.ONE; // 100% par défaut si pas de CA
        }
    }

    /**
     * Calcule le total turnover à partir de taxable + exempt
     */
    public void calculateTotalTurnover() {
        this.totalTurnover = taxableTurnover.add(exemptTurnover);
    }

    /**
     * Retourne le prorata en pourcentage (ex: 80.00 pour 0.8000)
     */
    public BigDecimal getProrataPercentage() {
        return prorataRate.multiply(new BigDecimal("100"))
            .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Verrouille le prorata
     */
    public void lock(String lockedBy) {
        this.isLocked = true;
        this.lockedAt = LocalDateTime.now();
        this.lockedBy = lockedBy;
    }

    /**
     * Vérifie si le prorata peut être modifié
     */
    public boolean canBeModified() {
        return !isLocked;
    }

    /**
     * Vérifie si le prorata nécessite une régularisation
     * (ex: écart > 10% entre provisoire et définitif)
     */
    public boolean needsRegularization(BigDecimal newProrataRate) {
        if (this.prorataType == ProrataType.PROVISIONAL && newProrataRate != null) {
            BigDecimal difference = this.prorataRate.subtract(newProrataRate).abs();
            BigDecimal threshold = new BigDecimal("0.10"); // 10%
            return difference.compareTo(threshold) > 0;
        }
        return false;
    }

    /**
     * Applique le prorata à un montant de TVA
     */
    public BigDecimal applyToVAT(BigDecimal vatAmount) {
        if (vatAmount == null || vatAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return vatAmount.multiply(prorataRate)
            .setScale(0, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Hook avant persist/update
     */
    @PrePersist
    @PreUpdate
    public void prePersist() {
        // Calculer le total si null
        if (totalTurnover == null || totalTurnover.compareTo(BigDecimal.ZERO) == 0) {
            calculateTotalTurnover();
        }

        // Calculer le prorata si null
        if (prorataRate == null) {
            calculateProrataRate();
        }

        // Date de calcul
        if (calculationDate == null) {
            calculationDate = LocalDateTime.now();
        }
    }
}
