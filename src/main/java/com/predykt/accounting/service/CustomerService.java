package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.Customer;
import com.predykt.accounting.dto.request.CustomerCreateRequest;
import com.predykt.accounting.dto.request.CustomerUpdateRequest;
import com.predykt.accounting.dto.response.CustomerResponse;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.exception.ValidationException;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des clients (Plan Tiers)
 * IMPORTANT: L'utilisation des clients est OPTIONNELLE
 * Le système fonctionne sans cette fonctionnalité (utilise les descriptions dans GeneralLedger)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;
    private final TiersAccountService tiersAccountService;
    private final com.predykt.accounting.mapper.CustomerMapper customerMapper;

    /**
     * Créer un nouveau client avec génération automatique du sous-compte auxiliaire OHADA
     */
    @Transactional
    public CustomerResponse createCustomer(Long companyId, CustomerCreateRequest request) {
        log.info("Création d'un nouveau client '{}' pour l'entreprise {}", request.getName(), companyId);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        // Vérifier si le client existe déjà
        if (customerRepository.existsByCompanyAndNameIgnoreCase(company, request.getName())) {
            throw new ValidationException("Un client avec ce nom existe déjà pour cette entreprise");
        }

        // Mapper la requête vers l'entité
        Customer customer = customerMapper.toEntity(request);
        customer.setCompany(company);
        customer.setIsActive(true);

        // Générer automatiquement le sous-compte auxiliaire OHADA (4111001, 4111002...)
        var auxiliaryAccount = tiersAccountService.createCustomerAuxiliaryAccount(company, customer);
        customer.setAuxiliaryAccount(auxiliaryAccount);

        // Sauvegarder le client
        customer = customerRepository.save(customer);

        log.info("✅ Client créé avec succès - ID: {}, Compte: {}",
            customer.getId(), customer.getAuxiliaryAccountNumber());

        return customerMapper.toResponse(customer);
    }

    /**
     * Obtenir un client par ID
     */
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long companyId, Long customerId) {
        Customer customer = findCustomerByIdAndCompany(companyId, customerId);
        return toResponse(customer);
    }

    /**
     * Lister tous les clients d'une entreprise
     */
    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers(Long companyId, Boolean activeOnly) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        List<Customer> customers;
        if (activeOnly != null && activeOnly) {
            customers = customerRepository.findByCompanyAndIsActiveTrueOrderByNameAsc(company);
        } else {
            customers = customerRepository.findByCompanyOrderByNameAsc(company);
        }

        return customers.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Rechercher des clients par nom
     */
    @Transactional(readOnly = true)
    public List<CustomerResponse> searchCustomers(Long companyId, String searchTerm) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        List<Customer> customers = customerRepository.searchByName(company, searchTerm);

        return customers.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Mettre à jour un client
     */
    @Transactional
    public CustomerResponse updateCustomer(Long companyId, Long customerId, CustomerUpdateRequest request) {
        log.info("Mise à jour du client {} pour l'entreprise {}", customerId, companyId);

        Customer customer = findCustomerByIdAndCompany(companyId, customerId);

        // Mettre à jour uniquement les champs non-null
        if (request.getName() != null) {
            // Vérifier si le nouveau nom n'existe pas déjà (sauf pour le client actuel)
            customerRepository.findByCompanyAndNameIgnoreCase(customer.getCompany(), request.getName())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(customerId)) {
                        throw new ValidationException("Un autre client avec ce nom existe déjà");
                    }
                });
            customer.setName(request.getName());
        }
        if (request.getTaxId() != null) customer.setTaxId(request.getTaxId());
        if (request.getNiuNumber() != null) customer.setNiuNumber(request.getNiuNumber());
        if (request.getEmail() != null) customer.setEmail(request.getEmail());
        if (request.getPhone() != null) customer.setPhone(request.getPhone());
        if (request.getAddress() != null) customer.setAddress(request.getAddress());
        if (request.getCity() != null) customer.setCity(request.getCity());
        if (request.getCountry() != null) customer.setCountry(request.getCountry());
        if (request.getCustomerType() != null) customer.setCustomerType(request.getCustomerType());
        if (request.getPaymentTerms() != null) customer.setPaymentTerms(request.getPaymentTerms());
        if (request.getCreditLimit() != null) customer.setCreditLimit(request.getCreditLimit());
        if (request.getIsActive() != null) customer.setIsActive(request.getIsActive());

        customer = customerRepository.save(customer);
        log.info("Client {} mis à jour avec succès", customerId);

        return toResponse(customer);
    }

    /**
     * Désactiver un client (soft delete)
     */
    @Transactional
    public void deactivateCustomer(Long companyId, Long customerId) {
        log.info("Désactivation du client {} pour l'entreprise {}", customerId, companyId);

        Customer customer = findCustomerByIdAndCompany(companyId, customerId);
        customer.setIsActive(false);
        customerRepository.save(customer);

        log.info("Client {} désactivé avec succès", customerId);
    }

    /**
     * Réactiver un client
     */
    @Transactional
    public void reactivateCustomer(Long companyId, Long customerId) {
        log.info("Réactivation du client {} pour l'entreprise {}", customerId, companyId);

        Customer customer = findCustomerByIdAndCompany(companyId, customerId);
        customer.setIsActive(true);
        customerRepository.save(customer);

        log.info("Client {} réactivé avec succès", customerId);
    }

    /**
     * Supprimer définitivement un client (hard delete)
     * ATTENTION: À utiliser avec précaution si des écritures sont liées
     */
    @Transactional
    public void deleteCustomer(Long companyId, Long customerId) {
        log.warn("Suppression DÉFINITIVE du client {} pour l'entreprise {}", customerId, companyId);

        Customer customer = findCustomerByIdAndCompany(companyId, customerId);
        customerRepository.delete(customer);

        log.info("Client {} supprimé définitivement", customerId);
    }

    /**
     * Obtenir les statistiques des clients
     */
    @Transactional(readOnly = true)
    public CustomerStatistics getCustomerStatistics(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        long totalCustomers = customerRepository.countByCompanyAndIsActiveTrue(company);
        long customersWithNiu = customerRepository.findByCompanyAndHasNiuTrueOrderByNameAsc(company).size();

        List<Customer> exportCustomers = customerRepository.findByCompanyAndCustomerTypeOrderByNameAsc(company, "EXPORT");
        List<Customer> wholesaleCustomers = customerRepository.findByCompanyAndCustomerTypeOrderByNameAsc(company, "WHOLESALE");
        List<Customer> retailCustomers = customerRepository.findByCompanyAndCustomerTypeOrderByNameAsc(company, "RETAIL");

        return CustomerStatistics.builder()
            .totalCustomers(totalCustomers)
            .customersWithNiu(customersWithNiu)
            .exportCustomersCount(exportCustomers.size())
            .wholesaleCustomersCount(wholesaleCustomers.size())
            .retailCustomersCount(retailCustomers.size())
            .build();
    }

    // ========== Méthodes privées ==========

    private Customer findCustomerByIdAndCompany(Long companyId, Long customerId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID: " + customerId));

        if (!customer.getCompany().getId().equals(companyId)) {
            throw new ValidationException("Ce client n'appartient pas à cette entreprise");
        }

        return customer;
    }

    private CustomerResponse toResponse(Customer customer) {
        return customerMapper.toResponse(customer);
    }

    // ========== Classe interne pour les statistiques ==========

    @lombok.Data
    @lombok.Builder
    public static class CustomerStatistics {
        private long totalCustomers;
        private long customersWithNiu;
        private long exportCustomersCount;
        private long wholesaleCustomersCount;
        private long retailCustomersCount;
    }
}
