package com.predykt.accounting.dto;

import com.predykt.accounting.domain.enums.TaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO pour les statistiques de suivi du temps
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeTrackingStatsDTO {

    private Long companyId;

    private String companyName;

    // Statistiques globales
    private Long totalEntries;

    private Double totalHours;

    private Integer totalMinutes;

    private BigDecimal totalBillableAmount;

    // Statistiques par type de tâche
    private Map<TaskType, TaskTypeStats> statsByTaskType;

    // Statistiques par utilisateur
    private Map<Long, UserStats> statsByUser;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskTypeStats {
        private TaskType taskType;
        private Long count;
        private Long totalMinutes;
        private Double totalHours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStats {
        private Long userId;
        private String firstName;
        private String lastName;
        private String fullName;
        private Long count;
        private Long totalMinutes;
        private Double totalHours;
    }
}
