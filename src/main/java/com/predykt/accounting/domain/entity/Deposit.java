package com.predykt.accounting.domain.entity;

import com.predykt.accounting.exception.ValidationException;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 🔵 ENTITÉ CRITIQUE: Deposit (Acompte/Avance Client)
 *
 * Représente les acomptes reçus des clients AVANT la facturation finale.
 * Conforme OHADA SYSCOHADA (Compte 4191 "Clients - Avances et acomptes reçus sur commandes").
 *
 * Cycle de vie d'un acompte:
 * 1. Réception acompte → Création Deposit + Reçu d'acompte (RA-YYYY-NNNN)
 * 2. Écriture comptable: Débit 512 Banque / Crédit 4191 Avances + 4431 TVA
 * 3. Facturation finale → Imputation acompte sur facture
 * 4. Écriture comptable: Débit 4191 Avances / Crédit 411 Clients
 *
 * Conformité CGI Cameroun:
 * - Article 128: TVA exigible sur encaissement (TVA calculée dès réception acompte)
 * - Taux TVA: 19.25% (standard Cameroun)
 * - Reçu d'acompte obligatoire (justificatif fiscal)
 *
 * Relations:
 * - Deposit (N) → (1) Company (multi-tenant isolation)
 * - Deposit (N) → (0..1) Customer (peut être NULL si acompte anonyme)
 * - Deposit (N) → (0..1) Invoice (NULL tant que non imputé)
 * - Deposit (1) → (1) Payment (lien avec encaissement bancaire)
 *
 * @author PREDYKT System Optimizer
 * @since Phase 3 - Conformité OHADA Avancée
 */
