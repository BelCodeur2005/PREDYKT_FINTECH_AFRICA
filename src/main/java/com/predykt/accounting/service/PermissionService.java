package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.Permission;
import com.predykt.accounting.domain.entity.Role;
import com.predykt.accounting.repository.PermissionRepository;
import com.predykt.accounting.repository.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Service pour la gestion des permissions (RBAC granulaire)
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    /**
     * Crée une nouvelle permission
     */
    public Permission createPermission(Permission permission) {
        log.info("Création d'une permission: {}", permission.getPermissionCode());

        // Vérifier l'unicité du code
        if (permissionRepository.existsByPermissionCode(permission.getPermissionCode())) {
            throw new IllegalArgumentException("Une permission avec ce code existe déjà");
        }

        // Vérifier l'unicité ressource + action
        if (permissionRepository.existsByResourceTypeAndAction(
                permission.getResourceType(), permission.getAction())) {
            throw new IllegalArgumentException(
                "Une permission existe déjà pour cette ressource et action");
        }

        return permissionRepository.save(permission);
    }

    /**
     * Crée une permission avec génération automatique du code
     */
    public Permission createPermission(String name, String resourceType, String action, String description) {
        String code = Permission.buildPermissionCode(resourceType, action);

        Permission permission = Permission.builder()
            .permissionCode(code)
            .name(name)
            .description(description)
            .resourceType(resourceType.toUpperCase())
            .action(action.toUpperCase())
            .isSystem(false)
            .build();

        return createPermission(permission);
    }

    /**
     * Met à jour une permission
     */
    public Permission updatePermission(Long permissionId, Permission updatedPermission) {
        log.info("Mise à jour de la permission ID: {}", permissionId);

        Permission permission = getPermissionById(permissionId);

        // Ne pas permettre la modification d'une permission système
        if (permission.isSystemPermission()) {
            throw new IllegalStateException("Impossible de modifier une permission système");
        }

        permission.setName(updatedPermission.getName());
        permission.setDescription(updatedPermission.getDescription());

        return permissionRepository.save(permission);
    }

    /**
     * Récupère une permission par ID
     */
    @Transactional(readOnly = true)
    public Permission getPermissionById(Long permissionId) {
        return permissionRepository.findById(permissionId)
            .orElseThrow(() -> new EntityNotFoundException("Permission non trouvée avec l'ID: " + permissionId));
    }

    /**
     * Récupère une permission par code
     */
    @Transactional(readOnly = true)
    public Permission getPermissionByCode(String code) {
        return permissionRepository.findByPermissionCode(code)
            .orElseThrow(() -> new EntityNotFoundException("Permission non trouvée avec le code: " + code));
    }

    /**
     * Récupère toutes les permissions
     */
    @Transactional(readOnly = true)
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    /**
     * Récupère les permissions par type de ressource
     */
    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByResourceType(String resourceType) {
        return permissionRepository.findByResourceType(resourceType);
    }

    /**
     * Récupère les permissions par action
     */
    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByAction(String action) {
        return permissionRepository.findByAction(action);
    }

    /**
     * Récupère les permissions système
     */
    @Transactional(readOnly = true)
    public List<Permission> getSystemPermissions() {
        return permissionRepository.findByIsSystemTrue();
    }

    /**
     * Récupère les permissions modifiables (non-système)
     */
    @Transactional(readOnly = true)
    public List<Permission> getCustomPermissions() {
        return permissionRepository.findByIsSystemFalse();
    }

    /**
     * Récupère toutes les permissions d'un rôle
     */
    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByRole(Integer roleId) {
        return permissionRepository.findByRoleId(roleId);
    }

    /**
     * Récupère toutes les permissions d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByUser(Long userId) {
        return permissionRepository.findByUserId(userId);
    }

    /**
     * Récupère les permissions par codes
     */
    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByCodes(Set<String> codes) {
        return permissionRepository.findByPermissionCodeIn(codes);
    }

    /**
     * Recherche de permissions par nom ou description
     */
    @Transactional(readOnly = true)
    public List<Permission> searchPermissions(String searchTerm) {
        return permissionRepository.searchByNameOrDescription(searchTerm);
    }

    /**
     * Trouve les permissions non assignées à aucun rôle
     */
    @Transactional(readOnly = true)
    public List<Permission> getUnassignedPermissions() {
        return permissionRepository.findUnassignedPermissions();
    }

    /**
     * Trouve les permissions de lecture
     */
    @Transactional(readOnly = true)
    public List<Permission> getReadPermissions() {
        return permissionRepository.findReadPermissions();
    }

    /**
     * Trouve les permissions d'écriture
     */
    @Transactional(readOnly = true)
    public List<Permission> getWritePermissions() {
        return permissionRepository.findWritePermissions();
    }

    /**
     * Assigne une permission à un rôle
     */
    public void assignPermissionToRole(Long permissionId, Integer roleId) {
        log.info("Assignation de la permission {} au rôle {}", permissionId, roleId);

        Permission permission = getPermissionById(permissionId);
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new EntityNotFoundException("Rôle non trouvé avec l'ID: " + roleId));

        role.addPermission(permission);
        roleRepository.save(role);
    }

    /**
     * Retire une permission d'un rôle
     */
    public void removePermissionFromRole(Long permissionId, Integer roleId) {
        log.info("Retrait de la permission {} du rôle {}", permissionId, roleId);

        Permission permission = getPermissionById(permissionId);
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new EntityNotFoundException("Rôle non trouvé avec l'ID: " + roleId));

        role.removePermission(permission);
        roleRepository.save(role);
    }

    /**
     * Compte les rôles utilisant une permission
     */
    @Transactional(readOnly = true)
    public Long countRolesUsingPermission(Long permissionId) {
        return permissionRepository.countRolesByPermissionId(permissionId);
    }

    /**
     * Supprime une permission
     */
    public void deletePermission(Long permissionId) {
        log.info("Suppression de la permission ID: {}", permissionId);

        Permission permission = getPermissionById(permissionId);

        // Ne pas permettre la suppression d'une permission système
        if (permission.isSystemPermission()) {
            throw new IllegalStateException("Impossible de supprimer une permission système");
        }

        // Vérifier qu'elle n'est utilisée par aucun rôle
        if (!permission.getRoles().isEmpty()) {
            throw new IllegalStateException(
                String.format("La permission est utilisée par %d rôle(s)", permission.getRoles().size())
            );
        }

        permissionRepository.delete(permission);
    }

    /**
     * Vérifie si une permission existe par code
     */
    @Transactional(readOnly = true)
    public boolean permissionExists(String code) {
        return permissionRepository.existsByPermissionCode(code);
    }

    /**
     * Vérifie si une permission existe pour une ressource et action
     */
    @Transactional(readOnly = true)
    public boolean permissionExists(String resourceType, String action) {
        return permissionRepository.existsByResourceTypeAndAction(resourceType, action);
    }
}
