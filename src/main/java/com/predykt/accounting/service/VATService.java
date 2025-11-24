package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.ChartOfAccounts;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.GeneralLedger;
import com.predykt.accounting.dto.response.VATSummaryResponse;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.repository.ChartOfAccountsRepository;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.repository.GeneralLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service de gestion de la TVA (OHADA)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VATService {
    
    private final CompanyRepository companyRepository;
    private final ChartOfAccountsRepository chartRepository;
    private final GeneralLedgerRepository glRepository;
    
    // Comptes TVA OHADA
    private static final String COMPTE_TVA_COLLECTEE = "4431";  // TVA collectée (créditeur)
    private static final String COMPTE_TVA_DEDUCTIBLE = "4451"; // TVA déductible (débiteur)
    private static final String COMPTE_TVA_A_PAYER = "4441";    // TVA à payer
    
    // Taux de TVA Cameroun (modifiable selon pays)
    private static final BigDecimal TAUX_TVA_NORMAL = new BigDecimal("19.25");  // 19.25%
    private static final BigDecimal TAUX_TVA_REDUIT = new BigDecimal("0.00");   // 0% (exonéré)
    
    /**
     * Calcule le résumé TVA pour une période
     */
    @Transactional(readOnly = true)
    public VATSummaryResponse calculateVATSummary(Long companyId, LocalDate startDate, LocalDate endDate) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));
        
        log.info("📊 Calcul TVA pour {} du {} au {}", company.getName(), startDate, endDate);
        
        // Récupérer les comptes TVA
        ChartOfAccounts compteTvaCollectee = getOrCreateAccount(company, COMPTE_TVA_COLLECTEE);
        ChartOfAccounts compteTvaDeductible = getOrCreateAccount(company, COMPTE_TVA_DEDUCTIBLE);
        
        // Calculer TVA collectée (crédit)
        BigDecimal tvaCollectee = calculateVATCollected(company, startDate, endDate);
        
        // Calculer TVA déductible (débit)
        BigDecimal tvaDeductible = calculateVATDeductible(company, startDate, endDate);
        
        // Calculer TVA à payer (ou crédit de TVA)
        BigDecimal tvaAPayer = tvaCollectee.subtract(tvaDeductible);
        
        String status = tvaAPayer.compareTo(BigDecimal.ZERO) >= 0 ? "A_PAYER" : "CREDIT";
        
        log.info("✅ TVA calculée: Collectée={}, Déductible={}, À payer={}", 
                 tvaCollectee, tvaDeductible, tvaAPayer);
        
        return VATSummaryResponse.builder()
            .startDate(startDate)
            .endDate(endDate)
            .vatCollected(tvaCollectee)
            .vatDeductible(tvaDeductible)
            .vatToPay(tvaAPayer.abs())
            .status(status)
            .build();
    }
    
    /**
     * Calcule la TVA collectée (sur les ventes)
     */
    private BigDecimal calculateVATCollected(Company company, LocalDate startDate, LocalDate endDate) {
        ChartOfAccounts compte = chartRepository
            .findByCompanyAndAccountNumber(company, COMPTE_TVA_COLLECTEE)
            .orElse(null);
        
        if (compte == null) {
            log.warn("⚠️ Compte TVA collectée ({}) non trouvé", COMPTE_TVA_COLLECTEE);
            return BigDecimal.ZERO;
        }
        
        // Récupérer toutes les écritures du compte TVA collectée
        List<GeneralLedger> entries = glRepository.findByAccountAndEntryDateBetween(
            compte, startDate, endDate
        );
        
        // Sommer les montants au crédit (TVA collectée)
        return entries.stream()
            .map(GeneralLedger::getCreditAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calcule la TVA déductible (sur les achats)
     */
    private BigDecimal calculateVATDeductible(Company company, LocalDate startDate, LocalDate endDate) {
        ChartOfAccounts compte = chartRepository
            .findByCompanyAndAccountNumber(company, COMPTE_TVA_DEDUCTIBLE)
            .orElse(null);
        
        if (compte == null) {
            log.warn("⚠️ Compte TVA déductible ({}) non trouvé", COMPTE_TVA_DEDUCTIBLE);
            return BigDecimal.ZERO;
        }
        
        // Récupérer toutes les écritures du compte TVA déductible
        List<GeneralLedger> entries = glRepository.findByAccountAndEntryDateBetween(
            compte, startDate, endDate
        );
        
        // Sommer les montants au débit (TVA déductible)
        return entries.stream()
            .map(GeneralLedger::getDebitAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calcule le montant HT à partir d'un montant TTC
     */
    public BigDecimal calculateAmountExcludingVAT(BigDecimal amountIncludingVAT, BigDecimal vatRate) {
        if (amountIncludingVAT == null || vatRate == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal divisor = BigDecimal.ONE.add(vatRate.divide(BigDecimal.valueOf(100)));
        return amountIncludingVAT.divide(divisor, 2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Calcule le montant de TVA à partir d'un montant HT
     */
    public BigDecimal calculateVATAmount(BigDecimal amountExcludingVAT, BigDecimal vatRate) {
        if (amountExcludingVAT == null || vatRate == null) {
            return BigDecimal.ZERO;
        }
        
        return amountExcludingVAT
            .multiply(vatRate)
            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Calcule le montant TTC à partir d'un montant HT
     */
    public BigDecimal calculateAmountIncludingVAT(BigDecimal amountExcludingVAT, BigDecimal vatRate) {
        if (amountExcludingVAT == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal vatAmount = calculateVATAmount(amountExcludingVAT, vatRate);
        return amountExcludingVAT.add(vatAmount);
    }
    
    /**
     * Vérifie si une entreprise est assujettie à la TVA
     */
    public boolean isVATRegistered(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));
        
        return company.getIsVatRegistered();
    }
    
    /**
     * Obtient le taux de TVA applicable selon le type de produit/service
     */
    public BigDecimal getApplicableVATRate(String productCategory) {
        // Logique à adapter selon les réglementations locales
        // Pour l'instant: taux normal par défaut
        
        // Produits exonérés (exemples)
        List<String> exemptCategories = List.of(
            "EDUCATION",
            "HEALTH",
            "AGRICULTURE",
            "EXPORT"
        );
        
        if (exemptCategories.contains(productCategory)) {
            return TAUX_TVA_REDUIT;
        }
        
        return TAUX_TVA_NORMAL;
    }
    
    /**
     * Génère le tableau de TVA détaillé par compte
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> generateDetailedVATReport(Long companyId, 
                                                             LocalDate startDate, 
                                                             LocalDate endDate) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));
        
        Map<String, BigDecimal> report = new HashMap<>();
        
        // TVA sur ventes (par compte de vente)
        List<String> salesAccounts = List.of("701", "702", "706", "707");
        
        for (String accountNumber : salesAccounts) {
            try {
                ChartOfAccounts account = chartRepository
                    .findByCompanyAndAccountNumber(company, accountNumber)
                    .orElse(null);
                
                if (account != null) {
                    BigDecimal salesAmount = glRepository.calculateAccountBalanceBetween(
                        account, startDate, endDate
                    );
                    
                    // Calculer TVA théorique
                    BigDecimal vatAmount = calculateVATAmount(salesAmount, TAUX_TVA_NORMAL);
                    
                    report.put("TVA_" + accountNumber + "_" + account.getAccountName(), vatAmount);
                }
            } catch (Exception e) {
                log.warn("Erreur calcul TVA pour compte {}", accountNumber, e);
            }
        }
        
        // TVA sur achats (par compte d'achat)
        List<String> purchaseAccounts = List.of("601", "602", "605");
        
        for (String accountNumber : purchaseAccounts) {
            try {
                ChartOfAccounts account = chartRepository
                    .findByCompanyAndAccountNumber(company, accountNumber)
                    .orElse(null);
                
                if (account != null) {
                    BigDecimal purchaseAmount = glRepository.calculateAccountBalanceBetween(
                        account, startDate, endDate
                    );
                    
                    BigDecimal vatAmount = calculateVATAmount(purchaseAmount, TAUX_TVA_NORMAL);
                    
                    report.put("TVA_DEDUCTIBLE_" + accountNumber + "_" + account.getAccountName(), vatAmount);
                }
            } catch (Exception e) {
                log.warn("Erreur calcul TVA déductible pour compte {}", accountNumber, e);
            }
        }
        
        return report;
    }
    
    /**
     * Récupère ou crée un compte s'il n'existe pas
     */
    private ChartOfAccounts getOrCreateAccount(Company company, String accountNumber) {
        return chartRepository
            .findByCompanyAndAccountNumber(company, accountNumber)
            .orElseGet(() -> {
                log.warn("⚠️ Compte {} non trouvé - Création automatique", accountNumber);
                // En production, éviter la création auto - lever une exception
                throw new ResourceNotFoundException("Compte TVA " + accountNumber + " non initialisé");
            });
    }
}