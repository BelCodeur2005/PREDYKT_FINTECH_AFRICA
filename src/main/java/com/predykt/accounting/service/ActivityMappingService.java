package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.ActivityMappingRule;
import com.predykt.accounting.domain.entity.ChartOfAccounts;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.dto.ActivityImportDto;
import com.predykt.accounting.dto.request.activity.ActivityMappingRuleRequest;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.repository.ActivityMappingRuleRepository;
import com.predykt.accounting.repository.ChartOfAccountsRepository;
import com.predykt.accounting.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service de gestion des règles de mapping activité → compte OHADA
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityMappingService {

    private final ActivityMappingRuleRepository mappingRuleRepository;
    private final CompanyRepository companyRepository;
    private final ChartOfAccountsRepository chartRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Trouve le compte OHADA pour une activité donnée
     * Applique les règles de mapping par ordre de priorité
     */
    public MappingResult findAccountForActivity(Long companyId, String activityName) {
        Company company = getCompany(companyId);

        // Récupérer toutes les règles actives triées par priorité
        List<ActivityMappingRule> rules = mappingRuleRepository
            .findByCompanyAndIsActiveTrueOrderByPriorityDesc(company);

        // Tester chaque règle
        for (ActivityMappingRule rule : rules) {
            if (rule.matches(activityName)) {
                log.debug("Activité '{}' matchée par règle: '{}' → compte {}",
                    activityName, rule.getActivityKeyword(), rule.getAccountNumber());

                // Incrémenter le compteur d'utilisation
                rule.incrementUsage();
                mappingRuleRepository.save(rule);

                // Récupérer le nom du compte OHADA
                String accountName = getAccountName(company, rule.getAccountNumber());

                return MappingResult.builder()
                    .accountNumber(rule.getAccountNumber())
                    .accountName(accountName)
                    .journalCode(rule.getJournalCode())
                    .confidenceScore(rule.getConfidenceScore())
                    .matchedRule(rule)
                    .build();
            }
        }

        // Aucune règle trouvée → Mapping par défaut
        log.warn("Aucune règle de mapping trouvée pour '{}', utilisation du compte par défaut", activityName);
        return getDefaultMapping(activityName);
    }

    /**
     * Applique le mapping à un ActivityImportDto
     */
    public void applyMapping(Long companyId, ActivityImportDto dto) {
        MappingResult result = findAccountForActivity(companyId, dto.getActivity());

        dto.setDetectedAccount(result.getAccountNumber());
        dto.setAccountName(result.getAccountName());
        dto.setJournalCode(result.getJournalCode());
        dto.setConfidenceScore(result.getConfidenceScore());

        if (result.getConfidenceScore() < 80) {
            dto.addWarning("Confiance faible (" + result.getConfidenceScore() + "%) pour le mapping du compte");
        }
    }

    /**
     * Crée ou met à jour une règle de mapping
     */
    @Transactional
    public ActivityMappingRule saveRule(Long companyId, ActivityMappingRuleRequest request) {
        Company company = getCompany(companyId);

        // Vérifier que le compte existe
        ChartOfAccounts account = chartRepository.findByCompanyAndAccountNumber(company, request.getAccountNumber())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Compte OHADA " + request.getAccountNumber() + " non trouvé"));

        ActivityMappingRule rule = ActivityMappingRule.builder()
            .company(company)
            .activityKeyword(request.getActivityKeyword())
            .accountNumber(request.getAccountNumber())
            .journalCode(request.getJournalCode())
            .matchType(request.getMatchType())
            .caseSensitive(request.getCaseSensitive())
            .priority(request.getPriority())
            .confidenceScore(request.getConfidenceScore())
            .isActive(request.getIsActive())
            .build();

        rule = mappingRuleRepository.save(rule);
        log.info("Règle de mapping créée: '{}' → {}", rule.getActivityKeyword(), rule.getAccountNumber());

        return rule;
    }

    /**
     * Met à jour une règle existante
     */
    @Transactional
    public ActivityMappingRule updateRule(Long ruleId, ActivityMappingRuleRequest request) {
        ActivityMappingRule rule = mappingRuleRepository.findById(ruleId)
            .orElseThrow(() -> new ResourceNotFoundException("Règle de mapping non trouvée: " + ruleId));

        rule.setActivityKeyword(request.getActivityKeyword());
        rule.setAccountNumber(request.getAccountNumber());
        rule.setJournalCode(request.getJournalCode());
        rule.setMatchType(request.getMatchType());
        rule.setCaseSensitive(request.getCaseSensitive());
        rule.setPriority(request.getPriority());
        rule.setConfidenceScore(request.getConfidenceScore());
        rule.setIsActive(request.getIsActive());

        return mappingRuleRepository.save(rule);
    }

    /**
     * Supprime une règle
     */
    @Transactional
    public void deleteRule(Long ruleId) {
        mappingRuleRepository.deleteById(ruleId);
        log.info("Règle de mapping supprimée: {}", ruleId);
    }

    /**
     * Liste toutes les règles d'une entreprise
     */
    public List<ActivityMappingRule> getAllRules(Long companyId) {
        Company company = getCompany(companyId);
        return mappingRuleRepository.findByCompanyOrderByPriorityDesc(company);
    }

    /**
     * Initialise les mappings par défaut OHADA pour une nouvelle entreprise
     */
    @Transactional
    public void initializeDefaultMappings(Long companyId) {
        Company company = getCompany(companyId);

        // Vérifier si des mappings existent déjà
        long existingCount = mappingRuleRepository.countByCompanyAndIsActiveTrue(company);
        if (existingCount > 0) {
            log.info("L'entreprise {} a déjà {} règles de mapping", companyId, existingCount);
            return;
        }

        // Copier les mappings par défaut depuis la table default_activity_mappings
        String sql = """
            INSERT INTO activity_mapping_rules (company_id, activity_keyword, account_number, journal_code, match_type, priority, confidence_score, is_active, created_at, updated_at)
            SELECT ?, activity_keyword, account_number, journal_code, match_type, priority, 100, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            FROM default_activity_mappings
            WHERE account_number IN (SELECT account_number FROM chart_of_accounts WHERE company_id = ?)
            """;

        int count = jdbcTemplate.update(sql, companyId, companyId);
        log.info("{} règles de mapping par défaut initialisées pour l'entreprise {}", count, companyId);
    }

    /**
     * Mapping par défaut basé sur des heuristiques simples
     */
    private MappingResult getDefaultMapping(String activityName) {
        String activityLower = activityName.toLowerCase();

        // Heuristiques basiques (fallback si aucune règle)
        if (activityLower.contains("vente") || activityLower.contains("sales") || activityLower.contains("revenue")) {
            return MappingResult.builder()
                .accountNumber("701")
                .accountName("Ventes de marchandises")
                .journalCode("VE")
                .confidenceScore(30)  // Faible confiance
                .build();
        }

        if (activityLower.contains("achat") || activityLower.contains("purchase")) {
            return MappingResult.builder()
                .accountNumber("601")
                .accountName("Achats de matières premières")
                .journalCode("AC")
                .confidenceScore(30)
                .build();
        }

        // Par défaut: charges diverses
        return MappingResult.builder()
            .accountNumber("658")
            .accountName("Charges diverses")
            .journalCode("OD")
            .confidenceScore(10)  // Très faible confiance
            .build();
    }

    /**
     * Récupère le nom d'un compte OHADA
     */
    private String getAccountName(Company company, String accountNumber) {
        return chartRepository.findByCompanyAndAccountNumber(company, accountNumber)
            .map(ChartOfAccounts::getAccountName)
            .orElse("Compte " + accountNumber);
    }

    private Company getCompany(Long companyId) {
        return companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée: " + companyId));
    }

    /**
     * Classe interne pour le résultat du mapping
     */
    @lombok.Data
    @lombok.Builder
    public static class MappingResult {
        private String accountNumber;
        private String accountName;
        private String journalCode;
        private Integer confidenceScore;
        private ActivityMappingRule matchedRule;
    }
}
