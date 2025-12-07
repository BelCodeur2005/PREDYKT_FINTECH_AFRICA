package com.predykt.accounting.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Entité représentant un client
 * Gère le NIU (Numéro d'Identifiant Unique) pour la déduction de TVA
 * IMPORTANT: Cette entité est OPTIONNELLE - le système fonctionne sans elle
 */
@Entity
@Table(name = "customers", indexes = {
    @Index(name = "idx_customers_company", columnList = "company_id"),
    @Index(name = "idx_customers_niu", columnList = "niu_number"),
    @Index(name = "idx_customers_active", columnList = "is_active")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @NotBlank(message = "Le nom du client est obligatoire")
    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "tax_id", length = 50)
    private String taxId;  // Numéro fiscal générique

    /**
     * NIU - Numéro d'Identifiant Unique
     * Important pour la conformité TVA et les exports
     */
    @Column(name = "niu_number", length = 50)
    private String niuNumber;

    @Column(name = "has_niu", nullable = false)
    @Builder.Default
    private Boolean hasNiu = false;

    @Email(message = "Email invalide")
    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 2)
    @Builder.Default
    private String country = "CM";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Type de client:
     * - RETAIL: Détail
     * - WHOLESALE: Grossiste
     * - EXPORT: Export international
     * - GOVERNMENT: Administrations publiques
     */
    @Column(name = "customer_type", length = 50)
    private String customerType;

    @Column(name = "payment_terms")
    @Builder.Default
    private Integer paymentTerms = 30;  // Jours de délai de paiement

    /**
     * Limite de crédit autorisée
     */
    @Column(name = "credit_limit", precision = 15, scale = 2)
    private java.math.BigDecimal creditLimit;

    /**
     * Vérifie si le client est un client export (peut bénéficier d'exonération TVA)
     */
    public boolean isExportCustomer() {
        return "EXPORT".equalsIgnoreCase(customerType);
    }

    /**
     * Sous-compte auxiliaire OHADA (4111001, 4111002...)
     * Auto-généré lors de la création du client
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auxiliary_account_id")
    private ChartOfAccounts auxiliaryAccount;

    /**
     * Vérifie si le client a un NIU valide
     */
    public boolean hasValidNiu() {
        return hasNiu && niuNumber != null && !niuNumber.trim().isEmpty();
    }

    /**
     * Récupère le numéro de compte auxiliaire
     */
    public String getAuxiliaryAccountNumber() {
        return auxiliaryAccount != null ? auxiliaryAccount.getAccountNumber() : null;
    }

    /**
     * Active le flag NIU si le numéro est renseigné
     */
    @PrePersist
    @PreUpdate
    private void checkNiu() {
        if (niuNumber != null && !niuNumber.trim().isEmpty()) {
            this.hasNiu = true;
        } else {
            this.hasNiu = false;
        }
    }
}
