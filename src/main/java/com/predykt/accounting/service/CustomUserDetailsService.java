package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.User;
import com.predykt.accounting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de chargement des utilisateurs pour Spring Security
 * Implémente UserDetailsService pour l'authentification
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    /**
     * Charge un utilisateur par son email (username)
     * Méthode appelée automatiquement par Spring Security lors de l'authentification
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("🔍 Recherche utilisateur: {}", email);
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> {
                log.warn("❌ Utilisateur non trouvé: {}", email);
                return new UsernameNotFoundException("Utilisateur non trouvé: " + email);
            });
        
        // Vérifier si le compte est actif
        if (!user.getIsActive()) {
            log.warn("⚠️ Tentative de connexion sur compte désactivé: {}", email);
            throw new UsernameNotFoundException("Compte désactivé");
        }
        
        // Vérifier si le compte n'est pas verrouillé
        if (!user.isAccountNonLocked()) {
            log.warn("🔒 Tentative de connexion sur compte verrouillé: {}", email);
            throw new UsernameNotFoundException("Compte temporairement verrouillé");
        }
        
        log.debug("✅ Utilisateur chargé: {} | Rôles: {}", 
                  email, user.getRoles().size());
        
        return user;
    }
    
    /**
     * Charge un utilisateur par son ID
     * Utilisé pour rafraîchir les détails après modification
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        log.debug("🔍 Chargement utilisateur par ID: {}", id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("❌ Utilisateur non trouvé: ID {}", id);
                return new UsernameNotFoundException("Utilisateur non trouvé: " + id);
            });
        
        return user;
    }
}