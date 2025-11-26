package com.predykt.accounting.security;

import com.predykt.accounting.domain.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provider JWT - Génération et validation des tokens
 */
@Component
@Slf4j
public class JwtTokenProvider {
    
    @Value("${spring.security.jwt.secret}")
    private String jwtSecret;
    
    @Value("${spring.security.jwt.expiration:86400000}") // 24h par défaut
    private long jwtExpirationMs;
    
    @Value("${TENANT_ID:}")
    private String tenantId;
    
    @Value("${TENANT_DOMAIN:}")
    private String tenantDomain;
    
    /**
     * Génère un token JWT pour un utilisateur authentifié
     */
    public String generateToken(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return generateToken(user);
    }
    
    /**
     * Génère un token JWT pour un utilisateur
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("companyId", user.getCompany().getId());
        claims.put("companyName", user.getCompany().getName());
        claims.put("tenantId", tenantId);
        claims.put("tenantDomain", tenantDomain);
        claims.put("roles", user.getRoles().stream()
            .map(role -> role.getName())
            .collect(Collectors.toList()));
        claims.put("permissions", user.getAuthorities().stream()
            .map(auth -> auth.getAuthority())
            .collect(Collectors.toList()));
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(user.getEmail())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .setIssuer("PREDYKT")
            .setAudience(tenantDomain)
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact();
    }
    
    /**
     * Génère un refresh token (validité 7 jours)
     */
    public String generateRefreshToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (7 * 24 * 60 * 60 * 1000)); // 7 jours
        
        return Jwts.builder()
            .setSubject(user.getEmail())
            .claim("userId", user.getId())
            .claim("type", "REFRESH")
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact();
    }
    
    /**
     * Extrait l'email (username) du token
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
        
        return claims.getSubject();
    }
    
    /**
     * Extrait l'ID utilisateur du token
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
        
        return claims.get("userId", Long.class);
    }
    
    /**
     * Extrait le Tenant ID du token
     */
    public String getTenantIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
        
        return claims.get("tenantId", String.class);
    }
    
    /**
     * Valide le token JWT
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            
            return true;
            
        } catch (SecurityException ex) {
            log.error("Signature JWT invalide");
        } catch (MalformedJwtException ex) {
            log.error("Token JWT malformé");
        } catch (ExpiredJwtException ex) {
            log.error("Token JWT expiré");
        } catch (UnsupportedJwtException ex) {
            log.error("Token JWT non supporté");
        } catch (IllegalArgumentException ex) {
            log.error("Claims JWT vide");
        }
        
        return false;
    }
    
    /**
     * Valide que le token appartient bien au tenant actuel
     */
    public boolean validateTenantContext(String token) {
        try {
            String tokenTenantId = getTenantIdFromToken(token);
            
            if (tenantId != null && !tenantId.isEmpty()) {
                boolean isValid = tenantId.equals(tokenTenantId);
                
                if (!isValid) {
                    log.warn("🚨 ALERTE SÉCURITÉ: Tentative d'utilisation d'un token d'un autre tenant");
                    log.warn("Tenant actuel: {} | Token tenant: {}", tenantId, tokenTenantId);
                }
                
                return isValid;
            }
            
            // En dev (pas de tenant), accepter tous les tokens
            return true;
            
        } catch (Exception e) {
            log.error("Erreur validation contexte tenant", e);
            return false;
        }
    }
    
    /**
     * Extrait la date d'expiration du token
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
        
        return claims.getExpiration();
    }
    
    /**
     * Obtient la clé de signature sécurisée
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Ajouter méthode pour extraire claims custom
   public Object getClaim(String token, String claimName) {
       Claims claims = Jwts.parserBuilder()
           .setSigningKey(getSigningKey())
           .build()
           .parseClaimsJws(token)
           .getBody();
       return claims.get(claimName);
   }
}