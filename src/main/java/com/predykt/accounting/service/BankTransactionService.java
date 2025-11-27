package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.BankTransaction;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.GeneralLedger;
import com.predykt.accounting.domain.enums.BankProvider;
import com.predykt.accounting.dto.request.BankTransactionImportDto;
import com.predykt.accounting.exception.ImportException;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.repository.BankTransactionRepository;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.repository.GeneralLedgerRepository;
import com.predykt.accounting.service.parser.BankStatementParser;
import com.predykt.accounting.service.parser.BankStatementParserFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankTransactionService {

    private final BankTransactionRepository transactionRepository;
    private final CompanyRepository companyRepository;
    private final GeneralLedgerRepository glRepository;
    private final BankStatementParserFactory parserFactory;
    
    /**
     * Importer des transactions depuis un fichier (tous formats supportés)
     * Formats supportés: OFX, MT940, CAMT.053, QIF, CSV
     *
     * @param companyId ID de l'entreprise
     * @param file Fichier à importer
     * @param bankProvider Banque émettrice (optionnel, aide à la détection du format)
     * @return Liste des transactions importées
     */
    @Transactional
    public List<BankTransaction> importTransactions(Long companyId, MultipartFile file, BankProvider bankProvider) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        if (file.isEmpty()) {
            throw new ImportException("Le fichier est vide");
        }

        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();

        log.info("Importing bank transactions from file: {} (type: {}, provider: {})",
            fileName, contentType, bankProvider != null ? bankProvider.getDisplayName() : "auto-detect");

        try {
            // Sélectionner le parser approprié
            BankStatementParser parser = parserFactory.getParser(fileName, contentType, bankProvider);

            // Parser le fichier
            List<BankTransactionImportDto> importDtos = parser.parse(file);
            log.info("Parsed {} transactions from file", importDtos.size());

            // Convertir en BankTransaction et sauvegarder
            List<BankTransaction> transactions = new ArrayList<>();
            int duplicateCount = 0;

            for (BankTransactionImportDto dto : importDtos) {
                // Vérifier les doublons par référence bancaire
                if (dto.getBankReference() != null &&
                    transactionRepository.existsByCompanyAndBankReference(company, dto.getBankReference())) {
                    log.debug("Duplicate transaction ignored: {}", dto.getBankReference());
                    duplicateCount++;
                    continue;
                }

                // Créer la transaction
                BankTransaction transaction = BankTransaction.builder()
                    .company(company)
                    .transactionDate(dto.getTransactionDate())
                    .valueDate(dto.getValueDate())
                    .amount(dto.getAmount())
                    .description(dto.getDescription())
                    .bankReference(dto.getBankReference())
                    .thirdPartyName(dto.getThirdPartyName())
                    .isReconciled(false)
                    .importedAt(LocalDate.now())
                    .importSource(parser.getFormatName())
                    .build();

                transactions.add(transactionRepository.save(transaction));
            }

            log.info("Import completed: {} transactions saved, {} duplicates ignored",
                transactions.size(), duplicateCount);

            return transactions;

        } catch (Exception e) {
            log.error("Error importing bank transactions from file: {}", fileName, e);
            throw new ImportException("Échec de l'import: " + e.getMessage(), e);
        }
    }

    /**
     * Import avec détection automatique du format (sans bankProvider)
     */
    @Transactional
    public List<BankTransaction> importTransactions(Long companyId, MultipartFile file, String format) {
        // Convertir le format string en BankProvider si possible
        BankProvider provider = null;
        if (format != null && !format.isEmpty()) {
            try {
                provider = BankProvider.valueOf(format.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.debug("Format '{}' is not a bank provider, will auto-detect", format);
            }
        }
        return importTransactions(companyId, file, provider);
    }
    
    public List<BankTransaction> getTransactionsByDateRange(Long companyId, 
                                                            LocalDate startDate, 
                                                            LocalDate endDate) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));
        
        return transactionRepository.findByCompanyAndTransactionDateBetween(company, startDate, endDate);
    }
    
    public List<BankTransaction> getUnreconciledTransactions(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));
        
        return transactionRepository.findByCompanyAndIsReconciledFalse(company);
    }
    
    @Transactional
    public void reconcileTransaction(Long companyId, Long transactionId, Long glEntryId) {
        BankTransaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction non trouvée"));
        
        if (!transaction.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Transaction n'appartient pas à cette entreprise");
        }
        
        GeneralLedger glEntry = glRepository.findById(glEntryId)
            .orElseThrow(() -> new ResourceNotFoundException("Écriture comptable non trouvée"));
        
        transaction.setGlEntry(glEntry);
        transaction.setIsReconciled(true);
        
        transactionRepository.save(transaction);
        
        log.info("Transaction {} réconciliée avec l'écriture {}", transactionId, glEntryId);
    }
    
}