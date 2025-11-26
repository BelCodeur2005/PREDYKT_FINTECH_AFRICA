package com.predykt.accounting.controller.cabinet;

import com.predykt.accounting.domain.entity.UserCompanyAccess;
import com.predykt.accounting.domain.enums.AccessLevel;
import com.predykt.accounting.dto.UserCompanyAccessDTO;
import com.predykt.accounting.service.UserCompanyAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST pour la gestion des accès utilisateurs aux dossiers (MODE CABINET)
 */
@RestController
@RequestMapping("/api/user-company-access")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('CABINET_MANAGER') or hasRole('ADMIN')")
public class UserCompanyAccessController {

    private final UserCompanyAccessService accessService;

    /**
     * Accorde l'accès à un dossier client
     */
    @PostMapping
    public ResponseEntity<UserCompanyAccess> grantAccess(@Valid @RequestBody UserCompanyAccessDTO dto) {
        log.info("Octroi d'accès {} à l'utilisateur {} pour l'entreprise {}",
            dto.getAccessLevel(), dto.getUserId(), dto.getCompanyId());

        UserCompanyAccess access = accessService.grantAccess(
            dto.getUserId(),
            dto.getCompanyId(),
            dto.getAccessLevel()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(access);
    }

    /**
     * Révoque l'accès d'un utilisateur à un dossier
     */
    @DeleteMapping("/user/{userId}/company/{companyId}")
    public ResponseEntity<Void> revokeAccess(
            @PathVariable Long userId,
            @PathVariable Long companyId) {
        log.info("Révocation de l'accès de l'utilisateur {} pour l'entreprise {}", userId, companyId);

        accessService.revokeAccess(userId, companyId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Récupère tous les accès d'un utilisateur
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('CABINET_MANAGER') or @securityService.isCurrentUser(#userId)")
    public ResponseEntity<List<UserCompanyAccess>> getUserAccesses(@PathVariable Long userId) {
        List<UserCompanyAccess> accesses = accessService.getUserAccesses(userId);
        return ResponseEntity.ok(accesses);
    }

    /**
     * Récupère tous les accès pour un dossier client
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<UserCompanyAccess>> getCompanyAccesses(@PathVariable Long companyId) {
        List<UserCompanyAccess> accesses = accessService.getCompanyAccesses(companyId);
        return ResponseEntity.ok(accesses);
    }

    /**
     * Récupère l'accès d'un utilisateur à un dossier
     */
    @GetMapping("/user/{userId}/company/{companyId}")
    public ResponseEntity<UserCompanyAccess> getAccess(
            @PathVariable Long userId,
            @PathVariable Long companyId) {
        UserCompanyAccess access = accessService.getAccess(userId, companyId);
        return ResponseEntity.ok(access);
    }

    /**
     * Récupère toutes les entreprises accessibles par un utilisateur
     */
    @GetMapping("/user/{userId}/companies")
    @PreAuthorize("hasRole('CABINET_MANAGER') or @securityService.isCurrentUser(#userId)")
    public ResponseEntity<List<Long>> getAccessibleCompanies(@PathVariable Long userId) {
        List<Long> companyIds = accessService.getAccessibleCompanyIds(userId);
        return ResponseEntity.ok(companyIds);
    }

    /**
     * Récupère toutes les entreprises où l'utilisateur a accès en écriture
     */
    @GetMapping("/user/{userId}/companies/writable")
    @PreAuthorize("hasRole('CABINET_MANAGER') or @securityService.isCurrentUser(#userId)")
    public ResponseEntity<List<Long>> getWritableCompanies(@PathVariable Long userId) {
        List<Long> companyIds = accessService.getWritableCompanyIds(userId);
        return ResponseEntity.ok(companyIds);
    }

    /**
     * Vérifie si un utilisateur a accès à un dossier
     */
    @GetMapping("/user/{userId}/company/{companyId}/has-access")
    public ResponseEntity<Boolean> hasAccess(
            @PathVariable Long userId,
            @PathVariable Long companyId) {
        boolean hasAccess = accessService.hasAccess(userId, companyId);
        return ResponseEntity.ok(hasAccess);
    }

    /**
     * Vérifie si un utilisateur peut écrire dans un dossier
     */
    @GetMapping("/user/{userId}/company/{companyId}/can-write")
    public ResponseEntity<Boolean> canWrite(
            @PathVariable Long userId,
            @PathVariable Long companyId) {
        boolean canWrite = accessService.canWrite(userId, companyId);
        return ResponseEntity.ok(canWrite);
    }

    /**
     * Vérifie si un utilisateur est admin d'un dossier
     */
    @GetMapping("/user/{userId}/company/{companyId}/is-admin")
    public ResponseEntity<Boolean> isAdmin(
            @PathVariable Long userId,
            @PathVariable Long companyId) {
        boolean isAdmin = accessService.isAdmin(userId, companyId);
        return ResponseEntity.ok(isAdmin);
    }

    /**
     * Promeut un utilisateur à un niveau d'accès supérieur
     */
    @PutMapping("/user/{userId}/company/{companyId}/promote")
    public ResponseEntity<UserCompanyAccess> promoteAccess(
            @PathVariable Long userId,
            @PathVariable Long companyId) {
        log.info("Promotion de l'accès de l'utilisateur {} pour l'entreprise {}", userId, companyId);

        UserCompanyAccess updated = accessService.promoteAccess(userId, companyId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Rétrograde un utilisateur à un niveau d'accès inférieur
     */
    @PutMapping("/user/{userId}/company/{companyId}/demote")
    public ResponseEntity<UserCompanyAccess> demoteAccess(
            @PathVariable Long userId,
            @PathVariable Long companyId) {
        log.info("Rétrogradation de l'accès de l'utilisateur {} pour l'entreprise {}", userId, companyId);

        UserCompanyAccess updated = accessService.demoteAccess(userId, companyId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Copie les accès d'un utilisateur vers un autre
     */
    @PostMapping("/copy/from/{sourceUserId}/to/{targetUserId}")
    public ResponseEntity<List<UserCompanyAccess>> copyAccesses(
            @PathVariable Long sourceUserId,
            @PathVariable Long targetUserId) {
        log.info("Copie des accès de l'utilisateur {} vers l'utilisateur {}", sourceUserId, targetUserId);

        List<UserCompanyAccess> accesses = accessService.copyAccesses(sourceUserId, targetUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(accesses);
    }

    /**
     * Révoque tous les accès d'un utilisateur
     */
    @DeleteMapping("/user/{userId}/all")
    public ResponseEntity<Void> revokeAllUserAccesses(@PathVariable Long userId) {
        log.info("Révocation de tous les accès de l'utilisateur {}", userId);

        accessService.revokeAllUserAccesses(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Révoque tous les accès pour un dossier client
     */
    @DeleteMapping("/company/{companyId}/all")
    public ResponseEntity<Void> revokeAllCompanyAccesses(@PathVariable Long companyId) {
        log.info("Révocation de tous les accès pour l'entreprise {}", companyId);

        accessService.revokeAllCompanyAccesses(companyId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Compte le nombre d'utilisateurs ayant accès à un dossier
     */
    @GetMapping("/company/{companyId}/users/count")
    public ResponseEntity<Long> countUsers(@PathVariable Long companyId) {
        Long count = accessService.countUsersByCompany(companyId);
        return ResponseEntity.ok(count);
    }

    /**
     * Récupère les accès avec niveau minimum
     */
    @GetMapping("/user/{userId}/minimum-level/{level}")
    public ResponseEntity<List<UserCompanyAccess>> getAccessesWithMinimumLevel(
            @PathVariable Long userId,
            @PathVariable AccessLevel level) {
        List<UserCompanyAccess> accesses = accessService.getAccessesWithMinimumLevel(userId, level);
        return ResponseEntity.ok(accesses);
    }
}
