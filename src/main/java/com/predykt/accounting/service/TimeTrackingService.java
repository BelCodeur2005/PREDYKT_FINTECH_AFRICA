package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.TimeTracking;
import com.predykt.accounting.domain.entity.User;
import com.predykt.accounting.domain.enums.TaskType;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.repository.TimeTrackingRepository;
import com.predykt.accounting.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service pour la gestion du suivi du temps (MODE CABINET)
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TimeTrackingService {

    private final TimeTrackingRepository timeTrackingRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    /**
     * Enregistre une entrée de temps
     */
    public TimeTracking createTimeEntry(TimeTracking timeTracking) {
        log.info("Création d'une entrée de temps pour l'utilisateur {} et l'entreprise {}",
            timeTracking.getUser().getId(), timeTracking.getCompany().getId());

        // Valider l'utilisateur
        User user = userRepository.findById(timeTracking.getUser().getId())
            .orElseThrow(() -> new EntityNotFoundException(
                "Utilisateur non trouvé avec l'ID: " + timeTracking.getUser().getId()));

        // Valider l'entreprise
        Company company = companyRepository.findById(timeTracking.getCompany().getId())
            .orElseThrow(() -> new EntityNotFoundException(
                "Entreprise non trouvée avec l'ID: " + timeTracking.getCompany().getId()));

        timeTracking.setUser(user);
        timeTracking.setCompany(company);

        return timeTrackingRepository.save(timeTracking);
    }

    /**
     * Met à jour une entrée de temps
     */
    public TimeTracking updateTimeEntry(Long timeEntryId, TimeTracking updatedEntry) {
        log.info("Mise à jour de l'entrée de temps ID: {}", timeEntryId);

        TimeTracking existing = timeTrackingRepository.findById(timeEntryId)
            .orElseThrow(() -> new EntityNotFoundException("Entrée de temps non trouvée avec l'ID: " + timeEntryId));

        existing.setTaskDate(updatedEntry.getTaskDate());
        existing.setDurationMinutes(updatedEntry.getDurationMinutes());
        existing.setTaskDescription(updatedEntry.getTaskDescription());
        existing.setTaskType(updatedEntry.getTaskType());
        existing.setIsBillable(updatedEntry.getIsBillable());
        existing.setHourlyRate(updatedEntry.getHourlyRate());

        return timeTrackingRepository.save(existing);
    }

    /**
     * Supprime une entrée de temps
     */
    public void deleteTimeEntry(Long timeEntryId) {
        log.info("Suppression de l'entrée de temps ID: {}", timeEntryId);

        if (!timeTrackingRepository.existsById(timeEntryId)) {
            throw new EntityNotFoundException("Entrée de temps non trouvée avec l'ID: " + timeEntryId);
        }

        timeTrackingRepository.deleteById(timeEntryId);
    }

    /**
     * Récupère une entrée de temps par ID
     */
    @Transactional(readOnly = true)
    public TimeTracking getTimeEntryById(Long timeEntryId) {
        return timeTrackingRepository.findById(timeEntryId)
            .orElseThrow(() -> new EntityNotFoundException("Entrée de temps non trouvée avec l'ID: " + timeEntryId));
    }

    /**
     * Récupère toutes les entrées de temps d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<TimeTracking> getUserTimeEntries(Long userId) {
        return timeTrackingRepository.findByUserId(userId);
    }

    /**
     * Récupère toutes les entrées de temps pour une entreprise
     */
    @Transactional(readOnly = true)
    public List<TimeTracking> getCompanyTimeEntries(Long companyId) {
        return timeTrackingRepository.findByCompanyId(companyId);
    }

    /**
     * Récupère les entrées de temps d'un utilisateur pour une entreprise
     */
    @Transactional(readOnly = true)
    public List<TimeTracking> getUserCompanyTimeEntries(Long userId, Long companyId) {
        return timeTrackingRepository.findByUserIdAndCompanyId(userId, companyId);
    }

    /**
     * Récupère les entrées de temps pour une période
     */
    @Transactional(readOnly = true)
    public List<TimeTracking> getTimeEntriesByPeriod(LocalDate startDate, LocalDate endDate) {
        return timeTrackingRepository.findByPeriod(startDate, endDate);
    }

    /**
     * Récupère les entrées de temps d'un utilisateur pour une période
     */
    @Transactional(readOnly = true)
    public List<TimeTracking> getUserTimeEntriesByPeriod(Long userId, LocalDate startDate, LocalDate endDate) {
        return timeTrackingRepository.findByUserIdAndPeriod(userId, startDate, endDate);
    }

    /**
     * Récupère les entrées de temps d'une entreprise pour une période
     */
    @Transactional(readOnly = true)
    public List<TimeTracking> getCompanyTimeEntriesByPeriod(Long companyId, LocalDate startDate, LocalDate endDate) {
        return timeTrackingRepository.findByCompanyIdAndPeriod(companyId, startDate, endDate);
    }

    /**
     * Récupère les entrées facturables d'une entreprise
     */
    @Transactional(readOnly = true)
    public List<TimeTracking> getBillableTimeEntries(Long companyId) {
        return timeTrackingRepository.findBillableByCompanyId(companyId);
    }

    /**
     * Calcule le total d'heures pour un utilisateur sur une période
     */
    @Transactional(readOnly = true)
    public Double calculateUserTotalHours(Long userId, LocalDate startDate, LocalDate endDate) {
        return timeTrackingRepository.calculateTotalHoursByUserIdAndPeriod(userId, startDate, endDate);
    }

    /**
     * Calcule le total d'heures pour une entreprise sur une période
     */
    @Transactional(readOnly = true)
    public Double calculateCompanyTotalHours(Long companyId, LocalDate startDate, LocalDate endDate) {
        return timeTrackingRepository.calculateTotalHoursByCompanyIdAndPeriod(companyId, startDate, endDate);
    }

    /**
     * Calcule le montant facturable pour une entreprise
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateBillableAmount(Long companyId) {
        return timeTrackingRepository.calculateBillableAmountByCompanyId(companyId);
    }

    /**
     * Calcule le montant facturable pour une entreprise sur une période
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateBillableAmountByPeriod(Long companyId, LocalDate startDate, LocalDate endDate) {
        return timeTrackingRepository.calculateBillableAmountByCompanyIdAndPeriod(companyId, startDate, endDate);
    }

    /**
     * Récupère les entrées par type de tâche
     */
    @Transactional(readOnly = true)
    public List<TimeTracking> getTimeEntriesByTaskType(Long companyId, TaskType taskType) {
        return timeTrackingRepository.findByCompanyIdAndTaskType(companyId, taskType);
    }

    /**
     * Génère des statistiques par type de tâche
     */
    @Transactional(readOnly = true)
    public Map<TaskType, Map<String, Object>> getTaskTypeStatistics(Long companyId) {
        List<Object[]> rawStats = timeTrackingRepository.getStatisticsByCompanyId(companyId);

        Map<TaskType, Map<String, Object>> statistics = new HashMap<>();

        for (Object[] row : rawStats) {
            TaskType taskType = (TaskType) row[0];
            Long count = (Long) row[1];
            Long totalMinutes = (Long) row[2];

            Map<String, Object> stats = new HashMap<>();
            stats.put("count", count);
            stats.put("totalMinutes", totalMinutes);
            stats.put("totalHours", totalMinutes / 60.0);

            statistics.put(taskType, stats);
        }

        return statistics;
    }

    /**
     * Génère des statistiques par utilisateur
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserStatistics(Long companyId) {
        List<Object[]> rawStats = timeTrackingRepository.getUserStatisticsByCompanyId(companyId);

        return rawStats.stream()
            .map(row -> {
                Map<String, Object> stats = new HashMap<>();
                stats.put("userId", row[0]);
                stats.put("firstName", row[1]);
                stats.put("lastName", row[2]);
                stats.put("count", row[3]);
                Long totalMinutes = (Long) row[4];
                stats.put("totalMinutes", totalMinutes);
                stats.put("totalHours", totalMinutes / 60.0);
                return stats;
            })
            .toList();
    }

    /**
     * Marque une entrée comme facturable
     */
    public TimeTracking markAsBillable(Long timeEntryId, BigDecimal hourlyRate) {
        log.info("Marquage de l'entrée {} comme facturable avec taux horaire {}", timeEntryId, hourlyRate);

        TimeTracking entry = getTimeEntryById(timeEntryId);
        entry.setIsBillable(true);
        entry.setHourlyRate(hourlyRate);

        return timeTrackingRepository.save(entry);
    }

    /**
     * Marque une entrée comme non facturable
     */
    public TimeTracking markAsNonBillable(Long timeEntryId) {
        log.info("Marquage de l'entrée {} comme non facturable", timeEntryId);

        TimeTracking entry = getTimeEntryById(timeEntryId);
        entry.setIsBillable(false);

        return timeTrackingRepository.save(entry);
    }

    /**
     * Duplique une entrée de temps
     */
    public TimeTracking duplicateTimeEntry(Long timeEntryId, LocalDate newDate) {
        log.info("Duplication de l'entrée de temps {} pour la date {}", timeEntryId, newDate);

        TimeTracking original = getTimeEntryById(timeEntryId);

        TimeTracking duplicate = TimeTracking.builder()
            .user(original.getUser())
            .company(original.getCompany())
            .taskDate(newDate)
            .durationMinutes(original.getDurationMinutes())
            .taskDescription(original.getTaskDescription())
            .taskType(original.getTaskType())
            .isBillable(original.getIsBillable())
            .hourlyRate(original.getHourlyRate())
            .build();

        return timeTrackingRepository.save(duplicate);
    }
}