@Entity
@Table(name = "deposits", indexes = {
    @Index(name = "idx_deposits_company_id", columnList = "company_id"),
    @Index(name = "idx_deposits_customer_id", columnList = "customer_id"),
    @Index(name = "idx_deposits_invoice_id", columnList = "invoice_id"),
    @Index(name = "idx_deposits_payment_id", columnList = "payment_id"),
    @Index(name = "idx_deposits_deposit_date", columnList = "deposit_date"),
    @Index(name = "idx_deposits_is_applied", columnList = "is_applied"),
    @Index(name = "idx_deposits_company_customer_date", columnList = "company_id, customer_id, deposit_date"),
    @Index(name = "idx_deposits_company_not_applied", columnList = "company_id, is_applied")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = {"id"})
@ToString(exclude = {"company", "customer", "invoice", "payment"})
@Slf4j
public class Deposit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== IDENTIFICATION ====================

    @Column(name = "deposit_number", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Le numéro de reçu d'acompte est obligatoire")
    @Pattern(regexp = "^RA-\\d{4}-\\d{4,6}$", message = "Format du numéro: RA-YYYY-NNNNNN")
    private String depositNumber;  // Ex: RA-2025-000001

    @Column(name = "deposit_date", nullable = false)
    @NotNull(message = "La date de réception de l'acompte est obligatoire")
    private LocalDate depositDate;

    // ==================== RELATIONS ====================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, foreignKey = @ForeignKey(name = "fk_deposit_company"))
    @NotNull(message = "La société est obligatoire")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", foreignKey = @ForeignKey(name = "fk_deposit_customer"))
    private Customer customer;  // Nullable: acompte peut être reçu avant identification client

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", foreignKey = @ForeignKey(name = "fk_deposit_invoice"))
    private Invoice invoice;  // Nullable: NULL tant que pas imputé sur facture finale

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", foreignKey = @ForeignKey(name = "fk_deposit_payment"))
    private Payment payment;  // Lien avec l'encaissement bancaire

    @OneToMany(mappedBy = "deposit", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private java.util.List<DepositApplication> applications = new java.util.ArrayList<>();  // Imputations partielles (Phase 2)

    // ==================== MONTANTS OHADA ====================

    @Column(name = "amount_ht", nullable = false, precision = 15, scale = 2)
    @NotNull(message = "Le montant HT est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant HT doit être positif")
    private BigDecimal amountHt = BigDecimal.ZERO;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    @NotNull(message = "Le taux de TVA est obligatoire")
    @DecimalMin(value = "0.00", message = "Le taux de TVA doit être positif ou nul")
    @DecimalMax(value = "100.00", message = "Le taux de TVA ne peut pas dépasser 100%")
    @Builder.Default
    private BigDecimal vatRate = new BigDecimal("19.25");  // Taux TVA Cameroun

    @Column(name = "vat_amount", nullable = false, precision = 15, scale = 2)
    @NotNull(message = "Le montant de TVA est obligatoire")
    @DecimalMin(value = "0.00", message = "Le montant de TVA doit être positif ou nul")
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "amount_ttc", nullable = false, precision = 15, scale = 2)
    @NotNull(message = "Le montant TTC est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant TTC doit être positif")
    private BigDecimal amountTtc = BigDecimal.ZERO;

    /**
     * Montant total déjà imputé sur des factures (Phase 2 - Imputation Partielle)
     * Somme de tous les deposit_applications.amount_ttc
     */
    @Column(name = "amount_applied", nullable = false, precision = 15, scale = 2)
    @NotNull(message = "Le montant appliqué est obligatoire")
    @DecimalMin(value = "0.00", message = "Le montant appliqué doit être positif ou nul")
    @Builder.Default
    private BigDecimal amountApplied = BigDecimal.ZERO;

    /**
     * Montant restant disponible pour imputation (Phase 2 - Imputation Partielle)
     * amountRemaining = amountTtc - amountApplied
     */
    @Column(name = "amount_remaining", nullable = false, precision = 15, scale = 2)
    @NotNull(message = "Le montant restant est obligatoire")
    @DecimalMin(value = "0.00", message = "Le montant restant doit être positif ou nul")
    @Builder.Default
    private BigDecimal amountRemaining = BigDecimal.ZERO;

    // ==================== ÉTAT DE L'ACOMPTE ====================

    @Column(name = "is_applied", nullable = false)
    @NotNull
    @Builder.Default
    private Boolean isApplied = false;  // Acompte imputé sur facture finale ?

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;  // Date d'imputation

    @Column(name = "applied_by", length = 100)
    private String appliedBy;  // Utilisateur ayant fait l'imputation

    // ==================== DOCUMENTATION ====================

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;  // Description de l'acompte

    @Column(name = "customer_order_reference", length = 100)
    private String customerOrderReference;  // Référence commande client

    @Column(name = "deposit_receipt_url", columnDefinition = "TEXT")
    private String depositReceiptUrl;  // URL du reçu d'acompte PDF

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;  // Notes internes

    // ==================== MÉTHODES MÉTIER ====================

    /**
     * Calcule automatiquement les montants TTC et TVA à partir du montant HT et du taux de TVA.
     * OHADA: TVA exigible sur encaissement (CGI Cameroun Article 128).
     *
     * Formule:
     * - vatAmount = amountHt × vatRate / 100
     * - amountTtc = amountHt + vatAmount
     */
    @PrePersist
    @PreUpdate
    public void calculateAmounts() {
        if (amountHt == null || vatRate == null) {
            return;
        }

        // Calcul TVA sur encaissement (OHADA)
        this.vatAmount = amountHt
            .multiply(vatRate)
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        // Calcul montant TTC
        this.amountTtc = amountHt.add(vatAmount);

        // Phase 2: Initialiser le montant restant lors de la création
        if (this.amountApplied == null) {
            this.amountApplied = BigDecimal.ZERO;
        }
        this.amountRemaining = this.amountTtc.subtract(this.amountApplied);

        log.debug("💰 Calcul acompte: {}% TVA sur {} XAF HT = {} XAF TVA → {} XAF TTC (restant: {} XAF)",
            vatRate, amountHt, vatAmount, amountTtc, amountRemaining);
    }

    /**
     * Valide la cohérence des montants selon les règles OHADA.
     *
     * @throws ValidationException si les montants sont incohérents
     */
    public void validateAmounts() {
        if (amountHt == null || amountHt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Le montant HT doit être strictement positif");
        }

        if (vatRate == null || vatRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Le taux de TVA ne peut pas être négatif");
        }

        if (vatAmount == null || vatAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Le montant de TVA ne peut pas être négatif");
        }

        if (amountTtc == null || amountTtc.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Le montant TTC doit être strictement positif");
        }

        // Vérifier cohérence: amountTtc = amountHt + vatAmount (avec tolérance 0.01 pour arrondis)
        BigDecimal expectedTtc = amountHt.add(vatAmount);
        BigDecimal difference = amountTtc.subtract(expectedTtc).abs();

        if (difference.compareTo(new BigDecimal("0.01")) > 0) {
            throw new ValidationException(String.format(
                "Incohérence des montants: TTC (%s) ≠ HT (%s) + TVA (%s)",
                amountTtc, amountHt, vatAmount
            ));
        }
    }

    /**
     * Impute l'acompte sur une facture finale.
     * OHADA: Débit compte 4191 / Crédit compte 411 Clients.
     *
     * @param invoice Facture finale
     * @param appliedBy Utilisateur effectuant l'imputation
     * @throws ValidationException si l'acompte est déjà imputé ou si la facture est invalide
     */
    public void applyToInvoice(Invoice invoice, String appliedBy) {
        if (this.isApplied) {
            throw new ValidationException(String.format(
                "L'acompte %s est déjà imputé sur la facture %s",
                this.depositNumber,
                this.invoice != null ? this.invoice.getInvoiceNumber() : "inconnue"
            ));
        }

        if (invoice == null) {
            throw new ValidationException("La facture ne peut pas être NULL");
        }

        // Vérifier que le client correspond
        if (this.customer != null && invoice.getCustomer() != null) {
            if (!this.customer.getId().equals(invoice.getCustomer().getId())) {
                throw new ValidationException(String.format(
                    "L'acompte appartient au client %s mais la facture au client %s",
                    this.customer.getName(),
                    invoice.getCustomer().getName()
                ));
            }
        }

        // Vérifier que la société correspond
        if (!this.company.getId().equals(invoice.getCompany().getId())) {
            throw new ValidationException("L'acompte et la facture n'appartiennent pas à la même société");
        }

        // Vérifier que le montant de l'acompte n'excède pas le montant de la facture
        if (this.amountTtc.compareTo(invoice.getTotalTtc()) > 0) {
            throw new ValidationException(String.format(
                "Le montant de l'acompte (%s XAF) dépasse le montant de la facture (%s XAF)",
                this.amountTtc, invoice.getTotalTtc()
            ));
        }

        // Imputer l'acompte
        this.invoice = invoice;
        this.isApplied = true;
        this.appliedAt = LocalDateTime.now();
        this.appliedBy = appliedBy;

        log.info("✅ Acompte {} ({} XAF) imputé sur facture {} par {}",
            this.depositNumber, this.amountTtc, invoice.getInvoiceNumber(), appliedBy);
    }

    /**
     * Annule l'imputation de l'acompte (en cas d'erreur ou modification).
     * Permet de ré-utiliser l'acompte sur une autre facture.
     *
     * @throws ValidationException si l'acompte n'est pas imputé
     */
    public void unapply() {
        if (!this.isApplied) {
            throw new ValidationException(String.format(
                "L'acompte %s n'est pas imputé, impossible d'annuler l'imputation",
                this.depositNumber
            ));
        }

        String previousInvoiceNumber = this.invoice != null ? this.invoice.getInvoiceNumber() : "inconnue";

        this.invoice = null;
        this.isApplied = false;
        this.appliedAt = null;
        this.appliedBy = null;

        log.warn("⚠️ Annulation imputation acompte {} (anciennement sur facture {})",
            this.depositNumber, previousInvoiceNumber);
    }

    /**
     * Vérifie si l'acompte peut être imputé sur une facture.
     *
     * @return true si l'acompte est disponible pour imputation
     */
    public boolean canBeApplied() {
        return !this.isApplied && this.amountTtc.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Retourne le montant restant disponible de l'acompte (Phase 2 - Imputation Partielle).
     * Calcule dynamiquement basé sur les applications effectuées.
     *
     * @return Montant disponible pour imputation
     */
    public BigDecimal getAvailableAmount() {
        if (this.amountRemaining != null) {
            return this.amountRemaining;
        }

        // Fallback: calculer basé sur les applications
        if (this.applications != null && !this.applications.isEmpty()) {
            BigDecimal totalApplied = this.applications.stream()
                .map(DepositApplication::getAmountTtc)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            return this.amountTtc.subtract(totalApplied);
        }

        return this.amountTtc;
    }

    /**
     * Génère une description automatique pour l'acompte si non fournie.
     *
     * @return Description formatée
     */
    public String getFormattedDescription() {
        if (description != null && !description.isBlank()) {
            return description;
        }

        StringBuilder desc = new StringBuilder("Acompte");

        if (customer != null) {
            desc.append(" - Client: ").append(customer.getName());
        }

        if (customerOrderReference != null && !customerOrderReference.isBlank()) {
            desc.append(" - Commande: ").append(customerOrderReference);
        }

        desc.append(" - ").append(amountTtc).append(" XAF TTC");

        return desc.toString();
    }

    // ==================== PHASE 2: MÉTHODES POUR IMPUTATION PARTIELLE ====================

    /**
     * Ajoute une imputation partielle à cet acompte.
     * Met à jour automatiquement amountApplied, amountRemaining et isApplied.
     *
     * @param application L'imputation partielle à ajouter
     */
    public void addApplication(DepositApplication application) {
        if (application == null) {
            throw new IllegalArgumentException("L'application ne peut pas être NULL");
        }

        this.applications.add(application);
        application.setDeposit(this);

        // Mettre à jour les montants
        this.amountApplied = this.amountApplied.add(application.getAmountTtc());
        this.amountRemaining = this.amountTtc.subtract(this.amountApplied);

        // Marquer comme complètement imputé si tout le montant est utilisé
        if (this.amountRemaining.compareTo(BigDecimal.ZERO) == 0) {
            this.isApplied = true;
        }

        log.info("➕ Application de {} XAF ajoutée à l'acompte {} (restant: {} XAF)",
            application.getAmountTtc(), this.depositNumber, this.amountRemaining);
    }

    /**
     * Retire une imputation partielle de cet acompte.
     * Met à jour automatiquement amountApplied, amountRemaining et isApplied.
     *
     * @param application L'imputation partielle à retirer
     */
    public void removeApplication(DepositApplication application) {
        if (application == null) {
            throw new IllegalArgumentException("L'application ne peut pas être NULL");
        }

        this.applications.remove(application);

        // Mettre à jour les montants
        this.amountApplied = this.amountApplied.subtract(application.getAmountTtc());
        this.amountRemaining = this.amountTtc.subtract(this.amountApplied);

        // Démarquer comme complètement imputé
        if (this.amountRemaining.compareTo(BigDecimal.ZERO) > 0) {
            this.isApplied = false;
        }

        log.warn("➖ Application de {} XAF retirée de l'acompte {} (restant: {} XAF)",
            application.getAmountTtc(), this.depositNumber, this.amountRemaining);
    }

    /**
     * Vérifie si l'acompte a des imputations partielles.
     *
     * @return true si au moins une imputation existe
     */
    public boolean hasApplications() {
        return this.applications != null && !this.applications.isEmpty();
    }

    /**
     * Retourne le nombre d'imputations partielles.
     *
     * @return Nombre d'applications
     */
    public int getApplicationCount() {
        return this.applications != null ? this.applications.size() : 0;
    }

    /**
     * Vérifie si l'acompte est partiellement imputé.
     *
     * @return true si partiellement imputé (amountApplied > 0 mais < amountTtc)
     */
    public boolean isPartiallyApplied() {
        return this.amountApplied.compareTo(BigDecimal.ZERO) > 0
            && this.amountRemaining.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Vérifie si l'acompte est complètement imputé.
     *
     * @return true si complètement imputé (amountRemaining == 0)
     */
    public boolean isFullyApplied() {
        return this.amountRemaining.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Retourne le pourcentage d'utilisation de l'acompte.
     *
     * @return Pourcentage (0-100)
     */
    public BigDecimal getUsagePercentage() {
        if (this.amountTtc.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return this.amountApplied
            .multiply(new BigDecimal("100"))
            .divide(this.amountTtc, 2, RoundingMode.HALF_UP);
    }

    /**
     * Recalcule les montants appliqués et restants basé sur les applications.
     * Utile pour la synchronisation après des modifications en masse.
     */
    public void recalculateApplicationAmounts() {
        if (this.applications == null || this.applications.isEmpty()) {
            this.amountApplied = BigDecimal.ZERO;
            this.amountRemaining = this.amountTtc;
            this.isApplied = false;
            return;
        }

        this.amountApplied = this.applications.stream()
            .map(DepositApplication::getAmountTtc)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.amountRemaining = this.amountTtc.subtract(this.amountApplied);
        this.isApplied = this.amountRemaining.compareTo(BigDecimal.ZERO) == 0;

        log.debug("♻️ Recalcul acompte {}: {} XAF appliqués, {} XAF restants",
            this.depositNumber, this.amountApplied, this.amountRemaining);
    }

    /**
     * Retourne le statut lisible de l'acompte.
     *
     * @return Statut ("Disponible", "Partiellement imputé", "Complètement imputé")
     */
    public String getStatus() {
        if (isFullyApplied()) {
            return "Complètement imputé";
        } else if (isPartiallyApplied()) {
            return String.format("Partiellement imputé (%.2f%%)", getUsagePercentage());
        } else {
            return "Disponible";
        }
    }
}
