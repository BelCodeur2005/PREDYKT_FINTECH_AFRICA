package com.predykt.accounting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;

/**
 * Service de validation de licence auprès du serveur central
 * Vérifie que le tenant est autorisé à fonctionner (abonnement actif)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantLicenseValidator {
    
    @Value("${TENANT_ID:}")
    private String tenantId;
    
    @Value("${CENTRAL_API_URL:http://central.predykt.com:8000}")
    private String centralApiUrl;
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private LocalDateTime lastValidation;
    private LicenseStatus currentStatus;
    
    /**
     * Validation au démarrage de l'application
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateLicenseOnStartup() {
        // Ne valider que si profil prod-tenant actif
        if (!"prod-tenant".equals(activeProfile)) {
            log.info("⚠️ Mode développement - Validation licence désactivée");
            return;
        }
        
        if (tenantId == null || tenantId.isEmpty()) {
            log.error("❌ TENANT_ID non défini - Impossible de valider la licence");
            System.exit(1);
        }
        
        log.info("🔍 Validation de la licence pour Tenant: {}", tenantId);
        
        boolean isValid = validateLicense();
        
        if (!isValid) {
            log.error("❌ LICENCE INVALIDE - Arrêt de l'application");
            System.exit(1);
        }
        
        log.info("✅ Licence validée avec succès - Application autorisée");
    }
    
    /**
     * Validation quotidienne de la licence (2h du matin)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledLicenseCheck() {
        if (!"prod-tenant".equals(activeProfile)) {
            return;
        }
        
        log.info("🔄 Vérification quotidienne de la licence...");
        
        boolean isValid = validateLicense();
        
        if (!isValid) {
            log.error("❌ Licence expirée ou invalide détectée lors de la vérification quotidienne");
            // En production: bloquer nouvelles opérations ou mode lecture seule
            currentStatus = LicenseStatus.EXPIRED;
        } else {
            log.info("✅ Licence toujours valide");
            currentStatus = LicenseStatus.ACTIVE;
        }
    }
    
    /**
     * Valide la licence auprès du serveur central
     */
    private boolean validateLicense() {
        try {
            String url = String.format("%s/api/v1/tenants/%s/license/validate", 
                                       centralApiUrl, tenantId);
            
            log.debug("📡 Appel API Central: {}", url);
            
            ResponseEntity<LicenseResponse> response = restTemplate.getForEntity(
                url, 
                LicenseResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                LicenseResponse license = response.getBody();
                
                lastValidation = LocalDateTime.now();
                
                log.info("📄 Statut licence: {}", license.getStatus());
                log.info("📅 Plan: {} | Expire le: {}", license.getPlan(), license.getExpiresAt());
                log.info("👥 Utilisateurs: {} / {}", license.getCurrentUsers(), license.getMaxUsers());
                log.info("📊 Transactions: {} / {}", license.getCurrentTransactions(), license.getMaxTransactions());
                
                if ("ACTIVE".equals(license.getStatus())) {
                    currentStatus = LicenseStatus.ACTIVE;
                    return true;
                } else if ("TRIAL".equals(license.getStatus())) {
                    log.warn("⚠️ Licence en période d'essai - Expire le: {}", license.getExpiresAt());
                    currentStatus = LicenseStatus.TRIAL;
                    return true;
                } else if ("EXPIRED".equals(license.getStatus())) {
                    log.error("❌ Licence expirée depuis le: {}", license.getExpiresAt());
                    currentStatus = LicenseStatus.EXPIRED;
                    return false;
                } else if ("SUSPENDED".equals(license.getStatus())) {
                    log.error("❌ Compte suspendu - Raison: {}", license.getSuspensionReason());
                    currentStatus = LicenseStatus.SUSPENDED;
                    return false;
                }
            }
            
            log.error("❌ Réponse invalide du serveur central");
            return false;
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la validation de la licence", e);
            
            // Stratégie de fallback
            if (lastValidation != null && 
                lastValidation.isAfter(LocalDateTime.now().minusDays(7))) {
                log.warn("⚠️ Utilisation de la dernière validation (< 7 jours)");
                return true;
            }
            
            return false;
        }
    }
    
    /**
     * Vérifie si le tenant peut créer une nouvelle transaction
     */
    public boolean canCreateTransaction() {
        if (!"prod-tenant".equals(activeProfile)) {
            return true; // Mode dev: pas de limite
        }
        
        return currentStatus == LicenseStatus.ACTIVE || 
               currentStatus == LicenseStatus.TRIAL;
    }
    
    /**
     * Vérifie si le tenant peut créer un nouvel utilisateur
     */
    public boolean canCreateUser(int currentUserCount) {
        if (!"prod-tenant".equals(activeProfile)) {
            return true;
        }
        
        // Appeler l'API central pour vérifier la limite
        try {
            String url = String.format("%s/api/v1/tenants/%s/license/validate", 
                                       centralApiUrl, tenantId);
            
            ResponseEntity<LicenseResponse> response = restTemplate.getForEntity(
                url, 
                LicenseResponse.class
            );
            
            if (response.getBody() != null) {
                LicenseResponse license = response.getBody();
                return currentUserCount < license.getMaxUsers();
            }
            
        } catch (Exception e) {
            log.error("Erreur vérification limite utilisateurs", e);
        }
        
        return false;
    }
    
    /**
     * Récupère le statut actuel de la licence
     */
    public LicenseStatus getCurrentStatus() {
        return currentStatus != null ? currentStatus : LicenseStatus.UNKNOWN;
    }
    
    /**
     * DTO de réponse de l'API Central
     */
    @lombok.Data
    public static class LicenseResponse {
        private String status;          // ACTIVE, TRIAL, EXPIRED, SUSPENDED
        private String plan;            // STARTER, PROFESSIONAL, ENTERPRISE
        private LocalDateTime expiresAt;
        private Integer maxUsers;
        private Integer currentUsers;
        private Integer maxTransactions;
        private Integer currentTransactions;
        private String suspensionReason;
    }
    
    /**
     * Enum des statuts de licence
     */
    public enum LicenseStatus {
        ACTIVE,
        TRIAL,
        EXPIRED,
        SUSPENDED,
        UNKNOWN
    }
}