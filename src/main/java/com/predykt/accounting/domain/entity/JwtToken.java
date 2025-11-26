package com.predykt.accounting.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entité représentant un token JWT pour gestion de révocation
 * Utilisée pour blacklister les tokens avant expiration
 */
@Entity
@Table(name = "jwt_tokens", indexes = {
    @Index(name = "idx_jwt_tokens_user", columnList = "user_id"),
    @Index(name = "idx_jwt_tokens_jti", columnList = "jti", unique = true),
    @Index(name = "idx_jwt_tokens_revoked", columnList = "is_revoked"),
    @Index(name = "idx_jwt_tokens_expires", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 36)
    private String jti;

    @Column(name = "token_type", nullable = false, length = 20)
    @Builder.Default
    private String tokenType = "ACCESS";

    @Column(name = "is_revoked", nullable = false)
    @Builder.Default
    private Boolean isRevoked = false;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_reason", length = 200)
    private String revokedReason;

    @Column(name = "issued_at", nullable = false)
    @Builder.Default
    private LocalDateTime issuedAt = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /**
     * Révoque le token
     */
    public void revoke(String reason) {
        this.isRevoked = true;
        this.revokedAt = LocalDateTime.now();
        this.revokedReason = reason;
    }

    /**
     * Vérifie si le token est expiré
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Vérifie si le token est valide (non révoqué et non expiré)
     */
    public boolean isValid() {
        return !isRevoked && !isExpired();
    }

    /**
     * Vérifie si le token est un token d'accès
     */
    public boolean isAccessToken() {
        return "ACCESS".equals(tokenType);
    }

    /**
     * Vérifie si le token est un refresh token
     */
    public boolean isRefreshToken() {
        return "REFRESH".equals(tokenType);
    }
}
