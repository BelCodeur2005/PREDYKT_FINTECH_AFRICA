package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.UserCompanyAccess;
import com.predykt.accounting.domain.entity.UserCompanyAccessId;
import com.predykt.accounting.domain.enums.AccessLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité UserCompanyAccess (MODE CABINET)
 */
@Repository
public interface UserCompanyAccessRepository extends JpaRepository<UserCompanyAccess, UserCompanyAccessId> {

    /**
     * Trouve tous les accès d'un utilisateur
     */
    @Query("SELECT uca FROM UserCompanyAccess uca WHERE uca.user.id = :userId")
    List<UserCompanyAccess> findByUserId(@Param("userId") Long userId);

    /**
     * Trouve tous les accès pour une entreprise
     */
    @Query("SELECT uca FROM UserCompanyAccess uca WHERE uca.company.id = :companyId")
    List<UserCompanyAccess> findByCompanyId(@Param("companyId") Long companyId);

    /**
     * Trouve l'accès d'un utilisateur à une entreprise
     */
    @Query("SELECT uca FROM UserCompanyAccess uca WHERE uca.user.id = :userId AND uca.company.id = :companyId")
    Optional<UserCompanyAccess> findByUserIdAndCompanyId(
        @Param("userId") Long userId,
        @Param("companyId") Long companyId
    );

    /**
     * Trouve tous les accès d'un utilisateur avec un niveau minimal
     */
    @Query("SELECT uca FROM UserCompanyAccess uca " +
           "WHERE uca.user.id = :userId AND uca.accessLevel IN :levels")
    List<UserCompanyAccess> findByUserIdAndAccessLevelIn(
        @Param("userId") Long userId,
        @Param("levels") List<AccessLevel> levels
    );

    /**
     * Trouve toutes les entreprises accessibles par un utilisateur
     */
    @Query("SELECT uca.company.id FROM UserCompanyAccess uca WHERE uca.user.id = :userId")
    List<Long> findCompanyIdsByUserId(@Param("userId") Long userId);

    /**
     * Trouve toutes les entreprises où l'utilisateur a accès en écriture
     */
    @Query("SELECT uca.company.id FROM UserCompanyAccess uca " +
           "WHERE uca.user.id = :userId " +
           "AND uca.accessLevel IN ('READ_WRITE', 'ADMIN')")
    List<Long> findWritableCompanyIdsByUserId(@Param("userId") Long userId);

    /**
     * Vérifie si un utilisateur a accès à une entreprise
     */
    @Query("SELECT CASE WHEN COUNT(uca) > 0 THEN true ELSE false END " +
           "FROM UserCompanyAccess uca " +
           "WHERE uca.user.id = :userId AND uca.company.id = :companyId")
    boolean hasAccess(@Param("userId") Long userId, @Param("companyId") Long companyId);

    /**
     * Vérifie si un utilisateur peut écrire dans une entreprise
     */
    @Query("SELECT CASE WHEN COUNT(uca) > 0 THEN true ELSE false END " +
           "FROM UserCompanyAccess uca " +
           "WHERE uca.user.id = :userId AND uca.company.id = :companyId " +
           "AND uca.accessLevel IN ('READ_WRITE', 'ADMIN')")
    boolean canWrite(@Param("userId") Long userId, @Param("companyId") Long companyId);

    /**
     * Vérifie si un utilisateur est admin d'une entreprise
     */
    @Query("SELECT CASE WHEN COUNT(uca) > 0 THEN true ELSE false END " +
           "FROM UserCompanyAccess uca " +
           "WHERE uca.user.id = :userId AND uca.company.id = :companyId " +
           "AND uca.accessLevel = 'ADMIN'")
    boolean isAdmin(@Param("userId") Long userId, @Param("companyId") Long companyId);

    /**
     * Compte le nombre d'utilisateurs ayant accès à une entreprise
     */
    @Query("SELECT COUNT(uca) FROM UserCompanyAccess uca WHERE uca.company.id = :companyId")
    Long countUsersByCompanyId(@Param("companyId") Long companyId);

    /**
     * Supprime tous les accès d'un utilisateur
     */
    @Query("DELETE FROM UserCompanyAccess uca WHERE uca.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * Supprime tous les accès pour une entreprise
     */
    @Query("DELETE FROM UserCompanyAccess uca WHERE uca.company.id = :companyId")
    void deleteByCompanyId(@Param("companyId") Long companyId);
}
