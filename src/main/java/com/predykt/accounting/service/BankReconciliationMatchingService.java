package com.predykt.accounting.service;

import com.predykt.accounting.config.BankReconciliationMatchingConfig;
import com.predykt.accounting.domain.entity.*;
import com.predykt.accounting.domain.enums.PendingItemType;
import com.predykt.accounting.domain.enums.SuggestionStatus;
import com.predykt.accounting.dto.response.AutoMatchResultDTO;
import com.predykt.accounting.dto.response.MatchSuggestionDTO;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.repository.BankReconciliationRepository;
import com.predykt.accounting.repository.BankReconciliationSuggestionRepository;
import com.predykt.accounting.repository.BankTransactionRepository;
import com.predykt.accounting.repository.GeneralLedgerRepository;
import com.predykt.accounting.repository.PaymentRepository;
import com.predykt.accounting.service.matching.AdvancedMatchingAlgorithms;
import com.predykt.accounting.service.ml.MLMatchingService;
import com.predykt.accounting.dto.ml.MLPredictionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service intelligent de matching automatique pour les rapprochements bancaires
 * VERSION 2.0 - ULTRA-OPTIMISÉ pour haute performance
 *
 * Améliorations majeures:
 * - Algorithmes avancés (Levenshtein, Jaro-Winkler, Subset Sum)
 * - Tolérance contextuelle des montants
 * - Timeout avec résultats partiels
 * - Vérification cohérence débit/crédit
 * - Filtrage précoce (early pruning)
 * - Gestion de milliers de transactions
 *
 * @author PREDYKT Team
 * @version 2.0
 */
@Service
@Transactional(readOnly = true, timeout = 90)
@RequiredArgsConstructor
@Slf4j
public class BankReconciliationMatchingService {

    private final BankReconciliationRepository reconciliationRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final GeneralLedgerRepository generalLedgerRepository;
    private final ChartOfAccountsService chartOfAccountsService;
    private final BankReconciliationSuggestionRepository suggestionRepository;
    private final AdvancedMatchingAlgorithms advancedAlgorithms;
    private final BankReconciliationMatchingConfig config;
    private final PaymentRepository paymentRepository;
    private final PaymentReconciliationService paymentReconciliationService;

    @Autowired(required = false)  // Optional - ML peut être désactivé
    private MLMatchingService mlMatchingService;

    // Variables de contrôle de timeout
    private long analysisStartTime;
    private long timeoutMillis;
    private boolean timeoutReached = false;

