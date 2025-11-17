package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.BankTransaction;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.GeneralLedger;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.repository.BankTransactionRepository;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.repository.GeneralLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankTransactionService {
    
    private final BankTransactionRepository transactionRepository;
    private final CompanyRepository companyRepository;
    private final GeneralLedgerRepository glRepository;
    
    /**
     * Importer des transactions depuis un fichier CSV
     */
    @Transactional
    public List<BankTransaction> importTransactions(Long companyId, MultipartFile file, String format) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));
        
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide");
        }
        
        List<BankTransaction> transactions = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isHeader = true;
            
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue; // Ignorer la ligne d'en-tête
                }
                
                String[] fields = line.split(",");
                
                if (fields.length < 3) {
                    log.warn("Ligne CSV invalide ignorée: {}", line);
                    continue;
                }
                
                BankTransaction transaction = BankTransaction.builder()
                    .company(company)
                    .transactionDate(parseDate(fields[0]))
                    .amount(new BigDecimal(fields[1].trim()))
                    .description(fields.length > 2 ? fields[2].trim() : "")
                    .bankReference(fields.length > 3 ? fields[3].trim() : null)
                    .isReconciled(false)
                    .importedAt(LocalDate.now())
                    .importSource("CSV")
                    .build();
                
                // Vérifier les doublons
                if (transaction.getBankReference() != null 
                    && transactionRepository.existsByCompanyAndBankReference(company, transaction.getBankReference())) {
                    log.warn("Transaction en doublon ignorée: {}", transaction.getBankReference());
                    continue;
                }
                
                transactions.add(transactionRepository.save(transaction));
            }
            
            log.info("Import réussi: {} transactions importées pour l'entreprise {}", 
                     transactions.size(), companyId);
            
        } catch (Exception e) {
            log.error("Erreur lors de l'import des transactions", e);
            throw new RuntimeException("Échec de l'import: " + e.getMessage());
        }
        
        return transactions;
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
    
    private LocalDate parseDate(String dateStr) {
        try {
            // Essayer plusieurs formats de date courants
            DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy")
            };
            
            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDate.parse(dateStr.trim(), formatter);
                } catch (Exception ignored) {
                }
            }
            
            throw new IllegalArgumentException("Format de date non reconnu: " + dateStr);
            
        } catch (Exception e) {
            log.error("Erreur lors du parsing de la date: {}", dateStr);
            throw e;
        }
    }
}