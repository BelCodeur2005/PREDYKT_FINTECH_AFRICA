package com.predykt.accounting.domain.entity;

import com.predykt.accounting.domain.enums.MatchType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Règle de mapping personnalisée activité → compte OHADA
 * Permet à chaque entreprise de définir ses propres règles de classification
 */
@Entity
@Table(name = "activity_mapping_rules", indexes = {
    @Index(name = "idx_mapping_company", columnList = "company_id"),
    @Index(name = "idx_mapping_keyword", columnList = "activity_keyword"),
    @Index(name = "idx_mapping_active", columnList = "is_active"),
    @Index(name = "idx_mapping_priority", columnList = "priority DESC")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityMappingRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @NotNull
    private Company company;

    // Règle de matching
    @Column(name = "activity_keyword", nullable = false)
    @NotBlank(message = "Le mot-clé ne peut pas être vide")
    private String activityKeyword;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", length = 20)
    @Builder.Default
    private MatchType matchType = MatchType.CONTAINS;

    @Column(name = "case_sensitive")
    @Builder.Default
    private Boolean caseSensitive = false;

    // Mapping
    @Column(name = "account_number", nullable = false, length = 20)
    @NotBlank(message = "Le numéro de compte est obligatoire")
    private String accountNumber;

    @Column(name = "journal_code", length = 10)
    private String journalCode;  // VE, AC, BQ, OD

    // Métadonnées
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;  // Plus élevé = plus prioritaire

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "confidence_score")
    @Min(0)
    @Max(100)
    @Builder.Default
    private Integer confidenceScore = 100;

    // Apprentissage automatique
    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /**
     * Vérifie si cette règle matche une activité donnée
     */
    public boolean matches(String activityName) {
        if (activityName == null || activityName.trim().isEmpty()) {
            return false;
        }

        String keyword = caseSensitive ? activityKeyword : activityKeyword.toLowerCase();
        String activity = caseSensitive ? activityName : activityName.toLowerCase();

        return switch (matchType) {
            case CONTAINS -> activity.contains(keyword);
            case EXACT -> activity.equals(keyword);
            case STARTS_WITH -> activity.startsWith(keyword);
            case ENDS_WITH -> activity.endsWith(keyword);
            case REGEX -> activity.matches(keyword);
        };
    }

    /**
     * Incrémente le compteur d'utilisation
     */
    public void incrementUsage() {
        this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
        this.lastUsedAt = LocalDateTime.now();
    }
}
