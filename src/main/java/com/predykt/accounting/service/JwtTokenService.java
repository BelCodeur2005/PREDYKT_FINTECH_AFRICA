package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.JwtToken;
import com.predykt.accounting.domain.entity.User;
import com.predykt.accounting.repository.JwtTokenRepository;
import com.predykt.accounting.repository.UserRepository;
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
 * Service pour la gestion des tokens JWT (révocation, nettoyage)
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class JwtTokenService {

    private final JwtTokenRepository tokenRepository;
    private final UserRepository userRepository;

    /**
     * Enregistre un nouveau token
     */
    public JwtToken createToken(Long userId, String jti, String tokenType,
                                LocalDateTime expiresAt, String ipAddress, String userAgent) {
        log.info("Enregistrement d'un token {} pour l'utilisateur {}", tokenType, userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé avec l'ID: " + userId));

        JwtToken token = JwtToken.builder()
            .user(user)
            .jti(jti)
            .tokenType(tokenType)
            .expiresAt(expiresAt)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();

        return tokenRepository.save(token);
    }

    /**
     * Génère un JTI unique
     */
    public String generateJti() {
        String jti;
        do {
            jti = UUID.randomUUID().toString();
        } while (tokenRepository.existsByJti(jti));
        return jti;
    }

    /**
     * Récupère un token par JTI
     */
    @Transactional(readOnly = true)
    public JwtToken getTokenByJti(String jti) {
        return tokenRepository.findByJti(jti)
            .orElseThrow(() -> new EntityNotFoundException("Token non trouvé avec le JTI: " + jti));
    }

    /**
     * Récupère tous les tokens d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<JwtToken> getUserTokens(Long userId) {
        return tokenRepository.findByUserId(userId);
    }

    /**
     * Récupère tous les tokens valides d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<JwtToken> getValidUserTokens(Long userId) {
        return tokenRepository.findValidTokensByUserId(userId, LocalDateTime.now());
    }

    /**
     * Récupère les tokens révoqués d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<JwtToken> getRevokedUserTokens(Long userId) {
        return tokenRepository.findRevokedTokensByUserId(userId);
    }

    /**
     * Vérifie si un token est révoqué
     */
    @Transactional(readOnly = true)
    public boolean isTokenRevoked(String jti) {
        return tokenRepository.isTokenRevoked(jti);
    }

    /**
     * Révoque un token spécifique
     */
    public void revokeToken(String jti, String reason) {
        log.info("Révocation du token {} - Raison: {}", jti, reason);
        tokenRepository.revokeToken(jti, LocalDateTime.now(), reason);
    }

    /**
     * Révoque tous les tokens d'un utilisateur
     */
    public void revokeAllUserTokens(Long userId, String reason) {
        log.info("Révocation de tous les tokens de l'utilisateur {} - Raison: {}", userId, reason);
        tokenRepository.revokeAllUserTokens(userId, LocalDateTime.now(), reason);
    }

    /**
     * Compte les tokens actifs d'un utilisateur
     */
    @Transactional(readOnly = true)
    public Long countActiveUserTokens(Long userId) {
        return tokenRepository.countActiveTokensByUserId(userId, LocalDateTime.now());
    }

    /**
     * Récupère les tokens par type
     */
    @Transactional(readOnly = true)
    public List<JwtToken> getTokensByType(Long userId, String tokenType) {
        return tokenRepository.findByUserIdAndTokenType(userId, tokenType);
    }

    /**
     * Récupère les tokens par adresse IP
     */
    @Transactional(readOnly = true)
    public List<JwtToken> getTokensByIpAddress(Long userId, String ipAddress) {
        return tokenRepository.findByUserIdAndIpAddress(userId, ipAddress);
    }

    /**
     * Révoque tous les tokens d'un utilisateur sauf le token actuel
     */
    public void revokeAllUserTokensExcept(Long userId, String currentJti, String reason) {
        log.info("Révocation de tous les tokens de l'utilisateur {} sauf {}", userId, currentJti);

        List<JwtToken> tokens = getValidUserTokens(userId);
        LocalDateTime now = LocalDateTime.now();

        tokens.stream()
            .filter(token -> !token.getJti().equals(currentJti))
            .forEach(token -> {
                token.revoke(reason);
                tokenRepository.save(token);
            });
    }

    /**
     * Nettoyage automatique des tokens expirés (tous les jours à 3h du matin)
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredTokens() {
        log.info("Début du nettoyage des tokens expirés");

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        tokenRepository.deleteExpiredTokens(cutoffDate);

        log.info("Nettoyage des tokens expirés terminé");
    }

    /**
     * Nettoyage automatique des tokens révoqués (tous les dimanches à 4h du matin)
     */
    @Scheduled(cron = "0 0 4 * * SUN")
    public void cleanupRevokedTokens() {
        log.info("Début du nettoyage des tokens révoqués");

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        tokenRepository.deleteOldRevokedTokens(cutoffDate);

        log.info("Nettoyage des tokens révoqués terminé");
    }

    /**
     * Vérifie si un token est valide (non révoqué et non expiré)
     */
    @Transactional(readOnly = true)
    public boolean isTokenValid(String jti) {
        return tokenRepository.findByJti(jti)
            .map(JwtToken::isValid)
            .orElse(false);
    }

    /**
     * Vérifie si un JTI existe
     */
    @Transactional(readOnly = true)
    public boolean jtiExists(String jti) {
        return tokenRepository.existsByJti(jti);
    }
}
