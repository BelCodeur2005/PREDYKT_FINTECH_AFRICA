package com.predykt.accounting.dto.response;

import com.predykt.accounting.domain.enums.AssetCategory;
import com.predykt.accounting.domain.enums.DepreciationMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Réponse contenant les détails d'une immobilisation
 * Version simple pour opérations CRUD (différent de DepreciationScheduleResponse)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedAssetResponse {

    // === IDENTIFICATION ===
    private Long id;
    private String assetNumber;
    private String assetName;
    private String description;

    // === CLASSIFICATION OHADA ===
    private AssetCategory category;
    private String categoryName;
    private String accountNumber;

    // === FOURNISSEUR ===
    private String supplierName;
    private String invoiceNumber;

    // === ACQUISITION ===
    private LocalDate acquisitionDate;
    private BigDecimal acquisitionCost;
    private BigDecimal acquisitionVat;
    private BigDecimal installationCost;
    private BigDecimal totalCost;

    // === AMORTISSEMENT ===
    private DepreciationMethod depreciationMethod;
    private String depreciationMethodName;
    private Integer usefulLifeYears;
    private BigDecimal depreciationRate;
    private BigDecimal residualValue;
    private LocalDate depreciationStartDate;

    // === CESSION ===
    private LocalDate disposalDate;
    private BigDecimal disposalAmount;
    private String disposalReason;

    // === LOCALISATION ===
    private String location;
    private String department;
    private String responsiblePerson;

    // === STATUTS ===
    private Boolean isActive;
    private Boolean isFullyDepreciated;
    private Boolean isDisposed;

    // === INFORMATIONS COMPLÉMENTAIRES ===
    private String serialNumber;
    private String registrationNumber;
    private String notes;

    // === CALCULS EN TEMPS RÉEL ===
    private BigDecimal currentAccumulatedDepreciation;
    private BigDecimal currentNetBookValue;
    private Integer ageInYears;
    private Integer ageInMonths;
    private BigDecimal depreciationProgress; // En % (0-100)

    // === PLUS-VALUE / MOINS-VALUE (si cédé) ===
    private BigDecimal disposalGainLoss;

    // === AUDIT ===
    private String createdBy;
    private LocalDate createdAt;
    private String updatedBy;
    private LocalDate updatedAt;

    // === ALERTES ===
    private String statusLabel; // "Actif", "Totalement amorti", "Cédé", "Obsolète"
    private String statusIcon;  // "✅", "⚠️", "🔴", etc.
    private Boolean needsRenewal; // true si totalement amorti ou > durée de vie
}
