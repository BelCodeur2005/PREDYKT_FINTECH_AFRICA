package com.predykt.accounting.domain.entity;

import com.predykt.accounting.domain.enums.VATRecoverableCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Règle de détection de la récupérabilité de la TVA
 * Stockée en base de données pour faciliter la maintenance et l'évolution
 *
 * Système de scoring et priorités pour gérer les cas complexes
 */
@Entity
@Table(name = "recoverability_rules", indexes = {
    @Index(name = "idx_recov_rule_priority", columnList = "priority"),
    @Index(name = "idx_recov_rule_active", columnList = "is_active"),
    @Index(name = "idx_recov_rule_category", columnList = "category"),
    @Index(name = "idx_recov_rule_scope", columnList = "scope_type, scope_id"),
    @Index(name = "idx_recov_rule_company", columnList = "company_id")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecoverabilityRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom de la règle est obligatoire")
    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * MULTI-TENANT: Portée de la règle (GLOBAL, COMPANY, CABINET, TENANT)
     * - GLOBAL: Règle partagée par tous (règles par défaut)
     * - COMPANY: Règle spécifique à une entreprise (mode SHARED)
     * - CABINET: Règle spécifique à un cabinet comptable (mode CABINET)
     * - TENANT: Règle spécifique à un tenant ETI (mode DEDICATED)
     */
    @Column(name = "scope_type", length = 20)
    private String scopeType = "GLOBAL";  // GLOBAL, COMPANY, CABINET, TENANT

    /**
     * ID de la portée (company_id, cabinet_id, tenant_id selon scopeType)
     * NULL si GLOBAL
     */
    @Column(name = "scope_id", length = 100)
    private String scopeId;

    /**
     * Référence à l'entreprise (pour règles COMPANY uniquement)
     * Facilite les requêtes
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    /**
     * Priorité de la règle (1 = plus haute priorité)
     * Les règles sont évaluées par ordre de priorité croissante
     */
    @NotNull(message = "La priorité est obligatoire")
    @Min(value = 1, message = "La priorité minimum est 1")
    @Max(value = 100, message = "La priorité maximum est 100")
    @Column(nullable = false)
    private Integer priority;

    /**
     * Score de confiance de la règle (0-100)
     * Utilisé pour gérer les ambiguïtés et suggérer des alternatives
     */
    @Min(value = 0)
    @Max(value = 100)
    @Column(name = "confidence_score")
    @Builder.Default
    private Integer confidenceScore = 100;

    /**
     * Pattern regex pour le numéro de compte OHADA
     * Ex: "^2441" pour comptes 2441x
     * null = s'applique à tous les comptes
     */
    @Column(name = "account_pattern", length = 100)
    private String accountPattern;

    /**
     * Pattern regex pour la description de la transaction
     * Ex: "(?i)\\b(tourisme|voiture|berline)\\b"
     * CASE INSENSITIVE par défaut
     */
    @Column(name = "description_pattern", length = 500)
    private String descriptionPattern;

    /**
     * Mots-clés obligatoires (séparés par virgule)
     * Ex: "véhicule,tourisme" = les deux doivent être présents
     */
    @Column(name = "required_keywords", length = 500)
    private String requiredKeywords;

    /**
     * Mots-clés exclus (séparés par virgule)
     * Ex: "utilitaire,camion" = si présents, règle ne s'applique pas
     */
    @Column(name = "excluded_keywords", length = 500)
    private String excludedKeywords;

    /**
     * Catégorie de récupérabilité à appliquer si la règle matche
     */
    @NotNull(message = "La catégorie est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VATRecoverableCategory category;

    /**
     * Raison/justification pour le logging et l'audit
     */
    @Column(columnDefinition = "TEXT")
    private String reason;

    /**
     * Référence légale (ex: "CGI Art. 132 - Exclusion véhicules de tourisme")
     */
    @Column(name = "legal_reference", length = 200)
    private String legalReference;

    /**
     * Règle active ou non
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Type de règle pour catégorisation
     * VEHICLE, FUEL, REPRESENTATION, LUXURY, PERSONAL, OTHER
     */
    @Column(name = "rule_type", length = 50)
    private String ruleType;

    /**
     * Nombre de fois que cette règle a matché
     * Utilisé pour les statistiques et l'optimisation
     */
    @Column(name = "match_count")
    @Builder.Default
    private Long matchCount = 0L;

    /**
     * Nombre de fois que cette règle a été corrigée manuellement
     * Indique si la règle doit être revue
     */
    @Column(name = "correction_count")
    @Builder.Default
    private Long correctionCount = 0L;

    /**
     * Taux de précision de la règle (%)
     * = (matchCount - correctionCount) / matchCount × 100
     */
    @Column(name = "accuracy_rate", precision = 5, scale = 2)
    @Builder.Default
    private java.math.BigDecimal accuracyRate = java.math.BigDecimal.valueOf(100);

    /**
     * Date de la dernière utilisation
     */
    @Column(name = "last_used_at")
    private java.time.LocalDateTime lastUsedAt;

    /**
     * Incrémente le compteur de matchs
     */
    public void incrementMatchCount() {
        this.matchCount++;
        this.lastUsedAt = java.time.LocalDateTime.now();
        recalculateAccuracy();
    }

    /**
     * Incrémente le compteur de corrections
     */
    public void incrementCorrectionCount() {
        this.correctionCount++;
        recalculateAccuracy();
    }

    /**
     * Recalcule le taux de précision
     */
    private void recalculateAccuracy() {
        if (matchCount == 0) {
            accuracyRate = java.math.BigDecimal.valueOf(100);
            return;
        }

        long correctMatches = matchCount - correctionCount;
        accuracyRate = java.math.BigDecimal.valueOf(correctMatches)
            .multiply(java.math.BigDecimal.valueOf(100))
            .divide(java.math.BigDecimal.valueOf(matchCount), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Vérifie si la règle a un taux de précision acceptable
     */
    public boolean isAccurate() {
        return accuracyRate.compareTo(java.math.BigDecimal.valueOf(80)) >= 0;
    }

    /**
     * Vérifie si la règle nécessite une révision
     */
    public boolean needsReview() {
        return correctionCount >= 5 ||
               (matchCount >= 20 && accuracyRate.compareTo(java.math.BigDecimal.valueOf(70)) < 0);
    }
}
