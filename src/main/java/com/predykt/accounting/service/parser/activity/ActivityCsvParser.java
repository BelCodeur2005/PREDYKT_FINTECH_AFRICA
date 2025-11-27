package com.predykt.accounting.service.parser.activity;

import com.predykt.accounting.domain.enums.ActivityCsvFormat;
import com.predykt.accounting.dto.ActivityImportDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * Interface commune pour tous les parsers d'activités CSV/Excel
 */
public interface ActivityCsvParser {

    /**
     * Parse un fichier multipart et retourne les activités
     *
     * @param file Fichier à parser
     * @return Liste des activités parsées
     * @throws Exception en cas d'erreur de parsing
     */
    List<ActivityImportDto> parse(MultipartFile file) throws Exception;

    /**
     * Parse un input stream
     *
     * @param inputStream Stream à parser
     * @param fileName Nom du fichier (pour détection)
     * @return Liste des activités parsées
     * @throws Exception en cas d'erreur de parsing
     */
    List<ActivityImportDto> parse(InputStream inputStream, String fileName) throws Exception;

    /**
     * Vérifie si ce parser supporte ce format
     *
     * @param fileName Nom du fichier
     * @param format Format détecté
     * @return true si supporté
     */
    boolean supports(String fileName, ActivityCsvFormat format);

    /**
     * Retourne le nom du format supporté
     */
    String getFormatName();

    /**
     * Description du format attendu
     */
    String getFormatDescription();
}
