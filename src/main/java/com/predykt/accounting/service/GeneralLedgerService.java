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
import java.math.RoundingMode;
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
    private final VATRecoverabilityService vatRecoverabilityService;
    
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
            
            GeneralLedger savedEntry = glRepository.save(entry);
            entries.add(savedEntry);

            // ======== DÉTECTION AUTOMATIQUE DE LA TVA ========
            // Si le compte est un compte de TVA déductible (445x), calculer automatiquement
            // la TVA récupérable en appliquant les règles de récupération + le prorata
            if (isVATDeductibleAccount(line.getAccountNumber())) {
                processVATEntry(company, savedEntry, request.getEntryDate());
            }
        }

        log.info("Écriture {} enregistrée avec succès : {} lignes",
                 request.getReference(), entries.size());

        return entries;
    }

    /**
     * Vérifie si un compte est un compte de TVA déductible
     * Comptes OHADA: 4451 - TVA récupérable
     */
    private boolean isVATDeductibleAccount(String accountNumber) {
        // Comptes de TVA déductible commencent par 4451
        // 4451 - TVA récupérable sur achats
        // 44511 - TVA sur immobilisations
        // 44512 - TVA sur marchandises
        // 44513 - TVA sur services
        return accountNumber != null && accountNumber.startsWith("4451");
    }

    /**
     * Traite automatiquement une écriture de TVA déductible
     *
     * Calcule la TVA récupérable en 2 étapes:
     * 1. Application des règles de récupération par nature (VU, VP, VER, etc.)
     * 2. Application du prorata de TVA (si activités mixtes)
     */
    private void processVATEntry(Company company, GeneralLedger vatEntry, LocalDate entryDate) {
        try {
            // Le montant au débit du compte 4451 représente la TVA déductible
            BigDecimal vatAmount = vatEntry.getDebitAmount();

            if (vatAmount == null || vatAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("⏩ Montant TVA nul ou crédit, pas de calcul de récupération");
                return;
            }

            // Récupérer l'année fiscale depuis la date d'écriture
            Integer fiscalYear = entryDate.getYear();

            // Rechercher l'écriture d'achat correspondante (pour avoir le HT et le taux de TVA)
            // Pour simplifier, on estime: HT = TVA / 0.1925 (taux 19.25%)
            BigDecimal estimatedVatRate = new BigDecimal("0.1925");
            BigDecimal htAmount = vatAmount.divide(estimatedVatRate, 2, RoundingMode.HALF_UP);

            // Récupérer le numéro de compte et la description de la charge associée
            String accountNumber = vatEntry.getAccount().getAccountNumber();
            String description = vatEntry.getDescription();

            log.debug("🔍 Détection automatique TVA: Compte {} - Montant {} FCFA - Description: {}",
                    accountNumber, vatAmount, description);

            // Calculer la TVA récupérable avec le moteur de règles + prorata
            VATRecoverabilityService.VATRecoveryResult result =
                vatRecoverabilityService.calculateRecoverableVATWithProrata(
                    company.getId(),
                    accountNumber,
                    description,
                    htAmount,
                    vatAmount,
                    estimatedVatRate,
                    fiscalYear,
                    vatEntry.getId()
                );

            // Logger le résultat
            if (result.getHasProrataImpact()) {
                log.info("✅ TVA détectée et calculée: {} FCFA → {} FCFA récupérable (après prorata {}%) - Catégorie: {}",
                        result.getTotalVAT(),
                        result.getRecoverableVAT(),
                        result.getProrataRate() != null ?
                            result.getProrataRate().multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) :
                            new BigDecimal("100.00"),
                        result.getRecoveryCategory().getDisplayName());
            } else {
                log.info("✅ TVA détectée et calculée: {} FCFA → {} FCFA récupérable - Catégorie: {}",
                        result.getTotalVAT(),
                        result.getRecoverableVAT(),
                        result.getRecoveryCategory().getDisplayName());
            }

        } catch (Exception e) {
            // Ne pas bloquer l'enregistrement de l'écriture si le calcul TVA échoue
            log.error("❌ Erreur lors du calcul automatique de TVA pour l'écriture {}: {}",
                    vatEntry.getId(), e.getMessage());
        }
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