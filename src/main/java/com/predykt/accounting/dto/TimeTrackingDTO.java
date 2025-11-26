package com.predykt.accounting.dto;

import com.predykt.accounting.domain.enums.TaskType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO pour TimeTracking (MODE CABINET)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeTrackingDTO {

    private Long id;

    @NotNull(message = "L'ID de l'utilisateur est obligatoire")
    private Long userId;

    private String userFullName;

    @NotNull(message = "L'ID de l'entreprise est obligatoire")
    private Long companyId;

    private String companyName;

    @NotNull(message = "La date de la tâche est obligatoire")
    private LocalDate taskDate;

    @NotNull(message = "La durée est obligatoire")
    @Positive(message = "La durée doit être positive")
    private Integer durationMinutes;

    private BigDecimal durationHours;

    @Size(max = 5000, message = "La description ne peut pas dépasser 5000 caractères")
    private String taskDescription;

    private TaskType taskType;

    private Boolean isBillable;

    private BigDecimal hourlyRate;

    private BigDecimal billableAmount;

    private Boolean canBeBilled;

    private LocalDateTime createdAt;

    private String createdBy;
}
