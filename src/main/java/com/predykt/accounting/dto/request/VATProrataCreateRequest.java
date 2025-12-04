package com.predykt.accounting.dto.request;

import com.predykt.accounting.domain.entity.VATProrata.ProrataType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Requête de création/mise à jour d'un prorata de TVA
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VATProrataCreateRequest {

    @NotNull(message = "L'année fiscale est obligatoire")
    @Min(value = 2000, message = "Année fiscale minimum: 2000")
    @Max(value = 2100, message = "Année fiscale maximum: 2100")
    private Integer fiscalYear;

    @NotNull(message = "Le CA taxable est obligatoire")
    @DecimalMin(value = "0.0", message = "Le CA taxable doit être positif")
    private BigDecimal taxableTurnover;

    @NotNull(message = "Le CA exonéré est obligatoire")
    @DecimalMin(value = "0.0", message = "Le CA exonéré doit être positif")
    private BigDecimal exemptTurnover;

    @NotNull(message = "Le type de prorata est obligatoire")
    private ProrataType prorataType;

    private String notes;
}
