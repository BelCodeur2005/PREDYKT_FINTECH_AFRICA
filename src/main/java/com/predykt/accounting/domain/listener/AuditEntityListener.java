package com.predykt.accounting.domain.listener;

import com.predykt.accounting.domain.entity.BaseEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

/**
 * Listener JPA pour l'audit automatique des entites
 * Remplit automatiquement les champs createdAt, updatedAt, createdBy, updatedBy
 *
 * Ce listener est utilise en complement de Spring Data JPA AuditingEntityListener
 * pour fournir un comportement d'audit personnalise
 */
@Slf4j
public class AuditEntityListener {

    /**
     * Callback appele avant la persistence d'une nouvelle entite
     * Remplit les champs createdAt et createdBy
     */
    @PrePersist
    public void setCreatedOn(BaseEntity entity) {
        LocalDateTime now = LocalDateTime.now();
        String currentUser = getCurrentAuditor();

        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }

        if (entity.getCreatedBy() == null) {
            entity.setCreatedBy(currentUser);
        }

        // Les champs updated sont egalement remplis lors de la creation
        if (entity.getUpdatedAt() == null) {
            entity.setUpdatedAt(now);
        }

        if (entity.getUpdatedBy() == null) {
            entity.setUpdatedBy(currentUser);
        }

        log.debug("Audit creation - Entite: {}, Utilisateur: {}, Date: {}",
            entity.getClass().getSimpleName(), currentUser, now);
    }

    /**
     * Callback appele avant la mise a jour d'une entite existante
     * Met a jour les champs updatedAt et updatedBy
     */
    @PreUpdate
    public void setUpdatedOn(BaseEntity entity) {
        LocalDateTime now = LocalDateTime.now();
        String currentUser = getCurrentAuditor();

        entity.setUpdatedAt(now);
        entity.setUpdatedBy(currentUser);

        log.debug("Audit modification - Entite: {}, Utilisateur: {}, Date: {}",
            entity.getClass().getSimpleName(), currentUser, now);
    }

    /**
     * Recupere l'utilisateur actuellement authentifie
     * Ordre de priorite:
     * 1. Utilisateur du SecurityContext (JWT)
     * 2. "system" par defaut
     */
    private String getCurrentAuditor() {
        // Essayer de recuperer depuis SecurityContext
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {

                String username = authentication.getName();
                if (username != null && !username.isEmpty()) {
                    return username;
                }
            }
        } catch (Exception e) {
            log.trace("Impossible de recuperer l'utilisateur depuis SecurityContext: {}", e.getMessage());
        }

        // Valeur par defaut
        return "system";
    }
}