    /**
     * Lance l'analyse automatique de matching pour un rapprochement bancaire
     * PERSISTE les suggestions en base de données pour utilisation ultérieure
     *
     * VERSION 2.0: Avec timeout et résultats partiels
     */
    @Transactional
    public AutoMatchResultDTO performAutoMatching(Long reconciliationId) {
        log.info("🚀 Début du matching automatique OPTIMISÉ pour le rapprochement {}", reconciliationId);

        // Initialiser le contrôle de timeout
        analysisStartTime = System.currentTimeMillis();
        timeoutMillis = config.getPerformance().getTimeoutSeconds() * 1000;
        timeoutReached = false;

        BankReconciliation reconciliation = reconciliationRepository.findById(reconciliationId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Rapprochement non trouvé avec l'ID: " + reconciliationId));

        // Supprimer les anciennes suggestions en attente pour ce rapprochement
        List<BankReconciliationSuggestion> oldSuggestions = suggestionRepository
            .findByReconciliationAndStatusOrderByConfidenceScoreDesc(
                reconciliation, SuggestionStatus.PENDING);
        suggestionRepository.deleteAll(oldSuggestions);
        log.info("🗑️ {} anciennes suggestions supprimées", oldSuggestions.size());

        Company company = reconciliation.getCompany();
        LocalDate periodStart = reconciliation.getPeriodStart();
        LocalDate periodEnd = reconciliation.getPeriodEnd();

        // 1. Récupérer les transactions bancaires non réconciliées
        List<BankTransaction> bankTransactions = bankTransactionRepository
            .findByCompanyAndTransactionDateBetween(company, periodStart, periodEnd)
            .stream()
            .filter(bt -> !bt.getIsReconciled())
            .collect(Collectors.toList());

        log.info("📊 {} transactions bancaires non réconciliées trouvées", bankTransactions.size());

        // ✅ OPTIMISATION: Limite stricte sur le nombre de transactions
        if (bankTransactions.size() > config.getPerformance().getMaxItemsPerPhase()) {
            log.warn("⚠️ Trop de transactions bancaires ({}), limitation à {} pour performance",
                bankTransactions.size(), config.getPerformance().getMaxItemsPerPhase());
            // Garder les plus récentes
            bankTransactions = bankTransactions.stream()
                .sorted(Comparator.comparing(BankTransaction::getTransactionDate).reversed())
                .limit(config.getPerformance().getMaxItemsPerPhase())
                .collect(Collectors.toList());
        }

        // 2. Récupérer les écritures du compte 52X (comptes bancaires)
        String bankAccountNumber = reconciliation.getGlAccountNumber() != null ?
            reconciliation.getGlAccountNumber() : "521";

        ChartOfAccounts bankAccount = chartOfAccountsService
            .getAccountByNumber(company.getId(), bankAccountNumber);

        List<GeneralLedger> glEntries = generalLedgerRepository
            .findByCompanyAndAccountAndEntryDateBetween(company, bankAccount, periodStart, periodEnd)
            .stream()
            .filter(gl -> gl.getBankTransaction() == null) // Non déjà réconcilié
            .collect(Collectors.toList());

        log.info("📚 {} écritures comptables non réconciliées trouvées", glEntries.size());

        // ✅ OPTIMISATION: Limite stricte sur les écritures GL
        if (glEntries.size() > config.getPerformance().getMaxItemsPerPhase()) {
            log.warn("⚠️ Trop d'écritures GL ({}), limitation à {} pour performance",
                glEntries.size(), config.getPerformance().getMaxItemsPerPhase());
            glEntries = glEntries.stream()
                .sorted(Comparator.comparing(GeneralLedger::getEntryDate).reversed())
                .limit(config.getPerformance().getMaxItemsPerPhase())
                .collect(Collectors.toList());
        }

        // 3. Effectuer le matching intelligent
        AutoMatchResultDTO result = performIntelligentMatching(
            reconciliation, bankTransactions, glEntries);

        long totalTime = System.currentTimeMillis() - analysisStartTime;
        log.info("✅ Matching terminé en {} ms - {} suggestions générées avec {}% de confiance moyenne",
            totalTime,
            result.getSuggestions().size(),
            result.getStatistics().getOverallConfidenceScore());

        if (timeoutReached) {
            log.warn("⏱️ TIMEOUT atteint - Résultats partiels retournés");
        }

        return result;
    }

    /**
     * Algorithme principal de matching intelligent
     * VERSION 2.0: Avec timeout, filtrage précoce, et algorithmes avancés
     */
    private AutoMatchResultDTO performIntelligentMatching(
        BankReconciliation reconciliation,
        List<BankTransaction> bankTransactions,
        List<GeneralLedger> glEntries) {

        AutoMatchResultDTO.AutoMatchResultDTOBuilder resultBuilder = AutoMatchResultDTO.builder()
            .reconciliationId(reconciliation.getId())
            .analyzedAt(LocalDateTime.now())
            .suggestions(new ArrayList<>())
            .unmatchedBankTransactions(new ArrayList<>())
            .unmatchedGLEntries(new ArrayList<>())
            .messages(new ArrayList<>());

        Set<Long> matchedBankTransactionIds = new HashSet<>();
        Set<Long> matchedGLEntryIds = new HashSet<>();

        int exactMatches = 0;
        int probableMatches = 0;
        int possibleMatches = 0;
        int autoApprovedCount = 0;
        int manualReviewCount = 0;
        BigDecimal totalConfidence = BigDecimal.ZERO;

        // ========== PHASE 1: Correspondances EXACTES ==========
        log.info("🔍 Phase 1: Recherche de correspondances exactes (montant + date identiques)");
        if (!checkTimeout()) {
            for (BankTransaction bt : bankTransactions) {
                if (checkTimeout()) break;
                if (matchedBankTransactionIds.contains(bt.getId())) continue;

                for (GeneralLedger gl : glEntries) {
                    if (matchedGLEntryIds.contains(gl.getId())) continue;

                    MatchScore score = calculateMatchScore(bt, gl);

                    if (score.score.compareTo(new BigDecimal("100")) == 0) {
                        // PERSISTANCE: Sauvegarder la suggestion en BDD
                        BankReconciliationSuggestion persistedSuggestion = persistSuggestion(
                            reconciliation, bt, gl, score, PendingItemType.UNCATEGORIZED);

                        MatchSuggestionDTO suggestion = convertToDTO(persistedSuggestion);

                        resultBuilder.suggestions(addToList(resultBuilder.build().getSuggestions(), suggestion));
                        matchedBankTransactionIds.add(bt.getId());
                        matchedGLEntryIds.add(gl.getId());
                        exactMatches++;

                        if (score.score.compareTo(config.getAutoApproveThreshold()) >= 0) {
                            autoApprovedCount++;
                        }

                        totalConfidence = totalConfidence.add(score.score);
                        log.debug("✓ Correspondance exacte: BT#{} <-> GL#{} (Score: {})",
                            bt.getId(), gl.getId(), score.score);
                        break;
                    }
                }
            }
        }
        log.info("✅ Phase 1 terminée: {} correspondances exactes", exactMatches);

        // ========== PHASE 2: Correspondances PROBABLES ==========
        log.info("🔍 Phase 2: Recherche de correspondances probables (montant exact, date proche)");
        if (!checkTimeout()) {
            for (BankTransaction bt : bankTransactions) {
                if (checkTimeout()) break;
                if (matchedBankTransactionIds.contains(bt.getId())) continue;

                for (GeneralLedger gl : glEntries) {
                    if (matchedGLEntryIds.contains(gl.getId())) continue;

                    MatchScore score = calculateMatchScore(bt, gl);

                    if (score.score.compareTo(new BigDecimal("90")) >= 0 &&
                        score.score.compareTo(new BigDecimal("100")) < 0) {

                        BankReconciliationSuggestion persistedSuggestion = persistSuggestion(
                            reconciliation, bt, gl, score, PendingItemType.UNCATEGORIZED);

                        MatchSuggestionDTO suggestion = convertToDTO(persistedSuggestion);

                        resultBuilder.suggestions(addToList(resultBuilder.build().getSuggestions(), suggestion));
                        matchedBankTransactionIds.add(bt.getId());
                        matchedGLEntryIds.add(gl.getId());
                        probableMatches++;
                        manualReviewCount++;

                        totalConfidence = totalConfidence.add(score.score);
                        log.debug("~ Correspondance probable: BT#{} <-> GL#{} (Score: {})",
                            bt.getId(), gl.getId(), score.score);
                        break;
                    }
                }
            }
        }
        log.info("✅ Phase 2 terminée: {} correspondances probables", probableMatches);

        // ========== PHASE 2.3: MATCHING PAYMENT ↔ BANKTRANSACTION ==========
        log.info("🔍 Phase 2.3: Rapprochement Payment ↔ BankTransaction (paiements logiques)");
        int paymentMatches = 0;
        if (!checkTimeout()) {
            paymentMatches = performPaymentMatching(
                reconciliation,
                bankTransactions,
                matchedBankTransactionIds,
                resultBuilder
            );
            probableMatches += paymentMatches;
            manualReviewCount += paymentMatches;
            log.info("✅ Phase 2.3 terminée: {} correspondances Payment ↔ BankTransaction", paymentMatches);
        }

        // ========== PHASE 2.4: MATCHING ML (INTELLIGENCE ARTIFICIELLE) ==========
        log.info("🔍 Phase 2.4: Prédictions ML (Random Forest - Auto-learning)");
        int mlMatches = 0;
        if (!checkTimeout() && mlMatchingService != null) {
            mlMatches = performMLMatching(
                reconciliation,
                bankTransactions,
                glEntries,
                matchedBankTransactionIds,
                matchedGLEntryIds,
                resultBuilder
            );
            probableMatches += mlMatches;
            manualReviewCount += mlMatches;
            log.info("✅ Phase 2.4 terminée: {} correspondances ML", mlMatches);
        } else if (mlMatchingService == null) {
            log.info("ℹ️  Phase 2.4 ignorée: ML désactivé (predykt.ml.enabled=false)");
        }

        // ========== PHASE 2.5: MATCHING MULTIPLE (OPTIMISÉ) ==========
        log.info("🔍 Phase 2.5: Recherche de matching multiple (N-à-1 et 1-à-N) OPTIMISÉ");
        int multipleMatches = 0;
        if (!checkTimeout() && config.getMultipleMatching().isEnabled()) {
            multipleMatches = performOptimizedMultipleMatching(
                reconciliation,
                bankTransactions,
                glEntries,
                matchedBankTransactionIds,
                matchedGLEntryIds,
                resultBuilder
            );
            possibleMatches += multipleMatches;
            manualReviewCount += multipleMatches;
            log.info("✅ Phase 2.5 terminée: {} correspondances multiples", multipleMatches);
        }

        // ========== PHASE 3: Transactions bancaires sans correspondance ==========
        log.info("🔍 Phase 3: Analyse des transactions bancaires sans correspondance");
        if (!checkTimeout()) {
            for (BankTransaction bt : bankTransactions) {
                if (checkTimeout()) break;
                if (matchedBankTransactionIds.contains(bt.getId())) continue;

                MatchSuggestionDTO suggestion = analyzeBankTransactionNotInGL(bt, reconciliation);
                if (suggestion != null) {
                    resultBuilder.suggestions(addToList(resultBuilder.build().getSuggestions(), suggestion));
                    possibleMatches++;
                    manualReviewCount++;
                    totalConfidence = totalConfidence.add(suggestion.getConfidenceScore());
                } else {
                    AutoMatchResultDTO.UnmatchedTransactionDTO unmatched = AutoMatchResultDTO.UnmatchedTransactionDTO.builder()
                        .id(bt.getId())
                        .type("BANK")
                        .date(bt.getTransactionDate().atStartOfDay())
                        .amount(bt.getAmount())
                        .description(bt.getDescription())
                        .reference(bt.getBankReference())
                        .reason("Aucune écriture comptable correspondante trouvée - Vérifier si transaction déjà enregistrée")
                        .build();
                    resultBuilder.unmatchedBankTransactions(
                        addToList(resultBuilder.build().getUnmatchedBankTransactions(), unmatched));
                }
            }
        }
        log.info("✅ Phase 3 terminée");

        // ========== PHASE 4: Écritures GL sans correspondance ==========
        log.info("🔍 Phase 4: Analyse des écritures comptables sans correspondance");
        if (!checkTimeout()) {
            for (GeneralLedger gl : glEntries) {
                if (checkTimeout()) break;
                if (matchedGLEntryIds.contains(gl.getId())) continue;

                MatchSuggestionDTO suggestion = analyzeGLEntryNotInBank(gl, reconciliation);
                if (suggestion != null) {
                    resultBuilder.suggestions(addToList(resultBuilder.build().getSuggestions(), suggestion));
                    possibleMatches++;
                    manualReviewCount++;
                    totalConfidence = totalConfidence.add(suggestion.getConfidenceScore());
                } else {
                    AutoMatchResultDTO.UnmatchedTransactionDTO unmatched = AutoMatchResultDTO.UnmatchedTransactionDTO.builder()
                        .id(gl.getId())
                        .type("GL")
                        .date(gl.getEntryDate().atStartOfDay())
                        .amount(gl.getDebitAmount().subtract(gl.getCreditAmount()))
                        .description(gl.getDescription())
                        .reference(gl.getReference())
                        .reason("Aucune transaction bancaire correspondante - Vérifier chèques non encaissés ou erreurs")
                        .build();
                    resultBuilder.unmatchedGLEntries(
                        addToList(resultBuilder.build().getUnmatchedGLEntries(), unmatched));
                }
            }
        }
        log.info("✅ Phase 4 terminée");

        // Calculer les statistiques finales
        int totalSuggestions = exactMatches + probableMatches + possibleMatches;
        BigDecimal overallConfidence = totalSuggestions > 0 ?
            totalConfidence.divide(new BigDecimal(totalSuggestions), 2, RoundingMode.HALF_UP) :
            BigDecimal.ZERO;

        AutoMatchResultDTO.MatchingStatistics stats = AutoMatchResultDTO.MatchingStatistics.builder()
            .totalBankTransactions(bankTransactions.size())
            .totalGLEntries(glEntries.size())
            .exactMatches(exactMatches)
            .probableMatches(probableMatches)
            .possibleMatches(possibleMatches)
            .unmatchedBankTransactions(resultBuilder.build().getUnmatchedBankTransactions().size())
            .unmatchedGLEntries(resultBuilder.build().getUnmatchedGLEntries().size())
            .overallConfidenceScore(overallConfidence)
            .autoApprovedCount(autoApprovedCount)
            .manualReviewCount(manualReviewCount)
            .build();

        resultBuilder.statistics(stats);

        // Messages récapitulatifs
        List<String> messages = new ArrayList<>();
        messages.add(String.format("✅ %d correspondances exactes trouvées (100%% confiance)", exactMatches));
        if (probableMatches > 0) {
            messages.add(String.format("⚠️ %d correspondances probables nécessitent une vérification", probableMatches));
        }
        if (possibleMatches > 0) {
            messages.add(String.format("📝 %d suggestions basées sur l'analyse des transactions", possibleMatches));
        }
        if (stats.getUnmatchedBankTransactions() > 0) {
            messages.add(String.format("❌ %d transactions bancaires sans correspondance - À analyser",
                stats.getUnmatchedBankTransactions()));
        }
        if (stats.getUnmatchedGLEntries() > 0) {
            messages.add(String.format("❌ %d écritures comptables sans correspondance - Chèques non encaissés?",
                stats.getUnmatchedGLEntries()));
        }

        if (timeoutReached) {
            messages.add("⏱️ ATTENTION: Analyse interrompue après " + config.getPerformance().getTimeoutSeconds() +
                " secondes - Résultats partiels. Considérez diviser le rapprochement en périodes plus courtes.");
        }

        resultBuilder.messages(messages);
        resultBuilder.isBalanced(false);

        return resultBuilder.build();
    }

    /**
     * ✅ NOUVEAU VERSION 2.0: Matching multiple ULTRA-OPTIMISÉ
     * Utilise l'algorithme glouton + subset sum avec limites strictes
     */
    private int performOptimizedMultipleMatching(
        BankReconciliation reconciliation,
        List<BankTransaction> allBankTransactions,
        List<GeneralLedger> allGlEntries,
        Set<Long> matchedBankTransactionIds,
        Set<Long> matchedGLEntryIds,
        AutoMatchResultDTO.AutoMatchResultDTOBuilder resultBuilder) {

        int matchCount = 0;
        int maxCandidates = config.getPerformance().getMaxCandidatesForMultipleMatching();

        // Filtrer les transactions non matchées
        List<BankTransaction> unmatchedBT = allBankTransactions.stream()
            .filter(bt -> !matchedBankTransactionIds.contains(bt.getId()))
            .collect(Collectors.toList());

        List<GeneralLedger> unmatchedGL = allGlEntries.stream()
            .filter(gl -> !matchedGLEntryIds.contains(gl.getId()))
            .collect(Collectors.toList());

        // ✅ OPTIMISATION CRITIQUE: Limiter le nombre de candidats
        if (unmatchedBT.size() > maxCandidates) {
            log.warn("⚠️ Trop de BT non matchées ({}), limitation à {} pour matching multiple",
                unmatchedBT.size(), maxCandidates);
            unmatchedBT = unmatchedBT.stream()
                .sorted(Comparator.comparing(bt -> bt.getAmount().abs()).reversed())
                .limit(maxCandidates)
                .collect(Collectors.toList());
        }

        if (unmatchedGL.size() > maxCandidates) {
            log.warn("⚠️ Trop de GL non matchées ({}), limitation à {} pour matching multiple",
                unmatchedGL.size(), maxCandidates);
            unmatchedGL = unmatchedGL.stream()
                .sorted(Comparator.comparing(gl ->
                    gl.getDebitAmount().subtract(gl.getCreditAmount()).abs()).reversed())
                .limit(maxCandidates)
                .collect(Collectors.toList());
        }

        long maxDateRange = config.getMultipleMatching().getMaxDateRangeDays();
        int maxSize = config.getMultipleMatching().getMaxTransactions();
        BigDecimal tolerancePercent = config.getAmountTolerancePercent();

        // === MATCHING N-à-1: Plusieurs BT → 1 GL ===
        for (GeneralLedger gl : unmatchedGL) {
            if (checkTimeout()) break;
            if (matchedGLEntryIds.contains(gl.getId())) continue;

            BigDecimal glAmount = gl.getDebitAmount().subtract(gl.getCreditAmount()).abs();
            LocalDate glDate = gl.getEntryDate();

            // ✅ FILTRAGE PRÉCOCE (Early Pruning)
            List<BankTransaction> candidateBTs = unmatchedBT.stream()
                .filter(bt -> !matchedBankTransactionIds.contains(bt.getId()))
                .filter(bt -> Math.abs(ChronoUnit.DAYS.between(bt.getTransactionDate(), glDate)) <= maxDateRange)
                .filter(bt -> bt.getAmount().abs().compareTo(glAmount.multiply(new BigDecimal("0.1"))) > 0)
                // Éliminer les montants < 10% du montant cible (trop petits)
                .collect(Collectors.toList());

            if (candidateBTs.size() < 2) continue; // Besoin d'au moins 2 transactions

            // ✅ UTILISER L'ALGORITHME GLOUTON OPTIMISÉ
            List<BankTransaction> bestCombo = advancedAlgorithms.findBestBankTransactionCombination(
                candidateBTs,
                glAmount,
                tolerancePercent,
                maxSize
            );

            if (!bestCombo.isEmpty()) {
                BigDecimal comboSum = bestCombo.stream()
                    .map(bt -> bt.getAmount().abs())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                String reason = String.format(
                    "Matching N-à-1 OPTIMISÉ: %d transactions bancaires (total: %s) → 1 écriture GL (%s)",
                    bestCombo.size(), comboSum, glAmount
                );

                BankReconciliationSuggestion persistedSuggestion = persistMultipleSuggestion(
                    reconciliation,
                    bestCombo,
                    Arrays.asList(gl),
                    new BigDecimal("75"),
                    PendingItemType.UNCATEGORIZED,
                    reason
                );

                MatchSuggestionDTO suggestion = convertToDTO(persistedSuggestion);
                resultBuilder.suggestions(addToList(resultBuilder.build().getSuggestions(), suggestion));

                bestCombo.forEach(bt -> matchedBankTransactionIds.add(bt.getId()));
                matchedGLEntryIds.add(gl.getId());
                matchCount++;

                log.info("✅ Matching N-à-1 trouvé: {} BT → GL#{} ({})",
                    bestCombo.size(), gl.getId(), reason);
            }
        }

        // === MATCHING 1-à-N: 1 BT → Plusieurs GL ===
        for (BankTransaction bt : unmatchedBT) {
            if (checkTimeout()) break;
            if (matchedBankTransactionIds.contains(bt.getId())) continue;

            BigDecimal btAmount = bt.getAmount().abs();
            LocalDate btDate = bt.getTransactionDate();

            // ✅ FILTRAGE PRÉCOCE
            List<GeneralLedger> candidateGLs = unmatchedGL.stream()
                .filter(gl -> !matchedGLEntryIds.contains(gl.getId()))
                .filter(gl -> Math.abs(ChronoUnit.DAYS.between(gl.getEntryDate(), btDate)) <= maxDateRange)
                .filter(gl -> gl.getDebitAmount().subtract(gl.getCreditAmount()).abs()
                    .compareTo(btAmount.multiply(new BigDecimal("0.1"))) > 0)
                .collect(Collectors.toList());

            if (candidateGLs.size() < 2) continue;

            // ✅ ALGORITHME GLOUTON
            List<GeneralLedger> bestCombo = advancedAlgorithms.findBestGLEntryCombination(
                candidateGLs,
                btAmount,
                tolerancePercent,
                maxSize
            );

            if (!bestCombo.isEmpty()) {
                BigDecimal comboSum = bestCombo.stream()
                    .map(gl -> gl.getDebitAmount().subtract(gl.getCreditAmount()).abs())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                String reason = String.format(
                    "Matching 1-à-N OPTIMISÉ: 1 transaction bancaire (%s) → %d écritures GL (total: %s)",
                    btAmount, bestCombo.size(), comboSum
                );

                BankReconciliationSuggestion persistedSuggestion = persistMultipleSuggestion(
                    reconciliation,
                    Arrays.asList(bt),
                    bestCombo,
                    new BigDecimal("75"),
                    PendingItemType.UNCATEGORIZED,
                    reason
                );

                MatchSuggestionDTO suggestion = convertToDTO(persistedSuggestion);
                resultBuilder.suggestions(addToList(resultBuilder.build().getSuggestions(), suggestion));

                matchedBankTransactionIds.add(bt.getId());
                bestCombo.forEach(gl -> matchedGLEntryIds.add(gl.getId()));
                matchCount++;

                log.info("✅ Matching 1-à-N trouvé: BT#{} → {} GL ({})",
                    bt.getId(), bestCombo.size(), reason);
            }
        }

        return matchCount;
    }

    /**
     * ✅ VERSION 2.0: Calcule le score de correspondance avec algorithmes avancés
     * Améliorations:
     * - Tolérance contextuelle des montants
     * - Similarité textuelle avancée (Jaro-Winkler + Levenshtein)
     * - Vérification cohérence débit/crédit
     */
    private MatchScore calculateMatchScore(BankTransaction bt, GeneralLedger gl) {
        BigDecimal score = BigDecimal.ZERO;
        List<String> reasons = new ArrayList<>();

        // 1. Comparaison des montants avec tolérance CONTEXTUELLE
        BigDecimal btAmount = bt.getAmount().abs();
        BigDecimal glAmount = gl.getDebitAmount().subtract(gl.getCreditAmount()).abs();

        boolean amountExactMatch = btAmount.compareTo(glAmount) == 0;
        boolean amountCloseMatch = isAmountCloseContextual(btAmount, glAmount);

        if (amountExactMatch) {
            score = score.add(new BigDecimal("50"));
            reasons.add("Montant exact: " + btAmount);
        } else if (amountCloseMatch) {
            score = score.add(new BigDecimal("30"));
            reasons.add("Montant proche: BT=" + btAmount + " GL=" + glAmount);
        } else {
            reasons.add("Montants différents: BT=" + btAmount + " GL=" + glAmount);
            return new MatchScore(BigDecimal.ZERO, reasons);
        }

        // 2. Comparaison des dates
        long daysDiff = Math.abs(ChronoUnit.DAYS.between(bt.getTransactionDate(), gl.getEntryDate()));

        if (daysDiff == 0) {
            score = score.add(new BigDecimal("50"));
            reasons.add("Date identique");
        } else if (daysDiff <= config.getDateThresholds().getGoodMatchDays()) {
            score = score.add(new BigDecimal("40"));
            reasons.add("Date proche (±" + daysDiff + " jours)");
        } else if (daysDiff <= config.getDateThresholds().getFairMatchDays()) {
            score = score.add(new BigDecimal("25"));
            reasons.add("Date acceptable (±" + daysDiff + " jours)");
        } else if (daysDiff <= config.getDateThresholds().getLowMatchDays()) {
            score = score.add(new BigDecimal("10"));
            reasons.add("Date éloignée (±" + daysDiff + " jours)");
        } else {
            reasons.add("Dates trop éloignées (±" + daysDiff + " jours)");
        }

        // ✅ NOUVEAU: Vérification de cohérence débit/crédit
        boolean btIsCredit = bt.getAmount().compareTo(BigDecimal.ZERO) > 0;
        boolean glIsDebit = gl.getDebitAmount().compareTo(BigDecimal.ZERO) > 0;

        // En banque (compte 521): Crédit BT = entrée d'argent = Débit GL
        // En banque: Débit BT = sortie d'argent = Crédit GL
        boolean sensCoherent = (btIsCredit && glIsDebit) || (!btIsCredit && !glIsDebit);

        if (!sensCoherent) {
            score = score.subtract(new BigDecimal("30"));
            reasons.add("⚠️ ATTENTION: Sens débit/crédit inversé - Vérifier impérativement");
        } else {
            reasons.add("✓ Sens débit/crédit cohérent");
        }

        // 3. Comparaison des références
        if (bt.getBankReference() != null && gl.getReference() != null) {
            if (bt.getBankReference().equalsIgnoreCase(gl.getReference())) {
                score = score.add(new BigDecimal("10"));
                reasons.add("Référence identique");
            }
        }

        // ✅ 4. NOUVEAU: Similarité textuelle AVANCÉE
        if (bt.getDescription() != null && gl.getDescription() != null) {
            double similarity = calculateAdvancedTextSimilarity(
                bt.getDescription(),
                gl.getDescription()
            );

            double threshold = config.getTextSimilarity().getThreshold();
            if (similarity >= threshold) {
                int weight = config.getTextSimilarity().getWeight();
                score = score.add(new BigDecimal(weight));
                reasons.add(String.format("Description similaire (%.1f%% - algorithme %s)",
                    similarity * 100, config.getTextSimilarity().getAlgorithm()));
            }
        }

        return new MatchScore(score, reasons);
    }

    /**
     * ✅ NOUVEAU: Tolérance de montant CONTEXTUELLE
     * Adapte la tolérance selon le montant (gros vs petits montants)
     */
    private boolean isAmountCloseContextual(BigDecimal amount1, BigDecimal amount2) {
        BigDecimal diff = amount1.subtract(amount2).abs();

        BankReconciliationMatchingConfig.AmountTolerance toleranceConfig = config.getAmountTolerance();

        BigDecimal tolerance;

        if (amount1.compareTo(toleranceConfig.getLargeAmountThreshold()) >= 0) {
            // Gros montants: tolérance réduite avec max absolu
            BigDecimal percentTolerance = amount1.multiply(toleranceConfig.getLargeAmountPercent());
            tolerance = percentTolerance.min(toleranceConfig.getMaximumAbsolute());
        } else {
            // Petits/moyens montants: tolérance normale avec min absolu
            BigDecimal percentTolerance = amount1.multiply(toleranceConfig.getSmallAmountPercent());
            tolerance = percentTolerance.max(toleranceConfig.getMinimumAbsolute());
        }

        return diff.compareTo(tolerance) <= 0;
    }

    /**
     * ✅ NOUVEAU: Similarité textuelle AVANCÉE
     * Utilise l'algorithme configuré (Jaccard, Levenshtein, Jaro-Winkler, ou Advanced)
     */
    private double calculateAdvancedTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;

        BankReconciliationMatchingConfig.TextSimilarity.SimilarityAlgorithm algorithm =
            config.getTextSimilarity().getAlgorithm();

        switch (algorithm) {
            case JACCARD:
                return calculateJaccardSimilarity(text1, text2);

            case LEVENSHTEIN:
                int distance = advancedAlgorithms.levenshteinDistance(text1, text2);
                int maxLength = Math.max(text1.length(), text2.length());
                return maxLength == 0 ? 1.0 : 1.0 - ((double) distance / maxLength);

            case JARO_WINKLER:
                return advancedAlgorithms.jaroWinklerSimilarity(text1, text2);

            case ADVANCED:
            default:
                return advancedAlgorithms.advancedTextSimilarity(text1, text2);
        }
    }

