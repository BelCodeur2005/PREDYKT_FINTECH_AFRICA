package com.predykt.accounting.service.parser.activity;

import com.predykt.accounting.domain.entity.ActivityImportTemplate;
import com.predykt.accounting.domain.enums.ActivityCsvFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Factory pour sélectionner le bon parser d'activités
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityParserFactory {

    private final GenericActivityCsvParser genericParser;
    private final CustomActivityCsvParser customParser;
    private final SapActivityCsvParser sapParser;

    /**
     * Récupère le parser approprié selon le format et le template
     *
     * @param fileName Nom du fichier
     * @param format Format spécifié (peut être null pour auto-détection)
     * @param template Template personnalisé (peut être null)
     * @return Parser approprié
     */
    public ActivityCsvParser getParser(String fileName, ActivityCsvFormat format, ActivityImportTemplate template) {
        log.info("Selecting parser for file: {}, format: {}, template: {}",
            fileName, format, template != null ? template.getTemplateName() : "none");

        // Si template fourni, utiliser CustomActivityCsvParser
        if (template != null) {
            log.info("Using custom template parser: {}", template.getTemplateName());
            customParser.setTemplate(template);
            return customParser;
        }

        // Si format non spécifié, détecter
        if (format == null) {
            format = ActivityCsvFormat.detectFromFileName(fileName);
            log.info("Auto-detected format: {}", format);
        }

        // Sélectionner le parser selon le format
        ActivityCsvParser parser = switch (format) {
            case SAP_EXPORT -> sapParser;
            case CUSTOM_TEMPLATE -> {
                log.warn("CUSTOM_TEMPLATE format sans template fourni, using generic parser");
                yield genericParser;
            }
            default -> genericParser;
        };

        log.info("Selected parser: {}", parser.getFormatName());
        return parser;
    }

    /**
     * Récupère le parser sans format spécifié (auto-détection)
     */
    public ActivityCsvParser getParser(String fileName, ActivityImportTemplate template) {
        return getParser(fileName, null, template);
    }
}
