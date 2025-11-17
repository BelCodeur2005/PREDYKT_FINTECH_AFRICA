// CompanyCreateRequest.java
package com.predykt.accounting.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompanyCreateRequest {
    
    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    @Size(min = 2, max = 200, message = "Le nom doit contenir entre 2 et 200 caractères")
    private String name;
    
    @Pattern(regexp = "^[A-Z0-9]{10,20}$", message = "Format du numéro fiscal invalide")
    private String taxId;
    
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;
    
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Format de téléphone invalide")
    private String phone;
    
    private String address;
    private String postalCode;
    private String city;
    
    @Pattern(regexp = "^[A-Z]{2}$", message = "Code pays ISO invalide (ex: CM)")
    private String country = "CM";
    
    @Pattern(regexp = "^[A-Z]{3}$", message = "Code devise ISO invalide (ex: XAF)")
    private String currency = "XAF";
    
    private String accountingStandard = "OHADA";
    private String vatNumber;
    private Boolean isVatRegistered = false;
}