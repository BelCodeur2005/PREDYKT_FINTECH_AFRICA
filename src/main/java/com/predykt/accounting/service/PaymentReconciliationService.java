package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.BankTransaction;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.Payment;
import com.predykt.accounting.domain.enums.PaymentStatus;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.exception.ValidationException;
import com.predykt.accounting.repository.BankTransactionRepository;
import com.predykt.accounting.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 🔴 SERVICE CRITIQUE: Rapprochement Payment ↔ BankTransaction
 *
 * Ce service établit le lien entre les paiements logiques (Payment) et les mouvements
 * bancaires réels (BankTransaction) importés depuis les relevés bancaires.
 *
 * Fonctionnalités:
 * - Rapprochement manuel (1 Payment ↔ 1 BankTransaction)
 * - Rapprochement groupé (N Payments ↔ 1 BankTransaction)
 * - Rapprochement automatique par scoring (montant + date + description)
 * - Validation de cohérence des montants avec tolérance
 * - Dé-rapprochement pour corriger les erreurs
 *
 * Conforme OHADA et Cameroun: Traçabilité complète des flux de trésorerie
 *
 * @author PREDYKT System Optimizer
 * @since Phase 3 - Corrections Critiques
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationService {

    private final PaymentRepository paymentRepository;
    private final BankTransactionRepository bankTransactionRepository;
    // TODO: Add proper audit logging using AuditService.logAction() with correct signature
    // private final AuditService auditService;

    // Tolérance de 1% pour la validation des montants (frais bancaires possibles)
    private static final BigDecimal AMOUNT_TOLERANCE_PERCENT = new BigDecimal("1.0");

    // Délai maximum acceptable entre date de paiement et date transaction bancaire (jours)
    private static final int MAX_DATE_DIFFERENCE_DAYS = 5;

    /**
     * Rapprochement manuel: 1 Payment ↔ 1 BankTransaction
     * Utilisé quand l'utilisateur identifie manuellement la correspondance
     */
    public void reconcilePaymentWithBankTransaction(Long paymentId, Long bankTransactionId, String reconciledBy) {
        log.info("🔗 Rapprochement manuel: Payment {} ↔ BankTransaction {}", paymentId, bankTransactionId);

        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        BankTransaction bankTransaction = bankTransactionRepository.findById(bankTransactionId)
            .orElseThrow(() -> new ResourceNotFoundException("BankTransaction not found: " + bankTransactionId));

        // Vérifications de cohérence
        validateReconciliation(payment, bankTransaction);

        // Effectuer le rapprochement
        performReconciliation(payment, bankTransaction, reconciledBy);

        log.info("✅ Rapprochement réussi: {} XAF", payment.getAmount());

        // TODO: Add proper audit logging
        // auditService.logAction("Payment", payment.getId(), AuditAction.UPDATE, ...);
    }

    /**
     * Rapprochement groupé: N Payments ↔ 1 BankTransaction
     * Utilisé pour les virements groupés où plusieurs paiements sont regroupés en une transaction
     */
    public void reconcileMultiplePaymentsWithBankTransaction(
        List<Long> paymentIds,
        Long bankTransactionId,
        String reconciledBy
    ) {
        log.info("🔗 Rapprochement groupé: {} Payments ↔ BankTransaction {}", paymentIds.size(), bankTransactionId);

        // Récupérer la transaction bancaire
        BankTransaction bankTransaction = bankTransactionRepository.findById(bankTransactionId)
            .orElseThrow(() -> new ResourceNotFoundException("BankTransaction not found: " + bankTransactionId));

        // Récupérer tous les paiements
        List<Payment> payments = new ArrayList<>();
        for (Long paymentId : paymentIds) {
            Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
            payments.add(payment);
        }

        // Vérifier que tous les paiements sont de la même société
        Company company = payments.get(0).getCompany();
        if (!payments.stream().allMatch(p -> p.getCompany().getId().equals(company.getId()))) {
            throw new ValidationException("Tous les paiements doivent appartenir à la même société");
        }

        // Calculer le total des paiements
        BigDecimal totalPayments = payments.stream()
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Vérifier que le total correspond à la transaction bancaire (avec tolérance)
        if (!amountsMatch(totalPayments, bankTransaction.getAmount().abs())) {
            throw new ValidationException(String.format(
                "Le total des paiements (%s XAF) ne correspond pas à la transaction bancaire (%s XAF)",
                totalPayments, bankTransaction.getAmount().abs()
            ));
        }

        // Rapprocher tous les paiements
        for (Payment payment : payments) {
            performReconciliation(payment, bankTransaction, reconciledBy);
        }

        log.info("✅ Rapprochement groupé réussi: {} paiements pour {} XAF", payments.size(), totalPayments);

        // TODO: Add proper audit logging
        // auditService.logAction("Payment", ..., AuditAction.UPDATE, ...);
    }

    /**
     * Rapprochement automatique par scoring
     * Algorithme intelligent qui score les correspondances possibles
     */
    public List<ReconciliationSuggestion> suggestReconciliations(Company company) {
        log.info("🤖 Recherche de rapprochements automatiques pour company {}", company.getId());

        // Récupérer les paiements non rapprochés
        List<Payment> unreconciledPayments = paymentRepository.findByCompanyAndIsReconciledFalseOrderByPaymentDateDesc(company);

        // Récupérer les transactions bancaires non rapprochées (ou partiellement rapprochées)
        List<BankTransaction> unreconciledBankTransactions = bankTransactionRepository.findByCompanyAndIsReconciledFalse(company);

        List<ReconciliationSuggestion> suggestions = new ArrayList<>();

        // Pour chaque paiement non rapproché
        for (Payment payment : unreconciledPayments) {
            // Chercher les transactions bancaires correspondantes
            for (BankTransaction bankTransaction : unreconciledBankTransactions) {
                // Calculer le score de correspondance
                int score = calculateMatchScore(payment, bankTransaction);

                // Si le score est suffisamment élevé (>= 70%)
                if (score >= 70) {
                    suggestions.add(new ReconciliationSuggestion(
                        payment.getId(),
                        payment.getPaymentNumber(),
                        payment.getAmount(),
                        payment.getPaymentDate(),
                        bankTransaction.getId(),
                        bankTransaction.getAmount(),
                        bankTransaction.getTransactionDate(),
                        score
                    ));
                }
            }
        }

        // Trier par score décroissant
        suggestions.sort((s1, s2) -> Integer.compare(s2.getScore(), s1.getScore()));

        log.info("✅ {} suggestions de rapprochement trouvées", suggestions.size());
        return suggestions;
    }

    /**
     * Dé-rapprochement: annuler un rapprochement erroné
     */
    public void unreconcilePayment(Long paymentId, String unreconciledBy) {
        log.info("🔓 Dé-rapprochement du Payment {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        if (!payment.getIsReconciled()) {
            throw new ValidationException("Ce paiement n'est pas rapproché");
        }

        BankTransaction bankTransaction = payment.getBankTransaction();

        // Annuler le rapprochement
        payment.unreconcile();
        payment.setBankTransaction(null);
        paymentRepository.save(payment);

        // Vérifier si la transaction bancaire a encore d'autres paiements rapprochés
        if (bankTransaction != null) {
            bankTransaction.getPayments().remove(payment);
            if (bankTransaction.getPayments().isEmpty()) {
                bankTransaction.setIsReconciled(false);
                bankTransactionRepository.save(bankTransaction);
            }
        }

        log.info("✅ Dé-rapprochement effectué pour Payment {}", payment.getPaymentNumber());

        // TODO: Add proper audit logging
        // auditService.logAction("Payment", payment.getId(), AuditAction.UPDATE, ...);
    }

    // ==================== MÉTHODES PRIVÉES ====================

    /**
     * Valide qu'un rapprochement est possible entre un Payment et une BankTransaction
     */
    private void validateReconciliation(Payment payment, BankTransaction bankTransaction) {
        // Vérifier que le paiement n'est pas déjà rapproché
        if (payment.getIsReconciled()) {
            throw new ValidationException("Ce paiement est déjà rapproché. Utilisez unreconcilePayment() d'abord.");
        }

        // Vérifier que les montants correspondent (avec tolérance)
        if (!amountsMatch(payment.getAmount(), bankTransaction.getAmount().abs())) {
            throw new ValidationException(String.format(
                "Les montants ne correspondent pas: Payment=%s XAF, BankTransaction=%s XAF (tolérance: %s%%)",
                payment.getAmount(), bankTransaction.getAmount().abs(), AMOUNT_TOLERANCE_PERCENT
            ));
        }

        // Vérifier que les dates sont cohérentes (écart maximum 5 jours)
        long daysDifference = Math.abs(
            payment.getPaymentDate().toEpochDay() - bankTransaction.getTransactionDate().toEpochDay()
        );
        if (daysDifference > MAX_DATE_DIFFERENCE_DAYS) {
            log.warn("⚠️ Écart de {} jours entre Payment et BankTransaction (max conseillé: {})",
                daysDifference, MAX_DATE_DIFFERENCE_DAYS);
        }

        // Vérifier que les sociétés correspondent
        if (!payment.getCompany().getId().equals(bankTransaction.getCompany().getId())) {
            throw new ValidationException("Le paiement et la transaction bancaire n'appartiennent pas à la même société");
        }

        // Vérifier que le statut du paiement permet le rapprochement
        if (payment.getStatus() == PaymentStatus.CANCELLED || payment.getStatus() == PaymentStatus.BOUNCED) {
            throw new ValidationException("Impossible de rapprocher un paiement annulé ou rejeté");
        }
    }

    /**
     * Effectue le rapprochement (mise à jour des entités)
     */
    private void performReconciliation(Payment payment, BankTransaction bankTransaction, String reconciledBy) {
        // Marquer le paiement comme rapproché
        payment.reconcile(reconciledBy);
        payment.setBankTransaction(bankTransaction);

        // Valider le paiement s'il était en attente
        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.validate();
        }

        paymentRepository.save(payment);

        // Marquer la transaction bancaire comme rapprochée
        bankTransaction.setIsReconciled(true);
        bankTransaction.getPayments().add(payment);
        bankTransactionRepository.save(bankTransaction);

        log.debug("💾 Rapprochement sauvegardé: Payment {} ↔ BankTransaction {}",
            payment.getPaymentNumber(), bankTransaction.getId());
    }

    /**
     * Vérifie si deux montants correspondent avec la tolérance définie
     */
    private boolean amountsMatch(BigDecimal amount1, BigDecimal amount2) {
        if (amount1 == null || amount2 == null) {
            return false;
        }

        // Calculer la différence absolue
        BigDecimal difference = amount1.subtract(amount2).abs();

        // Calculer la tolérance (1% du montant le plus grand)
        BigDecimal maxAmount = amount1.max(amount2);
        BigDecimal tolerance = maxAmount.multiply(AMOUNT_TOLERANCE_PERCENT)
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        return difference.compareTo(tolerance) <= 0;
    }

    /**
     * Calcule un score de correspondance entre un Payment et une BankTransaction
     * Score basé sur: montant (50%), date (30%), description (20%)
     *
     * @return Score entre 0 et 100
     */
    private int calculateMatchScore(Payment payment, BankTransaction bankTransaction) {
        int score = 0;

        // 1. Score sur le montant (50 points max)
        if (amountsMatch(payment.getAmount(), bankTransaction.getAmount().abs())) {
            score += 50; // Montant exact (avec tolérance)
        } else {
            BigDecimal difference = payment.getAmount().subtract(bankTransaction.getAmount().abs()).abs();
            BigDecimal diffPercent = difference.divide(payment.getAmount(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

            if (diffPercent.compareTo(new BigDecimal("5")) <= 0) {
                score += 40; // Différence < 5%
            } else if (diffPercent.compareTo(new BigDecimal("10")) <= 0) {
                score += 30; // Différence < 10%
            } else if (diffPercent.compareTo(new BigDecimal("20")) <= 0) {
                score += 15; // Différence < 20%
            }
        }

        // 2. Score sur la date (30 points max)
        long daysDifference = Math.abs(
            payment.getPaymentDate().toEpochDay() - bankTransaction.getTransactionDate().toEpochDay()
        );

        if (daysDifference == 0) {
            score += 30; // Même jour
        } else if (daysDifference <= 1) {
            score += 25; // 1 jour d'écart
        } else if (daysDifference <= 3) {
            score += 20; // 2-3 jours d'écart
        } else if (daysDifference <= 5) {
            score += 10; // 4-5 jours d'écart
        } else if (daysDifference <= 10) {
            score += 5; // 6-10 jours d'écart
        }

        // 3. Score sur la description (20 points max)
        if (bankTransaction.getDescription() != null && payment.getDescription() != null) {
            String btDesc = bankTransaction.getDescription().toLowerCase();
            String payDesc = payment.getDescription().toLowerCase();

            // Vérifier si le numéro de paiement apparaît dans la description bancaire
            if (btDesc.contains(payment.getPaymentNumber().toLowerCase())) {
                score += 20;
            }
            // Vérifier si la référence de transaction apparaît
            else if (payment.getTransactionReference() != null &&
                     btDesc.contains(payment.getTransactionReference().toLowerCase())) {
                score += 20;
            }
            // Vérifier la similarité générale des descriptions
            else if (calculateStringSimilarity(btDesc, payDesc) > 0.5) {
                score += 10;
            }
        }

        return score;
    }

    /**
     * Calcule la similarité entre deux chaînes (algorithme Jaccard sur les mots)
     *
     * @return Similarité entre 0.0 et 1.0
     */
    private double calculateStringSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null || s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }

        // Découper en mots
        List<String> words1 = List.of(s1.split("\\s+"));
        List<String> words2 = List.of(s2.split("\\s+"));

        // Calculer l'intersection
        long intersection = words1.stream()
            .filter(words2::contains)
            .count();

        // Calculer l'union
        long union = words1.size() + words2.size() - intersection;

        return union == 0 ? 0.0 : (double) intersection / union;
    }

    // ==================== CLASSES INTERNES ====================

    /**
     * DTO pour les suggestions de rapprochement automatique
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ReconciliationSuggestion {
        private Long paymentId;
        private String paymentNumber;
        private BigDecimal paymentAmount;
        private LocalDate paymentDate;
        private Long bankTransactionId;
        private BigDecimal bankTransactionAmount;
        private LocalDate bankTransactionDate;
        private int score; // Score de correspondance (0-100)
    }
}
