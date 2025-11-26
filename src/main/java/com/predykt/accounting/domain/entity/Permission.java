package com.predykt.accounting.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entité représentant une permission granulaire dans le système RBAC
 * Utilisée pour définir les droits d'accès aux ressources et actions
 */
@Entity
@Table(name = "permissions", indexes = {
    @Index(name = "idx_permissions_code", columnList = "permission_code", unique = true),
    @Index(name = "idx_permissions_resource", columnList = "resource_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "permission_code", nullable = false, unique = true, length = 100)
    private String permissionCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(nullable = false, length = 50)
    private String action;

    @ManyToMany(mappedBy = "permissions")
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Construit le code de permission à partir de la ressource et de l'action
     * Format: RESOURCE_TYPE:ACTION
     * Exemple: COMPANY:READ, JOURNAL_ENTRY:WRITE, REPORT:EXPORT
     */
    public static String buildPermissionCode(String resourceType, String action) {
        return resourceType.toUpperCase() + ":" + action.toUpperCase();
    }

    /**
     * Vérifie si cette permission permet l'action sur le type de ressource
     */
    public boolean allows(String resourceType, String action) {
        return this.resourceType.equalsIgnoreCase(resourceType) &&
               this.action.equalsIgnoreCase(action);
    }

    /**
     * Vérifie si cette permission est une permission système (non modifiable)
     */
    public boolean isSystemPermission() {
        return isSystem != null && isSystem;
    }

    /**
     * Vérifie si cette permission concerne la lecture
     */
    public boolean isReadPermission() {
        return "READ".equalsIgnoreCase(action) || "VIEW".equalsIgnoreCase(action);
    }

    /**
     * Vérifie si cette permission concerne l'écriture
     */
    public boolean isWritePermission() {
        return "WRITE".equalsIgnoreCase(action) ||
               "CREATE".equalsIgnoreCase(action) ||
               "UPDATE".equalsIgnoreCase(action) ||
               "DELETE".equalsIgnoreCase(action);
    }
}
