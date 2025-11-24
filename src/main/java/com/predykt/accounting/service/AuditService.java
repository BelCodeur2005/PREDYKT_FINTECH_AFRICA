package com.predykt.accounting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.predykt.accounting.domain.entity.AuditLog;
import com.predykt.accounting.domain.entity.User;
import com.predykt.accounting.domain.enums.AuditAction;
import com.predykt.accounting.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service d'audit - Traçabilité complète des actions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    
    private final AuditLogRepository auditRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Enregistre une action d'audit (asynchrone pour ne pas ralentir les transactions)
     */
    @Async
    @Transactional
    public void logAction(String entityType, Long entityId, AuditAction action, 
                         Object oldValue, Object newValue, String changes) {
        try {
            // Récupérer l'utilisateur connecté
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = null;
            Long userId = null;
            String username = "system";
            
            if (authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal())) {
                currentUser = (User) authentication.getPrincipal();
                userId = currentUser.getId();
                username = currentUser.getEmail();
            }
            
            // Récupérer les informations de la requête HTTP
            String ipAddress = null;
            String userAgent = null;
            
            try {
                ServletRequestAttributes attributes = 
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
                HttpServletRequest request = attributes.getRequest();
                
                ipAddress = getClientIpAddress(request);
                userAgent = request.getHeader("User-Agent");
            } catch (IllegalStateException e) {
                // Pas de contexte de requête (ex: tâche planifiée)
                log.debug("Pas de contexte HTTP pour l'audit");
            }
            
            // Sérialiser les valeurs en JSON
            String oldValueJson = null;
            String newValueJson = null;
            
            if (oldValue != null) {
                oldValueJson = objectMapper.writeValueAsString(oldValue);
            }
            
            if (newValue != null) {
                newValueJson = objectMapper.writeValueAsString(newValue);
            }
            
            // Créer l'entrée d'audit
            AuditLog auditLog = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .userId(userId)
                .username(username)
                .oldValue(oldValueJson)
                .newValue(newValueJson)
                .changes(changes)
                .ipAddress(ipAddress)
                .userAgent(userAgent != null && userAgent.length() > 255 
                    ? userAgent.substring(0, 255) : userAgent)
                .build();
            
            auditRepository.save(auditLog);
            
            log.debug("✅ Action auditée: {} {} par {}", action, entityType, username);
            
        } catch (JsonProcessingException e) {
            log.error("❌ Erreur sérialisation JSON pour audit", e);
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'audit", e);
        }
    }
    
    /**
     * Enregistre une création d'entité
     */
    public void logCreate(String entityType, Long entityId, Object newValue) {
        logAction(entityType, entityId, AuditAction.CREATE, null, newValue, 
                  "Création de " + entityType);
    }
    
    /**
     * Enregistre une modification d'entité
     */
    public void logUpdate(String entityType, Long entityId, Object oldValue, 
                         Object newValue, String changes) {
        logAction(entityType, entityId, AuditAction.UPDATE, oldValue, newValue, changes);
    }
    
    /**
     * Enregistre une suppression d'entité
     */
    public void logDelete(String entityType, Long entityId, Object oldValue) {
        logAction(entityType, entityId, AuditAction.DELETE, oldValue, null, 
                  "Suppression de " + entityType);
    }
    
    /**
     * Enregistre un verrouillage
     */
    public void logLock(String entityType, Long entityId) {
        logAction(entityType, entityId, AuditAction.LOCK, null, null, 
                  "Verrouillage de " + entityType);
    }
    
    /**
     * Enregistre une réconciliation
     */
    public void logReconcile(String entityType, Long entityId, String details) {
        logAction(entityType, entityId, AuditAction.RECONCILE, null, null, details);
    }
    
    /**
     * Enregistre un import de données
     */
    public void logImport(String entityType, int recordCount, String source) {
        logAction(entityType, null, AuditAction.IMPORT, null, null, 
                  String.format("Import de %d %s depuis %s", recordCount, entityType, source));
    }
    
    /**
     * Récupère l'historique d'audit d'une entité
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditTrail(String entityType, Long entityId) {
        return auditRepository.findAuditTrail(entityType, entityId);
    }
    
    /**
     * Récupère l'historique d'audit d'une entité (paginé)
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditTrailPaginated(String entityType, Long entityId, Pageable pageable) {
        return auditRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
    }
    
    /**
     * Récupère les actions d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getUserActions(Long userId) {
        return auditRepository.findByUserId(userId);
    }
    
    /**
     * Récupère les actions par type
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getActionsByType(AuditAction action) {
        return auditRepository.findByAction(action);
    }
    
    /**
     * Récupère les actions sur une période
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getActionsInPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        return auditRepository.findByTimestampBetween(startDate, endDate);
    }
    
    /**
     * Nettoie les anciens logs d'audit (conformité RGPD)
     * À exécuter périodiquement (ex: tous les mois)
     */
    @Transactional
    public void cleanOldAuditLogs(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        
        log.info("🧹 Nettoyage des logs d'audit avant le {}", cutoffDate);
        
        auditRepository.deleteByTimestampBefore(cutoffDate);
        
        log.info("✅ Logs d'audit nettoyés");
    }
    
    /**
     * Extrait l'adresse IP réelle du client (gère les proxies)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Si plusieurs IPs (proxy chain), prendre la première
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }
}