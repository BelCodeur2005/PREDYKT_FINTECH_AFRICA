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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser pour fichiers MT940 (SWIFT format)
 * Format texte SWIFT utilisé par SGBC, BICEC, Afriland First Bank
 *
 * Structure MT940:
 * :20:  Référence de la transaction
 * :25:  Numéro de compte
 * :28C: Numéro de relevé
 * :60F: Solde d'ouverture
 * :61:  Transaction
 * :86:  Informations complémentaires
 * :62F: Solde de clôture
 */
@Slf4j
@Component
public class Mt940BankStatementParser implements BankStatementParser {

    private static final DateTimeFormatter MT940_DATE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd");
    private static final Pattern TRANSACTION_PATTERN = Pattern.compile(
        ":(61):(\\d{6})(\\d{4})?(C|D|RC|RD)([\\d,\\.]+).*"
    );

    @Override
    public List<BankTransactionImportDto> parse(MultipartFile file) throws Exception {
        return parse(file.getInputStream(), file.getOriginalFilename());
    }

    @Override
    public List<BankTransactionImportDto> parse(InputStream inputStream, String fileName) throws Exception {
        log.info("Parsing MT940 file: {}", fileName);

        List<BankTransactionImportDto> transactions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            String accountNumber = null;
            String currentTransactionRef = null;
            LocalDate currentDate = null;
            BigDecimal currentAmount = null;
            String currentDescription = "";
            String debitCredit = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Numéro de compte (:25:)
                if (line.startsWith(":25:")) {
                    accountNumber = line.substring(4).trim();
                    log.debug("Account number: {}", accountNumber);
                }

                // Transaction (:61:)
                else if (line.startsWith(":61:")) {
                    // Sauvegarder la transaction précédente
                    if (currentDate != null && currentAmount != null) {
                        transactions.add(buildTransaction(
                            currentDate, currentAmount, debitCredit,
                            currentDescription, currentTransactionRef, accountNumber
                        ));
                    }

                    // Parser la nouvelle transaction
                    Mt940Transaction mt940Txn = parseMt940TransactionLine(line);
                    if (mt940Txn != null) {
                        currentDate = mt940Txn.date;
                        currentAmount = mt940Txn.amount;
                        debitCredit = mt940Txn.debitCredit;
                        currentTransactionRef = mt940Txn.reference;
                        currentDescription = "";
                    }
                }

                // Informations complémentaires (:86:)
                else if (line.startsWith(":86:")) {
                    currentDescription = line.substring(4).trim();
                }

                // Continuation de la ligne précédente (pas de tag)
                else if (!line.startsWith(":") && !currentDescription.isEmpty()) {
                    currentDescription += " " + line;
                }
            }

            // Sauvegarder la dernière transaction
            if (currentDate != null && currentAmount != null) {
                transactions.add(buildTransaction(
                    currentDate, currentAmount, debitCredit,
                    currentDescription, currentTransactionRef, accountNumber
                ));
            }
        }

        log.info("MT940 parsing completed: {} transactions found", transactions.size());
        return transactions;
    }

    @Override
    public boolean supports(String fileName, String contentType) {
        if (fileName == null) {
            return false;
        }
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".mt940") || lowerName.endsWith(".sta") ||
               (lowerName.endsWith(".txt") && contentType != null && contentType.contains("text"));
    }

    @Override
    public String getFormatName() {
        return "MT940 (SWIFT)";
    }

    /**
     * Parse une ligne de transaction MT940
     * Format: :61:YYMMDD[MMDD]C/D<montant>[monnaie][ref]
     */
    private Mt940Transaction parseMt940TransactionLine(String line) {
        try {
            // Retirer le tag :61:
            String content = line.substring(4);

            // Extraire la date (6 premiers caractères: YYMMDD)
            String dateStr = content.substring(0, 6);
            LocalDate date = LocalDate.parse(dateStr, MT940_DATE_FORMAT);

            // Chercher le marqueur C (crédit) ou D (débit)
            int dcIndex = -1;
            String debitCredit = null;
            for (int i = 6; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == 'C' || c == 'D') {
                    dcIndex = i;
                    debitCredit = String.valueOf(c);
                    break;
                }
                // Peut aussi être RC (reverse credit) ou RD (reverse debit)
                if (i + 1 < content.length() && c == 'R' &&
                    (content.charAt(i + 1) == 'C' || content.charAt(i + 1) == 'D')) {
                    dcIndex = i;
                    debitCredit = content.substring(i, i + 2);
                    break;
                }
            }

            if (dcIndex == -1) {
                log.warn("Cannot find D/C indicator in MT940 line: {}", line);
                return null;
            }

            // Extraire le montant (après D/C jusqu'au prochain caractère non-numérique)
            int amountStart = dcIndex + debitCredit.length();
            StringBuilder amountStr = new StringBuilder();
            for (int i = amountStart; i < content.length(); i++) {
                char c = content.charAt(i);
                if (Character.isDigit(c) || c == ',' || c == '.') {
                    amountStr.append(c);
                } else {
                    break;
                }
            }

            BigDecimal amount = new BigDecimal(amountStr.toString().replace(',', '.'));

            // Si débit, montant négatif
            if (debitCredit.equals("D") || debitCredit.equals("RD")) {
                amount = amount.negate();
            }

            // Extraire la référence (tout ce qui reste)
            String reference = content.substring(amountStart + amountStr.length()).trim();

            Mt940Transaction transaction = new Mt940Transaction();
            transaction.date = date;
            transaction.amount = amount;
            transaction.debitCredit = debitCredit;
            transaction.reference = reference;

            return transaction;

        } catch (Exception e) {
            log.warn("Error parsing MT940 transaction line: {} - {}", line, e.getMessage());
            return null;
        }
    }

    /**
     * Construit un BankTransactionImportDto
     */
    private BankTransactionImportDto buildTransaction(LocalDate date, BigDecimal amount,
                                                       String debitCredit, String description,
                                                       String reference, String accountNumber) {
        return BankTransactionImportDto.builder()
            .transactionDate(date)
            .valueDate(date)
            .amount(amount)
            .description(description)
            .bankReference(reference)
            .accountNumber(accountNumber)
            .additionalInfo("MT940 " + debitCredit)
            .build();
    }

    /**
     * Classe interne pour représenter une transaction MT940
     */
    private static class Mt940Transaction {
        LocalDate date;
        BigDecimal amount;
        String debitCredit;
        String reference;
    }
}
