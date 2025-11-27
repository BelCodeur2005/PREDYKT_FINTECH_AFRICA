package com.predykt.accounting.domain.entity;

import com.predykt.accounting.domain.enums.ImportStatus;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Historique des imports d'activités
 * Permet de tracer tous les imports et leurs résultats
 */
@Entity
@Table(name = "activity_import_history", indexes = {
    @Index(name = "idx_history_company", columnList = "company_id"),
    @Index(name = "idx_history_template", columnList = "template_id"),
    @Index(name = "idx_history_status", columnList = "status"),
    @Index(name = "idx_history_created", columnList = "created_at DESC")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityImportHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @NotNull
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private ActivityImportTemplate template;

    // Informations du fichier
    @Column(name = "file_name", nullable = false)
    @NotBlank
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;  // En bytes

    @Column(name = "file_format", length = 20)
    private String fileFormat;

    // Résultats de l'import
    @Column(name = "total_rows")
    @Builder.Default
    private Integer totalRows = 0;

    @Column(name = "success_count")
    @Builder.Default
    private Integer successCount = 0;

    @Column(name = "error_count")
    @Builder.Default
    private Integer errorCount = 0;

    @Column(name = "warning_count")
    @Builder.Default
    private Integer warningCount = 0;

    // Détails (JSON)
    @Type(JsonBinaryType.class)
    @Column(name = "errors", columnDefinition = "jsonb")
    private List<Map<String, Object>> errors;

    @Type(JsonBinaryType.class)
    @Column(name = "warnings", columnDefinition = "jsonb")
    private List<Map<String, Object>> warnings;

    // Statut
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private ImportStatus status = ImportStatus.PENDING;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Audit
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imported_by")
    private User importedBy;

    /**
     * Marque l'import comme démarré
     */
    public void start() {
        this.status = ImportStatus.PROCESSING;
        this.startedAt = LocalDateTime.now();
    }

    /**
     * Marque l'import comme terminé
     */
    public void complete() {
        this.completedAt = LocalDateTime.now();
        if (this.errorCount > 0) {
            this.status = ImportStatus.COMPLETED_WITH_ERRORS;
        } else {
            this.status = ImportStatus.COMPLETED;
        }
    }

    /**
     * Marque l'import comme échoué
     */
    public void fail() {
        this.completedAt = LocalDateTime.now();
        this.status = ImportStatus.FAILED;
    }

    /**
     * Calcule la durée d'exécution en secondes
     */
    public Long getDurationSeconds() {
        if (startedAt == null || completedAt == null) {
            return null;
        }
        return java.time.Duration.between(startedAt, completedAt).getSeconds();
    }
}
