package com.predykt.accounting.service.parser.activity;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.predykt.accounting.domain.enums.ActivityCsvFormat;
import com.predykt.accounting.dto.ActivityImportDto;
import com.predykt.accounting.exception.ImportException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parser pour le format générique PREDYKT
 * Format attendu: date de saisie;Activitées;description;Montant Brut;Type;Années
 */
@Slf4j
@Component
public class GenericActivityCsvParser implements ActivityCsvParser {

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("d/M/yyyy")
    };

    @Override
    public List<ActivityImportDto> parse(MultipartFile file) throws Exception {
        return parse(file.getInputStream(), file.getOriginalFilename());
    }

    @Override
    public List<ActivityImportDto> parse(InputStream inputStream, String fileName) throws Exception {
        log.info("Parsing generic CSV file: {}", fileName);

        List<ActivityImportDto> activities = new ArrayList<>();

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(reader)) {

            List<String[]> allRows = csvReader.readAll();

            if (allRows.isEmpty()) {
                throw new ImportException("Fichier CSV vide");
            }

            // Déterminer le séparateur
            char separator = detectSeparator(allRows.get(0));
            log.info("Séparateur détecté: '{}'", separator);

            // Skip header
            boolean isFirstRow = true;
            int rowNumber = 0;

            for (String[] row : allRows) {
                rowNumber++;

                if (isFirstRow) {
                    isFirstRow = false;
                    continue;
                }

                // Ignorer les lignes vides
                if (row.length == 0 || (row.length == 1 && (row[0] == null || row[0].trim().isEmpty()))) {
                    continue;
                }

                // Re-split si le séparateur n'est pas le bon
                String[] columns = row;
                if (separator != ',' && row.length == 1) {
                    columns = row[0].split(String.valueOf(separator));
                }

                try {
                    ActivityImportDto activity = parseRow(columns, rowNumber);
                    if (activity != null) {
                        activities.add(activity);
                    }
                } catch (Exception e) {
                    log.warn("Ligne {} ignorée: {} - {}", rowNumber, Arrays.toString(row), e.getMessage());
                }
            }

        } catch (IOException | CsvException e) {
            throw new ImportException("Erreur lecture fichier CSV: " + e.getMessage());
        }

        log.info("Generic CSV parsing completed: {} activities found", activities.size());
        return activities;
    }

    @Override
    public boolean supports(String fileName, ActivityCsvFormat format) {
        return format == ActivityCsvFormat.GENERIC;
    }

    @Override
    public String getFormatName() {
        return "Format Générique PREDYKT";
    }

    @Override
    public String getFormatDescription() {
        return "Format: date de saisie;Activitées;description;Montant Brut;Type;Années";
    }

    /**
     * Parse une ligne CSV
     * Format: date de saisie;Activitées;description;Montant Brut;Type;Années
     */
    private ActivityImportDto parseRow(String[] columns, int rowNumber) {
        if (columns.length < 5) {
            return null; // Ligne incomplète
        }

        try {
            // Colonne 0: Date
            LocalDate date = parseDate(columns[0].trim());
            if (date == null) {
                return null;
            }

            // Colonne 1: Activité
            String activity = columns[1].trim();

            // Colonne 2: Description
            String description = columns.length > 2 ? columns[2].trim() : "";
            if (description.isEmpty() || description.equalsIgnoreCase("Description manquante")) {
                description = activity;
            }

            // Colonne 3: Montant
            BigDecimal amount = parseAmount(columns[3].trim());

            // Colonne 4: Type
            String type = columns[4].trim();

            return ActivityImportDto.builder()
                .rowNumber(rowNumber)
                .date(date)
                .activity(activity)
                .description(description)
                .amount(amount)
                .type(type)
                .build();

        } catch (Exception e) {
            log.warn("Erreur parsing ligne {}: {} - {}", rowNumber, Arrays.toString(columns), e.getMessage());
            return null;
        }
    }

    /**
     * Détecte le séparateur
     */
    private char detectSeparator(String[] firstRow) {
        if (firstRow.length == 1 && firstRow[0].contains(";")) {
            return ';';
        }
        return ',';
    }

    /**
     * Parse une date
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        log.warn("Format de date non reconnu: {}", dateStr);
        return null;
    }

    /**
     * Parse un montant
     */
    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

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
}