    /**
     * Calcule la similarité de Jaccard (algorithme de base conservé pour compatibilité)
     */
    private double calculateJaccardSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;

        Set<String> words1 = new HashSet<>(Arrays.asList(text1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.toLowerCase().split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * Analyse une transaction bancaire qui n'a pas de correspondance en comptabilité
     * PERSISTE la suggestion en base de données
     */
    private MatchSuggestionDTO analyzeBankTransactionNotInGL(
        BankTransaction bt, BankReconciliation reconciliation) {

        String description = bt.getDescription() != null ? bt.getDescription().toLowerCase() : "";
        BigDecimal amount = bt.getAmount();

        PendingItemType suggestedType;
        BigDecimal confidence;
        String reason;

        // Règles heuristiques basées sur le montant et la description
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            // Crédit bancaire = argent entrant
            if (description.contains("virement") || description.contains("vir ")) {
                suggestedType = PendingItemType.CREDIT_NOT_RECORDED;
                confidence = new BigDecimal("85");
                reason = "Virement reçu détecté dans la description - Non enregistré en comptabilité";
            } else if (description.contains("intérêt") || description.contains("interet")) {
                suggestedType = PendingItemType.INTEREST_NOT_RECORDED;
                confidence = new BigDecimal("90");
                reason = "Intérêts bancaires détectés - À enregistrer";
            } else {
                suggestedType = PendingItemType.CREDIT_NOT_RECORDED;
                confidence = new BigDecimal("70");
                reason = "Crédit bancaire non identifié - Vérifier source";
            }
        } else {
            // Débit bancaire = argent sortant
            if (description.contains("frais") || description.contains("commission")) {
                suggestedType = PendingItemType.BANK_FEES_NOT_RECORDED;
                confidence = new BigDecimal("90");
                reason = "Frais bancaires détectés - À enregistrer";
            } else if (description.contains("agios") || description.contains("interet debiteur")) {
                suggestedType = PendingItemType.BANK_CHARGES_NOT_RECORDED;
                confidence = new BigDecimal("90");
                reason = "Agios détectés - À enregistrer";
            } else if (description.contains("prelevement") || description.contains("prel ")) {
                suggestedType = PendingItemType.DIRECT_DEBIT_NOT_RECORDED;
                confidence = new BigDecimal("85");
                reason = "Prélèvement automatique détecté - À enregistrer";
            } else {
                suggestedType = PendingItemType.DEBIT_NOT_RECORDED;
                confidence = new BigDecimal("70");
                reason = "Débit bancaire non identifié - Vérifier nature";
            }
        }

        // PERSISTANCE: Créer et sauvegarder la suggestion
        MatchScore score = new MatchScore(confidence, Arrays.asList(reason));
        BankReconciliationSuggestion persistedSuggestion = persistSuggestion(
            reconciliation, bt, null, score, suggestedType);

        return convertToDTO(persistedSuggestion);
    }

    /**
     * Analyse une écriture comptable qui n'a pas de correspondance sur le relevé bancaire
     * PERSISTE la suggestion en base de données
     */
    private MatchSuggestionDTO analyzeGLEntryNotInBank(
        GeneralLedger gl, BankReconciliation reconciliation) {

        String description = gl.getDescription() != null ? gl.getDescription().toLowerCase() : "";
        String reference = gl.getReference() != null ? gl.getReference().toLowerCase() : "";
        BigDecimal amount = gl.getDebitAmount().subtract(gl.getCreditAmount());

        PendingItemType suggestedType;
        BigDecimal confidence;
        String reason;

        // Règles heuristiques
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            // Crédit en comptabilité = sortie d'argent prévue
            if (reference.contains("chq") || reference.contains("cheque") ||
                description.contains("chèque") || description.contains("cheque")) {
                suggestedType = PendingItemType.CHEQUE_ISSUED_NOT_CASHED;
                confidence = new BigDecimal("90");
                reason = "Chèque émis détecté - Pas encore encaissé par le bénéficiaire";
            } else if (description.contains("virement") || description.contains("vir ")) {
                suggestedType = PendingItemType.DEPOSIT_IN_TRANSIT;
                confidence = new BigDecimal("80");
                reason = "Virement enregistré en comptabilité - En cours de traitement bancaire";
            } else {
                suggestedType = PendingItemType.CHEQUE_ISSUED_NOT_CASHED;
                confidence = new BigDecimal("65");
                reason = "Paiement enregistré en comptabilité - Pas encore débité en banque";
            }
        } else {
            // Débit en comptabilité = entrée d'argent prévue
            suggestedType = PendingItemType.DEPOSIT_IN_TRANSIT;
            confidence = new BigDecimal("70");
            reason = "Encaissement enregistré en comptabilité - En cours de traitement bancaire";
        }

        // PERSISTANCE: Créer et sauvegarder la suggestion
        MatchScore score = new MatchScore(confidence, Arrays.asList(reason));
        BankReconciliationSuggestion persistedSuggestion = persistSuggestion(
            reconciliation, null, gl, score, suggestedType);

        return convertToDTO(persistedSuggestion);
    }

