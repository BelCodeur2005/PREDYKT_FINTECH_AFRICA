package com.predykt.accounting.controller;

import com.predykt.accounting.dto.request.*;
import com.predykt.accounting.dto.response.ApiResponse;
import com.predykt.accounting.dto.response.AuthResponse;
import com.predykt.accounting.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur d'authentification - Login, Register, JWT
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Gestion de l'authentification et des tokens JWT")
public class AuthController {
    
    private final AuthenticationService authenticationService;
    
    @PostMapping("/login")
    @Operation(summary = "Connexion",
               description = "Authentifie un utilisateur et retourne un token JWT")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        
        AuthResponse response = authenticationService.login(request);
        
        return ResponseEntity.ok(ApiResponse.success(
            response,
            "Connexion réussie"
        ));
    }
    
    @PostMapping("/register")
    @Operation(summary = "Inscription",
               description = "Crée un nouveau compte utilisateur")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        
        AuthResponse response = authenticationService.register(request);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                response,
                "Compte créé avec succès"
            ));
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "Rafraîchir le token",
               description = "Génère un nouveau access token à partir d'un refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        
        AuthResponse response = authenticationService.refreshToken(request.getRefreshToken());
        
        return ResponseEntity.ok(ApiResponse.success(
            response,
            "Token rafraîchi"
        ));
    }
    
    @PostMapping("/logout")
    @Operation(summary = "Déconnexion",
               description = "Révoque le token JWT (déconnexion)")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader) {
        
        // Extraire le token du header "Bearer <token>"
        String token = authHeader.substring(7);
        
        authenticationService.logout(token);
        
        return ResponseEntity.ok(ApiResponse.success(
            null,
            "Déconnexion réussie"
        ));
    }
    
    @PostMapping("/password/reset-request")
    @Operation(summary = "Demander une réinitialisation de mot de passe",
               description = "Envoie un email avec un lien de réinitialisation")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        
        authenticationService.requestPasswordReset(request.getEmail());
        
        return ResponseEntity.ok(ApiResponse.success(
            null,
            "Email de réinitialisation envoyé"
        ));
    }
    
    @PostMapping("/password/reset-confirm")
    @Operation(summary = "Confirmer la réinitialisation de mot de passe",
               description = "Réinitialise le mot de passe avec le token reçu par email")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        
        authenticationService.resetPassword(request.getToken(), request.getNewPassword());
        
        return ResponseEntity.ok(ApiResponse.success(
            null,
            "Mot de passe réinitialisé avec succès"
        ));
    }
    
    @GetMapping("/validate")
    @Operation(summary = "Valider le token",
               description = "Vérifie si le token JWT est valide (pour debugging)")
    public ResponseEntity<ApiResponse<String>> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        
        // Ce endpoint est automatiquement protégé par le JwtAuthenticationFilter
        // Si on arrive ici, c'est que le token est valide
        
        return ResponseEntity.ok(ApiResponse.success(
            "Token valide",
            "Token JWT valide"
        ));
    }
}