package com.predykt.accounting.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Entité Utilisateur - Implémente UserDetails pour Spring Security
 */
@Entity
@Table(name = "users", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"email", "company_id"}),
       indexes = {
           @Index(name = "idx_users_company", columnList = "company_id"),
           @Index(name = "idx_users_email", columnList = "email"),
           @Index(name = "idx_users_active", columnList = "is_active")
       })
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"roles", "sessions"})
@ToString(exclude = {"passwordHash", "roles", "sessions"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format email invalide")
    @Column(nullable = false, length = 100)
    private String email;
    
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @Column(name = "first_name", length = 100)
    private String firstName;
    
    @Column(name = "last_name", length = 100)
    private String lastName;
    
    @Column(length = 20)
    private String phone;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "is_email_verified", nullable = false)
    @Builder.Default
    private Boolean isEmailVerified = false;
    
    @Column(name = "email_verification_token")
    private String emailVerificationToken;
    
    @Column(name = "password_reset_token")
    private String passwordResetToken;
    
    @Column(name = "password_reset_expires_at")
    private LocalDateTime passwordResetExpiresAt;
    
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @Column(name = "failed_login_attempts")
    @Builder.Default
    private Integer failedLoginAttempts = 0;
    
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
    
    // Relations
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UserSession> sessions = new HashSet<>();
    
    // ========== IMPLÉMENTATION UserDetails ==========
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(permission -> new SimpleGrantedAuthority(permission.getCode()))
            .collect(Collectors.toSet());
    }
    
    @Override
    public String getPassword() {
        return passwordHash;
    }
    
    @Override
    public String getUsername() {
        return email;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return lockedUntil == null || lockedUntil.isBefore(LocalDateTime.now());
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return isActive && isEmailVerified;
    }
    
    // ========== MÉTHODES MÉTIER ==========
    
    /**
     * Obtenir le nom complet
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return email;
    }
    
    /**
     * Vérifier si l'utilisateur a un rôle spécifique
     */
    public boolean hasRole(String roleName) {
        return roles.stream()
            .anyMatch(role -> role.getName().equals(roleName));
    }
    
    /**
     * Vérifier si l'utilisateur a une permission spécifique
     */
    public boolean hasPermission(String permissionCode) {
        return roles.stream()
            .flatMap(role -> role.getPermissions().stream())
            .anyMatch(permission -> permission.getCode().equals(permissionCode));
    }
    
    /**
     * Incrémenter les tentatives de connexion échouées
     */
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(30);
        }
    }
    
    /**
     * Réinitialiser les tentatives de connexion échouées
     */
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }
    
    /**
     * Enregistrer la connexion réussie
     */
    public void recordSuccessfulLogin() {
        this.lastLoginAt = LocalDateTime.now();
        resetFailedLoginAttempts();
    }
}