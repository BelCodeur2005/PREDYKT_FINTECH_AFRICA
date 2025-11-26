package com.predykt.accounting.domain.entity;

import com.predykt.accounting.config.TenantContextHolder;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entité représentant les métadonnées d'un tenant
 * Utilisée pour les 3 modes: SHARED, DEDICATED, CABINET
 */
@Entity
@Table(name = "tenant_metadata", indexes = {
    @Index(name = "idx_tenant_metadata_mode", columnList = "tenant_mode"),
    @Index(name = "idx_tenant_metadata_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenant_mode", nullable = false, length = 20)
    private TenantContextHolder.TenantMode tenantMode;

    @Column(name = "tenant_id", nullable = false, unique = true, length = 100)
    private String tenantId;

    // Pour MODE DEDICATED
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    // Pour MODE CABINET
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cabinet_id")
    private Cabinet cabinet;

    // Métadonnées
    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(length = 100)
    private String subdomain;

    @Column(name = "custom_domain", length = 200)
    private String customDomain;

    // Configuration
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> settings;

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
     * Vérifie si c'est un tenant en mode SHARED
     */
    public boolean isSharedMode() {
        return tenantMode == TenantContextHolder.TenantMode.SHARED;
    }

    /**
     * Vérifie si c'est un tenant en mode DEDICATED
     */
    public boolean isDedicatedMode() {
        return tenantMode == TenantContextHolder.TenantMode.DEDICATED;
    }

    /**
     * Vérifie si c'est un tenant en mode CABINET
     */
    public boolean isCabinetMode() {
        return tenantMode == TenantContextHolder.TenantMode.CABINET;
    }

    /**
     * Récupère un paramètre de configuration
     */
    @SuppressWarnings("unchecked")
    public <T> T getSetting(String key, T defaultValue) {
        if (settings == null || !settings.containsKey(key)) {
            return defaultValue;
        }
        return (T) settings.get(key);
    }

    /**
     * Définit un paramètre de configuration
     */
    public void setSetting(String key, Object value) {
        if (settings == null) {
            settings = new java.util.HashMap<>();
        }
        settings.put(key, value);
    }
}
