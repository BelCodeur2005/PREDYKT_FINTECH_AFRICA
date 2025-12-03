package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.*;
import com.predykt.accounting.domain.enums.VATAccountType;
import com.predykt.accounting.domain.enums.VATRecoverableCategory;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.repository.VATTransactionRepository;
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
 * Service de gestion de la récupérabilité de la TVA
 * Implémente les règles fiscales camerounaises sur la TVA non récupérable
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VATRecoverabilityService {

    private final VATTransactionRepository vatTransactionRepository;
    private final CompanyRepository companyRepository;

    /**
     * Enregistre une transaction de TVA avec sa catégorie de récupérabilité
     */
    @Transactional
    public VATTransaction recordVATTransaction(
            Company company,
            GeneralLedger ledgerEntry,
            Supplier supplier,
            LocalDate transactionDate,
            VATAccountType vatAccountType,
            String transactionType,
            BigDecimal amountExcludingVat,
            BigDecimal vatRate,
            BigDecimal vatAmount,
            VATRecoverableCategory recoverableCategory,
            String description,
            String invoiceReference
    ) {
        log.info("📝 Enregistrement transaction TVA - Type: {} - Catégorie: {} - Montant TVA: {} XAF",
            transactionType, recoverableCategory.getDisplayName(), vatAmount);

        VATTransaction transaction = VATTransaction.builder()
            .company(company)
            .ledgerEntry(ledgerEntry)
            .supplier(supplier)
            .transactionDate(transactionDate)
            .vatAccountType(vatAccountType)
            .transactionType(transactionType)
            .amountExcludingVat(amountExcludingVat)
            .vatRate(vatRate)
            .vatAmount(vatAmount)
            .recoverableCategory(recoverableCategory)
            .description(description)
            .invoiceReference(invoiceReference)
            .build();

        // Les montants récupérables/non récupérables sont calculés automatiquement par @PrePersist
        VATTransaction saved = vatTransactionRepository.save(transaction);

        if (saved.isNonRecoverable() || saved.isPartiallyRecoverable()) {
            log.warn("⚠️ TVA non/partiellement récupérable - Montant non récupérable: {} XAF - Raison: {}",
                saved.getNonRecoverableVatAmount(),
                saved.getRecoverableCategory().getDescription());
        }

        return saved;
    }

    /**
     * Détecte automatiquement la catégorie de récupérabilité selon le compte OHADA
     * Règles fiscales camerounaises
     */
    public VATRecoverableCategory detectRecoverableCategory(String accountNumber, String description) {
        if (accountNumber == null) {
            return VATRecoverableCategory.FULLY_RECOVERABLE;
        }

        String desc = description != null ? description.toLowerCase() : "";

        // COMPTES 24x - Immobilisations
        if (accountNumber.startsWith("24")) {
            // 2441 - Matériel de transport
            if (accountNumber.startsWith("2441")) {
                // Véhicules de tourisme (< 9 places)
                if (desc.contains("tourisme") || desc.contains("voiture") ||
                    desc.contains("berline") || desc.contains("citadine") ||
                    desc.contains("véhicule de tourisme") || desc.contains("vp")) {
                    return VATRecoverableCategory.NON_RECOVERABLE_TOURISM_VEHICLE;
                }
                // Véhicules utilitaires (camions, VU) - TVA 100% récupérable
                if (desc.contains("utilitaire") || desc.contains("camion") ||
                    desc.contains("fourgon") || desc.contains("vu")) {
                    return VATRecoverableCategory.FULLY_RECOVERABLE;
                }
            }
        }

        // COMPTES 60x - Achats
        if (accountNumber.startsWith("60")) {
            // 605 - Carburants
            if (accountNumber.startsWith("605") || desc.contains("carburant") ||
                desc.contains("essence") || desc.contains("gasoil") || desc.contains("diesel")) {

                // Carburant pour véhicules de tourisme - 0% récupérable
                if (desc.contains("vp") || desc.contains("voiture") ||
                    desc.contains("tourisme") || desc.contains("berline")) {
                    return VATRecoverableCategory.NON_RECOVERABLE_FUEL_VP;
                }

                // Carburant pour véhicules utilitaires - 80% récupérable
                if (desc.contains("vu") || desc.contains("utilitaire") ||
                    desc.contains("camion") || desc.contains("fourgon")) {
                    return VATRecoverableCategory.RECOVERABLE_80_PERCENT;
                }

                // Par défaut pour carburant sans précision - considérer comme VU (80%)
                return VATRecoverableCategory.RECOVERABLE_80_PERCENT;
            }
        }

        // COMPTES 62x - Services extérieurs
        if (accountNumber.startsWith("62")) {
            // 627 - Frais de représentation
            if (accountNumber.startsWith("627") ||
                desc.contains("restaurant") || desc.contains("représentation") ||
                desc.contains("réception") || desc.contains("cadeaux")) {
                return VATRecoverableCategory.NON_RECOVERABLE_REPRESENTATION;
            }
        }

        // Dépenses de luxe (non exhaustif)
        if (desc.contains("luxe") || desc.contains("somptuaire") ||
            desc.contains("golf") || desc.contains("yachting") ||
            desc.contains("chasse") || desc.contains("pêche")) {
            return VATRecoverableCategory.NON_RECOVERABLE_LUXURY;
        }

        // Dépenses personnelles
        if (desc.contains("personnel") || desc.contains("privé") ||
            desc.contains("dirigeant") || desc.contains("famille")) {
            return VATRecoverableCategory.NON_RECOVERABLE_PERSONAL;
        }

        // Par défaut : TVA 100% récupérable
        return VATRecoverableCategory.FULLY_RECOVERABLE;
    }

    /**
     * Calcule la TVA récupérable pour une période
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateRecoverableVAT(Long companyId, LocalDate startDate, LocalDate endDate, String accountType) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        return vatTransactionRepository.sumRecoverableVatByAccountType(
            company, startDate, endDate, accountType
        );
    }

    /**
     * Récupère les transactions avec TVA non récupérable
     */
    @Transactional(readOnly = true)
    public List<VATTransaction> getNonRecoverableTransactions(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        return vatTransactionRepository.findNonRecoverableTransactions(company);
    }

    /**
     * Récupère les statistiques de TVA non récupérable pour une période
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getNonRecoverableVATStatistics(Long companyId, LocalDate startDate, LocalDate endDate) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        List<Object[]> stats = vatTransactionRepository.getNonRecoverableVatStatistics(
            company, startDate, endDate
        );

        Map<String, Object> result = new HashMap<>();
        BigDecimal totalNonRecoverable = BigDecimal.ZERO;
        Map<String, Map<String, Object>> breakdown = new HashMap<>();

        for (Object[] row : stats) {
            VATRecoverableCategory category = (VATRecoverableCategory) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            Long count = (Long) row[2];

            totalNonRecoverable = totalNonRecoverable.add(amount);

            Map<String, Object> categoryData = new HashMap<>();
            categoryData.put("amount", amount);
            categoryData.put("transactionCount", count);
            categoryData.put("description", category.getDescription());
            categoryData.put("recoverablePercentage", category.getRecoverablePercentage());

            breakdown.put(category.getDisplayName(), categoryData);
        }

        result.put("totalNonRecoverableVAT", totalNonRecoverable);
        result.put("breakdown", breakdown);
        result.put("period", Map.of("start", startDate, "end", endDate));

        // Calculer le total récupérable pour comparaison
        BigDecimal totalRecoverable = vatTransactionRepository.sumRecoverableVatDeductible(
            company, startDate, endDate
        );
        result.put("totalRecoverableVAT", totalRecoverable != null ? totalRecoverable : BigDecimal.ZERO);

        // Calculer le taux de récupérabilité
        BigDecimal totalVAT = totalRecoverable.add(totalNonRecoverable);
        if (totalVAT.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal recoverabilityRate = totalRecoverable
                .multiply(BigDecimal.valueOf(100))
                .divide(totalVAT, 2, java.math.RoundingMode.HALF_UP);
            result.put("recoverabilityRate", recoverabilityRate);
        } else {
            result.put("recoverabilityRate", BigDecimal.valueOf(100));
        }

        log.info("📊 Statistiques TVA non récupérable - Période: {} à {} - Total non récupérable: {} XAF",
            startDate, endDate, totalNonRecoverable);

        return result;
    }

    /**
     * Récupère toutes les transactions pour une période
     */
    @Transactional(readOnly = true)
    public List<VATTransaction> getTransactionsByPeriod(Long companyId, LocalDate startDate, LocalDate endDate) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        return vatTransactionRepository.findByCompanyAndPeriod(company, startDate, endDate);
    }

    /**
     * Met à jour la catégorie de récupérabilité d'une transaction
     */
    @Transactional
    public VATTransaction updateRecoverableCategory(Long transactionId, VATRecoverableCategory newCategory, String justification) {
        VATTransaction transaction = vatTransactionRepository.findById(transactionId)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction TVA non trouvée"));

        VATRecoverableCategory oldCategory = transaction.getRecoverableCategory();
        transaction.setRecoverableCategory(newCategory);
        transaction.setNonRecoverableJustification(justification);

        VATTransaction saved = vatTransactionRepository.save(transaction);

        log.info("🔄 Catégorie de récupérabilité modifiée - Transaction ID: {} - Ancien: {} - Nouveau: {}",
            transactionId, oldCategory.getDisplayName(), newCategory.getDisplayName());

        return saved;
    }

    /**
     * Compte les transactions avec alertes
     */
    @Transactional(readOnly = true)
    public Long countTransactionsWithAlerts(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        return vatTransactionRepository.countTransactionsWithAlerts(company);
    }
}
