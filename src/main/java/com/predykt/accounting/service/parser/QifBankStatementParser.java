package com.predykt.accounting.service.parser;

import com.predykt.accounting.dto.request.BankTransactionImportDto;
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
 * Parser pour fichiers QIF (Quicken Interchange Format)
 * Format texte simple utilisé par plusieurs banques
 *
 * Structure QIF:
 * !Type:Bank
 * D<date>
 * T<montant>
 * P<bénéficiaire>
 * M<mémo>
 * N<numéro de chèque/référence>
 * ^
 */
@Slf4j
@Component
public class QifBankStatementParser implements BankStatementParser {

    private static final DateTimeFormatter[] QIF_DATE_FORMATS = {
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yy"),
        DateTimeFormatter.ofPattern("MM/dd/yy")
    };

    @Override
    public List<BankTransactionImportDto> parse(MultipartFile file) throws Exception {
        return parse(file.getInputStream(), file.getOriginalFilename());
    }

    @Override
    public List<BankTransactionImportDto> parse(InputStream inputStream, String fileName) throws Exception {
        log.info("Parsing QIF file: {}", fileName);

        List<BankTransactionImportDto> transactions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            QifTransaction currentTransaction = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                // Début d'un nouveau type de données
                if (line.startsWith("!Type:")) {
                    log.debug("QIF Type: {}", line.substring(6));
                    continue;
                }

                // Fin de transaction
                if (line.equals("^")) {
                    if (currentTransaction != null && currentTransaction.date != null &&
                        currentTransaction.amount != null) {
                        transactions.add(currentTransaction.toDto());
                    }
                    currentTransaction = null;
                    continue;
                }

                // Début d'une nouvelle transaction
                if (currentTransaction == null) {
                    currentTransaction = new QifTransaction();
                }

                // Parser les champs
                if (line.length() < 2) {
                    continue;
                }

                char fieldCode = line.charAt(0);
                String value = line.substring(1).trim();

                switch (fieldCode) {
                    case 'D': // Date
                        currentTransaction.date = parseQifDate(value);
                        break;
                    case 'T': // Montant (Transaction amount)
                        currentTransaction.amount = parseAmount(value);
                        break;
                    case 'U': // Montant (alternative)
                        if (currentTransaction.amount == null) {
                            currentTransaction.amount = parseAmount(value);
                        }
                        break;
                    case 'P': // Payee (bénéficiaire/tiers)
                        currentTransaction.payee = value;
                        break;
                    case 'M': // Memo (description)
                        currentTransaction.memo = value;
                        break;
                    case 'N': // Number (référence/numéro de chèque)
                        currentTransaction.reference = value;
                        break;
                    case 'C': // Cleared status
                        currentTransaction.clearedStatus = value;
                        break;
                    default:
                        log.debug("Unknown QIF field code: {}", fieldCode);
                }
            }

            // Sauvegarder la dernière transaction si elle n'a pas été terminée par ^
            if (currentTransaction != null && currentTransaction.date != null &&
                currentTransaction.amount != null) {
                transactions.add(currentTransaction.toDto());
            }
        }

        log.info("QIF parsing completed: {} transactions found", transactions.size());
        return transactions;
    }

    @Override
    public boolean supports(String fileName, String contentType) {
        if (fileName == null) {
            return false;
        }
        return fileName.toLowerCase().endsWith(".qif");
    }

    @Override
    public String getFormatName() {
        return "QIF (Quicken Interchange Format)";
    }

    /**
     * Parse une date QIF (plusieurs formats possibles)
     */
    private LocalDate parseQifDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        // Nettoyer les apostrophes qui peuvent être présentes
        dateStr = dateStr.replace("'", "");

        for (DateTimeFormatter formatter : QIF_DATE_FORMATS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (Exception ignored) {
            }
        }

        log.warn("Cannot parse QIF date: {}", dateStr);
        return null;
    }

    /**
     * Parse un montant QIF
     */
    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.isEmpty()) {
            return null;
        }

        try {
            // Supprimer les espaces et les symboles monétaires
            String cleaned = amountStr
                .replaceAll("[^0-9.,\\-]", "")
                .replace(",", "");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Cannot parse QIF amount: {}", amountStr);
            return null;
        }
    }

    /**
     * Classe interne pour représenter une transaction QIF
     */
    private static class QifTransaction {
        LocalDate date;
        BigDecimal amount;
        String payee;
        String memo;
        String reference;
        String clearedStatus;

        BankTransactionImportDto toDto() {
            String description = "";
            if (payee != null && !payee.isEmpty()) {
                description = payee;
            }
            if (memo != null && !memo.isEmpty()) {
                if (!description.isEmpty()) {
                    description += " - ";
                }
                description += memo;
            }

            return BankTransactionImportDto.builder()
                .transactionDate(date)
                .valueDate(date)
                .amount(amount)
                .description(description.isEmpty() ? "Transaction" : description)
                .thirdPartyName(payee)
                .bankReference(reference)
                .additionalInfo(clearedStatus)
                .build();
        }
    }
}
