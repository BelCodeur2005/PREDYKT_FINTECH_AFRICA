package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.Cabinet;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.User;
import com.predykt.accounting.repository.CabinetRepository;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service pour la gestion des cabinets comptables (MODE CABINET)
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CabinetService {

    private final CabinetRepository cabinetRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    /**
     * Crée un nouveau cabinet
     */
    public Cabinet createCabinet(Cabinet cabinet) {
        log.info("Création d'un nouveau cabinet: {}", cabinet.getName());

        if (cabinetRepository.existsByName(cabinet.getName())) {
            throw new IllegalArgumentException("Un cabinet avec ce nom existe déjà");
        }

        if (cabinet.getCode() != null && cabinetRepository.existsByCode(cabinet.getCode())) {
            throw new IllegalArgumentException("Un cabinet avec ce code existe déjà");
        }

        return cabinetRepository.save(cabinet);
    }

    /**
     * Met à jour un cabinet
     */
    public Cabinet updateCabinet(Long cabinetId, Cabinet updatedCabinet) {
        log.info("Mise à jour du cabinet ID: {}", cabinetId);

        Cabinet cabinet = getCabinetById(cabinetId);

        // Vérifier l'unicité du nom si changé
        if (!cabinet.getName().equals(updatedCabinet.getName())) {
            if (cabinetRepository.existsByName(updatedCabinet.getName())) {
                throw new IllegalArgumentException("Un cabinet avec ce nom existe déjà");
            }
        }

        // Vérifier l'unicité du code si changé
        if (updatedCabinet.getCode() != null &&
            !updatedCabinet.getCode().equals(cabinet.getCode())) {
            if (cabinetRepository.existsByCode(updatedCabinet.getCode())) {
                throw new IllegalArgumentException("Un cabinet avec ce code existe déjà");
            }
        }

        cabinet.setName(updatedCabinet.getName());
        cabinet.setCode(updatedCabinet.getCode());
        cabinet.setAddress(updatedCabinet.getAddress());
        cabinet.setPhone(updatedCabinet.getPhone());
        cabinet.setEmail(updatedCabinet.getEmail());
        cabinet.setMaxCompanies(updatedCabinet.getMaxCompanies());
        cabinet.setMaxUsers(updatedCabinet.getMaxUsers());
        cabinet.setPlan(updatedCabinet.getPlan());

        return cabinetRepository.save(cabinet);
    }

    /**
     * Récupère un cabinet par ID
     */
    @Transactional(readOnly = true)
    public Cabinet getCabinetById(Long cabinetId) {
        return cabinetRepository.findById(cabinetId)
            .orElseThrow(() -> new EntityNotFoundException("Cabinet non trouvé avec l'ID: " + cabinetId));
    }

    /**
     * Récupère un cabinet par nom
     */
    @Transactional(readOnly = true)
    public Cabinet getCabinetByName(String name) {
        return cabinetRepository.findByName(name)
            .orElseThrow(() -> new EntityNotFoundException("Cabinet non trouvé avec le nom: " + name));
    }

    /**
     * Récupère un cabinet par code
     */
    @Transactional(readOnly = true)
    public Cabinet getCabinetByCode(String code) {
        return cabinetRepository.findByCode(code)
            .orElseThrow(() -> new EntityNotFoundException("Cabinet non trouvé avec le code: " + code));
    }

    /**
     * Récupère tous les cabinets
     */
    @Transactional(readOnly = true)
    public List<Cabinet> getAllCabinets() {
        return cabinetRepository.findAll();
    }

    /**
     * Ajoute une entreprise à un cabinet
     */
    public Company addCompanyToCabinet(Long cabinetId, Long companyId) {
        log.info("Ajout de l'entreprise {} au cabinet {}", companyId, cabinetId);

        Cabinet cabinet = getCabinetById(cabinetId);
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        // Vérifier la limite d'entreprises
        if (cabinet.hasReachedCompanyLimit()) {
            throw new IllegalStateException(
                String.format("Le cabinet a atteint sa limite de %d entreprises", cabinet.getMaxCompanies())
            );
        }

        company.setCabinet(cabinet);
        return companyRepository.save(company);
    }

    /**
     * Retire une entreprise d'un cabinet
     */
    public void removeCompanyFromCabinet(Long cabinetId, Long companyId) {
        log.info("Retrait de l'entreprise {} du cabinet {}", companyId, cabinetId);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        if (company.getCabinet() == null || !company.getCabinet().getId().equals(cabinetId)) {
            throw new IllegalArgumentException("Cette entreprise n'appartient pas à ce cabinet");
        }

        company.setCabinet(null);
        companyRepository.save(company);
    }

    /**
     * Récupère toutes les entreprises d'un cabinet
     */
    @Transactional(readOnly = true)
    public List<Company> getCompaniesByCabinet(Long cabinetId) {
        Cabinet cabinet = getCabinetById(cabinetId);
        return cabinet.getCompanies().stream().toList();
    }

    /**
     * Ajoute un utilisateur à un cabinet
     */
    public User addUserToCabinet(Long cabinetId, Long userId) {
        log.info("Ajout de l'utilisateur {} au cabinet {}", userId, cabinetId);

        Cabinet cabinet = getCabinetById(cabinetId);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé avec l'ID: " + userId));

        // Vérifier la limite d'utilisateurs
        if (cabinet.hasReachedUserLimit()) {
            throw new IllegalStateException(
                String.format("Le cabinet a atteint sa limite de %d utilisateurs", cabinet.getMaxUsers())
            );
        }

        user.setCabinet(cabinet);
        return userRepository.save(user);
    }

    /**
     * Récupère tous les utilisateurs d'un cabinet
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByCabinet(Long cabinetId) {
        Cabinet cabinet = getCabinetById(cabinetId);
        return cabinet.getUsers().stream().toList();
    }

    /**
     * Met à jour le plan d'un cabinet
     */
    public Cabinet updateCabinetPlan(Long cabinetId, String plan, Integer maxCompanies, Integer maxUsers) {
        log.info("Mise à jour du plan du cabinet {} vers {}", cabinetId, plan);

        Cabinet cabinet = getCabinetById(cabinetId);

        cabinet.setPlan(plan);
        if (maxCompanies != null) {
            cabinet.setMaxCompanies(maxCompanies);
        }
        if (maxUsers != null) {
            cabinet.setMaxUsers(maxUsers);
        }

        return cabinetRepository.save(cabinet);
    }

    /**
     * Supprime un cabinet
     */
    public void deleteCabinet(Long cabinetId) {
        log.info("Suppression du cabinet ID: {}", cabinetId);

        Cabinet cabinet = getCabinetById(cabinetId);

        // Vérifier qu'il n'y a plus d'entreprises
        if (!cabinet.getCompanies().isEmpty()) {
            throw new IllegalStateException("Impossible de supprimer un cabinet avec des entreprises actives");
        }

        // Vérifier qu'il n'y a plus d'utilisateurs
        if (!cabinet.getUsers().isEmpty()) {
            throw new IllegalStateException("Impossible de supprimer un cabinet avec des utilisateurs actifs");
        }

        cabinetRepository.delete(cabinet);
    }

    /**
     * Vérifie si un cabinet peut ajouter une entreprise
     */
    @Transactional(readOnly = true)
    public boolean canAddCompany(Long cabinetId) {
        Cabinet cabinet = getCabinetById(cabinetId);
        return !cabinet.hasReachedCompanyLimit();
    }

    /**
     * Vérifie si un cabinet peut ajouter un utilisateur
     */
    @Transactional(readOnly = true)
    public boolean canAddUser(Long cabinetId) {
        Cabinet cabinet = getCabinetById(cabinetId);
        return !cabinet.hasReachedUserLimit();
    }

    /**
     * Compte le nombre d'entreprises d'un cabinet
     */
    @Transactional(readOnly = true)
    public Long countCompanies(Long cabinetId) {
        return cabinetRepository.countCompaniesByCabinetId(cabinetId);
    }

    /**
     * Compte le nombre d'utilisateurs d'un cabinet
     */
    @Transactional(readOnly = true)
    public Long countUsers(Long cabinetId) {
        return cabinetRepository.countUsersByCabinetId(cabinetId);
    }
}
