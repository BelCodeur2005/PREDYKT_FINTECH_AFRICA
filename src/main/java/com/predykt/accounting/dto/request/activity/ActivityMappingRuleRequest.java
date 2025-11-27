package com.predykt.accounting.dto.request.activity;

import com.predykt.accounting.domain.enums.MatchType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Requête pour créer/modifier une règle de mapping activité → compte OHADA
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityMappingRuleRequest {

    @NotBlank(message = "Le mot-clé est obligatoire")
    private String activityKeyword;

    @NotBlank(message = "Le numéro de compte OHADA est obligatoire")
    private String accountNumber;

    private String journalCode;  // VE, AC, BQ, OD

    @Builder.Default
    private MatchType matchType = MatchType.CONTAINS;

    @Builder.Default
    private Boolean caseSensitive = false;

    @Min(0)
    @Max(1000)
    @Builder.Default
    private Integer priority = 0;

    @Min(0)
    @Max(100)
    @Builder.Default
    private Integer confidenceScore = 100;

    @Builder.Default
    private Boolean isActive = true;
}
