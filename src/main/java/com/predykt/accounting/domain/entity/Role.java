package com.predykt.accounting.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entité Rôle - Pour le système RBAC (Role-Based Access Control)
 */
@Entity
@Table(name = "roles")
@Data
@EqualsAndHashCode(exclude = {"permissions"})
@ToString(exclude = {"permissions"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @NotBlank(message = "Le nom du rôle est obligatoire")
    @Column(nullable = false, unique = true, length = 50)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Relations
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();
    
    // ========== MÉTHODES MÉTIER ==========
    
    /**
     * Vérifier si le rôle a une permission spécifique
     */
    public boolean hasPermission(String permissionCode) {
        return permissions.stream()
            .anyMatch(permission -> permission.getCode().equals(permissionCode));
    }
    
    /**
     * Ajouter une permission au rôle
     */
    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }
    
    /**
     * Retirer une permission du rôle
     */
    public void removePermission(Permission permission) {
        this.permissions.remove(permission);
    }
}

/**
 * Entité Permission - Permissions granulaires
 */
@Entity
@Table(name = "permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class Permission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String code;
    
    @Column(nullable = false, length = 100)
    private String resource;
    
    @Column(nullable = false, length = 50)
    private String action;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

/**
 * Entité UserSession - Tracking des sessions utilisateurs
 */
@Entity
@Table(name = "user_sessions",
       indexes = {
           @Index(name = "idx_sessions_user", columnList = "user_id"),
           @Index(name = "idx_sessions_active", columnList = "is_active")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class UserSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "session_token", nullable = false, unique = true)
    private String sessionToken;
    
    @Column(name = "ip_address", columnDefinition = "inet")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "device_info")
    private String deviceInfo;
    
    @Column(length = 100)
    private String location;
    
    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();
    
    @Column(name = "last_activity_at", nullable = false)
    @Builder.Default
    private LocalDateTime lastActivityAt = LocalDateTime.now();
    
    @Column(name = "ended_at")
    private LocalDateTime endedAt;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    /**
     * Mettre à jour l'activité de la session
     */
    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }
    
    /**
     * Terminer la session
     */
    public void endSession() {
        this.endedAt = LocalDateTime.now();
        this.isActive = false;
    }
}