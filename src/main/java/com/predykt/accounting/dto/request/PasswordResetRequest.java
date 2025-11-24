// ============================================
// PasswordResetRequest.java
// ============================================
package com.predykt.accounting.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetRequest {
    
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format email invalide")
    private String email;
}
