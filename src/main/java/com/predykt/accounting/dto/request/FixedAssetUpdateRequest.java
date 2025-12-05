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
 * Requête de mise à jour d'une immobilisation
 * Tous les champs sont optionnels (PATCH)
 * Conforme OHADA et fiscalité camerounaise
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedAssetUpdateRequest {

    // === IDENTIFICATION ===

    @Size(max = 255, message = "Le nom ne peut dépasser 255 caractères")
    private String assetName;

    @Size(max = 2000, message = "La description ne peut dépasser 2000 caractères")
    private String description;

    // === CLASSIFICATION ===
    // Note: La catégorie et le compte ne peuvent normalement pas être modifiés
    // après création (risque de désynchronisation comptable)

    // === FOURNISSEUR ===

    @Size(max = 200, message = "Le nom du fournisseur ne peut dépasser 200 caractères")
    private String supplierName;

    @Size(max = 100, message = "Le numéro de facture ne peut dépasser 100 caractères")
    private String invoiceNumber;

    // === ACQUISITION ===
    // Note: Les montants d'acquisition ne peuvent être modifiés après création
    // (sauf correction d'erreur avec validation comptable)

    @DecimalMin(value = "0.00", message = "La TVA ne peut être négative")
    @Digits(integer = 18, fraction = 2, message = "La TVA ne peut avoir plus de 2 décimales")
    private BigDecimal acquisitionVat;

    @DecimalMin(value = "0.00", message = "Les frais d'installation ne peuvent être négatifs")
    @Digits(integer = 18, fraction = 2, message = "Les frais d'installation ne peuvent avoir plus de 2 décimales")
    private BigDecimal installationCost;

    // === AMORTISSEMENT ===
    // Note: La méthode d'amortissement peut être modifiée uniquement
    // si aucun amortissement n'a encore été comptabilisé

    @Min(value = 1, message = "La durée de vie doit être d'au moins 1 an")
    @Max(value = 50, message = "La durée de vie ne peut dépasser 50 ans")
    private Integer usefulLifeYears;

    @DecimalMin(value = "0.00", message = "La valeur résiduelle ne peut être négative")
    @Digits(integer = 18, fraction = 2, message = "La valeur résiduelle ne peut avoir plus de 2 décimales")
    private BigDecimal residualValue;

    private LocalDate depreciationStartDate;

    // === LOCALISATION ===

    @Size(max = 200, message = "La localisation ne peut dépasser 200 caractères")
    private String location;

    @Size(max = 100, message = "Le département ne peut dépasser 100 caractères")
    private String department;

    @Size(max = 200, message = "Le nom du responsable ne peut dépasser 200 caractères")
    private String responsiblePerson;

    // === INFORMATIONS COMPLÉMENTAIRES ===

    @Size(max = 100, message = "Le numéro de série ne peut dépasser 100 caractères")
    private String serialNumber;

    @Size(max = 100, message = "L'immatriculation ne peut dépasser 100 caractères")
    @Pattern(regexp = "^[A-Z0-9\\s-]*$", message = "L'immatriculation contient des caractères invalides")
    private String registrationNumber;

    @Size(max = 2000, message = "Les notes ne peuvent dépasser 2000 caractères")
    private String notes;
}
