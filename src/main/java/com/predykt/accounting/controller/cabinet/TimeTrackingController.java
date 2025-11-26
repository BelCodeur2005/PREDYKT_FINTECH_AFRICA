package com.predykt.accounting.controller.cabinet;

import com.predykt.accounting.domain.entity.TimeTracking;
import com.predykt.accounting.domain.enums.TaskType;
import com.predykt.accounting.dto.TimeTrackingDTO;
import com.predykt.accounting.dto.TimeTrackingStatsDTO;
import com.predykt.accounting.service.TimeTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controller REST pour la gestion du suivi du temps (MODE CABINET)
 */
@RestController
@RequestMapping("/api/time-tracking")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('CABINET_USER') or hasRole('CABINET_MANAGER')")
public class TimeTrackingController {

    private final TimeTrackingService timeTrackingService;

    /**
     * Crée une entrée de temps
     */
    @PostMapping
    public ResponseEntity<TimeTracking> createTimeEntry(@Valid @RequestBody TimeTrackingDTO dto) {
        log.info("Création d'une entrée de temps pour l'utilisateur {} et l'entreprise {}",
            dto.getUserId(), dto.getCompanyId());

        TimeTracking timeTracking = mapToEntity(dto);
        TimeTracking created = timeTrackingService.createTimeEntry(timeTracking);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Met à jour une entrée de temps
     */
    @PutMapping("/{id}")
    public ResponseEntity<TimeTracking> updateTimeEntry(
            @PathVariable Long id,
            @Valid @RequestBody TimeTrackingDTO dto) {
        log.info("Mise à jour de l'entrée de temps ID: {}", id);

        TimeTracking timeTracking = mapToEntity(dto);
        TimeTracking updated = timeTrackingService.updateTimeEntry(id, timeTracking);

        return ResponseEntity.ok(updated);
    }

    /**
     * Supprime une entrée de temps
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTimeEntry(@PathVariable Long id) {
        log.info("Suppression de l'entrée de temps ID: {}", id);

        timeTrackingService.deleteTimeEntry(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Récupère une entrée de temps par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<TimeTracking> getTimeEntryById(@PathVariable Long id) {
        TimeTracking entry = timeTrackingService.getTimeEntryById(id);
        return ResponseEntity.ok(entry);
    }

    /**
     * Récupère toutes les entrées d'un utilisateur
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TimeTracking>> getUserTimeEntries(@PathVariable Long userId) {
        List<TimeTracking> entries = timeTrackingService.getUserTimeEntries(userId);
        return ResponseEntity.ok(entries);
    }

    /**
     * Récupère toutes les entrées d'une entreprise
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<TimeTracking>> getCompanyTimeEntries(@PathVariable Long companyId) {
        List<TimeTracking> entries = timeTrackingService.getCompanyTimeEntries(companyId);
        return ResponseEntity.ok(entries);
    }

    /**
     * Récupère les entrées d'un utilisateur pour une entreprise
     */
    @GetMapping("/user/{userId}/company/{companyId}")
    public ResponseEntity<List<TimeTracking>> getUserCompanyTimeEntries(
            @PathVariable Long userId,
            @PathVariable Long companyId) {
        List<TimeTracking> entries = timeTrackingService.getUserCompanyTimeEntries(userId, companyId);
        return ResponseEntity.ok(entries);
    }

    /**
     * Récupère les entrées pour une période
     */
    @GetMapping("/period")
    public ResponseEntity<List<TimeTracking>> getTimeEntriesByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<TimeTracking> entries = timeTrackingService.getTimeEntriesByPeriod(startDate, endDate);
        return ResponseEntity.ok(entries);
    }

    /**
     * Récupère les entrées d'un utilisateur pour une période
     */
    @GetMapping("/user/{userId}/period")
    public ResponseEntity<List<TimeTracking>> getUserTimeEntriesByPeriod(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<TimeTracking> entries = timeTrackingService.getUserTimeEntriesByPeriod(userId, startDate, endDate);
        return ResponseEntity.ok(entries);
    }

    /**
     * Récupère les entrées d'une entreprise pour une période
     */
    @GetMapping("/company/{companyId}/period")
    public ResponseEntity<List<TimeTracking>> getCompanyTimeEntriesByPeriod(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<TimeTracking> entries = timeTrackingService.getCompanyTimeEntriesByPeriod(companyId, startDate, endDate);
        return ResponseEntity.ok(entries);
    }

