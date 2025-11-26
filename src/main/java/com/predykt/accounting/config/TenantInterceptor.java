package com.predykt.accounting.config;

import com.predykt.accounting.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Intercepteur pour détecter et définir le contexte tenant
 * Supporte 3 modes de détection :
 * 
 * 1. MODE SHARED : Tenant détecté depuis JWT (company_id)
 * 2. MODE DEDICATED : Tenant détecté depuis ENV (TENANT_ID fixe)
 * 3. MODE CABINET : Tenant détecté depuis ENV (TENANT_ID = cabinet_id)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantInterceptor implements HandlerInterceptor {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    @Value("${predykt.tenant.mode:SHARED}")
    private String tenantMode;
    
    @Value("${predykt.tenant.id:}")
    private String configuredTenantId;
    
    @Value("${predykt.tenant.cabinet-id:}")
    private String configuredCabinetId;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            TenantContextHolder.TenantMode mode = TenantContextHolder.TenantMode.valueOf(tenantMode);
            
            TenantContextHolder.TenantContext context = switch (mode) {
                case SHARED -> resolveSharedTenant(request);
                case DEDICATED -> resolveDedicatedTenant(request);
                case CABINET -> resolveCabinetTenant(request);
            };
            
            TenantContextHolder.setContext(context);
            
            return true;
            
        } catch (Exception e) {
            log.error("❌ Erreur résolution tenant", e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        TenantContextHolder.clear();
    }
    
    /**
     * MODE SHARED : Extraire tenant depuis JWT
     */
    private TenantContextHolder.TenantContext resolveSharedTenant(HttpServletRequest request) {
        // Extraire le token JWT
        String jwt = extractJwtFromRequest(request);
        
        Long companyId = null;
        if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
            // Extraire company_id depuis le JWT
            companyId = (Long) jwtTokenProvider.getClaim(jwt, "companyId");
        }
        
        // Si pas de JWT (endpoints publics), on laisse null
        // Le service métier gérera l'erreur si company_id est requis
        
        return TenantContextHolder.TenantContext.builder()
            .mode(TenantContextHolder.TenantMode.SHARED)
            .tenantId("shared")
            .companyId(companyId)
            .subdomain(extractSubdomain(request))
            .displayName("PME Mutualisée")
            .build();
    }
    
    /**
     * MODE DEDICATED : Tenant fixe depuis ENV
     */
    private TenantContextHolder.TenantContext resolveDedicatedTenant(HttpServletRequest request) {
        if (!StringUtils.hasText(configuredTenantId)) {
            throw new IllegalStateException("TENANT_ID non configuré en mode DEDICATED");
        }
        
        // En mode dédié, le tenant est fixe (1 entreprise = 1 instance)
        // Le company_id peut être extrait du JWT si besoin
        String jwt = extractJwtFromRequest(request);
        Long companyId = null;
        
        if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
            companyId = (Long) jwtTokenProvider.getClaim(jwt, "companyId");
        }
        
        return TenantContextHolder.TenantContext.builder()
            .mode(TenantContextHolder.TenantMode.DEDICATED)
            .tenantId(configuredTenantId)
            .companyId(companyId)
            .subdomain(extractSubdomain(request))
            .displayName("ETI Dédiée")
            .build();
    }
    
    /**
     * MODE CABINET : Tenant = cabinet_id depuis ENV
     */
    private TenantContextHolder.TenantContext resolveCabinetTenant(HttpServletRequest request) {
        if (!StringUtils.hasText(configuredCabinetId)) {
            throw new IllegalStateException("CABINET_ID non configuré en mode CABINET");
        }
        
        Long cabinetId = Long.parseLong(configuredCabinetId);
        
        // Extraire company_id depuis JWT (dossier client)
        String jwt = extractJwtFromRequest(request);
        Long companyId = null;
        
        if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
            companyId = (Long) jwtTokenProvider.getClaim(jwt, "companyId");
        }
        
        return TenantContextHolder.TenantContext.builder()
            .mode(TenantContextHolder.TenantMode.CABINET)
            .tenantId("cabinet-" + cabinetId)
            .cabinetId(cabinetId)
            .companyId(companyId)
            .subdomain(extractSubdomain(request))
            .displayName("Cabinet Comptable")
            .build();
    }
    
    /**
     * Extraire le JWT depuis le header Authorization
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
    
    /**
     * Extraire le sous-domaine depuis l'URL
     */
    private String extractSubdomain(HttpServletRequest request) {
        String host = request.getHeader("Host");
        if (host != null && host.contains(".")) {
            return host.split("\\.")[0];
        }
        return "localhost";
    }
}