    /**
     * Calcule le niveau de confiance en texte
     */
    private String calculateConfidenceLevel(BigDecimal score) {
        if (score.compareTo(new BigDecimal("95")) >= 0) {
            return "EXCELLENT";
        } else if (score.compareTo(new BigDecimal("80")) >= 0) {
            return "GOOD";
        } else if (score.compareTo(new BigDecimal("60")) >= 0) {
            return "FAIR";
        } else {
            return "LOW";
        }
    }

    /**
     * Helper pour ajouter à une liste (builder workaround)
     */
    private <T> List<T> addToList(List<T> list, T item) {
        List<T> newList = new ArrayList<>(list);
        newList.add(item);
        return newList;
    }

    /**
     * Persiste une suggestion en base de données (matching SINGLE 1-à-1)
     */
    private BankReconciliationSuggestion persistSuggestion(
        BankReconciliation reconciliation,
        BankTransaction bt,
        GeneralLedger gl,
        MatchScore score,
        PendingItemType type) {

        BankReconciliationSuggestion suggestion = BankReconciliationSuggestion.builder()
            .reconciliation(reconciliation)
            .suggestedItemType(type)
            .confidenceScore(score.score)
            .confidenceLevel(calculateConfidenceLevel(score.score))
            .status(SuggestionStatus.PENDING)
            .suggestedAmount(bt != null ? bt.getAmount().abs() :
                             gl.getDebitAmount().subtract(gl.getCreditAmount()).abs())
            .description(bt != null ? "Correspondance: " + bt.getDescription() : gl.getDescription())
            .thirdParty(bt != null ? bt.getThirdPartyName() : null)
            .transactionDate(bt != null ? bt.getTransactionDate() : gl.getEntryDate())
            .matchingReason(String.join("; ", score.reasons))
            .requiresManualReview(score.score.compareTo(config.getAutoApproveThreshold()) < 0)
            .matchType("SINGLE")
            .build();

        if (bt != null) {
            suggestion.addBankTransaction(bt);
        }
        if (gl != null) {
            suggestion.addGlEntry(gl);
        }

        return suggestionRepository.save(suggestion);
    }

