package com.predykt.accounting.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Entité représentant un fournisseur
 * Gère le NIU (Numéro d'Identifiant Unique) pour le calcul de l'AIR (précompte)
 */
@Entity
@Table(name = "suppliers", indexes = {
    @Index(name = "idx_suppliers_company", columnList = "company_id"),
    @Index(name = "idx_suppliers_niu", columnList = "niu_number"),
    @Index(name = "idx_suppliers_active", columnList = "is_active")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @NotBlank(message = "Le nom du fournisseur est obligatoire")
    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "tax_id", length = 50)
    private String taxId;  // Numéro fiscal générique

    /**
     * NIU - Numéro d'Identifiant Unique
     * CRUCIAL pour le calcul de l'AIR (précompte):
     * - Si NIU présent: AIR = 2,2%
     * - Si NIU absent: AIR = 5,5% (pénalité) + alerte
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
     * Type de fournisseur:
     * - GOODS: Fourniture de marchandises
     * - SERVICES: Prestation de services
     * - RENT: Loueur (soumis à IRPP Loyer 15%)
     * - UTILITIES: Services publics (eau, électricité)
     */
    @Column(name = "supplier_type", length = 50)
    private String supplierType;

    @Column(name = "payment_terms")
    @Builder.Default
    private Integer paymentTerms = 30;  // Jours de délai de paiement

    /**
     * Vérifie si le fournisseur est un loueur (soumis à IRPP Loyer 15%)
     */
    public boolean isRentSupplier() {
        return "RENT".equalsIgnoreCase(supplierType);
    }

    /**
     * Vérifie si le fournisseur a un NIU valide
     */
    public boolean hasValidNiu() {
        return hasNiu && niuNumber != null && !niuNumber.trim().isEmpty();
    }

    /**
     * Retourne le taux AIR applicable selon le NIU
     */
    public java.math.BigDecimal getApplicableAirRate() {
        return hasValidNiu()
            ? new java.math.BigDecimal("2.2")
            : new java.math.BigDecimal("5.5");
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
