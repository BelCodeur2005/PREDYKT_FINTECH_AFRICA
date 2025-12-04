package com.predykt.accounting.dto.response;

import com.predykt.accounting.domain.entity.VATProrata.ProrataType;
import com.predykt.accounting.domain.enums.VATRecoverableCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Réponse contenant les détails d'un calcul de TVA récupérable
 * Montre le calcul en 2 étapes: Nature + Prorata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VATRecoveryCalculationResponse {

    private Long id;

    private Long companyId;

    private String companyName;

    private Long generalLedgerId;

    private String accountNumber;

    private String description;

    // Montants de base
    private BigDecimal htAmount;

    private BigDecimal vatAmount;

    private BigDecimal vatRate;

    // ÉTAPE 1: Récupération par nature
    private VATRecoverableCategory recoveryCategory;

    private String recoveryCategoryName;

    private BigDecimal recoveryByNatureRate;

    private BigDecimal recoverableByNature;

    // ÉTAPE 2: Application du prorata
    private Long prorataId;

    private Integer fiscalYear;

    private ProrataType prorataType;

    private BigDecimal prorataRate;

    private BigDecimal prorataPercentage;

    private BigDecimal recoverableWithProrata;

    // RÉSULTAT FINAL
    private BigDecimal recoverableVat;

    private BigDecimal nonRecoverableVat;

    private BigDecimal recoveryPercentage;

    // Indicateurs
    private Boolean hasProrataImpact;

    private String appliedRule;

    private LocalDateTime calculationDate;

    private LocalDateTime createdAt;

    private String createdBy;

    /**
     * Message expliquant le calcul
     */
    private String calculationExplanation;
}
