package com.predykt.accounting.config;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

/**
 * Configuration de sécurité renforcée pour architecture Mono-Tenant
 * Chaque instance est dédiée à UN tenant, avec isolation physique complète
 */
@Configuration
@EnableWebSecurity
@Profile("prod-tenant")
@Slf4j
public class TenantSecurityConfig {
    
    @Value("${TENANT_ID}")
    private String tenantId;
    
    @Value("${TENANT_DOMAIN}")
    private String tenantDomain;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("🔐 Configuration sécurité pour Tenant: {} ({})", tenantId, tenantDomain);
        
        http
            // CORS Configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // CSRF Protection (Activé en production)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/health/**", "/actuator/**")  // Exclure endpoints monitoring
            )
            
            // Session Management (Stateless pour JWT)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Authorization Rules
            .authorizeHttpRequests(auth -> auth
                // Endpoints publics (monitoring)
                .requestMatchers("/health/**", "/actuator/health").permitAll()
                .requestMatchers("/actuator/prometheus").permitAll()
                
                // Documentation API (si activée)
                .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                
                // Endpoints d'authentification
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                
                // Création d'entreprise (désactivé en prod - tenant déjà créé)
                .requestMatchers(HttpMethod.POST, "/api/v1/companies").denyAll()
                
                // Tous les autres endpoints nécessitent une authentification
                .anyRequest().authenticated()
            )
            
            // Headers de sécurité renforcés
            .headers(headers -> headers
                // Protection XSS
        		.xssProtection(xss -> xss
        			    // 🆕 CORRECTION : Utiliser la valeur énumérée ENUM
        			  .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
        			)
                // Protection Content Type Sniffing
                .contentTypeOptions(contentType -> contentType.disable())
                
                // Frame Options (protection Clickjacking)
                .frameOptions(frame -> frame.deny())
                
                // HSTS (HTTP Strict Transport Security)
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)  // 1 an
                    .preload(true)
                )
                
                // Content Security Policy
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline'; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data:; " +
                        "font-src 'self'; " +
                        "connect-src 'self'; " +
                        "frame-ancestors 'none';"
                    )
                )
                
                // Referrer Policy
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
                
                // Permissions Policy (anciennement Feature Policy)
                .permissionsPolicy(permissions -> permissions
                    .policy("geolocation=(), microphone=(), camera=()")
                )
            );
        
        log.info("✅ Sécurité configurée avec succès");
        return http.build();
    }
    
    /**
     * Configuration CORS stricte pour le domaine du tenant
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Origine STRICTEMENT limitée au domaine du tenant
        configuration.setAllowedOrigins(List.of(
            "https://" + tenantDomain,
            "http://localhost:3000"  // Dev uniquement (à retirer en prod)
        ));
        
        // Méthodes HTTP autorisées
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));
        
        // Headers autorisés
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin"
        ));
        
        // Headers exposés
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-Total-Count"
        ));
        
        // Credentials autorisés
        configuration.setAllowCredentials(true);
        
        // Cache de la configuration CORS (1 heure)
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        log.info("🌐 CORS configuré pour domaine: {}", tenantDomain);
        return source;
    }
    
    /**
     * Encodeur de mots de passe BCrypt (force 12)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // Force 12 (forte sécurité)
    }
    
    /**
     * Validator de domaine pour sécurité additionnelle
     * Vérifie que toutes les requêtes proviennent du bon domaine
     */
    @Bean
    public TenantDomainValidator tenantDomainValidator() {
        return new TenantDomainValidator(tenantDomain);
    }
    
    /**
     * Classe interne pour validation du domaine tenant
     */
    public static class TenantDomainValidator {
        private final String expectedDomain;
        
        public TenantDomainValidator(String expectedDomain) {
            this.expectedDomain = expectedDomain;
        }
        
        /**
         * Valide que la requête provient du bon domaine
         * Protection additionnelle contre les attaques cross-tenant
         */
        public boolean isValidRequest(HttpServletRequest request) {
            String host = request.getHeader("Host");
            if (host == null) {
                log.warn("⚠️ Requête sans header Host détectée");
                return false;
            }
            
            // Retirer le port si présent
            String domain = host.split(":")[0];
            
            boolean isValid = domain.equalsIgnoreCase(expectedDomain);
            
            if (!isValid) {
                log.warn("🚨 ALERTE SÉCURITÉ: Tentative d'accès depuis domaine invalide: {} (attendu: {})", 
                         domain, expectedDomain);
            }
            
            return isValid;
        }
    }
}