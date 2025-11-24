// ============================================
// UserCreateRequest.java
// ============================================
package com.predykt.accounting.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class UserCreateRequest {
    
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format email invalide")
    private String email;
    
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;
    
    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;
    
    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;
    
    private String phone;
    
    @NotEmpty(message = "Au moins un rôle est requis")
    private Set<Integer> roleIds;
}