    /**
     * Récupère les entrées facturables d'une entreprise
     */
    @GetMapping("/company/{companyId}/billable")
    public ResponseEntity<List<TimeTracking>> getBillableTimeEntries(@PathVariable Long companyId) {
        List<TimeTracking> entries = timeTrackingService.getBillableTimeEntries(companyId);
        return ResponseEntity.ok(entries);
    }

    /**
     * Calcule le total d'heures pour un utilisateur
     */
    @GetMapping("/user/{userId}/total-hours")
    public ResponseEntity<Double> calculateUserTotalHours(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Double totalHours = timeTrackingService.calculateUserTotalHours(userId, startDate, endDate);
        return ResponseEntity.ok(totalHours);
    }

    /**
     * Calcule le total d'heures pour une entreprise
     */
    @GetMapping("/company/{companyId}/total-hours")
    public ResponseEntity<Double> calculateCompanyTotalHours(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Double totalHours = timeTrackingService.calculateCompanyTotalHours(companyId, startDate, endDate);
        return ResponseEntity.ok(totalHours);
    }

    /**
     * Calcule le montant facturable pour une entreprise
     */
    @GetMapping("/company/{companyId}/billable-amount")
    public ResponseEntity<BigDecimal> calculateBillableAmount(@PathVariable Long companyId) {
        BigDecimal amount = timeTrackingService.calculateBillableAmount(companyId);
        return ResponseEntity.ok(amount);
    }

    /**
     * Calcule le montant facturable pour une période
     */
    @GetMapping("/company/{companyId}/billable-amount/period")
    public ResponseEntity<BigDecimal> calculateBillableAmountByPeriod(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        BigDecimal amount = timeTrackingService.calculateBillableAmountByPeriod(companyId, startDate, endDate);
        return ResponseEntity.ok(amount);
    }

    /**
     * Récupère les entrées par type de tâche
     */
    @GetMapping("/company/{companyId}/task-type/{taskType}")
    public ResponseEntity<List<TimeTracking>> getTimeEntriesByTaskType(
            @PathVariable Long companyId,
            @PathVariable TaskType taskType) {
        List<TimeTracking> entries = timeTrackingService.getTimeEntriesByTaskType(companyId, taskType);
        return ResponseEntity.ok(entries);
    }

    /**
     * Génère des statistiques par type de tâche
     */
    @GetMapping("/company/{companyId}/stats/task-type")
    public ResponseEntity<Map<TaskType, Map<String, Object>>> getTaskTypeStatistics(@PathVariable Long companyId) {
        Map<TaskType, Map<String, Object>> stats = timeTrackingService.getTaskTypeStatistics(companyId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Génère des statistiques par utilisateur
     */
    @GetMapping("/company/{companyId}/stats/user")
    public ResponseEntity<List<Map<String, Object>>> getUserStatistics(@PathVariable Long companyId) {
        List<Map<String, Object>> stats = timeTrackingService.getUserStatistics(companyId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Marque une entrée comme facturable
     */
    @PutMapping("/{id}/mark-billable")
    public ResponseEntity<TimeTracking> markAsBillable(
            @PathVariable Long id,
            @RequestParam BigDecimal hourlyRate) {
        TimeTracking updated = timeTrackingService.markAsBillable(id, hourlyRate);
        return ResponseEntity.ok(updated);
    }

    /**
     * Marque une entrée comme non facturable
     */
    @PutMapping("/{id}/mark-non-billable")
    public ResponseEntity<TimeTracking> markAsNonBillable(@PathVariable Long id) {
        TimeTracking updated = timeTrackingService.markAsNonBillable(id);
        return ResponseEntity.ok(updated);
    }

    /**
     * Duplique une entrée de temps
     */
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<TimeTracking> duplicateTimeEntry(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDate) {
        TimeTracking duplicate = timeTrackingService.duplicateTimeEntry(id, newDate);
        return ResponseEntity.status(HttpStatus.CREATED).body(duplicate);
    }

    // Méthodes utilitaires

    private TimeTracking mapToEntity(TimeTrackingDTO dto) {
        return TimeTracking.builder()
            .taskDate(dto.getTaskDate())
            .durationMinutes(dto.getDurationMinutes())
            .taskDescription(dto.getTaskDescription())
            .taskType(dto.getTaskType())
            .isBillable(dto.getIsBillable())
            .hourlyRate(dto.getHourlyRate())
            .build();
    }
}
