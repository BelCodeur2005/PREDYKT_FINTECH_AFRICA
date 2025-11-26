package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.User;
import com.predykt.accounting.domain.entity.UserCompanyAccess;
import com.predykt.accounting.domain.enums.AccessLevel;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.repository.UserCompanyAccessRepository;
import com.predykt.accounting.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Service pour la gestion des accès utilisateurs aux dossiers clients (MODE CABINET)
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserCompanyAccessService {

    private final UserCompanyAccessRepository accessRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    /**
     * Accorde l'accès à un dossier client pour un utilisateur
     */
    public UserCompanyAccess grantAccess(Long userId, Long companyId, AccessLevel accessLevel) {
        log.info("Octroi d'accès {} à l'utilisateur {} pour l'entreprise {}", accessLevel, userId, companyId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé avec l'ID: " + userId));

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvée avec l'ID: " + companyId));

        // Vérifier si l'accès existe déjà
        return accessRepository.findByUserIdAndCompanyId(userId, companyId)
            .map(existing -> {
                log.info("Mise à jour de l'accès existant de {} vers {}", existing.getAccessLevel(), accessLevel);
                existing.setAccessLevel(accessLevel);
                return accessRepository.save(existing);
            })
            .orElseGet(() -> {
                UserCompanyAccess newAccess = UserCompanyAccess.builder()
                    .user(user)
                    .company(company)
                    .accessLevel(accessLevel)
                    .build();
                return accessRepository.save(newAccess);
            });
    }

    /**
     * Révoque l'accès d'un utilisateur à un dossier client
     */
    public void revokeAccess(Long userId, Long companyId) {
        log.info("Révocation de l'accès de l'utilisateur {} pour l'entreprise {}", userId, companyId);

        UserCompanyAccess access = accessRepository.findByUserIdAndCompanyId(userId, companyId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Aucun accès trouvé pour l'utilisateur " + userId + " et l'entreprise " + companyId
            ));

        accessRepository.delete(access);
    }

    /**
     * Récupère tous les accès d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<UserCompanyAccess> getUserAccesses(Long userId) {
        return accessRepository.findByUserId(userId);
    }

    /**
     * Récupère tous les accès pour un dossier client
     */
    @Transactional(readOnly = true)
    public List<UserCompanyAccess> getCompanyAccesses(Long companyId) {
        return accessRepository.findByCompanyId(companyId);
    }

    /**
     * Récupère l'accès d'un utilisateur à un dossier client
     */
    @Transactional(readOnly = true)
    public UserCompanyAccess getAccess(Long userId, Long companyId) {
        return accessRepository.findByUserIdAndCompanyId(userId, companyId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Aucun accès trouvé pour l'utilisateur " + userId + " et l'entreprise " + companyId
            ));
    }

    /**
     * Récupère toutes les entreprises accessibles par un utilisateur
     */
    @Transactional(readOnly = true)
    public List<Long> getAccessibleCompanyIds(Long userId) {
        return accessRepository.findCompanyIdsByUserId(userId);
    }

    /**
     * Récupère toutes les entreprises où l'utilisateur a accès en écriture
     */
    @Transactional(readOnly = true)
    public List<Long> getWritableCompanyIds(Long userId) {
        return accessRepository.findWritableCompanyIdsByUserId(userId);
    }

    /**
     * Vérifie si un utilisateur a accès à un dossier client
     */
    @Transactional(readOnly = true)
    public boolean hasAccess(Long userId, Long companyId) {
        return accessRepository.hasAccess(userId, companyId);
    }

    /**
     * Vérifie si un utilisateur peut écrire dans un dossier client
     */
    @Transactional(readOnly = true)
    public boolean canWrite(Long userId, Long companyId) {
        return accessRepository.canWrite(userId, companyId);
    }

    /**
     * Vérifie si un utilisateur est admin d'un dossier client
     */
    @Transactional(readOnly = true)
    public boolean isAdmin(Long userId, Long companyId) {
        return accessRepository.isAdmin(userId, companyId);
    }

    /**
     * Promeut un utilisateur à un niveau d'accès supérieur
     */
    public UserCompanyAccess promoteAccess(Long userId, Long companyId) {
        log.info("Promotion de l'accès de l'utilisateur {} pour l'entreprise {}", userId, companyId);

        UserCompanyAccess access = getAccess(userId, companyId);

        AccessLevel newLevel = switch (access.getAccessLevel()) {
            case READ_ONLY -> AccessLevel.READ_WRITE;
            case READ_WRITE -> AccessLevel.ADMIN;
            case ADMIN -> throw new IllegalStateException("L'utilisateur a déjà le niveau d'accès maximal");
        };

        access.setAccessLevel(newLevel);
        return accessRepository.save(access);
    }

    /**
     * Rétrograde un utilisateur à un niveau d'accès inférieur
     */
    public UserCompanyAccess demoteAccess(Long userId, Long companyId) {
        log.info("Rétrogradation de l'accès de l'utilisateur {} pour l'entreprise {}", userId, companyId);

        UserCompanyAccess access = getAccess(userId, companyId);

        AccessLevel newLevel = switch (access.getAccessLevel()) {
            case ADMIN -> AccessLevel.READ_WRITE;
            case READ_WRITE -> AccessLevel.READ_ONLY;
            case READ_ONLY -> throw new IllegalStateException("L'utilisateur a déjà le niveau d'accès minimal");
        };

        access.setAccessLevel(newLevel);
        return accessRepository.save(access);
    }

    /**
     * Copie les accès d'un utilisateur vers un autre
     */
    public List<UserCompanyAccess> copyAccesses(Long sourceUserId, Long targetUserId) {
        log.info("Copie des accès de l'utilisateur {} vers l'utilisateur {}", sourceUserId, targetUserId);

        User targetUser = userRepository.findById(targetUserId)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur cible non trouvé avec l'ID: " + targetUserId));

        List<UserCompanyAccess> sourceAccesses = accessRepository.findByUserId(sourceUserId);

        return sourceAccesses.stream()
            .map(sourceAccess -> {
                // Vérifier si l'accès n'existe pas déjà
                if (accessRepository.hasAccess(targetUserId, sourceAccess.getCompany().getId())) {
                    log.warn("L'utilisateur {} a déjà accès à l'entreprise {}",
                        targetUserId, sourceAccess.getCompany().getId());
                    return null;
                }

                UserCompanyAccess newAccess = UserCompanyAccess.builder()
                    .user(targetUser)
                    .company(sourceAccess.getCompany())
                    .accessLevel(sourceAccess.getAccessLevel())
                    .build();
                return accessRepository.save(newAccess);
            })
            .filter(access -> access != null)
            .toList();
    }

    /**
     * Révoque tous les accès d'un utilisateur
     */
    public void revokeAllUserAccesses(Long userId) {
        log.info("Révocation de tous les accès de l'utilisateur {}", userId);
        accessRepository.deleteByUserId(userId);
    }

    /**
     * Révoque tous les accès pour un dossier client
     */
    public void revokeAllCompanyAccesses(Long companyId) {
        log.info("Révocation de tous les accès pour l'entreprise {}", companyId);
        accessRepository.deleteByCompanyId(companyId);
    }

    /**
     * Compte le nombre d'utilisateurs ayant accès à un dossier client
     */
    @Transactional(readOnly = true)
    public Long countUsersByCompany(Long companyId) {
        return accessRepository.countUsersByCompanyId(companyId);
    }

    /**
     * Récupère les accès avec niveau minimum
     */
    @Transactional(readOnly = true)
    public List<UserCompanyAccess> getAccessesWithMinimumLevel(Long userId, AccessLevel minimumLevel) {
        List<AccessLevel> allowedLevels = switch (minimumLevel) {
            case READ_ONLY -> Arrays.asList(AccessLevel.READ_ONLY, AccessLevel.READ_WRITE, AccessLevel.ADMIN);
            case READ_WRITE -> Arrays.asList(AccessLevel.READ_WRITE, AccessLevel.ADMIN);
            case ADMIN -> Arrays.asList(AccessLevel.ADMIN);
        };

        return accessRepository.findByUserIdAndAccessLevelIn(userId, allowedLevels);
    }
}
