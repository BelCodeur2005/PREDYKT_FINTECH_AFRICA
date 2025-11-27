package com.predykt.accounting.service.parser;

import com.predykt.accounting.dto.request.BankTransactionImportDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * Interface commune pour tous les parsers de relevés bancaires
 */
public interface BankStatementParser {

    /**
     * Parse un fichier de relevé bancaire et retourne une liste de transactions
     *
     * @param file Fichier multipart à parser
     * @return Liste des transactions parsées
     * @throws Exception en cas d'erreur de parsing
     */
    List<BankTransactionImportDto> parse(MultipartFile file) throws Exception;

    /**
     * Parse un input stream de relevé bancaire
     *
     * @param inputStream Stream à parser
     * @param fileName Nom du fichier (pour détection format)
     * @return Liste des transactions parsées
     * @throws Exception en cas d'erreur de parsing
     */
    List<BankTransactionImportDto> parse(InputStream inputStream, String fileName) throws Exception;

    /**
     * Vérifie si le parser peut traiter ce type de fichier
     *
     * @param fileName Nom du fichier
     * @param contentType Type MIME du fichier
     * @return true si le parser supporte ce format
     */
    boolean supports(String fileName, String contentType);

    /**
     * Retourne le nom du format supporté
     */
    String getFormatName();
}
