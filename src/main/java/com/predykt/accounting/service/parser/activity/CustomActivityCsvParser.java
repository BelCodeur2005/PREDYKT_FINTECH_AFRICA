package com.predykt.accounting.service.parser.activity;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.predykt.accounting.domain.entity.ActivityImportTemplate;
import com.predykt.accounting.domain.enums.ActivityCsvFormat;
import com.predykt.accounting.dto.ActivityImportDto;
import com.predykt.accounting.exception.ImportException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parser basé sur un template personnalisé
 * Utilise la configuration du template pour parser le fichier
 */
@Slf4j
@Component
public class CustomActivityCsvParser implements ActivityCsvParser {

    private ActivityImportTemplate template;

    /**
     * Configure le parser avec un template
     */
    public void setTemplate(ActivityImportTemplate template) {
        this.template = template;
    }

    @Override
    public List<ActivityImportDto> parse(MultipartFile file) throws Exception {
        if (template == null) {
            throw new IllegalStateException("Template non configuré");
        }
        return parse(file.getInputStream(), file.getOriginalFilename());
    }

    @Override
    public List<ActivityImportDto> parse(InputStream inputStream, String fileName) throws Exception {
        if (template == null) {
            throw new IllegalStateException("Template non configuré");
        }

        log.info("Parsing custom CSV with template: {}", template.getTemplateName());

        List<ActivityImportDto> activities = new ArrayList<>();

        Charset charset = Charset.forName(template.getEncoding());
        char separator = template.getSeparator();

        try (CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream, charset))) {
            List<String[]> allRows = csvReader.readAll();

            if (allRows.isEmpty()) {
                throw new ImportException("Fichier CSV vide");
            }

            // Skip header si configuré
            int startIndex = template.getHasHeader() ? 1 : 0;

            // Skip rows additionnels si configuré
            startIndex += template.getSkipRows();

            int rowNumber = 0;
            for (int i = startIndex; i < allRows.size(); i++) {
                rowNumber++;
                String[] row = allRows.get(i);

                // Ignorer lignes vides
                if (row.length == 0) {
                    continue;
                }

                try {
                    ActivityImportDto activity = parseRowWithTemplate(row, rowNumber);
                    if (activity != null) {
                        activities.add(activity);
                    }
                } catch (Exception e) {
                    log.warn("Erreur parsing ligne {}: {}", rowNumber, e.getMessage());
                }
            }

        } catch (IOException | CsvException e) {
            throw new ImportException("Erreur lecture fichier CSV: " + e.getMessage());
        }

        log.info("Custom CSV parsing completed: {} activities found", activities.size());
        return activities;
    }

    @Override
    public boolean supports(String fileName, ActivityCsvFormat format) {
        return format == ActivityCsvFormat.CUSTOM_TEMPLATE;
    }

    @Override
    public String getFormatName() {
        return template != null ? "Template: " + template.getTemplateName() : "Template Personnalisé";
    }

    @Override
    public String getFormatDescription() {
        return template != null ? template.getDescription() : "Format personnalisé basé sur template";
    }

    /**
     * Parse une ligne selon le template
     */
    private ActivityImportDto parseRowWithTemplate(String[] row, int rowNumber) {
        Map<String, Object> columnMapping = template.getColumnMapping();

        try {
            // Extraire date
            LocalDate date = extractDate(row, columnMapping);
            if (date == null) {
                return null;
            }

            // Extraire activité
            String activity = extractString(row, columnMapping, "activity");

            // Extraire description (optionnel)
            String description = extractString(row, columnMapping, "description");
            if (description == null || description.isEmpty()) {
                description = activity;
            }

            // Extraire montant
            BigDecimal amount = extractAmount(row, columnMapping);

            // Extraire type (optionnel)
            String type = extractString(row, columnMapping, "type");

            return ActivityImportDto.builder()
                .rowNumber(rowNumber)
                .date(date)
                .activity(activity)
                .description(description)
                .amount(amount)
                .type(type)
                .build();

        } catch (Exception e) {
            log.warn("Erreur parsing ligne {} avec template: {}", rowNumber, e.getMessage());
            return null;
        }
    }

    /**
     * Extrait une date selon la configuration
     */
    private LocalDate extractDate(String[] row, Map<String, Object> columnMapping) {
        @SuppressWarnings("unchecked")
        Map<String, Object> dateConfig = (Map<String, Object>) columnMapping.get("date");

        if (dateConfig == null) {
            return null;
        }

        int columnIndex = getColumnIndex(dateConfig, row);
        if (columnIndex >= row.length) {
            return null;
        }

        String dateStr = row[columnIndex].trim();
        String dateFormat = (String) dateConfig.getOrDefault("dateFormat", "dd/MM/yyyy");

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
            return LocalDate.parse(dateStr, formatter);
        } catch (Exception e) {
            log.warn("Erreur parsing date '{}' avec format '{}': {}", dateStr, dateFormat, e.getMessage());
            return null;
        }
    }

    /**
     * Extrait un string selon la configuration
     */
    private String extractString(String[] row, Map<String, Object> columnMapping, String fieldName) {
        @SuppressWarnings("unchecked")
        Map<String, Object> fieldConfig = (Map<String, Object>) columnMapping.get(fieldName);

        if (fieldConfig == null) {
            return null;
        }

        int columnIndex = getColumnIndex(fieldConfig, row);
        if (columnIndex >= row.length) {
            return null;
        }

        return row[columnIndex].trim();
    }

    /**
     * Extrait un montant selon la configuration
     */
    private BigDecimal extractAmount(String[] row, Map<String, Object> columnMapping) {
        @SuppressWarnings("unchecked")
        Map<String, Object> amountConfig = (Map<String, Object>) columnMapping.get("amount");

        if (amountConfig == null) {
            return BigDecimal.ZERO;
        }

        int columnIndex = getColumnIndex(amountConfig, row);
        if (columnIndex >= row.length) {
            return BigDecimal.ZERO;
        }

        String amountStr = row[columnIndex].trim();

        try {
            String cleaned = amountStr
                .replaceAll("\\s", "")
                .replace(",", ".");

            if (cleaned.startsWith("(") && cleaned.endsWith(")")) {
                cleaned = "-" + cleaned.substring(1, cleaned.length() - 1);
            }

            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Format de montant invalide: {}", amountStr);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Récupère l'index de la colonne
     */
    private int getColumnIndex(Map<String, Object> fieldConfig, String[] row) {
        // Vérifier columnIndex
        if (fieldConfig.containsKey("columnIndex")) {
            return ((Number) fieldConfig.get("columnIndex")).intValue();
        }

        // Vérifier columnName
        if (fieldConfig.containsKey("columnName") && template.getHasHeader()) {
            String columnName = (String) fieldConfig.get("columnName");
            // Rechercher dans le header (row[0] si disponible)
            // Pour simplifier, on retourne 0 ici
            // TODO: Améliorer en cherchant dans le header réel
            return 0;
        }

        return 0;
    }
}
