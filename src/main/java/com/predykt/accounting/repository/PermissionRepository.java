package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository pour l'entité Permission
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * Trouve une permission par son code
     */
    Optional<Permission> findByPermissionCode(String permissionCode);

    /**
     * Trouve toutes les permissions pour un type de ressource
     */
    List<Permission> findByResourceType(String resourceType);

    /**
     * Trouve toutes les permissions pour une action
     */
    List<Permission> findByAction(String action);

    /**
     * Trouve toutes les permissions pour un type de ressource et une action
     */
    Optional<Permission> findByResourceTypeAndAction(String resourceType, String action);

    /**
     * Trouve toutes les permissions système
     */
    List<Permission> findByIsSystemTrue();

    /**
     * Trouve toutes les permissions non-système (modifiables)
     */
    List<Permission> findByIsSystemFalse();

    /**
     * Vérifie si une permission existe par code
     */
    boolean existsByPermissionCode(String permissionCode);

    /**
     * Vérifie si une permission existe pour une ressource et action
     */
    boolean existsByResourceTypeAndAction(String resourceType, String action);

    /**
     * Trouve les permissions par codes
     */
    @Query("SELECT p FROM Permission p WHERE p.permissionCode IN :codes")
    List<Permission> findByPermissionCodeIn(@Param("codes") Set<String> codes);

    /**
     * Trouve toutes les permissions d'un rôle
     */
    @Query("SELECT p FROM Permission p JOIN p.roles r WHERE r.id = :roleId")
    List<Permission> findByRoleId(@Param("roleId") Integer roleId);

    /**
     * Trouve toutes les permissions d'un utilisateur via ses rôles
     */
    @Query("SELECT DISTINCT p FROM Permission p " +
           "JOIN p.roles r " +
           "JOIN r.users u " +
           "WHERE u.id = :userId")
    List<Permission> findByUserId(@Param("userId") Long userId);

    /**
     * Compte les permissions par type de ressource
     */
    @Query("SELECT p.resourceType, COUNT(p) FROM Permission p GROUP BY p.resourceType")
    List<Object[]> countByResourceType();

    /**
     * Trouve les permissions de lecture
     */
    @Query("SELECT p FROM Permission p " +
           "WHERE p.action IN ('READ', 'VIEW') " +
           "ORDER BY p.resourceType, p.action")
    List<Permission> findReadPermissions();

    /**
     * Trouve les permissions d'écriture
     */
    @Query("SELECT p FROM Permission p " +
           "WHERE p.action IN ('WRITE', 'CREATE', 'UPDATE', 'DELETE') " +
           "ORDER BY p.resourceType, p.action")
    List<Permission> findWritePermissions();

    /**
     * Recherche de permissions par nom ou description
     */
    @Query("SELECT p FROM Permission p " +
           "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Permission> searchByNameOrDescription(@Param("searchTerm") String searchTerm);

    /**
     * Trouve les permissions non assignées à aucun rôle
     */
    @Query("SELECT p FROM Permission p WHERE SIZE(p.roles) = 0")
    List<Permission> findUnassignedPermissions();

    /**
     * Compte les rôles utilisant une permission
     */
    @Query("SELECT COUNT(r) FROM Role r JOIN r.permissions p WHERE p.id = :permissionId")
    Long countRolesByPermissionId(@Param("permissionId") Long permissionId);
}
