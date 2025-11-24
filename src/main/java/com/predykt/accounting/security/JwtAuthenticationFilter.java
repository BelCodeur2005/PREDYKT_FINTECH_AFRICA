package com.predykt.accounting.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.predykt.accounting.service.CustomUserDetailsService;

import java.io.IOException;

/**
 * Filtre JWT - Intercepte toutes les requêtes pour valider le token JWT
 * S'exécute une seule fois par requête (OncePerRequestFilter)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) 
            throws ServletException, IOException {
        
        try {
            // Extraire le token JWT de la requête
            String jwt = extractJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt)) {
                // Valider le token
                if (tokenProvider.validateToken(jwt)) {
                    // Valider que le token appartient au bon tenant (sécurité mono-tenant)
                    if (!tokenProvider.validateTenantContext(jwt)) {
                        log.warn("🚨 Token invalide pour ce tenant - Accès refusé");
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.getWriter().write("{\"error\":\"Token invalide pour ce tenant\"}");
                        return;
                    }
                    
                    // Extraire l'email de l'utilisateur
                    String username = tokenProvider.getUsernameFromToken(jwt);
                    
                    // Charger les détails complets de l'utilisateur
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    
                    // Créer l'authentification Spring Security
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, 
                            null, 
                            userDetails.getAuthorities()
                        );
                    
                    authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    
                    // Injecter l'authentification dans le contexte Spring Security
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("✅ Utilisateur authentifié: {}", username);
                }
            }
        } catch (Exception ex) {
            log.error("❌ Erreur lors de l'authentification JWT", ex);
        }
        
        // Continuer la chaîne de filtres
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extrait le token JWT du header Authorization
     * Format attendu: "Bearer <token>"
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Retirer "Bearer "
        }
        
        return null;
    }
    
    /**
     * Ne pas appliquer ce filtre sur certains endpoints publics
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Endpoints publics (pas besoin de token)
        return path.startsWith("/api/v1/auth/") ||
               path.startsWith("/health") ||
               path.startsWith("/actuator") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/api-docs");
    }
}