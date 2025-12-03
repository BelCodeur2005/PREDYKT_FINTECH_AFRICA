package com.predykt.accounting.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la création d'un fournisseur
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierCreateRequest {

    @NotBlank(message = "Le nom du fournisseur est obligatoire")
    private String name;

    private String taxId;  // Numéro fiscal générique

    /**
     * NIU - Numéro d'Identifiant Unique
     * Détermine le taux AIR: 2,2% si présent, 5,5% si absent
     */
    private String niuNumber;

    @Email(message = "Email invalide")
    private String email;

    private String phone;

    private String address;

    private String city;

    private String country = "CM";  // Cameroun par défaut

    /**
     * Type de fournisseur:
     * - GOODS: Fourniture de marchandises
     * - SERVICES: Prestation de services
     * - RENT: Loueur (soumis à IRPP Loyer 15%)
     * - UTILITIES: Services publics
     */
    private String supplierType;

    private Integer paymentTerms = 30;  // Jours de délai de paiement
}
