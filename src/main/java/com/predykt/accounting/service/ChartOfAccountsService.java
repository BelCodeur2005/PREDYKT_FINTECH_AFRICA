package com.predykt.accounting.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.predykt.accounting.domain.entity.ChartOfAccounts;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.enums.AccountType;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.repository.ChartOfAccountsRepository;
import com.predykt.accounting.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChartOfAccountsService {
    
    private final ChartOfAccountsRepository chartRepository;
    private final CompanyRepository companyRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Initialiser le plan comptable OHADA par défaut
     */
    @Transactional
    public void initializeDefaultChartOfAccounts(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));
        
        // Vérifier si le plan comptable existe déjà
        if (chartRepository.countByCompany(company) > 0) {
            log.warn("Plan comptable déjà initialisé pour l'entreprise {}", companyId);
            return;
        }
        
        try {
            // Charger le plan comptable OHADA depuis le fichier JSON
            ClassPathResource resource = new ClassPathResource("ohada/chart-of-accounts-ohada.json");
            List<Map<String, Object>> accounts = objectMapper.readValue(
                resource.getInputStream(),
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            log.info("Chargement de {} comptes OHADA pour l'entreprise {}", accounts.size(), companyId);
            
            // Créer les comptes en 2 passes (d'abord les parents, puis les enfants)
            createAccountsRecursively(company, accounts, null);
            
            log.info("Plan comptable OHADA initialisé avec succès pour l'entreprise {}", companyId);
            
        } catch (IOException e) {
            log.error("Erreur lors du chargement du plan comptable OHADA", e);
            throw new RuntimeException("Impossible d'initialiser le plan comptable", e);
        }
    }
    
    private void createAccountsRecursively(Company company, List<Map<String, Object>> accounts, String parentNumber) {
        accounts.stream()
            .filter(acc -> {
                String parent = (String) acc.get("parentNumber");
                return (parentNumber == null && parent == null) || 
                       (parentNumber != null && parentNumber.equals(parent));
            })
            .forEach(accountData -> {
                String accountNumber = (String) accountData.get("accountNumber");
                
                ChartOfAccounts account = ChartOfAccounts.builder()
                    .company(company)
                    .accountNumber(accountNumber)
                    .accountName((String) accountData.get("accountName"))
                    .accountType(AccountType.fromAccountNumber(accountNumber))
                    .isActive(true)
                    .description((String) accountData.get("description"))
                    .build();
                
                // Lier au parent si existe
                if (parentNumber != null) {
                    chartRepository.findByCompanyAndAccountNumber(company, parentNumber)
                        .ifPresent(account::setParentAccount);
                }
                
                chartRepository.save(account);
                
                // Créer les sous-comptes
                createAccountsRecursively(company, accounts, accountNumber);
            });
    }
    
    /**
     * Récupérer un compte par numéro
     */
    @Cacheable(value = "chartOfAccounts", key = "#companyId + '-' + #accountNumber")
    public ChartOfAccounts getAccountByNumber(Long companyId, String accountNumber) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));
        
        return chartRepository.findByCompanyAndAccountNumber(company, accountNumber)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Compte " + accountNumber + " non trouvé"
            ));
    }
    
    /**
     * Lister les comptes actifs d'une entreprise
     */
    @Cacheable(value = "chartOfAccountsList", key = "#companyId")
    public List<ChartOfAccounts> getActiveAccounts(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));
        
        return chartRepository.findByCompanyAndIsActiveTrue(company);
    }
    
    /**
     * Lister les comptes par type
     */
    public List<ChartOfAccounts> getAccountsByType(Long companyId, AccountType accountType) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));
        
        return chartRepository.findByCompanyAndAccountType(company, accountType);
    }
    
    /**
     * Créer un compte personnalisé
     */
    @Transactional
    public ChartOfAccounts createCustomAccount(Long companyId, ChartOfAccounts account) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));
        
        // Vérifier que le compte n'existe pas déjà
        if (chartRepository.existsByCompanyAndAccountNumber(company, account.getAccountNumber())) {
            throw new IllegalArgumentException("Le compte " + account.getAccountNumber() + " existe déjà");
        }
        
        account.setCompany(company);
        account.setAccountType(AccountType.fromAccountNumber(account.getAccountNumber()));
        
        return chartRepository.save(account);
    }

    public void activateAccount(Long companyId, Long accountId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'activateAccount'");
    }

    public void desactivateAccount(Long companyId, Long accountId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'desactivateAccount'");
    }

    public List<ChartOfAccounts> searchAccounts(Long companyId, String query) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'searchAccounts'");
    }
}