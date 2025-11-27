package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.ActivityImportHistory;
import com.predykt.accounting.domain.entity.ActivityImportTemplate;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.enums.ActivityCsvFormat;
import com.predykt.accounting.domain.enums.ImportStatus;
import com.predykt.accounting.dto.ActivityImportDto;
import com.predykt.accounting.dto.request.activity.ActivityImportRequest;
import com.predykt.accounting.dto.request.JournalEntryLineRequest;
import com.predykt.accounting.dto.request.JournalEntryRequest;
import com.predykt.accounting.dto.response.ImportResultResponse;
import com.predykt.accounting.dto.response.activity.ActivityPreviewRow;
import com.predykt.accounting.dto.response.activity.PreviewResponse;
import com.predykt.accounting.exception.ImportException;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.repository.ActivityImportHistoryRepository;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.service.parser.activity.ActivityCsvParser;
import com.predykt.accounting.service.parser.activity.ActivityParserFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service principal d'orchestration pour l'import d'activités
 * Coordonne parsing, mapping, création d'écritures et historique
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityImportService {

    private final CompanyRepository companyRepository;
    private final ActivityParserFactory parserFactory;
    private final ActivityMappingService mappingService;
    private final ActivityTemplateService templateService;
    private final GeneralLedgerService glService;
    private final ActivityImportHistoryRepository historyRepository;

    /**
     * Import principal d'activités
     */
    @Transactional
    public ImportResultResponse importActivities(Long companyId, MultipartFile file, ActivityImportRequest request) {
        Company company = getCompany(companyId);

        log.info("Starting activity import for company {}: file={}, template={}, preview={}",
            companyId, file.getOriginalFilename(), request.getTemplateId(), request.getPreview());

        // Créer l'historique
        ActivityImportHistory history = createHistory(company, file, request.getTemplateId());

        try {
            history.start();
            historyRepository.save(history);

            // 1. Récupérer le template si spécifié
            ActivityImportTemplate template = null;
            if (request.getTemplateId() != null) {
                template = templateService.getTemplate(request.getTemplateId());
                templateService.incrementUsage(request.getTemplateId());
            } else {
                // Utiliser template par défaut si existe
                template = templateService.getDefaultTemplate(companyId).orElse(null);
            }

            // 2. Sélectionner et exécuter le parser
            ActivityCsvFormat format = request.getFormat() != null
                ? ActivityCsvFormat.valueOf(request.getFormat().toUpperCase())
                : null;

            ActivityCsvParser parser = parserFactory.getParser(file.getOriginalFilename(), format, template);
            List<ActivityImportDto> activities = parser.parse(file);

            log.info("Parsed {} activities from file", activities.size());
            history.setTotalRows(activities.size());

            // 3. Appliquer le mapping à chaque activité
            for (ActivityImportDto activity : activities) {
                mappingService.applyMapping(companyId, activity);
            }

            log.info("Mapping applied to all activities");

            // 4. Si mode preview, retourner sans sauvegarder
            if (Boolean.TRUE.equals(request.getPreview())) {
                return createPreviewResult(activities, file.getOriginalFilename());
            }

            // 5. Créer les écritures comptables
            int successCount = 0;
            int errorCount = 0;
            List<Map<String, Object>> errors = new ArrayList<>();

            for (ActivityImportDto activity : activities) {
                try {
                    createJournalEntryFromActivity(company, activity);
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                    errors.add(Map.of(
                        "row", activity.getRowNumber(),
                        "activity", activity.getActivity(),
                        "error", e.getMessage()
                    ));
                    log.warn("Error creating journal entry for row {}: {}", activity.getRowNumber(), e.getMessage());
                }
            }

            // 6. Finaliser l'historique
            history.setSuccessCount(successCount);
            history.setErrorCount(errorCount);
            history.setErrors(errors);
            history.complete();
            historyRepository.save(history);

            log.info("Import completed: {}/{} activities imported successfully", successCount, activities.size());

            return ImportResultResponse.builder()
                .totalRows(activities.size())
                .successCount(successCount)
                .errorCount(errorCount)
                .errors(errors.stream().map(m -> m.get("error").toString()).collect(Collectors.toList()))
                .message(String.format("Import terminé: %d/%d lignes importées", successCount, activities.size()))
                .build();

        } catch (Exception e) {
            history.fail();
            historyRepository.save(history);
            log.error("Import failed for company {}: {}", companyId, e.getMessage(), e);
            throw new ImportException("Échec de l'import: " + e.getMessage(), e);
        }
    }

    /**
     * Prévisualisation sans sauvegarde
     */
    public PreviewResponse previewImport(Long companyId, MultipartFile file, ActivityImportRequest request) {
        request.setPreview(true);
        ImportResultResponse result = importActivities(companyId, file, request);

        // Convertir en PreviewResponse
        return (PreviewResponse) result.getData();
    }

    /**
     * Crée une écriture comptable à partir d'une activité
     */
    private void createJournalEntryFromActivity(Company company, ActivityImportDto activity) {
        String accountNumber = activity.getDetectedAccount();
        String contraAccountNumber = determineContraAccount(activity);

        JournalEntryRequest request = new JournalEntryRequest();
        request.setEntryDate(activity.getDate());
        request.setReference("IMPORT-" + UUID.randomUUID().toString().substring(0, 8));
        request.setJournalCode(activity.getJournalCode() != null ? activity.getJournalCode() : "OD");

        List<JournalEntryLineRequest> lines = new ArrayList<>();

        // Ligne principale
        JournalEntryLineRequest mainLine = new JournalEntryLineRequest();
        mainLine.setAccountNumber(accountNumber);
        mainLine.setDescription(activity.getDescription());

        // Ligne de contrepartie
        JournalEntryLineRequest contraLine = new JournalEntryLineRequest();
        contraLine.setAccountNumber(contraAccountNumber);
        contraLine.setDescription(activity.getDescription());

        // Répartir débit/crédit
        BigDecimal absAmount = activity.getAmount().abs();

        if (activity.getType() != null && activity.getType().equalsIgnoreCase("Revenu")) {
            mainLine.setDebitAmount(BigDecimal.ZERO);
            mainLine.setCreditAmount(absAmount);
            contraLine.setDebitAmount(absAmount);
            contraLine.setCreditAmount(BigDecimal.ZERO);
        } else {
            mainLine.setDebitAmount(absAmount);
            mainLine.setCreditAmount(BigDecimal.ZERO);
            contraLine.setDebitAmount(BigDecimal.ZERO);
            contraLine.setCreditAmount(absAmount);
        }

        lines.add(mainLine);
        lines.add(contraLine);
        request.setLines(lines);

        glService.recordJournalEntry(company.getId(), request);
    }

    /**
     * Détermine le compte de contrepartie
     */
    private String determineContraAccount(ActivityImportDto activity) {
        if (activity.getType() != null) {
            return switch (activity.getType().toUpperCase()) {
                case "CAPEX" -> "404";  // Fournisseurs d'immobilisations
                case "FINANCING" -> "521";  // Banques
                default -> "521";  // Banques (par défaut)
            };
        }
        return "521";
    }

    /**
     * Crée l'historique d'import
     */
    private ActivityImportHistory createHistory(Company company, MultipartFile file, Long templateId) {
        ActivityImportTemplate template = null;
        if (templateId != null) {
            template = templateService.getTemplate(templateId);
        }

        return ActivityImportHistory.builder()
            .company(company)
            .template(template)
            .fileName(file.getOriginalFilename())
            .fileSize(file.getSize())
            .fileFormat(file.getContentType())
            .status(ImportStatus.PENDING)
            .build();
    }

    /**
     * Crée le résultat de prévisualisation
     */
    private ImportResultResponse createPreviewResult(List<ActivityImportDto> activities, String fileName) {
        // Limiter à 50 lignes pour la preview
        List<ActivityPreviewRow> previewRows = activities.stream()
            .limit(50)
            .map(this::toPreviewRow)
            .collect(Collectors.toList());

        // Distribution des comptes
        Map<String, Integer> accountDistribution = activities.stream()
            .filter(a -> a.getDetectedAccount() != null)
            .collect(Collectors.groupingBy(
                ActivityImportDto::getDetectedAccount,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));

        // Distribution de confiance
        Map<String, Integer> confidenceDistribution = activities.stream()
            .collect(Collectors.groupingBy(
                ActivityImportDto::getConfidenceLevel,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));

        PreviewResponse preview = PreviewResponse.builder()
            .fileName(fileName)
            .totalRows(activities.size())
            .validRows((int) activities.stream().filter(ActivityImportDto::isValid).count())
            .invalidRows((int) activities.stream().filter(a -> !a.isValid()).count())
            .rows(previewRows)
            .accountDistribution(accountDistribution)
            .confidenceDistribution(confidenceDistribution)
            .build();

        return ImportResultResponse.builder()
            .totalRows(activities.size())
            .successCount(0)
            .errorCount(0)
            .message("Prévisualisation générée")
            .data(preview)
            .build();
    }

    private ActivityPreviewRow toPreviewRow(ActivityImportDto dto) {
        return ActivityPreviewRow.builder()
            .rowNumber(dto.getRowNumber())
            .date(dto.getDate())
            .activity(dto.getActivity())
            .description(dto.getDescription())
            .amount(dto.getAmount())
            .type(dto.getType())
            .detectedAccount(dto.getDetectedAccount())
            .accountName(dto.getAccountName())
            .journalCode(dto.getJournalCode())
            .confidence(dto.getConfidenceLevel())
            .isValid(dto.isValid())
            .warnings(dto.getWarnings())
            .errors(dto.getErrors())
            .build();
    }

    private Company getCompany(Long companyId) {
        return companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée: " + companyId));
    }
}
