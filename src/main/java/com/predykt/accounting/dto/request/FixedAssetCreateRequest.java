package com.predykt.accounting.dto.request;

import com.predykt.accounting.domain.enums.AssetCategory;
import com.predykt.accounting.domain.enums.DepreciationMethod;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Requête de création d'une immobilisation
 * Conforme OHADA et fiscalité camerounaise (CGI)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedAssetCreateRequest {

    // === IDENTIFICATION ===

    @NotBlank(message = "Le numéro d'immobilisation est obligatoire")
    @Size(max = 50, message = "Le numéro ne peut dépasser 50 caractères")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "Le numéro doit contenir uniquement des lettres majuscules, chiffres et tirets")
    private String assetNumber; // Ex: IMM-2024-001

    @NotBlank(message = "Le nom de l'immobilisation est obligatoire")
    @Size(max = 255, message = "Le nom ne peut dépasser 255 caractères")
    private String assetName; // Ex: Véhicule Renault Duster

    @Size(max = 2000, message = "La description ne peut dépasser 2000 caractères")
    private String description;

    // === CLASSIFICATION OHADA ===

    @NotNull(message = "La catégorie est obligatoire")
    private AssetCategory category;

    @NotBlank(message = "Le numéro de compte OHADA est obligatoire")
    @Pattern(regexp = "^2[1-6][0-9]{0,2}$", message = "Le compte doit être de la classe 2 (immobilisations)")
    private String accountNumber; // Ex: 2410, 245, 22, 211

    // === FOURNISSEUR ===

    @Size(max = 200, message = "Le nom du fournisseur ne peut dépasser 200 caractères")
    private String supplierName;

    @Size(max = 100, message = "Le numéro de facture ne peut dépasser 100 caractères")
    private String invoiceNumber;

    // === ACQUISITION ===

    @NotNull(message = "La date d'acquisition est obligatoire")
    @PastOrPresent(message = "La date d'acquisition ne peut être dans le futur")
    private LocalDate acquisitionDate;

    @NotNull(message = "Le coût d'acquisition est obligatoire")
    @DecimalMin(value = "1.00", message = "Le coût d'acquisition doit être supérieur à 0")
    @Digits(integer = 18, fraction = 2, message = "Le coût d'acquisition ne peut avoir plus de 2 décimales")
    private BigDecimal acquisitionCost; // Montant HT

    @DecimalMin(value = "0.00", message = "La TVA ne peut être négative")
    @Digits(integer = 18, fraction = 2, message = "La TVA ne peut avoir plus de 2 décimales")
    private BigDecimal acquisitionVat; // TVA sur acquisition (généralement 19,25% au Cameroun)

    @DecimalMin(value = "0.00", message = "Les frais d'installation ne peuvent être négatifs")
    @Digits(integer = 18, fraction = 2, message = "Les frais d'installation ne peuvent avoir plus de 2 décimales")
    private BigDecimal installationCost; // Frais d'installation, transport, mise en service

    // === AMORTISSEMENT ===

    @NotNull(message = "La méthode d'amortissement est obligatoire")
    private DepreciationMethod depreciationMethod;

    @NotNull(message = "La durée de vie utile est obligatoire")
    @Min(value = 1, message = "La durée de vie doit être d'au moins 1 an")
    @Max(value = 50, message = "La durée de vie ne peut dépasser 50 ans")
    private Integer usefulLifeYears;

    @DecimalMin(value = "0.00", message = "La valeur résiduelle ne peut être négative")
    @Digits(integer = 18, fraction = 2, message = "La valeur résiduelle ne peut avoir plus de 2 décimales")
    private BigDecimal residualValue; // Valeur résiduelle estimée

    private LocalDate depreciationStartDate; // Si différent de la date d'acquisition

    // === LOCALISATION ===

    @Size(max = 200, message = "La localisation ne peut dépasser 200 caractères")
    private String location; // Ex: Siège Yaoundé, Agence Douala

    @Size(max = 100, message = "Le département ne peut dépasser 100 caractères")
    private String department; // Ex: Service Commercial, Direction Générale

    @Size(max = 200, message = "Le nom du responsable ne peut dépasser 200 caractères")
    private String responsiblePerson; // Ex: Jean KAMGA (Directeur Commercial)

    // === INFORMATIONS COMPLÉMENTAIRES ===

    @Size(max = 100, message = "Le numéro de série ne peut dépasser 100 caractères")
    private String serialNumber;

    @Size(max = 100, message = "L'immatriculation ne peut dépasser 100 caractères")
    @Pattern(regexp = "^[A-Z0-9\\s-]*$", message = "L'immatriculation contient des caractères invalides")
    private String registrationNumber; // Pour véhicules (ex: LT-1234-ABC)

    @Size(max = 2000, message = "Les notes ne peuvent dépasser 2000 caractères")
    private String notes;

    // === VALIDATIONS MÉTIER PERSONNALISÉES ===

    /**
     * Valider que la méthode d'amortissement est compatible avec la catégorie
     */
    @AssertTrue(message = "La méthode d'amortissement dégressif n'est pas autorisée pour cette catégorie d'immobilisation")
    public boolean isDepreciationMethodValid() {
        if (depreciationMethod == null || category == null) {
            return true; // Sera validé par @NotNull
        }

        // Vérifier que la méthode est autorisée pour la catégorie
        return depreciationMethod.isAllowedForCategory(category);
    }

    /**
     * Valider que la durée de vie est cohérente avec la catégorie
     */
    @AssertTrue(message = "La durée de vie ne correspond pas aux normes fiscales camerounaises pour cette catégorie")
    public boolean isUsefulLifeValid() {
        if (usefulLifeYears == null || category == null) {
            return true;
        }

        // Tolérance de +/- 50% par rapport aux durées fiscales standards
        Integer defaultLife = category.getDefaultUsefulLifeYears();
        if (defaultLife == 0) {
            // Catégorie non amortissable (terrains, financières)
            return true;
        }

        int minLife = (int) (defaultLife * 0.5);
        int maxLife = (int) (defaultLife * 1.5);

        return usefulLifeYears >= minLife && usefulLifeYears <= maxLife;
    }

    /**
     * Valider que la valeur résiduelle n'excède pas le coût d'acquisition
     */
    @AssertTrue(message = "La valeur résiduelle ne peut excéder le coût d'acquisition")
    public boolean isResidualValueValid() {
        if (residualValue == null || acquisitionCost == null) {
            return true;
        }

        return residualValue.compareTo(acquisitionCost) <= 0;
    }

    /**
     * Valider que le compte OHADA correspond à la catégorie
     */
    @AssertTrue(message = "Le compte OHADA ne correspond pas à la catégorie sélectionnée")
    public boolean isAccountNumberValid() {
        if (accountNumber == null || category == null) {
            return true;
        }

        // Vérifier que le compte commence par le préfixe de la catégorie
        return accountNumber.startsWith(category.getAccountPrefix());
    }
}
