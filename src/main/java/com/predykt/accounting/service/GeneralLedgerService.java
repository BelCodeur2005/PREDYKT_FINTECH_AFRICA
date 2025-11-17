package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.ChartOfAccounts;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.GeneralLedger;
import com.predykt.accounting.dto.request.JournalEntryLineRequest;
import com.predykt.accounting.dto.request.JournalEntryRequest;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.exception.UnbalancedEntryException;
import com.predykt.accounting.repository.ChartOfAccountsRepository;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.repository.GeneralLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeneralLedgerService {
    
    private final GeneralLedgerRepository glRepository;
    private final CompanyRepository companyRepository;
    private final ChartOfAccountsRepository chartRepository;
    
    /**
     * Enregistrer une écriture comptable (respecte la partie double)
     */
    @Transactional
    public List<GeneralLedger> recordJournalEntry(Long companyId, JournalEntryRequest request) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));
        
        // VALIDATION 1 : Équilibre débit/crédit
        BigDecimal totalDebit = request.getLines().stream()
            .map(JournalEntryLineRequest::getDebitAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCredit = request.getLines().stream()
            .map(JournalEntryLineRequest::getCreditAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new UnbalancedEntryException(
                String.format("Écriture déséquilibrée : Débit=%s, Crédit=%s", totalDebit, totalCredit)
            );
        }
        
        log.debug("Enregistrement écriture {} : Débit={}, Crédit={}", 
                  request.getReference(), totalDebit, totalCredit);
        
        // VALIDATION 2 : Vérifier que les comptes existent
        List<GeneralLedger> entries = new ArrayList<>();
        
        for (JournalEntryLineRequest line : request.getLines()) {
            ChartOfAccounts account = chartRepository
                .findByCompanyAndAccountNumber(company, line.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Compte " + line.getAccountNumber() + " non trouvé"
                ));
            
            // Créer l'écriture
            GeneralLedger entry = GeneralLedger.builder()
                .company(company)
                .entryDate(request.getEntryDate())
                .account(account)
                .debitAmount(line.getDebitAmount())
                .creditAmount(line.getCreditAmount())
                .description(line.getDescription())
                .reference(request.getReference())
                .journalCode(request.getJournalCode())
                .isLocked(false)
                .build();
            
            entries.add(glRepository.save(entry));
        }
        
        log.info("Écriture {} enregistrée avec succès : {} lignes", 
                 request.getReference(), entries.size());
        
        return entries;
    }
    
    /**
     * Calculer le solde d'un compte à une date donnée
     */
    public BigDecimal getAccountBalance(Long companyId, String accountNumber, LocalDate asOfDate) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));
        
        ChartOfAccounts account = chartRepository
            .findByCompanyAndAccountNumber(company, accountNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Compte non trouvé"));
        
        BigDecimal balance = glRepository.calculateAccountBalance(account, asOfDate);
        
        // Ajuster le signe selon la nature du compte
        if (!account.getAccountType().isDebitNature()) {
            balance = balance.negate();
        }
        
        return balance;
    }
    
    /**
     * Récupérer les écritures d'un compte sur une période
     */
    public List<GeneralLedger> getAccountLedger(Long companyId, String accountNumber, 
                                                 LocalDate startDate, LocalDate endDate) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));
        
        ChartOfAccounts account = chartRepository
            .findByCompanyAndAccountNumber(company, accountNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Compte non trouvé"));
        
        return glRepository.findByAccountAndEntryDateBetween(account, startDate, endDate);
    }
    
    /**
     * Verrouiller les écritures d'une période (clôture)
     */
    @Transactional
    public void lockPeriod(Long companyId, LocalDate startDate, LocalDate endDate) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));
        
        List<GeneralLedger> entries = glRepository.findByCompanyAndEntryDateBetween(
            company, startDate, endDate
        );
        
        entries.forEach(entry -> entry.setIsLocked(true));
        glRepository.saveAll(entries);
        
        log.info("Période {}-{} verrouillée pour l'entreprise {} : {} écritures",
                 startDate, endDate, companyId, entries.size());
    }
    
    /**
     * Obtenir la balance de vérification
     */
    public List<TrialBalanceEntry> getTrialBalance(Long companyId, LocalDate startDate, LocalDate endDate) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));
        
        List<Object[]> rawData = glRepository.getTrialBalance(company, startDate, endDate);
        
        return rawData.stream()
            .map(row -> new TrialBalanceEntry(
                (String) row[0],  // accountNumber
                (String) row[1],  // accountName
                (BigDecimal) row[2],  // totalDebit
                (BigDecimal) row[3]   // totalCredit
            ))
            .toList();
    }
    
    public record TrialBalanceEntry(
        String accountNumber,
        String accountName,
        BigDecimal totalDebit,
        BigDecimal totalCredit
    ) {}
}