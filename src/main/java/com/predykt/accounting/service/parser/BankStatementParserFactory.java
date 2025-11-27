package com.predykt.accounting.service.parser;

import com.predykt.accounting.domain.enums.BankProvider;
import com.predykt.accounting.domain.enums.BankStatementFormat;
import com.predykt.accounting.exception.ImportException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory pour sélectionner le bon parser de relevé bancaire
 * en fonction du format du fichier et de la banque
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BankStatementParserFactory {

    private final OfxBankStatementParser ofxParser;
    private final Mt940BankStatementParser mt940Parser;
    private final Camt053BankStatementParser camt053Parser;
    private final QifBankStatementParser qifParser;
    private final CsvGenericBankStatementParser csvGenericParser;

    /**
     * Obtient le parser approprié pour un fichier donné
     *
     * @param fileName    Nom du fichier
     * @param contentType Type MIME du fichier
     * @param provider    Banque émettrice (optionnel)
     * @return Parser approprié
     * @throws ImportException si aucun parser ne peut traiter ce fichier
     */
    public BankStatementParser getParser(String fileName, String contentType, BankProvider provider) {
        log.info("Selecting parser for file: {}, contentType: {}, provider: {}",
            fileName, contentType, provider != null ? provider.getDisplayName() : "unknown");

        // Détecter le format du fichier
        BankStatementFormat format;
        if (provider != null) {
            format = BankStatementFormat.fromBankProvider(provider, fileName);
        } else {
            format = BankStatementFormat.fromFileName(fileName);
        }

        log.info("Detected format: {}", format.getDisplayName());

        // Sélectionner le parser approprié
        BankStatementParser parser = switch (format) {
            case OFX -> ofxParser;
            case MT940 -> mt940Parser;
            case CAMT_053 -> camt053Parser;
            case QIF -> qifParser;
            case CSV_GENERIC, CSV_AFRILAND, CSV_ECOBANK, CSV_UBA, CSV_SGBC -> csvGenericParser;
        };

        // Vérifier que le parser supporte bien ce fichier
        if (!parser.supports(fileName, contentType)) {
            log.warn("Parser {} does not support file {}", parser.getFormatName(), fileName);
            // Fallback sur CSV générique
            parser = csvGenericParser;
        }

        log.info("Selected parser: {}", parser.getFormatName());
        return parser;
    }

    /**
     * Obtient le parser approprié (sans provider)
     */
    public BankStatementParser getParser(String fileName, String contentType) {
        return getParser(fileName, contentType, null);
    }

    /**
     * Obtient tous les parsers disponibles
     */
    public List<BankStatementParser> getAllParsers() {
        return List.of(ofxParser, mt940Parser, camt053Parser, qifParser, csvGenericParser);
    }

    /**
     * Détecte automatiquement le meilleur parser en testant tous les parsers
     * (utile quand le nom de fichier ne suffit pas)
     */
    public BankStatementParser detectParser(String fileName, String contentType) {
        log.info("Auto-detecting parser for file: {}", fileName);

        // Essayer chaque parser dans l'ordre de préférence
        List<BankStatementParser> parsersToTry = List.of(
            ofxParser,      // OFX est le plus structuré
            camt053Parser,  // CAMT.053 est aussi très structuré
            mt940Parser,    // MT940 est moins structuré
            qifParser,      // QIF est simple
            csvGenericParser // CSV en dernier recours
        );

        for (BankStatementParser parser : parsersToTry) {
            if (parser.supports(fileName, contentType)) {
                log.info("Auto-detected parser: {}", parser.getFormatName());
                return parser;
            }
        }

        // Par défaut, retourner CSV générique
        log.warn("No specific parser detected, using CSV generic parser");
        return csvGenericParser;
    }

    /**
     * Retourne les formats supportés par une banque
     */
    public List<BankStatementFormat> getSupportedFormats(BankProvider provider) {
        return switch (provider) {
            case ECOBANK_CEMAC, ECOBANK_UEMOA -> List.of(
                BankStatementFormat.OFX,
                BankStatementFormat.CSV_ECOBANK,
                BankStatementFormat.CSV_GENERIC
            );
            case UBA_CAMEROUN, UBA_GROUP -> List.of(
                BankStatementFormat.OFX,
                BankStatementFormat.CSV_UBA,
                BankStatementFormat.CSV_GENERIC
            );
            case SGBC -> List.of(
                BankStatementFormat.CAMT_053,
                BankStatementFormat.MT940,
                BankStatementFormat.CSV_SGBC,
                BankStatementFormat.CSV_GENERIC
            );
            case AFRILAND_FIRST_BANK -> List.of(
                BankStatementFormat.MT940,
                BankStatementFormat.CSV_AFRILAND,
                BankStatementFormat.CSV_GENERIC
            );
            case BOA, STANDARD_BANK -> List.of(
                BankStatementFormat.OFX,
                BankStatementFormat.MT940,
                BankStatementFormat.CSV_GENERIC
            );
            case BICEC -> List.of(
                BankStatementFormat.MT940,
                BankStatementFormat.CSV_GENERIC
            );
            default -> List.of(
                BankStatementFormat.CSV_GENERIC,
                BankStatementFormat.OFX,
                BankStatementFormat.QIF
            );
        };
    }
}
