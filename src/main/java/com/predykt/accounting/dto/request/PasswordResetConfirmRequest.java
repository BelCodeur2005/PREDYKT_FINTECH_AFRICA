// ============================================
// PasswordResetConfirmRequest.java
// ============================================
package com.predykt.accounting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetConfirmRequest {
    
    @NotBlank(message = "Le token est obligatoire")
    private String token;
    
    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String newPassword;
}