package com.predykt.accounting.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Entité représentant un cabinet comptable
 * Utilisée uniquement en MODE CABINET
 */
@Entity
@Table(name = "cabinets", indexes = {
    @Index(name = "idx_cabinets_active", columnList = "is_active"),
    @Index(name = "idx_cabinets_plan", columnList = "plan")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cabinet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "code", unique = true, length = 50)
    private String code;

    @Column(name = "tax_id", unique = true, length = 50)
    private String taxId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(length = 100)
    private String city;

    @Column(nullable = false, length = 2)
    private String country = "CM";

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Limites du plan
    @Column(name = "max_companies", nullable = false)
    private Integer maxCompanies = 10;

    @Column(name = "max_users", nullable = false)
    private Integer maxUsers = 5;

    @Column(length = 50)
    private String plan = "STARTER"; // STARTER, PROFESSIONAL, ENTERPRISE

    // Relations
    @OneToMany(mappedBy = "cabinet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Company> companies = new HashSet<>();

    @OneToMany(mappedBy = "cabinet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<User> users = new HashSet<>();

    @OneToMany(mappedBy = "cabinet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CabinetInvoice> invoices = new HashSet<>();

    // Méthodes utilitaires
    public void addCompany(Company company) {
        companies.add(company);
        company.setCabinet(this);
    }

    public void removeCompany(Company company) {
        companies.remove(company);
        company.setCabinet(null);
    }

    public void addUser(User user) {
        users.add(user);
        user.setCabinet(this);
    }

    public void removeUser(User user) {
        users.remove(user);
        user.setCabinet(null);
    }

    /**
     * Vérifie si le cabinet a atteint sa limite d'entreprises
     */
    public boolean hasReachedCompanyLimit() {
        return companies.size() >= maxCompanies;
    }

    /**
     * Vérifie si le cabinet a atteint sa limite d'utilisateurs
     */
    public boolean hasReachedUserLimit() {
        return users.size() >= maxUsers;
    }

    /**
     * Calcule le taux d'utilisation des dossiers
     */
    public double getCompanyUsagePercentage() {
        if (maxCompanies == 0) return 0;
        return (double) companies.size() / maxCompanies * 100;
    }

    /**
     * Calcule le taux d'utilisation des utilisateurs
     */
    public double getUserUsagePercentage() {
        if (maxUsers == 0) return 0;
        return (double) users.size() / maxUsers * 100;
    }
}
