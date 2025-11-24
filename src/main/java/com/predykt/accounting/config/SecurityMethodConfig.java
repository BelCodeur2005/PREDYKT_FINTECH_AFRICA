// ============================================
// SecurityMethodConfig.java
// ============================================
package com.predykt.accounting.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Configuration pour activer la sécurité au niveau des méthodes
 * Permet d'utiliser @PreAuthorize, @PostAuthorize, etc.
 */
@Configuration
@EnableMethodSecurity(
    prePostEnabled = true,  // @PreAuthorize, @PostAuthorize
    securedEnabled = true,  // @Secured
    jsr250Enabled = true    // @RolesAllowed
)
public class SecurityMethodConfig {
}