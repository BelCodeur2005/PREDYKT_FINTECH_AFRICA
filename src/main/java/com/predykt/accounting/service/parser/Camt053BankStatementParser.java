package com.predykt.accounting.service.parser;

import com.predykt.accounting.dto.request.BankTransactionImportDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser pour fichiers CAMT.053 (ISO 20022)
 * Format XML européen utilisé par les banques du groupe Société Générale (SGBC)
 *
 * CAMT.053 = Bank-to-Customer Account Report
 */
@Slf4j
@Component
public class Camt053BankStatementParser implements BankStatementParser {

    private static final DateTimeFormatter CAMT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter CAMT_DATETIME_FORMAT = DateTimeFormatter.ISO_DATE_TIME;

    @Override
    public List<BankTransactionImportDto> parse(MultipartFile file) throws Exception {
        return parse(file.getInputStream(), file.getOriginalFilename());
    }

    @Override
    public List<BankTransactionImportDto> parse(InputStream inputStream, String fileName) throws Exception {
        log.info("Parsing CAMT.053 file: {}", fileName);

        List<BankTransactionImportDto> transactions = new ArrayList<>();

        // Parser le XML
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(inputStream);

        // Extraire le numéro de compte
        String accountNumber = extractAccountNumber(doc);

        // Extraire les transactions (Entry elements)
        NodeList entryList = doc.getElementsByTagNameNS("*", "Ntry");

        for (int i = 0; i < entryList.getLength(); i++) {
            Element entry = (Element) entryList.item(i);

            try {
                BankTransactionImportDto transaction = parseEntry(entry, accountNumber);
                if (transaction != null) {
                    transactions.add(transaction);
                }
            } catch (Exception e) {
                log.warn("Error parsing CAMT.053 entry: {}", e.getMessage());
            }
        }

        log.info("CAMT.053 parsing completed: {} transactions found", transactions.size());
        return transactions;
    }

    @Override
    public boolean supports(String fileName, String contentType) {
        if (fileName == null) {
            return false;
        }
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".xml") || lowerName.endsWith(".camt") ||
               (contentType != null && contentType.contains("xml"));
    }

    @Override
    public String getFormatName() {
        return "CAMT.053 (ISO 20022)";
    }

    /**
     * Extrait le numéro de compte du document CAMT.053
     */
    private String extractAccountNumber(Document doc) {
        NodeList acctList = doc.getElementsByTagNameNS("*", "Acct");
        if (acctList.getLength() > 0) {
            Element acct = (Element) acctList.item(0);
            NodeList ibanList = acct.getElementsByTagNameNS("*", "IBAN");
            if (ibanList.getLength() > 0) {
                return ibanList.item(0).getTextContent();
            }

            // Si pas d'IBAN, chercher un autre identifiant
            NodeList idList = acct.getElementsByTagNameNS("*", "Id");
            if (idList.getLength() > 0) {
                Element id = (Element) idList.item(0);
                NodeList othrList = id.getElementsByTagNameNS("*", "Othr");
                if (othrList.getLength() > 0) {
                    Element othr = (Element) othrList.item(0);
                    NodeList idNodeList = othr.getElementsByTagNameNS("*", "Id");
                    if (idNodeList.getLength() > 0) {
                        return idNodeList.item(0).getTextContent();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Parse un élément Entry (transaction)
     */
    private BankTransactionImportDto parseEntry(Element entry, String accountNumber) {
        // Date de la transaction
        LocalDate bookingDate = parseDate(getElementText(entry, "BookgDt", "Dt"));
        LocalDate valueDate = parseDate(getElementText(entry, "ValDt", "Dt"));

        // Montant
        Element amtElement = getFirstElement(entry, "Amt");
        if (amtElement == null) {
            return null;
        }
        BigDecimal amount = new BigDecimal(amtElement.getTextContent());

        // Devise
        String currency = amtElement.getAttribute("Ccy");

        // Crédit ou Débit
        String cdtDbtInd = getElementText(entry, "CdtDbtInd");
        if ("DBIT".equals(cdtDbtInd)) {
            amount = amount.negate();
        }

        // Référence bancaire
        String bankReference = getElementText(entry, "AcctSvcrRef");

        // Détails de la transaction
        Element ntryDtls = getFirstElement(entry, "NtryDtls");
        String description = "";
        String thirdPartyName = "";

        if (ntryDtls != null) {
            Element txDtls = getFirstElement(ntryDtls, "TxDtls");
            if (txDtls != null) {
                // Informations sur le tiers
                Element rltdPties = getFirstElement(txDtls, "RltdPties");
                if (rltdPties != null) {
                    // Créditeur
                    Element cdtr = getFirstElement(rltdPties, "Cdtr");
                    if (cdtr != null) {
                        thirdPartyName = getElementText(cdtr, "Nm");
                    }

                    // Débiteur
                    Element dbtr = getFirstElement(rltdPties, "Dbtr");
                    if (dbtr != null && thirdPartyName.isEmpty()) {
                        thirdPartyName = getElementText(dbtr, "Nm");
                    }
                }

                // Description
                Element rmtInf = getFirstElement(txDtls, "RmtInf");
                if (rmtInf != null) {
                    description = getElementText(rmtInf, "Ustrd");
                }

                // Si pas de description, utiliser AddtlTxInf
                if (description.isEmpty()) {
                    description = getElementText(txDtls, "AddtlTxInf");
                }
            }
        }

        // Si pas de description, utiliser AddtlNtryInf au niveau Entry
        if (description.isEmpty()) {
            description = getElementText(entry, "AddtlNtryInf");
        }

        return BankTransactionImportDto.builder()
            .transactionDate(bookingDate)
            .valueDate(valueDate)
            .amount(amount)
            .description(description.isEmpty() ? "Transaction" : description)
            .bankReference(bankReference)
            .thirdPartyName(thirdPartyName)
            .accountNumber(accountNumber)
            .currency(currency)
            .build();
    }

    /**
     * Récupère le texte d'un élément (avec namespace wildcard)
     */
    private String getElementText(Element parent, String... tagNames) {
        Element current = parent;
        for (String tagName : tagNames) {
            NodeList nodeList = current.getElementsByTagNameNS("*", tagName);
            if (nodeList.getLength() == 0) {
                return "";
            }
            current = (Element) nodeList.item(0);
        }
        String text = current.getTextContent();
        return text != null ? text.trim() : "";
    }

    /**
     * Récupère le premier élément enfant avec le tag donné
     */
    private Element getFirstElement(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagNameNS("*", tagName);
        if (nodeList.getLength() > 0) {
            return (Element) nodeList.item(0);
        }
        return null;
    }

    /**
     * Parse une date CAMT.053 (format ISO: yyyy-MM-dd)
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        try {
            // Essayer format date simple
            return LocalDate.parse(dateStr, CAMT_DATE_FORMAT);
        } catch (Exception e) {
            try {
                // Essayer format datetime
                return LocalDate.parse(dateStr.substring(0, 10), CAMT_DATE_FORMAT);
            } catch (Exception ex) {
                log.warn("Cannot parse CAMT.053 date: {}", dateStr);
                return null;
            }
        }
    }
}
