package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.Supplier;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.exception.ValidationException;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service de gestion des fournisseurs
 * Gère le NIU (Numéro d'Identifiant Unique) pour le calcul de l'AIR
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final CompanyRepository companyRepository;

    /**
     * Crée un nouveau fournisseur
     */
    @Transactional
    public Supplier createSupplier(Long companyId, Supplier supplier) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        // Vérifier que le nom n'existe pas déjà
        if (supplierRepository.existsByCompanyAndName(company, supplier.getName())) {
            throw new ValidationException("Un fournisseur avec ce nom existe déjà");
        }

        supplier.setCompany(company);

        // Log si NIU manquant
        if (!supplier.hasValidNiu()) {
            log.warn("⚠️ Fournisseur créé SANS NIU: {} - AIR sera à 5,5% (pénalité)", supplier.getName());
        } else {
            log.info("✅ Fournisseur créé avec NIU: {} - AIR sera à 2,2%", supplier.getName());
        }

        return supplierRepository.save(supplier);
    }

    /**
     * Met à jour un fournisseur
     */
    @Transactional
    public Supplier updateSupplier(Long supplierId, Supplier updatedSupplier) {
        Supplier existingSupplier = supplierRepository.findById(supplierId)
            .orElseThrow(() -> new ResourceNotFoundException("Fournisseur non trouvé"));

        boolean niuChanged = false;
        String oldNiu = existingSupplier.getNiuNumber();
        String newNiu = updatedSupplier.getNiuNumber();

        // Détecter changement de NIU
        if ((oldNiu == null && newNiu != null) || (oldNiu != null && !oldNiu.equals(newNiu))) {
            niuChanged = true;
        }

        // Mise à jour des champs
        existingSupplier.setName(updatedSupplier.getName());
        existingSupplier.setTaxId(updatedSupplier.getTaxId());
        existingSupplier.setNiuNumber(updatedSupplier.getNiuNumber());
        existingSupplier.setEmail(updatedSupplier.getEmail());
        existingSupplier.setPhone(updatedSupplier.getPhone());
        existingSupplier.setAddress(updatedSupplier.getAddress());
        existingSupplier.setCity(updatedSupplier.getCity());
        existingSupplier.setCountry(updatedSupplier.getCountry());
        existingSupplier.setSupplierType(updatedSupplier.getSupplierType());
        existingSupplier.setPaymentTerms(updatedSupplier.getPaymentTerms());
        existingSupplier.setIsActive(updatedSupplier.getIsActive());

        Supplier saved = supplierRepository.save(existingSupplier);

        if (niuChanged) {
            if (saved.hasValidNiu()) {
                log.info("✅ NIU ajouté pour {}: {} - AIR passera de 5,5% à 2,2%",
                    saved.getName(), saved.getNiuNumber());
            } else {
                log.warn("⚠️ NIU supprimé pour {} - AIR passera de 2,2% à 5,5% (pénalité)",
                    saved.getName());
            }
        }

        return saved;
    }

    /**
     * Récupère tous les fournisseurs d'une entreprise
     */
    @Transactional(readOnly = true)
    public List<Supplier> getAllSuppliers(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        return supplierRepository.findByCompany(company);
    }

    /**
     * Récupère les fournisseurs actifs
     */
    @Transactional(readOnly = true)
    public List<Supplier> getActiveSuppliers(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        return supplierRepository.findByCompanyAndIsActiveTrue(company);
    }

    /**
     * Récupère un fournisseur par ID
     */
    @Transactional(readOnly = true)
    public Supplier getSupplierById(Long supplierId) {
        return supplierRepository.findById(supplierId)
            .orElseThrow(() -> new ResourceNotFoundException("Fournisseur non trouvé"));
    }

    /**
     * Recherche un fournisseur par nom
     */
    @Transactional(readOnly = true)
    public Supplier findByName(Long companyId, String name) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        return supplierRepository.findByCompanyAndName(company, name)
            .orElse(null);
    }

    /**
     * Récupère les fournisseurs SANS NIU (pour alertes)
     */
    @Transactional(readOnly = true)
    public List<Supplier> getSuppliersWithoutNiu(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        List<Supplier> suppliers = supplierRepository.findSuppliersWithoutNiu(company);

        log.warn("⚠️ {} fournisseurs sans NIU trouvés (pénalité AIR 5,5%)", suppliers.size());

        return suppliers;
    }

    /**
     * Récupère les fournisseurs de type "RENT" (loueurs)
     */
    @Transactional(readOnly = true)
    public List<Supplier> getRentSuppliers(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        return supplierRepository.findRentSuppliers(company);
    }

    /**
     * Supprime un fournisseur (désactivation logique)
     */
    @Transactional
    public void deleteSupplier(Long supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
            .orElseThrow(() -> new ResourceNotFoundException("Fournisseur non trouvé"));

        supplier.setIsActive(false);
        supplierRepository.save(supplier);

        log.info("🗑️ Fournisseur désactivé: {}", supplier.getName());
    }

    /**
     * Réactive un fournisseur
     */
    @Transactional
    public void reactivateSupplier(Long supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
            .orElseThrow(() -> new ResourceNotFoundException("Fournisseur non trouvé"));

        supplier.setIsActive(true);
        supplierRepository.save(supplier);

        log.info("✅ Fournisseur réactivé: {}", supplier.getName());
    }

    /**
     * Ajoute ou met à jour le NIU d'un fournisseur
     */
    @Transactional
    public Supplier updateNiu(Long supplierId, String niuNumber) {
        Supplier supplier = supplierRepository.findById(supplierId)
            .orElseThrow(() -> new ResourceNotFoundException("Fournisseur non trouvé"));

        boolean hadNiu = supplier.hasValidNiu();
        supplier.setNiuNumber(niuNumber);

        Supplier saved = supplierRepository.save(supplier);

        if (!hadNiu && saved.hasValidNiu()) {
            log.info("✅ NIU ajouté pour {}: {} - AIR passera de 5,5% à 2,2% (économie de 3,3%)",
                saved.getName(), saved.getNiuNumber());
        } else if (hadNiu && !saved.hasValidNiu()) {
            log.warn("⚠️ NIU supprimé pour {} - AIR passera de 2,2% à 5,5% (surcoût de 3,3%)",
                saved.getName());
        }

        return saved;
    }

    /**
     * Compte les fournisseurs sans NIU
     */
    @Transactional(readOnly = true)
    public Long countSuppliersWithoutNiu(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        return supplierRepository.countSuppliersWithoutNiu(company);
    }

    /**
     * Crée ou récupère un fournisseur par nom (pour imports CSV)
     */
    @Transactional
    public Supplier getOrCreateSupplier(Long companyId, String supplierName, String supplierType) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        // Chercher un fournisseur existant
        return supplierRepository.findByCompanyAndName(company, supplierName)
            .orElseGet(() -> {
                // Créer un nouveau fournisseur
                Supplier newSupplier = Supplier.builder()
                    .company(company)
                    .name(supplierName)
                    .supplierType(supplierType)
                    .hasNiu(false)
                    .isActive(true)
                    .build();

                Supplier saved = supplierRepository.save(newSupplier);

                log.info("📝 Nouveau fournisseur créé automatiquement: {} (type: {}) - ⚠️ SANS NIU",
                    supplierName, supplierType);

                return saved;
            });
    }
}
