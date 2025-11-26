package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité UserSession
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    /**
     * Trouve une session par son ID
     */
    Optional<UserSession> findBySessionId(String sessionId);

    /**
     * Trouve toutes les sessions d'un utilisateur
     */
    @Query("SELECT us FROM UserSession us WHERE us.user.id = :userId ORDER BY us.createdAt DESC")
    List<UserSession> findByUserId(@Param("userId") Long userId);

    /**
     * Trouve toutes les sessions actives d'un utilisateur
     */
    @Query("SELECT us FROM UserSession us " +
           "WHERE us.user.id = :userId " +
           "AND us.isActive = true " +
           "AND us.expiresAt > :now " +
           "ORDER BY us.lastActivityAt DESC")
    List<UserSession> findActiveSessionsByUserId(
        @Param("userId") Long userId,
        @Param("now") LocalDateTime now
    );

    /**
     * Trouve les sessions inactives depuis X minutes
     */
    @Query("SELECT us FROM UserSession us " +
           "WHERE us.isActive = true " +
           "AND us.lastActivityAt < :inactivityThreshold")
    List<UserSession> findInactiveSessions(@Param("inactivityThreshold") LocalDateTime inactivityThreshold);

    /**
     * Trouve les sessions expirées
     */
    @Query("SELECT us FROM UserSession us " +
           "WHERE us.isActive = true AND us.expiresAt < :now")
    List<UserSession> findExpiredSessions(@Param("now") LocalDateTime now);

    /**
     * Compte les sessions actives d'un utilisateur
     */
    @Query("SELECT COUNT(us) FROM UserSession us " +
           "WHERE us.user.id = :userId " +
           "AND us.isActive = true " +
           "AND us.expiresAt > :now")
    Long countActiveSessionsByUserId(
        @Param("userId") Long userId,
        @Param("now") LocalDateTime now
    );

    /**
     * Termine une session
     */
    @Modifying
    @Query("UPDATE UserSession us " +
           "SET us.isActive = false, us.endedAt = :now, us.endReason = :reason " +
           "WHERE us.sessionId = :sessionId")
    void endSession(
        @Param("sessionId") String sessionId,
        @Param("now") LocalDateTime now,
        @Param("reason") String reason
    );

    /**
     * Termine toutes les sessions d'un utilisateur
     */
    @Modifying
    @Query("UPDATE UserSession us " +
           "SET us.isActive = false, us.endedAt = :now, us.endReason = :reason " +
           "WHERE us.user.id = :userId AND us.isActive = true")
    void endAllUserSessions(
        @Param("userId") Long userId,
        @Param("now") LocalDateTime now,
        @Param("reason") String reason
    );

    /**
     * Termine les sessions inactives
     */
    @Modifying
    @Query("UPDATE UserSession us " +
           "SET us.isActive = false, us.endedAt = :now, us.endReason = 'INACTIVITY_TIMEOUT' " +
           "WHERE us.isActive = true AND us.lastActivityAt < :inactivityThreshold")
    int endInactiveSessions(
        @Param("now") LocalDateTime now,
        @Param("inactivityThreshold") LocalDateTime inactivityThreshold
    );

    /**
     * Termine les sessions expirées
     */
    @Modifying
    @Query("UPDATE UserSession us " +
           "SET us.isActive = false, us.endedAt = :now, us.endReason = 'EXPIRED' " +
           "WHERE us.isActive = true AND us.expiresAt < :now")
    int endExpiredSessions(@Param("now") LocalDateTime now);

    /**
     * Met à jour la dernière activité d'une session
     */
    @Modifying
    @Query("UPDATE UserSession us SET us.lastActivityAt = :now WHERE us.sessionId = :sessionId")
    void updateLastActivity(
        @Param("sessionId") String sessionId,
        @Param("now") LocalDateTime now
    );

    /**
     * Supprime les sessions terminées plus anciennes que N jours
     */
    @Modifying
    @Query("DELETE FROM UserSession us " +
           "WHERE us.isActive = false AND us.endedAt < :cutoffDate")
    void deleteOldEndedSessions(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Trouve les sessions par adresse IP
     */
    @Query("SELECT us FROM UserSession us " +
           "WHERE us.user.id = :userId AND us.ipAddress = :ipAddress " +
           "ORDER BY us.createdAt DESC")
    List<UserSession> findByUserIdAndIpAddress(
        @Param("userId") Long userId,
        @Param("ipAddress") String ipAddress
    );

    /**
     * Trouve les sessions par device
     */
    @Query("SELECT us FROM UserSession us " +
           "WHERE us.user.id = :userId AND us.device = :device " +
           "ORDER BY us.createdAt DESC")
    List<UserSession> findByUserIdAndDevice(
        @Param("userId") Long userId,
        @Param("device") String device
    );

    /**
     * Vérifie si un sessionId existe
     */
    boolean existsBySessionId(String sessionId);

    /**
     * Statistiques des sessions par utilisateur
     */
    @Query("SELECT us.user.id, COUNT(us), " +
           "SUM(CASE WHEN us.isActive = true THEN 1 ELSE 0 END) " +
           "FROM UserSession us " +
           "GROUP BY us.user.id")
    List<Object[]> getSessionStatistics();
}
