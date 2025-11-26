package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.User;
import com.predykt.accounting.domain.entity.UserSession;
import com.predykt.accounting.repository.UserRepository;
import com.predykt.accounting.repository.UserSessionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service pour la gestion des sessions utilisateurs
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserSessionService {

    private final UserSessionRepository sessionRepository;
    private final UserRepository userRepository;

    // Configuration: durée de session par défaut (8 heures)
    private static final int DEFAULT_SESSION_DURATION_HOURS = 8;
    // Configuration: timeout d'inactivité (30 minutes)
    private static final int INACTIVITY_TIMEOUT_MINUTES = 30;

    /**
     * Crée une nouvelle session
     */
    public UserSession createSession(Long userId, String ipAddress, String userAgent,
                                     String device, String location) {
        log.info("Création d'une session pour l'utilisateur {} depuis {}", userId, ipAddress);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé avec l'ID: " + userId));

        String sessionId = generateSessionId();

        UserSession session = UserSession.builder()
            .user(user)
            .sessionId(sessionId)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .device(device)
            .location(location)
            .expiresAt(LocalDateTime.now().plusHours(DEFAULT_SESSION_DURATION_HOURS))
            .build();

        return sessionRepository.save(session);
    }

    /**
     * Génère un ID de session unique
     */
    public String generateSessionId() {
        String sessionId;
        do {
            sessionId = UUID.randomUUID().toString();
        } while (sessionRepository.existsBySessionId(sessionId));
        return sessionId;
    }

    /**
     * Récupère une session par ID
     */
    @Transactional(readOnly = true)
    public UserSession getSessionById(String sessionId) {
        return sessionRepository.findBySessionId(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("Session non trouvée avec l'ID: " + sessionId));
    }

    /**
     * Récupère toutes les sessions d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<UserSession> getUserSessions(Long userId) {
        return sessionRepository.findByUserId(userId);
    }

    /**
     * Récupère toutes les sessions actives d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<UserSession> getActiveUserSessions(Long userId) {
        return sessionRepository.findActiveSessionsByUserId(userId, LocalDateTime.now());
    }

    /**
     * Compte les sessions actives d'un utilisateur
     */
    @Transactional(readOnly = true)
    public Long countActiveUserSessions(Long userId) {
        return sessionRepository.countActiveSessionsByUserId(userId, LocalDateTime.now());
    }

    /**
     * Met à jour la dernière activité d'une session
     */
    public void updateSessionActivity(String sessionId) {
        sessionRepository.updateLastActivity(sessionId, LocalDateTime.now());
    }

    /**
     * Prolonge une session
     */
    public UserSession extendSession(String sessionId, int additionalMinutes) {
        log.info("Prolongation de la session {} de {} minutes", sessionId, additionalMinutes);

        UserSession session = getSessionById(sessionId);
        session.extend(additionalMinutes);

        return sessionRepository.save(session);
    }

    /**
     * Termine une session
     */
    public void endSession(String sessionId, String reason) {
        log.info("Fin de la session {} - Raison: {}", sessionId, reason);
        sessionRepository.endSession(sessionId, LocalDateTime.now(), reason);
    }

    /**
     * Termine toutes les sessions d'un utilisateur
     */
    public void endAllUserSessions(Long userId, String reason) {
        log.info("Fin de toutes les sessions de l'utilisateur {} - Raison: {}", userId, reason);
        sessionRepository.endAllUserSessions(userId, LocalDateTime.now(), reason);
    }

    /**
     * Termine toutes les sessions d'un utilisateur sauf la session actuelle
     */
    public void endAllUserSessionsExcept(Long userId, String currentSessionId, String reason) {
        log.info("Fin de toutes les sessions de l'utilisateur {} sauf {}", userId, currentSessionId);

        List<UserSession> sessions = getActiveUserSessions(userId);
        LocalDateTime now = LocalDateTime.now();

        sessions.stream()
            .filter(session -> !session.getSessionId().equals(currentSessionId))
            .forEach(session -> {
                session.end(reason);
                sessionRepository.save(session);
            });
    }

    /**
     * Récupère les sessions par adresse IP
     */
    @Transactional(readOnly = true)
    public List<UserSession> getSessionsByIpAddress(Long userId, String ipAddress) {
        return sessionRepository.findByUserIdAndIpAddress(userId, ipAddress);
    }

    /**
     * Récupère les sessions par device
     */
    @Transactional(readOnly = true)
    public List<UserSession> getSessionsByDevice(Long userId, String device) {
        return sessionRepository.findByUserIdAndDevice(userId, device);
    }

    /**
     * Vérifie si une session est valide
     */
    @Transactional(readOnly = true)
    public boolean isSessionValid(String sessionId) {
        return sessionRepository.findBySessionId(sessionId)
            .map(UserSession::isValid)
            .orElse(false);
    }

    /**
     * Vérifie si un sessionId existe
     */
    @Transactional(readOnly = true)
    public boolean sessionExists(String sessionId) {
        return sessionRepository.existsBySessionId(sessionId);
    }

    /**
     * Nettoyage automatique des sessions expirées (toutes les heures)
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredSessions() {
        log.info("Début du nettoyage des sessions expirées");

        int count = sessionRepository.endExpiredSessions(LocalDateTime.now());

        log.info("Nettoyage des sessions expirées terminé - {} sessions fermées", count);
    }

    /**
     * Nettoyage automatique des sessions inactives (toutes les 30 minutes)
     */
    @Scheduled(cron = "0 */30 * * * *")
    public void cleanupInactiveSessions() {
        log.info("Début du nettoyage des sessions inactives");

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(INACTIVITY_TIMEOUT_MINUTES);
        int count = sessionRepository.endInactiveSessions(LocalDateTime.now(), threshold);

        log.info("Nettoyage des sessions inactives terminé - {} sessions fermées", count);
    }

    /**
     * Suppression des anciennes sessions terminées (tous les dimanches à 5h du matin)
     */
    @Scheduled(cron = "0 0 5 * * SUN")
    public void cleanupOldEndedSessions() {
        log.info("Début de la suppression des anciennes sessions terminées");

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
        sessionRepository.deleteOldEndedSessions(cutoffDate);

        log.info("Suppression des anciennes sessions terminées terminée");
    }

    /**
     * Récupère les statistiques des sessions
     */
    @Transactional(readOnly = true)
    public List<Object[]> getSessionStatistics() {
        return sessionRepository.getSessionStatistics();
    }

    /**
     * Récupère les sessions inactives
     */
    @Transactional(readOnly = true)
    public List<UserSession> getInactiveSessions(int inactivityMinutes) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(inactivityMinutes);
        return sessionRepository.findInactiveSessions(threshold);
    }

    /**
     * Récupère les sessions expirées
     */
    @Transactional(readOnly = true)
    public List<UserSession> getExpiredSessions() {
        return sessionRepository.findExpiredSessions(LocalDateTime.now());
    }
}
