package com.predykt.accounting.dto;

import com.predykt.accounting.config.TenantContextHolder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO pour TenantMetadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantMetadataDTO {

    private Long id;

    @NotNull(message = "Le mode tenant est obligatoire")
    private TenantContextHolder.TenantMode tenantMode;

    @NotBlank(message = "L'ID du tenant est obligatoire")
    @Size(max = 100, message = "L'ID du tenant ne peut pas dépasser 100 caractères")
    private String tenantId;

    private Long companyId;

    private String companyName;

    private Long cabinetId;

    private String cabinetName;

    @Size(max = 200, message = "Le nom d'affichage ne peut pas dépasser 200 caractères")
    private String displayName;

    @Size(max = 100, message = "Le sous-domaine ne peut pas dépasser 100 caractères")
    private String subdomain;

    @Size(max = 200, message = "Le domaine personnalisé ne peut pas dépasser 200 caractères")
    private String customDomain;

    private Boolean isActive;

    private Map<String, Object> settings;

    private Boolean isSharedMode;

    private Boolean isDedicatedMode;

    private Boolean isCabinetMode;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
