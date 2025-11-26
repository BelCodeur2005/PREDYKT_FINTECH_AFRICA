package com.predykt.accounting.domain.entity;

import com.predykt.accounting.domain.enums.TaskType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entité représentant le suivi du temps passé sur un dossier client
 * Utilisée uniquement en MODE CABINET
 */
@Entity
@Table(name = "time_tracking", indexes = {
    @Index(name = "idx_time_tracking_user", columnList = "user_id"),
    @Index(name = "idx_time_tracking_company", columnList = "company_id"),
    @Index(name = "idx_time_tracking_date", columnList = "task_date"),
    @Index(name = "idx_time_tracking_billable", columnList = "is_billable")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "task_date", nullable = false)
    private LocalDate taskDate;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "task_description", columnDefinition = "TEXT")
    private String taskDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", length = 50)
    private TaskType taskType;

    @Column(name = "is_billable", nullable = false)
    @Builder.Default
    private Boolean isBillable = true;

    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by", length = 100)
    private String createdBy;

    /**
     * Calcule la durée en heures
     */
    public BigDecimal getDurationHours() {
        if (durationMinutes == null) return BigDecimal.ZERO;
        return BigDecimal.valueOf(durationMinutes)
            .divide(BigDecimal.valueOf(60), 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Calcule le montant facturable
     */
    public BigDecimal calculateBillableAmount() {
        if (!isBillable || hourlyRate == null || durationMinutes == null) {
            return BigDecimal.ZERO;
        }
        return hourlyRate.multiply(getDurationHours());
    }

    /**
     * Vérifie si la tâche est facturable
     */
    public boolean canBeBilled() {
        return isBillable && hourlyRate != null && hourlyRate.compareTo(BigDecimal.ZERO) > 0;
    }
}
