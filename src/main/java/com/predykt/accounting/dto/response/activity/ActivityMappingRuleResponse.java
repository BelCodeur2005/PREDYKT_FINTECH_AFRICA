package com.predykt.accounting.dto.response.activity;

import com.predykt.accounting.domain.enums.MatchType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityMappingRuleResponse {
    private Long id;
    private String activityKeyword;
    private String accountNumber;
    private String accountName;        // Nom du compte OHADA
    private String journalCode;
    private MatchType matchType;
    private Boolean caseSensitive;
    private Integer priority;
    private Integer confidenceScore;
    private Boolean isActive;
    private Integer usageCount;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
}
