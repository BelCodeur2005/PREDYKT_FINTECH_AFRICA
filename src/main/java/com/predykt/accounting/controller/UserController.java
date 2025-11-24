package com.predykt.accounting.controller;

import com.predykt.accounting.dto.request.PasswordChangeRequest;
import com.predykt.accounting.dto.request.UserCreateRequest;
import com.predykt.accounting.dto.request.UserUpdateRequest;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.UserResponse;
import com.predykt.accounting.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur de gestion des utilisateurs
 */
@RestController
@RequestMapping("/companies/{companyId}/users")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs", description = "Gestion des utilisateurs")
public class UserController {
    
    private final UserService userService;
    
    @GetMapping
    @PreAuthorize("hasAuthority('USERS_MANAGE')")
    @Operation(summary = "Lister les utilisateurs",
               description = "Récupère tous les utilisateurs de l'entreprise")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers(
            @PathVariable Long companyId) {
        
        List<UserResponse> users = userService.getUsersByCompany(companyId);
        
        return ResponseEntity.ok(ApiResponse.success(users));
    }
    
    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('USERS_MANAGE') or #userId == authentication.principal.id")
    @Operation(summary = "Obtenir un utilisateur",
               description = "Récupère les détails d'un utilisateur spécifique")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable Long companyId,
            @PathVariable Long userId) {
        
        UserResponse user = userService.getUserById(companyId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(user));
    }
    
    @GetMapping("/me")
    @Operation(summary = "Obtenir mon profil",
               description = "Récupère les informations de l'utilisateur connecté")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @PathVariable Long companyId,
            Authentication authentication) {
        
        Long userId = ((com.predykt.accounting.domain.entity.User) authentication.getPrincipal()).getId();
        UserResponse user = userService.getUserById(companyId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(user));
    }
    
    @PostMapping
    @PreAuthorize("hasAuthority('USERS_MANAGE')")
    @Operation(summary = "Créer un utilisateur",
               description = "Ajoute un nouvel utilisateur à l'entreprise")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @PathVariable Long companyId,
            @Valid @RequestBody UserCreateRequest request) {
        
        UserResponse user = userService.createUser(companyId, request);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                user,
                "Utilisateur créé avec succès"
            ));
    }
    
    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('USERS_MANAGE') or #userId == authentication.principal.id")
    @Operation(summary = "Mettre à jour un utilisateur",
               description = "Modifie les informations d'un utilisateur")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long companyId,
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request) {
        
        UserResponse user = userService.updateUser(companyId, userId, request);
        
        return ResponseEntity.ok(ApiResponse.success(
            user,
            "Utilisateur mis à jour"
        ));
    }
    
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('USERS_MANAGE')")
    @Operation(summary = "Supprimer un utilisateur",
               description = "Désactive un utilisateur (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long companyId,
            @PathVariable Long userId) {
        
        userService.deleteUser(companyId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(
            null,
            "Utilisateur supprimé"
        ));
    }
    
    @PostMapping("/{userId}/change-password")
    @PreAuthorize("#userId == authentication.principal.id")
    @Operation(summary = "Changer le mot de passe",
               description = "Permet à un utilisateur de changer son propre mot de passe")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long companyId,
            @PathVariable Long userId,
            @Valid @RequestBody PasswordChangeRequest request) {
        
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        
        return ResponseEntity.ok(ApiResponse.success(
            null,
            "Mot de passe changé avec succès"
        ));
    }
    
    @PutMapping("/{userId}/activate")
    @PreAuthorize("hasAuthority('USERS_MANAGE')")
    @Operation(summary = "Activer un utilisateur",
               description = "Réactive un utilisateur désactivé")
    public ResponseEntity<ApiResponse<Void>> activateUser(
            @PathVariable Long companyId,
            @PathVariable Long userId) {
        
        userService.activateUser(companyId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(
            null,
            "Utilisateur activé"
        ));
    }
    
    @PutMapping("/{userId}/deactivate")
    @PreAuthorize("hasAuthority('USERS_MANAGE')")
    @Operation(summary = "Désactiver un utilisateur",
               description = "Désactive un utilisateur sans le supprimer")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @PathVariable Long companyId,
            @PathVariable Long userId) {
        
        userService.deactivateUser(companyId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(
            null,
            "Utilisateur désactivé"
        ));
    }
}

