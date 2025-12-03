package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.dto.request.CompanyCreateRequest;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.mapper.CompanyMapper;
import com.predykt.accounting.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final TaxService taxService;
    private final ChartOfAccountsService chartOfAccountsService;
    
    @Transactional
    public Company createCompany(CompanyCreateRequest request) {
        // Vérifier unicité email et taxId
        if (companyRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Un compte avec cet email existe déjà");
        }
        
        if (request.getTaxId() != null && companyRepository.existsByTaxId(request.getTaxId())) {
            throw new IllegalArgumentException("Un compte avec ce numéro fiscal existe déjà");
        }
        
        Company company = companyMapper.toEntity(request);
        company.setIsActive(true);
        
        Company savedCompany = companyRepository.save(company);
        log.info("Entreprise créée avec succès: ID={}, Nom={}", savedCompany.getId(), savedCompany.getName());

        // Initialiser le plan comptable OHADA
        try {
            chartOfAccountsService.initializeDefaultChartOfAccounts(savedCompany.getId());
            log.info("✅ Plan comptable OHADA initialisé pour {}", savedCompany.getName());
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'initialisation du plan comptable pour {}", savedCompany.getName(), e);
        }

        // Initialiser les configurations fiscales par défaut
        try {
            taxService.initializeDefaultTaxConfigurations(savedCompany);
            log.info("✅ Configurations fiscales initialisées pour {}", savedCompany.getName());
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'initialisation des taxes pour {}", savedCompany.getName(), e);
        }

        return savedCompany;
    }
    
    public Company getCompanyById(Long id) {
        return companyRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée: " + id));
    }
    
    public List<Company> getAllActiveCompanies() {
        return companyRepository.findByIsActiveTrue();
    }
    
    @Transactional
    public Company updateCompany(Long id, CompanyCreateRequest request) {
        Company company = getCompanyById(id);
        
        // Vérifier que l'email n'est pas déjà utilisé par une autre entreprise
        if (!company.getEmail().equals(request.getEmail()) 
            && companyRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }
        
        companyMapper.updateEntityFromRequest(request, company);
        
        return companyRepository.save(company);
    }
    
    @Transactional
    public void deactivateCompany(Long id) {
        Company company = getCompanyById(id);
        company.setIsActive(false);
        companyRepository.save(company);
        
        log.info("Entreprise désactivée: ID={}", id);
    }
}