    /**
     * Persiste une suggestion pour matching multiple (N-à-1 ou 1-à-N)
     */
    private BankReconciliationSuggestion persistMultipleSuggestion(
        BankReconciliation reconciliation,
        List<BankTransaction> bankTransactions,
        List<GeneralLedger> glEntries,
        BigDecimal confidenceScore,
        PendingItemType type,
        String reason) {

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (BankTransaction bt : bankTransactions) {
            totalAmount = totalAmount.add(bt.getAmount().abs());
        }
        for (GeneralLedger gl : glEntries) {
            totalAmount = totalAmount.add(gl.getDebitAmount().subtract(gl.getCreditAmount()).abs());
        }

        BankReconciliationSuggestion suggestion = BankReconciliationSuggestion.builder()
            .reconciliation(reconciliation)
            .suggestedItemType(type)
            .confidenceScore(confidenceScore)
            .confidenceLevel(calculateConfidenceLevel(confidenceScore))
            .status(SuggestionStatus.PENDING)
            .suggestedAmount(totalAmount.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP))
            .description("Matching multiple: " + bankTransactions.size() + " BT ↔ " + glEntries.size() + " GL")
            .transactionDate(bankTransactions.isEmpty() ?
                            (glEntries.isEmpty() ? LocalDate.now() : glEntries.get(0).getEntryDate()) :
                            bankTransactions.get(0).getTransactionDate())
            .matchingReason(reason)
            .requiresManualReview(true)
            .build();

