package com.predykt.accounting.dto.response;

import com.predykt.accounting.domain.entity.VATProrata.ProrataType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Réponse contenant les détails d'un prorata de TVA
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VATProrataResponse {

    private Long id;

    private Long companyId;

    private String companyName;

    private Integer fiscalYear;

    private BigDecimal taxableTurnover;

    private BigDecimal exemptTurnover;

    private BigDecimal totalTurnover;

    /**
     * Taux de prorata (0.0000 à 1.0000)
     */
    private BigDecimal prorataRate;

    /**
     * Pourcentage de prorata (0.00 à 100.00)
     * Calculé comme: prorataRate × 100
     */
    private BigDecimal prorataPercentage;

    private ProrataType prorataType;

    private Boolean isActive;

    private Boolean isLocked;

    private LocalDateTime calculationDate;

    private LocalDateTime lockedAt;

    private String lockedBy;

    private String notes;

    private LocalDateTime createdAt;

    private String createdBy;

    private LocalDateTime updatedAt;

    private String updatedBy;

    /**
     * Indique si une régularisation est nécessaire
     * (Écart > 10% entre provisoire et définitif)
     */
    private Boolean needsRegularization;

    /**
     * Message d'information sur le prorata
     */
    private String infoMessage;
}
