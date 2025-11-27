package com.predykt.accounting.service.parser;

import com.opencsv.CSVReader;
import com.predykt.accounting.dto.request.BankTransactionImportDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser pour fichiers CSV génériques
 * Format attendu: Date, Description, Montant, Référence
 * ou: Date, Débit, Crédit, Description, Référence
 */
@Slf4j
@Component
public class CsvGenericBankStatementParser implements BankStatementParser {

    private static final DateTimeFormatter[] DATE_FORMATS = {
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy")
    };

    @Override
    public List<BankTransactionImportDto> parse(MultipartFile file) throws Exception {
        return parse(file.getInputStream(), file.getOriginalFilename());
    }

    @Override
    public List<BankTransactionImportDto> parse(InputStream inputStream, String fileName) throws Exception {
        log.info("Parsing CSV file: {}", fileName);

        List<BankTransactionImportDto> transactions = new ArrayList<>();

        try (CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            List<String[]> allRows = csvReader.readAll();

            if (allRows.isEmpty()) {
                log.warn("CSV file is empty");
                return transactions;
            }

            // Détecter le format du CSV à partir du header
            String[] header = allRows.get(0);
            CsvFormat format = detectCsvFormat(header);
            log.info("Detected CSV format: {}", format);

            // Parser les lignes de données
            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);

                // Ignorer les lignes vides
                if (row.length == 0 || (row.length == 1 && (row[0] == null || row[0].trim().isEmpty()))) {
                    continue;
                }

                try {
                    BankTransactionImportDto transaction = parseRow(row, format);
                    if (transaction != null) {
                        transactions.add(transaction);
                    }
                } catch (Exception e) {
                    log.warn("Error parsing CSV row {}: {} - {}", i, String.join(",", row), e.getMessage());
                }
            }
        }

        log.info("CSV parsing completed: {} transactions found", transactions.size());
        return transactions;
    }

    @Override
    public boolean supports(String fileName, String contentType) {
        if (fileName == null) {
            return false;
        }
        return fileName.toLowerCase().endsWith(".csv");
    }

    @Override
    public String getFormatName() {
        return "CSV Générique";
    }

    /**
     * Détecte le format CSV à partir du header
     */
    private CsvFormat detectCsvFormat(String[] header) {
        if (header.length < 3) {
            return CsvFormat.SIMPLE;
        }

        // Vérifier si on a des colonnes Débit/Crédit séparées
        boolean hasDebit = false;
        boolean hasCredit = false;

        for (String col : header) {
            String colLower = col.toLowerCase();
            if (colLower.contains("débit") || colLower.contains("debit")) {
                hasDebit = true;
            }
            if (colLower.contains("crédit") || colLower.contains("credit")) {
                hasCredit = true;
            }
        }

        if (hasDebit && hasCredit) {
            return CsvFormat.DEBIT_CREDIT;
        }

        return CsvFormat.SIMPLE;
    }

    /**
     * Parse une ligne CSV selon le format détecté
     */
    private BankTransactionImportDto parseRow(String[] row, CsvFormat format) {
        if (format == CsvFormat.DEBIT_CREDIT) {
            return parseDebitCreditRow(row);
        } else {
            return parseSimpleRow(row);
        }
    }

    /**
     * Format simple: Date, Description, Montant, Référence
     */
    private BankTransactionImportDto parseSimpleRow(String[] row) {
        if (row.length < 3) {
            return null;
        }

        LocalDate date = parseDate(row[0]);
        if (date == null) {
            return null;
        }

        String description = row.length > 1 ? row[1].trim() : "";
        BigDecimal amount = parseAmount(row.length > 2 ? row[2].trim() : "0");
        String reference = row.length > 3 ? row[3].trim() : null;

        return BankTransactionImportDto.builder()
            .transactionDate(date)
            .valueDate(date)
            .amount(amount)
            .description(description)
            .bankReference(reference)
            .build();
    }

    /**
     * Format débit/crédit: Date, Débit, Crédit, Description, Référence
     */
    private BankTransactionImportDto parseDebitCreditRow(String[] row) {
        if (row.length < 4) {
            return null;
        }

        LocalDate date = parseDate(row[0]);
        if (date == null) {
            return null;
        }

        BigDecimal debit = parseAmount(row[1]);
        BigDecimal credit = parseAmount(row[2]);

        // Montant = Crédit - Débit (positif si crédit, négatif si débit)
        BigDecimal amount = credit.subtract(debit);

        String description = row.length > 3 ? row[3].trim() : "";
        String reference = row.length > 4 ? row[4].trim() : null;

        return BankTransactionImportDto.builder()
            .transactionDate(date)
            .valueDate(date)
            .amount(amount)
            .description(description)
            .bankReference(reference)
            .build();
    }

    /**
     * Parse une date avec plusieurs formats possibles
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(dateStr.trim(), formatter);
            } catch (Exception ignored) {
            }
        }

        log.warn("Cannot parse date: {}", dateStr);
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
            // Nettoyer le montant
            String cleaned = amountStr
                .replaceAll("\\s", "")
                .replace(",", ".");

            // Gérer les parenthèses (montant négatif)
            if (cleaned.startsWith("(") && cleaned.endsWith(")")) {
                cleaned = "-" + cleaned.substring(1, cleaned.length() - 1);
            }

            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Cannot parse amount: {}", amountStr);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Formats CSV supportés
     */
    private enum CsvFormat {
        SIMPLE,        // Date, Description, Montant, Référence
        DEBIT_CREDIT   // Date, Débit, Crédit, Description, Référence
    }
}