        // Ajouter toutes les transactions
        for (BankTransaction bt : bankTransactions) {
            suggestion.addBankTransaction(bt);
        }
        for (GeneralLedger gl : glEntries) {
            suggestion.addGlEntry(gl);
        }

        return suggestionRepository.save(suggestion);
    }

    /**
     * Convertit une entité BankReconciliationSuggestion en DTO
     */
    private MatchSuggestionDTO convertToDTO(BankReconciliationSuggestion suggestion) {
        MatchSuggestionDTO.MatchSuggestionDTOBuilder builder = MatchSuggestionDTO.builder()
            .suggestionId(suggestion.getId().toString())
            .suggestedItemType(suggestion.getSuggestedItemType())
            .confidenceScore(suggestion.getConfidenceScore())
            .confidenceLevel(suggestion.getConfidenceLevel())
            .suggestedAmount(suggestion.getSuggestedAmount())
            .description(suggestion.getDescription())
            .thirdParty(suggestion.getThirdParty())
            .transactionDate(suggestion.getTransactionDate())
            .matchingReason(suggestion.getMatchingReason())
            .requiresManualReview(suggestion.isRequiresManualReview());

        // Ajouter les infos de la première transaction bancaire
        if (!suggestion.getBankTransactions().isEmpty()) {
            BankTransaction bt = suggestion.getBankTransactions().get(0);
            builder.bankTransactionId(bt.getId())
                   .bankTransactionDate(bt.getTransactionDate())
                   .bankTransactionAmount(bt.getAmount())
                   .bankTransactionDescription(bt.getDescription())
                   .bankReference(bt.getBankReference());
        }

        // Ajouter les infos de la première écriture GL
        if (!suggestion.getGlEntries().isEmpty()) {
            GeneralLedger gl = suggestion.getGlEntries().get(0);
            builder.glEntryId(gl.getId())
                   .glEntryDate(gl.getEntryDate())
                   .glEntryAmount(gl.getDebitAmount().subtract(gl.getCreditAmount()))
                   .glEntryDescription(gl.getDescription())
                   .glEntryReference(gl.getReference())
                   .accountNumber(gl.getAccount().getAccountNumber())
                   .accountName(gl.getAccount().getAccountName());
        }

        return builder.build();
    }

    /**
     * ✅ NOUVEAU VERSION 3.0: Matching ML (Intelligence Artificielle)
     * Utilise Random Forest pour prédire les matches automatiquement
     */
    private int performMLMatching(
        BankReconciliation reconciliation,
        List<BankTransaction> allBankTransactions,
        List<GeneralLedger> allGlEntries,
        Set<Long> matchedBankTransactionIds,
        Set<Long> matchedGLEntryIds,
        AutoMatchResultDTO.AutoMatchResultDTOBuilder resultBuilder) {

        int matchCount = 0;

        // Filtrer les transactions non matchées
        List<BankTransaction> unmatchedBT = allBankTransactions.stream()
            .filter(bt -> !matchedBankTransactionIds.contains(bt.getId()))
            .collect(Collectors.toList());

        List<GeneralLedger> unmatchedGL = allGlEntries.stream()
            .filter(gl -> !matchedGLEntryIds.contains(gl.getId()))
            .collect(Collectors.toList());

        if (unmatchedBT.isEmpty() || unmatchedGL.isEmpty()) {
            return 0;
        }

        // Pour chaque transaction bancaire non matchée, prédire le meilleur GL
        for (BankTransaction bt : unmatchedBT) {
            if (checkTimeout()) break;
            if (matchedBankTransactionIds.contains(bt.getId())) continue;

            try {
                // Utiliser le service ML pour prédire
                Optional<MLPredictionResult> predictionOpt = mlMatchingService.predictWithFiltering(
                    bt,
                    unmatchedGL,
                    reconciliation.getCompany()
                );

                if (predictionOpt.isPresent()) {
                    MLPredictionResult prediction = predictionOpt.get();

                    // Seuil de confiance minimum pour suggestions ML (85%)
                    if (prediction.getConfidenceScore() >= 85.0) {
                        GeneralLedger predictedGL = prediction.getGlEntry();

                        // Vérifier si pas déjà matché
                        if (!matchedGLEntryIds.contains(predictedGL.getId())) {
                            // Créer la suggestion avec explication ML
                            MatchScore mlScore = new MatchScore(
                                BigDecimal.valueOf(prediction.getConfidenceScore()),
                                Arrays.asList(
                                    "🤖 Prédiction ML (Random Forest)",
                                    prediction.getExplanation()
                                )
                            );

                            BankReconciliationSuggestion persistedSuggestion = persistSuggestion(
                                reconciliation,
                                bt,
                                predictedGL,
                                mlScore,
                                PendingItemType.UNCATEGORIZED
                            );

                            // Marquer comme ML-generated
                            persistedSuggestion.setMatchType("ML_PREDICTED");
                            suggestionRepository.save(persistedSuggestion);

                            MatchSuggestionDTO suggestion = convertToDTO(persistedSuggestion);
                            resultBuilder.suggestions(addToList(resultBuilder.build().getSuggestions(), suggestion));

                            matchedBankTransactionIds.add(bt.getId());
                            matchedGLEntryIds.add(predictedGL.getId());
                            matchCount++;

                            log.info("🤖 ML Match: BT#{} → GL#{} (confiance: {:.1f}%, modèle: {})",
                                bt.getId(), predictedGL.getId(),
                                prediction.getConfidenceScore(),
                                prediction.getModelVersion());
                        }
                    }
                }

            } catch (Exception e) {
                log.error("Erreur prédiction ML pour BT {}: {}", bt.getId(), e.getMessage());
                // Continuer avec les autres transactions
            }
        }

        return matchCount;
    }

    /**
     * ✅ NOUVEAU VERSION 3.0 PHASE 2.3: Matching Payment ↔ BankTransaction
     * Rapproche les paiements logiques (Payment) avec les transactions bancaires réelles
     *
     * Cette phase comble le gap critique identifié dans l'audit de cohérence:
     * - Les Payments représentent les paiements logiques liés aux factures
     * - Les BankTransactions sont les mouvements bancaires réels importés
     * - Ce matching permet de tracer la réalisation effective des paiements
     */
    private int performPaymentMatching(
        BankReconciliation reconciliation,
        List<BankTransaction> allBankTransactions,
        Set<Long> matchedBankTransactionIds,
        AutoMatchResultDTO.AutoMatchResultDTOBuilder resultBuilder) {

        int matchCount = 0;
        Company company = reconciliation.getCompany();

        // Utiliser le PaymentReconciliationService pour obtenir des suggestions
        List<PaymentReconciliationService.ReconciliationSuggestion> suggestions =
            paymentReconciliationService.suggestReconciliations(company);

        log.info("📋 {} suggestions de rapprochement Payment ↔ BankTransaction trouvées", suggestions.size());

        // Traiter chaque suggestion
        for (PaymentReconciliationService.ReconciliationSuggestion suggestion : suggestions) {
            if (checkTimeout()) break;

            // Vérifier que la BankTransaction n'est pas déjà matchée
            if (matchedBankTransactionIds.contains(suggestion.getBankTransactionId())) {
                continue;
            }

            // Vérifier que la BankTransaction fait partie du rapprochement en cours
            boolean btInCurrentReconciliation = allBankTransactions.stream()
                .anyMatch(bt -> bt.getId().equals(suggestion.getBankTransactionId()));

            if (!btInCurrentReconciliation) {
                continue;
            }

            // Récupérer les entités complètes
            BankTransaction bt = allBankTransactions.stream()
                .filter(b -> b.getId().equals(suggestion.getBankTransactionId()))
                .findFirst()
                .orElse(null);

            if (bt == null) continue;

            Payment payment = paymentRepository.findById(suggestion.getPaymentId()).orElse(null);
            if (payment == null) continue;

            // Créer la suggestion de matching pour le rapprochement bancaire
            // Score basé sur le score du PaymentReconciliationService
            BigDecimal confidenceScore = new BigDecimal(suggestion.getScore());

            String reason = String.format(
                "💳 Payment %s (Date: %s, Montant: %s XAF) correspond à la transaction bancaire (Score: %d%%)",
                payment.getPaymentNumber(),
                payment.getPaymentDate(),
                payment.getAmount(),
                suggestion.getScore()
            );

            List<String> reasons = new ArrayList<>();
            reasons.add(reason);
            reasons.add("✓ Montant Payment: " + suggestion.getPaymentAmount());
            reasons.add("✓ Montant BankTransaction: " + suggestion.getBankTransactionAmount());
            reasons.add("✓ Date Payment: " + suggestion.getPaymentDate());
            reasons.add("✓ Date BankTransaction: " + suggestion.getBankTransactionDate());

            if (payment.getTransactionReference() != null) {
                reasons.add("✓ Référence transaction: " + payment.getTransactionReference());
            }

            MatchScore matchScore = new MatchScore(confidenceScore, reasons);

            // Créer une suggestion basée sur le Payment (sans GL car c'est un matching Payment)
            BankReconciliationSuggestion persistedSuggestion = BankReconciliationSuggestion.builder()
                .reconciliation(reconciliation)
                .suggestedItemType(PendingItemType.UNCATEGORIZED)
                .confidenceScore(confidenceScore)
                .confidenceLevel(calculateConfidenceLevel(confidenceScore))
                .status(SuggestionStatus.PENDING)
                .suggestedAmount(suggestion.getPaymentAmount())
                .description(String.format(
                    "Payment %s → %s",
                    payment.getPaymentNumber(),
                    payment.getDescription() != null ? payment.getDescription() : "Paiement"
                ))
                .thirdParty(payment.isCustomerPayment() ?
                    (payment.getCustomer() != null ? payment.getCustomer().getName() : null) :
                    (payment.getSupplier() != null ? payment.getSupplier().getName() : null))
                .transactionDate(payment.getPaymentDate())
                .matchingReason(String.join("; ", reasons))
                .requiresManualReview(confidenceScore.compareTo(config.getAutoApproveThreshold()) < 0)
                .matchType("PAYMENT_TO_BANK")
                .build();

            // Ajouter la transaction bancaire
            persistedSuggestion.addBankTransaction(bt);

            // Note: On n'ajoute pas de GL Entry car le Payment peut avoir son propre GL Entry
            // qui n'est pas nécessairement dans le compte bancaire 521

            // Sauvegarder la suggestion
            persistedSuggestion = suggestionRepository.save(persistedSuggestion);

            // Convertir en DTO et ajouter au résultat
            MatchSuggestionDTO suggestionDTO = convertToDTO(persistedSuggestion);
            resultBuilder.suggestions(addToList(resultBuilder.build().getSuggestions(), suggestionDTO));

            // Marquer la BankTransaction comme matchée
            matchedBankTransactionIds.add(bt.getId());
            matchCount++;

            log.info("💳 Payment Match: Payment #{} ({}) ↔ BankTransaction #{} (Score: {}%)",
                payment.getId(),
                payment.getPaymentNumber(),
                bt.getId(),
                suggestion.getScore());
        }

        return matchCount;
    }

    /**
     * ✅ NOUVEAU: Vérification du timeout
     * Retourne true si le timeout est atteint
     */
    private boolean checkTimeout() {
        if (timeoutReached) return true;

        long elapsed = System.currentTimeMillis() - analysisStartTime;
        if (elapsed > timeoutMillis) {
            log.warn("⏱️ TIMEOUT atteint ({} ms) - Arrêt gracieux de l'analyse", elapsed);
            timeoutReached = true;
            return true;
        }

        return false;
    }

    /**
     * Classe interne pour représenter un score de matching
     */
    private static class MatchScore {
        BigDecimal score;
        List<String> reasons;

        MatchScore(BigDecimal score, List<String> reasons) {
            this.score = score;
            this.reasons = reasons;
        }
    }
}
