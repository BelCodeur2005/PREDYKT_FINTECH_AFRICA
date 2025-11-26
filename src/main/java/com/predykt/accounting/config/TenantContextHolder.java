package com.predykt.accounting.config;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-Local Holder pour le contexte Tenant
 * Stocke le tenant actuel pour la requête en cours
 * 
 * Supporte 3 modes :
 * - SHARED : Multi-tenant partagé (PME)
 * - DEDICATED : Mono-tenant dédié (ETI)
 * - CABINET : Hybride cabinet comptable
 */
@Slf4j
public class TenantContextHolder {
    
    private static final ThreadLocal<TenantContext> CONTEXT = new InheritableThreadLocal<>();
    
    /**
     * Définit le contexte tenant pour la requête actuelle
     */
    public static void setContext(TenantContext context) {
        if (context == null) {
            log.warn("⚠️ Tentative de définition d'un contexte tenant null");
            return;
        }
        
        CONTEXT.set(context);
        log.debug("🔐 Contexte tenant défini: Mode={}, Tenant={}, Cabinet={}, Company={}", 
                  context.getMode(), context.getTenantId(), context.getCabinetId(), context.getCompanyId());
    }
    
    /**
     * Récupère le contexte tenant actuel
     */
    public static TenantContext getContext() {
        TenantContext context = CONTEXT.get();
        if (context == null) {
            log.warn("⚠️ Aucun contexte tenant défini pour cette requête");
            throw new IllegalStateException("Contexte tenant non défini");
        }
        return context;
    }
    
    /**
     * Récupère le Tenant ID (peut être company_id, cabinet_id, ou tenant_id selon le mode)
     */
    public static String getTenantId() {
        return getContext().getTenantId();
    }
    
    /**
     * Récupère le Cabinet ID (null en mode SHARED ou DEDICATED)
     */
    public static Long getCabinetId() {
        return getContext().getCabinetId();
    }
    
    /**
     * Récupère le Company ID (null en mode DEDICATED si pas encore contextualisé)
     */
    public static Long getCompanyId() {
        return getContext().getCompanyId();
    }
    
    /**
     * Récupère le mode de déploiement
     */
    public static TenantMode getMode() {
        return getContext().getMode();
    }
    
    /**
     * Nettoie le contexte (à appeler à la fin de chaque requête)
     */
    public static void clear() {
        TenantContext context = CONTEXT.get();
        if (context != null) {
            log.debug("🧹 Nettoyage contexte tenant: {}", context.getTenantId());
        }
        CONTEXT.remove();
    }
    
    /**
     * Vérifie si un contexte est défini
     */
    public static boolean hasContext() {
        return CONTEXT.get() != null;
    }
    
    /**
     * Classe interne : Contexte Tenant
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class TenantContext {
        
        /**
         * Mode de déploiement
         */
        private TenantMode mode;
        
        /**
         * Identifiant unique du tenant (utilisé pour routing BDD en mode DEDICATED)
         */
        private String tenantId;
        
        /**
         * ID du cabinet (mode CABINET uniquement)
         */
        private Long cabinetId;
        
        /**
         * ID de l'entreprise/dossier (tous modes)
         */
        private Long companyId;
        
        /**
         * Sous-domaine utilisé (ex: "eti-x" ou "cabinet-y")
         */
        private String subdomain;
        
        /**
         * Nom d'affichage du tenant
         */
        private String displayName;
    }
    
    /**
     * Enum : Modes de déploiement
     */
    public enum TenantMode {
        /**
         * Mode partagé : Toutes les PME dans 1 instance + 1 BDD
         * Isolation logique par company_id
         */
        SHARED,
        
        /**
         * Mode dédié : 1 instance + 1 BDD par grosse entreprise (ETI)
         * Isolation physique totale
         */
        DEDICATED,
        
        /**
         * Mode cabinet : 1 instance + 1 BDD par cabinet
         * Isolation physique entre cabinets + isolation logique interne par company_id
         */
        CABINET
    }
}