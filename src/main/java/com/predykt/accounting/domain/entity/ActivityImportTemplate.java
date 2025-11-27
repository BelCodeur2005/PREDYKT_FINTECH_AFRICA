package com.predykt.accounting.domain.entity;

import com.predykt.accounting.domain.enums.ImportFileFormat;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Template personnalisé pour l'import d'activités
 * Permet à chaque entreprise de définir son propre format d'import
 */
@Entity
@Table(name = "activity_import_templates", indexes = {
    @Index(name = "idx_template_company", columnList = "company_id"),
    @Index(name = "idx_template_active", columnList = "is_active")
}, uniqueConstraints = {
    @UniqueConstraint(name = "unique_template_name", columnNames = {"company_id", "template_name"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityImportTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @NotNull
    private Company company;

    // Identité du template
    @Column(name = "template_name", nullable = false)
    @NotBlank(message = "Le nom du template est obligatoire")
    private String templateName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // Configuration du fichier
    @Enumerated(EnumType.STRING)
    @Column(name = "file_format", length = 20)
    @Builder.Default
    private ImportFileFormat fileFormat = ImportFileFormat.CSV;

    @Column(name = "separator", length = 1)
    @Builder.Default
    private Character separator = ';';

    @Column(name = "encoding", length = 20)
    @Builder.Default
    private String encoding = "UTF-8";

    @Column(name = "has_header")
    @Builder.Default
    private Boolean hasHeader = true;

    @Column(name = "skip_rows")
    @Builder.Default
    private Integer skipRows = 0;

    // Configuration Excel spécifique
    @Column(name = "worksheet_name")
    private String worksheetName;

    @Column(name = "start_row")
    private Integer startRow;

    @Column(name = "end_row")
    private Integer endRow;

    // Mapping des colonnes (structure JSON)
    @Type(JsonBinaryType.class)
    @Column(name = "column_mapping", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> columnMapping;

    // Règles de validation (structure JSON)
    @Type(JsonBinaryType.class)
    @Column(name = "validation_rules", columnDefinition = "jsonb")
    private Map<String, Object> validationRules;

    // Transformations (structure JSON)
    @Type(JsonBinaryType.class)
    @Column(name = "transformations", columnDefinition = "jsonb")
    private Map<String, Object> transformations;

    // Statistiques d'utilisation
    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /**
     * Incrémente le compteur d'utilisation
     */
    public void incrementUsage() {
        this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
        this.lastUsedAt = LocalDateTime.now();
    }
}
