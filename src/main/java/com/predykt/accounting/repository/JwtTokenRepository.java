package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.JwtToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité JwtToken
 */
@Repository
public interface JwtTokenRepository extends JpaRepository<JwtToken, Long> {

    /**
     * Trouve un token par son JTI (JWT ID)
     */
    Optional<JwtToken> findByJti(String jti);

    /**
     * Trouve tous les tokens d'un utilisateur
     */
    @Query("SELECT jt FROM JwtToken jt WHERE jt.user.id = :userId ORDER BY jt.issuedAt DESC")
    List<JwtToken> findByUserId(@Param("userId") Long userId);

    /**
     * Trouve tous les tokens valides d'un utilisateur
     */
    @Query("SELECT jt FROM JwtToken jt " +
           "WHERE jt.user.id = :userId " +
           "AND jt.isRevoked = false " +
           "AND jt.expiresAt > :now " +
           "ORDER BY jt.issuedAt DESC")
    List<JwtToken> findValidTokensByUserId(
        @Param("userId") Long userId,
        @Param("now") LocalDateTime now
    );

    /**
     * Trouve les tokens révoqués d'un utilisateur
     */
    @Query("SELECT jt FROM JwtToken jt " +
           "WHERE jt.user.id = :userId AND jt.isRevoked = true " +
           "ORDER BY jt.revokedAt DESC")
    List<JwtToken> findRevokedTokensByUserId(@Param("userId") Long userId);

    /**
     * Vérifie si un token est révoqué
     */
    @Query("SELECT CASE WHEN COUNT(jt) > 0 THEN true ELSE false END " +
           "FROM JwtToken jt WHERE jt.jti = :jti AND jt.isRevoked = true")
    boolean isTokenRevoked(@Param("jti") String jti);

    /**
     * Vérifie si un JTI existe
     */
    boolean existsByJti(String jti);

    /**
     * Révoque tous les tokens d'un utilisateur
     */
    @Modifying
    @Query("UPDATE JwtToken jt SET jt.isRevoked = true, jt.revokedAt = :now, " +
           "jt.revokedReason = :reason " +
           "WHERE jt.user.id = :userId AND jt.isRevoked = false")
    void revokeAllUserTokens(
        @Param("userId") Long userId,
        @Param("now") LocalDateTime now,
        @Param("reason") String reason
    );

    /**
     * Révoque un token spécifique
     */
    @Modifying
    @Query("UPDATE JwtToken jt SET jt.isRevoked = true, jt.revokedAt = :now, " +
           "jt.revokedReason = :reason " +
           "WHERE jt.jti = :jti")
    void revokeToken(
        @Param("jti") String jti,
        @Param("now") LocalDateTime now,
        @Param("reason") String reason
    );

    /**
     * Supprime les tokens expirés
     */
    @Modifying
    @Query("DELETE FROM JwtToken jt WHERE jt.expiresAt < :cutoffDate")
    void deleteExpiredTokens(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Supprime les tokens révoqués plus anciens que N jours
     */
    @Modifying
    @Query("DELETE FROM JwtToken jt " +
           "WHERE jt.isRevoked = true AND jt.revokedAt < :cutoffDate")
    void deleteOldRevokedTokens(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Compte les tokens actifs d'un utilisateur
     */
    @Query("SELECT COUNT(jt) FROM JwtToken jt " +
           "WHERE jt.user.id = :userId " +
           "AND jt.isRevoked = false " +
           "AND jt.expiresAt > :now")
    Long countActiveTokensByUserId(
        @Param("userId") Long userId,
        @Param("now") LocalDateTime now
    );

    /**
     * Trouve les tokens par type
     */
    @Query("SELECT jt FROM JwtToken jt " +
           "WHERE jt.user.id = :userId AND jt.tokenType = :tokenType " +
           "ORDER BY jt.issuedAt DESC")
    List<JwtToken> findByUserIdAndTokenType(
        @Param("userId") Long userId,
        @Param("tokenType") String tokenType
    );

    /**
     * Trouve les tokens par adresse IP
     */
    @Query("SELECT jt FROM JwtToken jt " +
           "WHERE jt.user.id = :userId AND jt.ipAddress = :ipAddress " +
           "ORDER BY jt.issuedAt DESC")
    List<JwtToken> findByUserIdAndIpAddress(
        @Param("userId") Long userId,
        @Param("ipAddress") String ipAddress
    );
}
