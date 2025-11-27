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
 * Parser pour fichiers OFX (Open Financial Exchange)
 * Format XML standard utilisé par Ecobank, UBA, BOA, Standard Bank
 */
@Slf4j
@Component
public class OfxBankStatementParser implements BankStatementParser {

    private static final DateTimeFormatter OFX_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter OFX_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public List<BankTransactionImportDto> parse(MultipartFile file) throws Exception {
        return parse(file.getInputStream(), file.getOriginalFilename());
    }

    @Override
    public List<BankTransactionImportDto> parse(InputStream inputStream, String fileName) throws Exception {
        log.info("Parsing OFX file: {}", fileName);

        List<BankTransactionImportDto> transactions = new ArrayList<>();

        // Le format OFX peut être SGML ou XML
        // On va d'abord nettoyer le contenu pour le rendre XML-compatible
        String content = new String(inputStream.readAllBytes());
        String xmlContent = convertOfxToXml(content);

        // Parser le XML
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new java.io.ByteArrayInputStream(xmlContent.getBytes()));

        // Extraire les transactions du relevé bancaire (STMTTRN)
        NodeList stmtTrnList = doc.getElementsByTagName("STMTTRN");

        for (int i = 0; i < stmtTrnList.getLength(); i++) {
            Element stmtTrn = (Element) stmtTrnList.item(i);

            BankTransactionImportDto transaction = BankTransactionImportDto.builder()
                .transactionDate(parseOfxDate(getElementText(stmtTrn, "DTPOSTED")))
                .valueDate(parseOfxDate(getElementText(stmtTrn, "DTUSER")))
                .amount(new BigDecimal(getElementText(stmtTrn, "TRNAMT")))
                .bankReference(getElementText(stmtTrn, "FITID"))
                .description(getElementText(stmtTrn, "NAME") + " - " + getElementText(stmtTrn, "MEMO"))
                .thirdPartyName(getElementText(stmtTrn, "NAME"))
                .additionalInfo(getElementText(stmtTrn, "MEMO"))
                .currency(getElementText(stmtTrn, "CURRENCY"))
                .build();

            // Extraire le numéro de compte du relevé
            NodeList acctIdList = doc.getElementsByTagName("ACCTID");
            if (acctIdList.getLength() > 0) {
                transaction.setAccountNumber(acctIdList.item(0).getTextContent());
            }

            transactions.add(transaction);
        }

        log.info("OFX parsing completed: {} transactions found", transactions.size());
        return transactions;
    }

    @Override
    public boolean supports(String fileName, String contentType) {
        if (fileName == null) {
            return false;
        }
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".ofx") || lowerName.endsWith(".qfx");
    }

    @Override
    public String getFormatName() {
        return "OFX (Open Financial Exchange)";
    }

    /**
     * Convertit le format OFX SGML en XML valide
     */
    private String convertOfxToXml(String ofxContent) {
        // Supprimer le header OFX et garder seulement la partie XML/SGML
        int ofxStart = ofxContent.indexOf("<OFX>");
        if (ofxStart > 0) {
            ofxContent = ofxContent.substring(ofxStart);
        }

        // Ajouter la déclaration XML si absente
        if (!ofxContent.trim().startsWith("<?xml")) {
            ofxContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + ofxContent;
        }

        return ofxContent;
    }

    /**
     * Parse une date au format OFX (YYYYMMDD ou YYYYMMDDHHMMSS)
     */
    private LocalDate parseOfxDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        try {
            // Extraire seulement la partie date (8 premiers caractères)
            String datePart = dateStr.substring(0, Math.min(8, dateStr.length()));
            return LocalDate.parse(datePart, OFX_DATE_FORMAT);
        } catch (Exception e) {
            log.warn("Cannot parse OFX date: {}", dateStr);
            return null;
        }
    }

    /**
     * Récupère le texte d'un élément XML
     */
    private String getElementText(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            String text = nodeList.item(0).getTextContent();
            return text != null ? text.trim() : "";
        }
        return "";
    }
}
