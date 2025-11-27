package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.ActivityImportTemplate;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.dto.request.activity.TemplateConfigurationRequest;
import com.predykt.accounting.exception.ImportException;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.repository.ActivityImportTemplateRepository;
import com.predykt.accounting.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service de gestion des templates d'import personnalisés
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityTemplateService {

    private final ActivityImportTemplateRepository templateRepository;
    private final CompanyRepository companyRepository;

    /**
     * Crée un nouveau template
     */
    @Transactional
    public ActivityImportTemplate createTemplate(Long companyId, TemplateConfigurationRequest request) {
        Company company = getCompany(companyId);

        // Vérifier l'unicité du nom
        if (templateRepository.existsByCompanyAndTemplateName(company, request.getTemplateName())) {
            throw new ImportException("Un template avec ce nom existe déjà: " + request.getTemplateName());
        }

        // Valider la configuration
        List<String> validationErrors = validateTemplateConfig(request);
        if (!validationErrors.isEmpty()) {
            throw new ImportException("Configuration invalide: " + String.join(", ", validationErrors));
        }

        ActivityImportTemplate template = ActivityImportTemplate.builder()
            .company(company)
            .templateName(request.getTemplateName())
            .description(request.getDescription())
            .fileFormat(request.getFileFormat())
            .separator(request.getSeparator())
            .encoding(request.getEncoding())
            .hasHeader(request.getHasHeader())
            .skipRows(request.getSkipRows())
            .worksheetName(request.getWorksheetName())
            .startRow(request.getStartRow())
            .endRow(request.getEndRow())
            .columnMapping(request.getColumnMapping())
            .validationRules(request.getValidationRules())
            .transformations(request.getTransformations())
            .isDefault(request.getIsDefault())
            .isActive(request.getIsActive())
            .build();

        template = templateRepository.save(template);
        log.info("Template créé: {} pour l'entreprise {}", template.getTemplateName(), companyId);

        return template;
    }

    /**
     * Met à jour un template existant
     */
    @Transactional
    public ActivityImportTemplate updateTemplate(Long templateId, TemplateConfigurationRequest request) {
        ActivityImportTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new ResourceNotFoundException("Template non trouvé: " + templateId));

        // Valider la configuration
        List<String> validationErrors = validateTemplateConfig(request);
        if (!validationErrors.isEmpty()) {
            throw new ImportException("Configuration invalide: " + String.join(", ", validationErrors));
        }

        template.setTemplateName(request.getTemplateName());
        template.setDescription(request.getDescription());
        template.setFileFormat(request.getFileFormat());
        template.setSeparator(request.getSeparator());
        template.setEncoding(request.getEncoding());
        template.setHasHeader(request.getHasHeader());
        template.setSkipRows(request.getSkipRows());
        template.setWorksheetName(request.getWorksheetName());
        template.setStartRow(request.getStartRow());
        template.setEndRow(request.getEndRow());
        template.setColumnMapping(request.getColumnMapping());
        template.setValidationRules(request.getValidationRules());
        template.setTransformations(request.getTransformations());
        template.setIsDefault(request.getIsDefault());
        template.setIsActive(request.getIsActive());

        return templateRepository.save(template);
    }

    /**
     * Supprime un template
     */
    @Transactional
    public void deleteTemplate(Long templateId) {
        ActivityImportTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new ResourceNotFoundException("Template non trouvé: " + templateId));

        if (template.getIsDefault()) {
            throw new ImportException("Impossible de supprimer le template par défaut");
        }

        templateRepository.delete(template);
        log.info("Template supprimé: {}", templateId);
    }

    /**
     * Récupère un template par ID
     */
    public ActivityImportTemplate getTemplate(Long templateId) {
        return templateRepository.findById(templateId)
            .orElseThrow(() -> new ResourceNotFoundException("Template non trouvé: " + templateId));
    }

    /**
     * Liste tous les templates actifs d'une entreprise
     */
    public List<ActivityImportTemplate> getActiveTemplates(Long companyId) {
        Company company = getCompany(companyId);
        return templateRepository.findByCompanyAndIsActiveTrue(company);
    }

    /**
     * Liste tous les templates d'une entreprise
     */
    public List<ActivityImportTemplate> getAllTemplates(Long companyId) {
        Company company = getCompany(companyId);
        return templateRepository.findByCompany(company);
    }

    /**
     * Récupère le template par défaut d'une entreprise
     */
    public Optional<ActivityImportTemplate> getDefaultTemplate(Long companyId) {
        Company company = getCompany(companyId);
        return templateRepository.findByCompanyAndIsDefaultTrue(company);
    }

    /**
     * Définit un template comme template par défaut
     */
    @Transactional
    public ActivityImportTemplate setAsDefault(Long templateId) {
        ActivityImportTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new ResourceNotFoundException("Template non trouvé: " + templateId));

        template.setIsDefault(true);
        return templateRepository.save(template);
        // Note: Le trigger SQL ensure_single_default_template désactive automatiquement les autres
    }

    /**
     * Valide la configuration d'un template
     */
    public List<String> validateTemplateConfig(TemplateConfigurationRequest request) {
        List<String> errors = new ArrayList<>();

        // Vérifier que columnMapping contient au moins les champs requis
        Map<String, Object> columnMapping = request.getColumnMapping();
        if (columnMapping == null || columnMapping.isEmpty()) {
            errors.add("Le mapping des colonnes est obligatoire");
            return errors;
        }

        // Champs obligatoires
        String[] requiredFields = {"date", "activity", "amount"};
        for (String field : requiredFields) {
            if (!columnMapping.containsKey(field)) {
                errors.add("Le champ '" + field + "' est obligatoire dans le mapping");
            }
        }

        // Vérifier que chaque champ a soit columnIndex, soit columnLetter, soit columnName
        for (Map.Entry<String, Object> entry : columnMapping.entrySet()) {
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fieldConfig = (Map<String, Object>) entry.getValue();

                boolean hasIndex = fieldConfig.containsKey("columnIndex");
                boolean hasLetter = fieldConfig.containsKey("columnLetter");
                boolean hasName = fieldConfig.containsKey("columnName");

                if (!hasIndex && !hasLetter && !hasName) {
                    errors.add("Le champ '" + entry.getKey() + "' doit avoir soit columnIndex, columnLetter ou columnName");
                }
            }
        }

        return errors;
    }

    /**
     * Incrémente le compteur d'utilisation d'un template
     */
    @Transactional
    public void incrementUsage(Long templateId) {
        ActivityImportTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new ResourceNotFoundException("Template non trouvé: " + templateId));

        template.incrementUsage();
        templateRepository.save(template);
    }

    private Company getCompany(Long companyId) {
        return companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée: " + companyId));
    }
}
