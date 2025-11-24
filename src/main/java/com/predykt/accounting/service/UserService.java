package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.Role;
import com.predykt.accounting.domain.entity.User;
import com.predykt.accounting.dto.request.UserCreateRequest;
import com.predykt.accounting.dto.request.UserUpdateRequest;
import com.predykt.accounting.dto.response.UserResponse;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.mapper.UserMapper;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.repository.RoleRepository;
import com.predykt.accounting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service de gestion des utilisateurs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final TenantLicenseValidator licenseValidator;
    
    /**
     * Créer un utilisateur
     */
    @Transactional
    public UserResponse createUser(Long companyId, UserCreateRequest request) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));
        
        // Vérifier si l'email existe déjà pour cette company
        if (userRepository.existsByEmailAndCompany(request.getEmail(), company)) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
        }
        
        // Vérifier la limite d'utilisateurs (license)
        long currentUserCount = userRepository.findByCompany(company).size();
        if (!licenseValidator.canCreateUser((int) currentUserCount)) {
            throw new IllegalArgumentException("Limite d'utilisateurs atteinte pour votre plan");
        }
        
        // Récupérer les rôles
        Set<Role> roles = request.getRoleIds().stream()
            .map(roleId -> roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle non trouvé: " + roleId)))
            .collect(Collectors.toSet());
        
        // Créer l'utilisateur
        User user = User.builder()
            .company(company)
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phone(request.getPhone())
            .isActive(true)
            .isEmailVerified(false)
            .roles(roles)
            .build();
        
        user = userRepository.save(user);
        
        log.info("✅ Utilisateur créé: {} (ID: {})", user.getEmail(), user.getId());
        
        return userMapper.toResponse(user);
    }
    
    /**
     * Récupérer un utilisateur par ID
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long companyId, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        // Vérifier que l'utilisateur appartient à la company
        if (!user.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Utilisateur n'appartient pas à cette entreprise");
        }
        
        return userMapper.toResponse(user);
    }
    
    /**
     * Lister les utilisateurs d'une company
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByCompany(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));
        
        return userRepository.findByCompany(company).stream()
            .map(userMapper::toResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Mettre à jour un utilisateur
     */
    @Transactional
    public UserResponse updateUser(Long companyId, Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        // Vérifier que l'utilisateur appartient à la company
        if (!user.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Utilisateur n'appartient pas à cette entreprise");
        }
        
        // Mettre à jour les champs
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        
        // Mettre à jour les rôles si spécifiés
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            Set<Role> roles = request.getRoleIds().stream()
                .map(roleId -> roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle non trouvé: " + roleId)))
                .collect(Collectors.toSet());
            user.setRoles(roles);
        }
        
        user = userRepository.save(user);
        
        log.info("✅ Utilisateur mis à jour: {} (ID: {})", user.getEmail(), user.getId());
        
        return userMapper.toResponse(user);
    }
    
    /**
     * Désactiver un utilisateur
     */
    @Transactional
    public void deactivateUser(Long companyId, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        // Vérifier que l'utilisateur appartient à la company
        if (!user.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Utilisateur n'appartient pas à cette entreprise");
        }
        
        user.setIsActive(false);
        userRepository.save(user);
        
        log.info("🚫 Utilisateur désactivé: {} (ID: {})", user.getEmail(), user.getId());
    }
    
    /**
     * Supprimer un utilisateur (soft delete via désactivation)
     */
    @Transactional
    public void deleteUser(Long companyId, Long userId) {
        deactivateUser(companyId, userId);
    }
    
    /**
     * Changer le mot de passe d'un utilisateur
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        // Vérifier l'ancien mot de passe
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Ancien mot de passe incorrect");
        }
        
        // Changer le mot de passe
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.resetFailedLoginAttempts();
        
        userRepository.save(user);
        
        log.info("🔑 Mot de passe changé pour: {}", user.getEmail());
    }
}




