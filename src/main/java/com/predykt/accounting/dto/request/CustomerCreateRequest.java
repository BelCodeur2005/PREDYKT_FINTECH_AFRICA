package com.predykt.accounting.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerCreateRequest {

    @NotBlank(message = "Le nom du client est obligatoire")
    @Size(min = 2, max = 200, message = "Le nom doit contenir entre 2 et 200 caractères")
    private String name;

    @Size(max = 50, message = "Le numéro fiscal ne peut dépasser 50 caractères")
    private String taxId;

    @Size(max = 50, message = "Le NIU ne peut dépasser 50 caractères")
    private String niuNumber;

    @Email(message = "Format d'email invalide")
    @Size(max = 100, message = "L'email ne peut dépasser 100 caractères")
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Format de téléphone invalide")
    private String phone;

    private String address;

    @Size(max = 100, message = "La ville ne peut dépasser 100 caractères")
    private String city;

    @Pattern(regexp = "^[A-Z]{2}$", message = "Code pays ISO invalide (ex: CM)")
    private String country = "CM";

    /**
     * Type de client: RETAIL, WHOLESALE, EXPORT, GOVERNMENT
     */
    @Pattern(regexp = "^(RETAIL|WHOLESALE|EXPORT|GOVERNMENT)$", message = "Type de client invalide")
    private String customerType;

    private Integer paymentTerms = 30;  // Jours de délai de paiement

    private BigDecimal creditLimit;  // Limite de crédit autorisée
}
