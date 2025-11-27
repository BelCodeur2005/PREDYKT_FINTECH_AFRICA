package com.predykt.accounting.service.parser.activity;

import com.predykt.accounting.domain.enums.ActivityCsvFormat;
import com.predykt.accounting.dto.ActivityImportDto;
import com.predykt.accounting.exception.ImportException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser pour export SAP
 * Format: Posting Date|Document Type|GL Account|Amount|Description
 */
@Slf4j
@Component
public class SapActivityCsvParser implements ActivityCsvParser {

    private static final DateTimeFormatter SAP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String SEPARATOR = "\\|";

    @Override
    public List<ActivityImportDto> parse(MultipartFile file) throws Exception {
        return parse(file.getInputStream(), file.getOriginalFilename());
    }

    @Override
    public List<ActivityImportDto> parse(InputStream inputStream, String fileName) throws Exception {
        log.info("Parsing SAP export file: {}", fileName);

        List<ActivityImportDto> activities = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            boolean isFirstRow = true;
            int rowNumber = 0;

            while ((line = reader.readLine()) != null) {
                rowNumber++;

                if (isFirstRow) {
                    isFirstRow = false;
                    continue; // Skip header
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    ActivityImportDto activity = parseSapRow(line, rowNumber);
                    if (activity != null) {
                        activities.add(activity);
                    }
                } catch (Exception e) {
                    log.warn("Erreur parsing ligne SAP {}: {}", rowNumber, e.getMessage());
                }
            }

        } catch (Exception e) {
            throw new ImportException("Erreur lecture fichier SAP: " + e.getMessage());
        }

        log.info("SAP parsing completed: {} activities found", activities.size());
        return activities;
    }

    @Override
    public boolean supports(String fileName, ActivityCsvFormat format) {
        return format == ActivityCsvFormat.SAP_EXPORT;
    }

    @Override
    public String getFormatName() {
        return "Export SAP";
    }

    @Override
    public String getFormatDescription() {
        return "Format: Posting Date|Document Type|GL Account|Amount|Description (séparateur: |)";
    }

    /**
     * Parse une ligne SAP
     * Format: Posting Date|Document Type|GL Account|Amount|Description
     */
    private ActivityImportDto parseSapRow(String line, int rowNumber) {
        String[] columns = line.split(SEPARATOR);

        if (columns.length < 4) {
            log.warn("Ligne SAP incomplète: {}", line);
            return null;
        }

        try {
            // Colonne 0: Posting Date (YYYYMMDD)
            LocalDate date = LocalDate.parse(columns[0].trim(), SAP_DATE_FORMAT);

            // Colonne 1: Document Type (SA, KR, etc.)
            String documentType = columns[1].trim();

            // Colonne 2: GL Account (compte)
            String glAccount = columns[2].trim();

            // Colonne 3: Amount
            BigDecimal amount = new BigDecimal(columns[3].trim());

            // Colonne 4: Description
            String description = columns.length > 4 ? columns[4].trim() : documentType;

            return ActivityImportDto.builder()
                .rowNumber(rowNumber)
                .date(date)
                .activity(glAccount + " - " + documentType)
                .description(description)
                .amount(amount)
                .type(mapDocumentTypeToType(documentType))
                .build();

        } catch (Exception e) {
            log.warn("Erreur parsing ligne SAP: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Mappe le type de document SAP vers le type PREDYKT
     */
    private String mapDocumentTypeToType(String documentType) {
        return switch (documentType.toUpperCase()) {
            case "SA", "DR" -> "Revenu";      // Sales, Debit
            case "KR", "RE" -> "Dépenses";    // Vendor invoice, Credit
            case "AB" -> "Capex";              // Asset posting
            case "DZ" -> "Financing";          // Payment
            default -> "Dépenses";
        };
    }
}
