package com.predykt.accounting.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entité représentant une session utilisateur active
 * Utilisée pour le suivi des connexions et la gestion multi-sessions
 */
@Entity
@Table(name = "user_sessions", indexes = {
    @Index(name = "idx_user_sessions_user", columnList = "user_id"),
    @Index(name = "idx_user_sessions_session_id", columnList = "session_id", unique = true),
    @Index(name = "idx_user_sessions_active", columnList = "is_active"),
    @Index(name = "idx_user_sessions_expires", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_id", nullable = false, unique = true, length = 100)
    private String sessionId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(length = 100)
    private String device;

    @Column(length = 100)
    private String location;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_activity_at", nullable = false)
    @Builder.Default
    private LocalDateTime lastActivityAt = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "end_reason", length = 100)
    private String endReason;

    /**
     * Met à jour la dernière activité
     */
    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }

    /**
     * Termine la session
     */
    public void end(String reason) {
        this.isActive = false;
        this.endedAt = LocalDateTime.now();
        this.endReason = reason;
    }

    /**
     * Vérifie si la session est expirée
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Vérifie si la session est inactive depuis plus de X minutes
     */
    public boolean isInactiveSince(int minutes) {
        return LocalDateTime.now().isAfter(lastActivityAt.plusMinutes(minutes));
    }

    /**
     * Vérifie si la session est valide (active et non expirée)
     */
    public boolean isValid() {
        return isActive && !isExpired();
    }

    /**
     * Prolonge la session
     */
    public void extend(int additionalMinutes) {
        this.expiresAt = expiresAt.plusMinutes(additionalMinutes);
        updateActivity();
    }
}
