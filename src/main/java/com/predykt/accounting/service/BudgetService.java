package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.Budget;
import com.predykt.accounting.domain.entity.ChartOfAccounts;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.GeneralLedger;
import com.predykt.accounting.repository.BudgetRepository;
import com.predykt.accounting.repository.ChartOfAccountsRepository;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.repository.GeneralLedgerRepository;
import jakarta.persistence.EntityNotFoundException;
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
 * Service pour la gestion des budgets
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CompanyRepository companyRepository;
    private final ChartOfAccountsRepository chartOfAccountsRepository;
    private final GeneralLedgerRepository generalLedgerRepository;

    /**
     * Cree un nouveau budget
     */
    public Budget createBudget(Budget budget) {
        log.info("Creation d'un budget pour l'entreprise {} - Compte: {}, Periode: {} a {}",
            budget.getCompany().getId(),
            budget.getAccount() != null ? budget.getAccount().getAccountNumber() : "N/A",
            budget.getPeriodStart(),
            budget.getPeriodEnd());

        // Verifier que l'entreprise existe
        Company company = companyRepository.findById(budget.getCompany().getId())
            .orElseThrow(() -> new EntityNotFoundException(
                "Entreprise non trouvee avec l'ID: " + budget.getCompany().getId()));

        budget.setCompany(company);

        // Verifier que le compte existe si specifie
        if (budget.getAccount() != null && budget.getAccount().getId() != null) {
            ChartOfAccounts account = chartOfAccountsRepository.findById(budget.getAccount().getId())
                .orElseThrow(() -> new EntityNotFoundException(
                    "Compte non trouve avec l'ID: " + budget.getAccount().getId()));
            budget.setAccount(account);
        }

        // Calculer la variance initiale
        budget.calculateVariance();

        return budgetRepository.save(budget);
    }

    /**
     * Met a jour un budget existant
     */
    public Budget updateBudget(Long budgetId, Budget updatedBudget) {
        log.info("Mise a jour du budget {}", budgetId);

        Budget existingBudget = getBudgetById(budgetId);

        // Verifier si le budget est verrouille
        if (existingBudget.getIsLocked()) {
            throw new IllegalStateException("Impossible de modifier un budget verrouille");
        }

        // Mettre a jour les champs
        existingBudget.setBudgetedAmount(updatedBudget.getBudgetedAmount());
        existingBudget.setBudgetType(updatedBudget.getBudgetType());
        existingBudget.setPeriodStart(updatedBudget.getPeriodStart());
        existingBudget.setPeriodEnd(updatedBudget.getPeriodEnd());

        // Recalculer la variance
        existingBudget.calculateVariance();

        return budgetRepository.save(existingBudget);
    }

    /**
     * Supprime un budget
     */
    public void deleteBudget(Long budgetId) {
        log.info("Suppression du budget {}", budgetId);

        Budget budget = getBudgetById(budgetId);

        if (budget.getIsLocked()) {
            throw new IllegalStateException("Impossible de supprimer un budget verrouille");
        }

        budgetRepository.delete(budget);
    }

    /**
     * Recupere un budget par ID
     */
    @Transactional(readOnly = true)
    public Budget getBudgetById(Long budgetId) {
        return budgetRepository.findById(budgetId)
            .orElseThrow(() -> new EntityNotFoundException("Budget non trouve avec l'ID: " + budgetId));
    }

    /**
     * Recupere tous les budgets d'une entreprise
     */
    @Transactional(readOnly = true)
    public List<Budget> getCompanyBudgets(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvee avec l'ID: " + companyId));

        return budgetRepository.findByCompanyOrderByPeriodStartDesc(company);
    }

    /**
     * Recupere les budgets par annee fiscale
     */
    @Transactional(readOnly = true)
    public List<Budget> getBudgetsByFiscalYear(Long companyId, String fiscalYear) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvee avec l'ID: " + companyId));

        return budgetRepository.findByCompanyAndFiscalYear(company, fiscalYear);
    }

    /**
     * Met a jour le montant reel et recalcule la variance
     */
    public Budget updateActualAmount(Long budgetId, BigDecimal actualAmount) {
        log.info("Mise a jour du montant reel pour le budget {}: {}", budgetId, actualAmount);

        Budget budget = getBudgetById(budgetId);
        budget.updateActualAmount(actualAmount);

        return budgetRepository.save(budget);
    }

    /**
     * Calcule les montants reels pour tous les budgets d'une periode
     * en se basant sur les ecritures comptables
     */
    public int updateActualAmountsFromLedger(Long companyId, String fiscalYear) {
        log.info("Calcul des montants reels depuis le grand livre pour l'entreprise {} - Annee {}",
            companyId, fiscalYear);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvee avec l'ID: " + companyId));

        List<Budget> budgets = budgetRepository.findByCompanyAndFiscalYear(company, fiscalYear);
        int updatedCount = 0;

        for (Budget budget : budgets) {
            if (budget.getAccount() != null) {
                // Calculer le solde reel du compte pour la periode
                BigDecimal actualAmount = calculateActualAmountForAccount(
                    company,
                    budget.getAccount(),
                    budget.getPeriodStart(),
                    budget.getPeriodEnd()
                );

                budget.updateActualAmount(actualAmount);
                budgetRepository.save(budget);
                updatedCount++;
            }
        }

        log.info("{} budgets mis a jour depuis le grand livre", updatedCount);
        return updatedCount;
    }

    /**
     * Calcule le montant reel d'un compte pour une periode donnee
     */
    private BigDecimal calculateActualAmountForAccount(
        Company company,
        ChartOfAccounts account,
        LocalDate startDate,
        LocalDate endDate
    ) {
        List<GeneralLedger> entries = generalLedgerRepository
            .findByCompanyAndAccountAndEntryDateBetween(company, account, startDate, endDate);

        BigDecimal total = BigDecimal.ZERO;

        for (GeneralLedger entry : entries) {
            // Pour les comptes de charges (6) et produits (7), on prend les debits ou credits
            String accountClass = account.getAccountNumber().substring(0, 1);

            if ("6".equals(accountClass)) {
                // Charges : debits
                total = total.add(entry.getDebitAmount());
            } else if ("7".equals(accountClass)) {
                // Produits : credits
                total = total.add(entry.getCreditAmount());
            } else {
                // Autres comptes : solde net
                total = total.add(entry.getDebitAmount()).subtract(entry.getCreditAmount());
            }
        }

        return total;
    }

    /**
     * Recupere les budgets depasses
     */
    @Transactional(readOnly = true)
    public List<Budget> getOverBudgets(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvee avec l'ID: " + companyId));

        return budgetRepository.findOverBudgetByCompany(company);
    }

    /**
     * Recupere les budgets avec variance significative
     */
    @Transactional(readOnly = true)
    public List<Budget> getBudgetsWithHighVariance(Long companyId, BigDecimal thresholdPct) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvee avec l'ID: " + companyId));

        return budgetRepository.findBudgetsWithHighVariance(company, thresholdPct);
    }

    /**
     * Verrouille un budget
     */
    public Budget lockBudget(Long budgetId) {
        log.info("Verrouillage du budget {}", budgetId);

        Budget budget = getBudgetById(budgetId);
        budget.lock();

        return budgetRepository.save(budget);
    }

    /**
     * Deverrouille un budget
     */
    public Budget unlockBudget(Long budgetId) {
        log.info("Deverrouillage du budget {}", budgetId);

        Budget budget = getBudgetById(budgetId);
        budget.unlock();

        return budgetRepository.save(budget);
    }

    /**
     * Genere des statistiques budgetaires pour une annee
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getBudgetStatistics(Long companyId, String fiscalYear) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvee avec l'ID: " + companyId));

        Map<String, Object> stats = new HashMap<>();

        BigDecimal totalBudgeted = budgetRepository.sumBudgetedAmountByCompanyAndYear(company, fiscalYear);
        BigDecimal totalActual = budgetRepository.sumActualAmountByCompanyAndYear(company, fiscalYear);
        BigDecimal totalVariance = budgetRepository.sumVarianceByCompanyAndYear(company, fiscalYear);

        stats.put("totalBudgeted", totalBudgeted != null ? totalBudgeted : BigDecimal.ZERO);
        stats.put("totalActual", totalActual != null ? totalActual : BigDecimal.ZERO);
        stats.put("totalVariance", totalVariance != null ? totalVariance : BigDecimal.ZERO);

        // Calculer le taux d'execution global
        if (totalBudgeted != null && totalBudgeted.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal executionRate = totalActual
                .divide(totalBudgeted, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            stats.put("executionRate", executionRate);
        } else {
            stats.put("executionRate", BigDecimal.ZERO);
        }

        // Compter les budgets
        List<Budget> budgets = budgetRepository.findByCompanyAndFiscalYear(company, fiscalYear);
        stats.put("totalBudgets", budgets.size());
        stats.put("lockedBudgets", budgetRepository.countByCompanyAndIsLocked(company, true));
        stats.put("overBudgets", budgetRepository.findOverBudgetByCompany(company).size());

        return stats;
    }

    /**
     * Compare budget vs reel pour une periode
     */
    @Transactional(readOnly = true)
    public Map<String, Object> compareBudgetVsActual(Long companyId, LocalDate startDate, LocalDate endDate) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvee avec l'ID: " + companyId));

        List<Budget> budgets = budgetRepository.findByCompanyAndPeriodStartBetween(company, startDate, endDate);

        Map<String, Object> comparison = new HashMap<>();
        comparison.put("period", Map.of("start", startDate, "end", endDate));
        comparison.put("budgets", budgets);
        comparison.put("totalBudgets", budgets.size());

        return comparison;
    }
}
