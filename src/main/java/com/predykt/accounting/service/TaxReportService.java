package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.Supplier;
import com.predykt.accounting.domain.entity.TaxCalculation;
import com.predykt.accounting.domain.enums.TaxType;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.repository.TaxCalculationRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 🟡 SERVICE MOYEN: Rapports fiscaux (AIR, IRPP, alertes NIU)
 *
 * Ce service génère des rapports fiscaux détaillés conformes aux exigences
 * du Code Général des Impôts du Cameroun et OHADA.
 *
 * Fonctionnalités principales:
 * - Rapport mensuel AIR (Acompte sur Impôt sur le Revenu) - Art. 156 CGI
 * - Rapport IRPP Loyer (Impôt sur le Revenu des Personnes Physiques) - Art. 65 CGI
 * - Analyse des fournisseurs sans NIU (pénalité 3,3%)
 * - Calendrier fiscal des échéances (15 du mois suivant)
 * - Résumé fiscal multi-taxes pour tableaux de bord
 *
 * OHADA + Cameroun Compliance:
 * - AIR avec NIU: 2,2% (CGI Art. 156)
 * - AIR sans NIU: 5,5% (pénalité) + alerte automatique
 * - IRPP Loyer: 15% retenue à la source (CGI Art. 65)
 * - TVA: 19,25% (CGI Art. 127)
 * - Échéances: 15 du mois suivant pour toutes taxes
 *
 * @author PREDYKT System Optimizer
 * @since Phase 3 - Optimisations Moyennes
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class TaxReportService {

    private final TaxCalculationRepository taxCalculationRepository;
    private final CompanyRepository companyRepository;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    // Taux de référence pour calculs de pénalité
    private static final BigDecimal AIR_NORMAL_RATE = new BigDecimal("2.2");
    private static final BigDecimal AIR_PENALTY_RATE = new BigDecimal("5.5");
    private static final BigDecimal AIR_PENALTY_DIFFERENCE = AIR_PENALTY_RATE.subtract(AIR_NORMAL_RATE); // 3.3%

    /**
     * 📊 Génère le rapport mensuel AIR (Acompte sur Impôt sur le Revenu)
     *
     * Ce rapport est requis pour la déclaration fiscale mensuelle auprès de la DGI
     * (Direction Générale des Impôts) Cameroun.
     *
     * Échéance: 15 du mois suivant
     * Formulaire: DGI/D10/A (Déclaration mensuelle AIR)
     *
     * @param companyId ID de l'entreprise
     * @param year Année fiscale
     * @param month Mois (1-12)
     * @return Rapport AIR détaillé avec ventilation par fournisseur
     */
    public AIRMonthlyReport generateMonthlyAIRReport(Long companyId, int year, int month) {
        log.info("📊 Génération rapport AIR mensuel: {}/{} pour company {}", year, month, companyId);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));

        // Calculer les dates du mois
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // Récupérer tous les calculs AIR du mois
        List<TaxCalculation> allAirCalculations = taxCalculationRepository
            .findByCompanyAndPeriod(company, startDate, endDate)
            .stream()
            .filter(tc -> tc.getTaxType() == TaxType.AIR_WITH_NIU || tc.getTaxType() == TaxType.AIR_WITHOUT_NIU)
            .collect(Collectors.toList());

        log.debug("  → {} calculs AIR trouvés pour {}", allAirCalculations.size(), yearMonth.format(MONTH_FORMATTER));

        // Séparer AIR avec NIU vs sans NIU
        List<TaxCalculation> airWithNiu = allAirCalculations.stream()
            .filter(tc -> tc.getTaxType() == TaxType.AIR_WITH_NIU)
            .collect(Collectors.toList());

        List<TaxCalculation> airWithoutNiu = allAirCalculations.stream()
            .filter(tc -> tc.getTaxType() == TaxType.AIR_WITHOUT_NIU)
            .collect(Collectors.toList());

        // Calculer totaux
        BigDecimal totalAirWithNiu = airWithNiu.stream()
            .map(TaxCalculation::getTaxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAirWithoutNiu = airWithoutNiu.stream()
            .map(TaxCalculation::getTaxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAir = totalAirWithNiu.add(totalAirWithoutNiu);

        // Calculer base imposable totale
        BigDecimal totalBaseAmount = allAirCalculations.stream()
            .map(TaxCalculation::getBaseAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculer le coût de la pénalité (surcoût dû aux fournisseurs sans NIU)
        BigDecimal penaltyCost = airWithoutNiu.stream()
            .map(TaxCalculation::calculatePenaltyCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Grouper par fournisseur
        Map<Supplier, List<TaxCalculation>> bySupplier = allAirCalculations.stream()
            .filter(tc -> tc.getSupplier() != null)
            .collect(Collectors.groupingBy(TaxCalculation::getSupplier));

        List<AIRSupplierDetail> supplierDetails = bySupplier.entrySet().stream()
            .map(entry -> {
                Supplier supplier = entry.getKey();
                List<TaxCalculation> calculations = entry.getValue();

                BigDecimal supplierTotalBase = calculations.stream()
                    .map(TaxCalculation::getBaseAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal supplierTotalAir = calculations.stream()
                    .map(TaxCalculation::getTaxAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal supplierRate = calculations.get(0).getTaxRate();
                boolean hasNiu = calculations.get(0).getTaxType() == TaxType.AIR_WITH_NIU;

                BigDecimal supplierPenalty = hasNiu ? BigDecimal.ZERO :
                    calculations.stream()
                        .map(TaxCalculation::calculatePenaltyCost)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                return AIRSupplierDetail.builder()
                    .supplierId(supplier.getId())
                    .supplierName(supplier.getName())
                    .niuNumber(supplier.getNiuNumber())
                    .hasNiu(hasNiu)
                    .transactionCount(calculations.size())
                    .totalBaseAmount(supplierTotalBase)
                    .airRate(supplierRate)
                    .totalAirAmount(supplierTotalAir)
                    .penaltyCost(supplierPenalty)
                    .build();
            })
            .sorted(Comparator.comparing(AIRSupplierDetail::getTotalAirAmount).reversed())
            .collect(Collectors.toList());

        // Calculer date d'échéance (15 du mois suivant)
        LocalDate dueDate = yearMonth.plusMonths(1).atDay(15);

        log.info("✅ Rapport AIR généré: {} transactions, {} XAF total AIR, {} XAF pénalités",
            allAirCalculations.size(), totalAir, penaltyCost);

        return AIRMonthlyReport.builder()
            .companyId(companyId)
            .companyName(company.getName())
            .year(year)
            .month(month)
            .monthName(yearMonth.format(MONTH_FORMATTER))
            .startDate(startDate)
            .endDate(endDate)
            .dueDate(dueDate)
            .totalTransactions(allAirCalculations.size())
            .totalBaseAmount(totalBaseAmount)
            .transactionsWithNiu(airWithNiu.size())
            .totalAirWithNiu(totalAirWithNiu)
            .transactionsWithoutNiu(airWithoutNiu.size())
            .totalAirWithoutNiu(totalAirWithoutNiu)
            .totalAirAmount(totalAir)
            .penaltyCost(penaltyCost)
            .supplierDetails(supplierDetails)
            .generatedAt(java.time.LocalDateTime.now())
            .build();
    }

    /**
     * 🏠 Génère le rapport IRPP Loyer (Impôt sur le Revenu des Personnes Physiques)
     *
     * Retenue à la source de 15% sur les loyers payés (CGI Art. 65)
     * - Entreprise verse 85% au bailleur
     * - Entreprise reverse 15% à l'État (DGI)
     *
     * Échéance: 15 du mois suivant
     * Formulaire: DGI/IR/C (Déclaration IRPP Loyer)
     *
     * @param companyId ID de l'entreprise
     * @param year Année fiscale
     * @param month Mois (1-12)
     * @return Rapport IRPP Loyer avec ventilation par propriétaire
     */
    public IRPPRentReport generateIRPPRentReport(Long companyId, int year, int month) {
        log.info("🏠 Génération rapport IRPP Loyer: {}/{} pour company {}", year, month, companyId);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // Récupérer tous les calculs IRPP Loyer du mois
        List<TaxCalculation> irppCalculations = taxCalculationRepository
            .findByCompanyAndPeriod(company, startDate, endDate)
            .stream()
            .filter(tc -> tc.getTaxType() == TaxType.IRPP_RENT)
            .collect(Collectors.toList());

        log.debug("  → {} calculs IRPP Loyer trouvés", irppCalculations.size());

        // Calculer totaux
        BigDecimal totalRentAmount = irppCalculations.stream()
            .map(TaxCalculation::getBaseAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalIrppWithheld = irppCalculations.stream()
            .map(TaxCalculation::getTaxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Montant net versé aux bailleurs (85%)
        BigDecimal totalNetPaidToLandlords = totalRentAmount.subtract(totalIrppWithheld);

        // Grouper par bailleur (supplier)
        Map<Supplier, List<TaxCalculation>> byLandlord = irppCalculations.stream()
            .filter(tc -> tc.getSupplier() != null)
            .collect(Collectors.groupingBy(TaxCalculation::getSupplier));

        List<IRPPLandlordDetail> landlordDetails = byLandlord.entrySet().stream()
            .map(entry -> {
                Supplier landlord = entry.getKey();
                List<TaxCalculation> calculations = entry.getValue();

                BigDecimal totalRent = calculations.stream()
                    .map(TaxCalculation::getBaseAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalIrpp = calculations.stream()
                    .map(TaxCalculation::getTaxAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal netPaid = totalRent.subtract(totalIrpp);

                return IRPPLandlordDetail.builder()
                    .landLordId(landlord.getId())
                    .landLordName(landlord.getName())
                    .taxId(landlord.getTaxId())
                    .paymentCount(calculations.size())
                    .totalRentAmount(totalRent)
                    .irppRate(new BigDecimal("15.0"))
                    .irppWithheld(totalIrpp)
                    .netPaidToLandlord(netPaid)
                    .build();
            })
            .sorted(Comparator.comparing(IRPPLandlordDetail::getTotalRentAmount).reversed())
            .collect(Collectors.toList());

        LocalDate dueDate = yearMonth.plusMonths(1).atDay(15);

        log.info("✅ Rapport IRPP Loyer généré: {} paiements, {} XAF loyers, {} XAF IRPP retenu",
            irppCalculations.size(), totalRentAmount, totalIrppWithheld);

        return IRPPRentReport.builder()
            .companyId(companyId)
            .companyName(company.getName())
            .year(year)
            .month(month)
            .monthName(yearMonth.format(MONTH_FORMATTER))
            .startDate(startDate)
            .endDate(endDate)
            .dueDate(dueDate)
            .totalPayments(irppCalculations.size())
            .totalRentAmount(totalRentAmount)
            .totalIrppWithheld(totalIrppWithheld)
            .totalNetPaidToLandlords(totalNetPaidToLandlords)
            .landlordDetails(landlordDetails)
            .generatedAt(java.time.LocalDateTime.now())
            .build();
    }

    /**
     * ⚠️ Génère le rapport d'alertes fournisseurs sans NIU
     *
     * Identifie tous les fournisseurs qui causent une pénalité de 3,3%
     * (5,5% - 2,2%) sur les transactions d'achat.
     *
     * Ce rapport est crucial pour:
     * - Service Achats: Régulariser les dossiers fournisseurs
     * - Direction Financière: Optimiser les coûts fiscaux
     * - Conformité: Réduire les risques fiscaux
     *
     * @param companyId ID de l'entreprise
     * @param startDate Date de début d'analyse
     * @param endDate Date de fin d'analyse
     * @return Rapport d'alerte avec coûts détaillés
     */
    public SupplierNIUAlertReport generateSupplierNIUAlertReport(
        Long companyId,
        LocalDate startDate,
        LocalDate endDate
    ) {
        log.info("⚠️ Génération rapport alertes NIU: {} à {} pour company {}",
            startDate, endDate, companyId);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));

        // Récupérer tous les calculs AIR sans NIU avec alertes
        List<TaxCalculation> alertCalculations = taxCalculationRepository
            .findByCompanyAndPeriod(company, startDate, endDate)
            .stream()
            .filter(tc -> tc.getTaxType() == TaxType.AIR_WITHOUT_NIU && tc.getHasAlert())
            .collect(Collectors.toList());

        log.debug("  → {} calculs avec alerte NIU trouvés", alertCalculations.size());

        // Calculer coût total des pénalités
        BigDecimal totalPenaltyCost = alertCalculations.stream()
            .map(TaxCalculation::calculatePenaltyCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBaseAmount = alertCalculations.stream()
            .map(TaxCalculation::getBaseAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAirPaid = alertCalculations.stream()
            .map(TaxCalculation::getTaxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculer ce qui aurait été payé avec NIU (2,2%)
        BigDecimal totalAirIfNiu = totalBaseAmount
            .multiply(AIR_NORMAL_RATE)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Grouper par fournisseur pour identifier les plus coûteux
        Map<Supplier, List<TaxCalculation>> bySupplier = alertCalculations.stream()
            .filter(tc -> tc.getSupplier() != null)
            .collect(Collectors.groupingBy(TaxCalculation::getSupplier));

        List<SupplierNIUAlert> supplierAlerts = bySupplier.entrySet().stream()
            .map(entry -> {
                Supplier supplier = entry.getKey();
                List<TaxCalculation> calculations = entry.getValue();

                BigDecimal supplierTotalBase = calculations.stream()
                    .map(TaxCalculation::getBaseAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal supplierPenalty = calculations.stream()
                    .map(TaxCalculation::calculatePenaltyCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal supplierAirPaid = calculations.stream()
                    .map(TaxCalculation::getTaxAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal supplierAirIfNiu = supplierTotalBase
                    .multiply(AIR_NORMAL_RATE)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                // Calculer le % de pénalité par rapport au total
                BigDecimal penaltyPercentage = totalPenaltyCost.compareTo(BigDecimal.ZERO) > 0
                    ? supplierPenalty.multiply(BigDecimal.valueOf(100))
                        .divide(totalPenaltyCost, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

                return SupplierNIUAlert.builder()
                    .supplierId(supplier.getId())
                    .supplierName(supplier.getName())
                    .email(supplier.getEmail())
                    .phone(supplier.getPhone())
                    .transactionCount(calculations.size())
                    .totalPurchaseAmount(supplierTotalBase)
                    .airPaidAt55Percent(supplierAirPaid)
                    .airIfHadNiuAt22Percent(supplierAirIfNiu)
                    .penaltyCost(supplierPenalty)
                    .penaltyPercentageOfTotal(penaltyPercentage)
                    .firstTransactionDate(calculations.stream()
                        .map(TaxCalculation::getCalculationDate)
                        .min(LocalDate::compareTo)
                        .orElse(null))
                    .lastTransactionDate(calculations.stream()
                        .map(TaxCalculation::getCalculationDate)
                        .max(LocalDate::compareTo)
                        .orElse(null))
                    .actionRequired("Demander le NIU au fournisseur pour économiser " +
                        supplierPenalty + " XAF par période similaire")
                    .build();
            })
            .sorted(Comparator.comparing(SupplierNIUAlert::getPenaltyCost).reversed())
            .collect(Collectors.toList());

        // Calcul potentiel d'économies annuelles (extrapolation)
        long daysCovered = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        BigDecimal annualizedPenaltyCost = BigDecimal.ZERO;
        if (daysCovered > 0) {
            BigDecimal dailyPenalty = totalPenaltyCost.divide(
                BigDecimal.valueOf(daysCovered), 2, RoundingMode.HALF_UP
            );
            annualizedPenaltyCost = dailyPenalty.multiply(BigDecimal.valueOf(365));
        }

        log.info("✅ Rapport NIU généré: {} fournisseurs sans NIU, {} XAF pénalités, {} XAF/an estimé",
            supplierAlerts.size(), totalPenaltyCost, annualizedPenaltyCost);

        return SupplierNIUAlertReport.builder()
            .companyId(companyId)
            .companyName(company.getName())
            .startDate(startDate)
            .endDate(endDate)
            .totalSuppliersWithoutNiu(supplierAlerts.size())
            .totalTransactionsAffected(alertCalculations.size())
            .totalPurchaseAmount(totalBaseAmount)
            .totalAirPaidAt55Percent(totalAirPaid)
            .totalAirIfHadNiuAt22Percent(totalAirIfNiu)
            .totalPenaltyCost(totalPenaltyCost)
            .estimatedAnnualPenaltyCost(annualizedPenaltyCost)
            .supplierAlerts(supplierAlerts)
            .generatedAt(java.time.LocalDateTime.now())
            .build();
    }

    /**
     * 📅 Génère le calendrier fiscal (échéances de paiement)
     *
     * Liste toutes les échéances fiscales pour une période donnée
     * selon les règles du CGI Cameroun (15 du mois suivant).
     *
     * @param companyId ID de l'entreprise
     * @param year Année fiscale
     * @return Calendrier des échéances par mois
     */
    public TaxPaymentSchedule generateTaxPaymentSchedule(Long companyId, int year) {
        log.info("📅 Génération calendrier fiscal {} pour company {}", year, companyId);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));

        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        List<MonthlyTaxDue> monthlySchedule = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            YearMonth yearMonth = YearMonth.of(year, month);
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();
            LocalDate dueDate = yearMonth.plusMonths(1).atDay(15);

            // Calculer montants dus pour ce mois
            List<TaxCalculation> monthCalculations = taxCalculationRepository
                .findByCompanyAndPeriod(company, startDate, endDate);

            BigDecimal totalVAT = sumByTaxType(monthCalculations, TaxType.VAT);
            BigDecimal totalAIR = sumByTaxType(monthCalculations, TaxType.AIR_WITH_NIU)
                .add(sumByTaxType(monthCalculations, TaxType.AIR_WITHOUT_NIU));
            BigDecimal totalIRPP = sumByTaxType(monthCalculations, TaxType.IRPP_RENT);
            BigDecimal totalIS = sumByTaxType(monthCalculations, TaxType.IS_ADVANCE);

            BigDecimal totalDue = totalVAT.add(totalAIR).add(totalIRPP).add(totalIS);

            monthlySchedule.add(MonthlyTaxDue.builder()
                .month(month)
                .monthName(yearMonth.format(MONTH_FORMATTER))
                .periodStart(startDate)
                .periodEnd(endDate)
                .dueDate(dueDate)
                .vatDue(totalVAT)
                .airDue(totalAIR)
                .irppDue(totalIRPP)
                .isDue(totalIS)
                .totalDue(totalDue)
                .build());
        }

        BigDecimal annualTotal = monthlySchedule.stream()
            .map(MonthlyTaxDue::getTotalDue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("✅ Calendrier fiscal généré: 12 mois, {} XAF total annuel", annualTotal);

        return TaxPaymentSchedule.builder()
            .companyId(companyId)
            .companyName(company.getName())
            .year(year)
            .monthlySchedule(monthlySchedule)
            .annualTotalDue(annualTotal)
            .generatedAt(java.time.LocalDateTime.now())
            .build();
    }

    /**
     * 📊 Génère un résumé fiscal multi-taxes pour tableau de bord
     *
     * Vue d'ensemble de toutes les taxes pour une période donnée.
     *
     * @param companyId ID de l'entreprise
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Résumé fiscal consolidé
     */
    public TaxSummaryReport generateTaxSummary(Long companyId, LocalDate startDate, LocalDate endDate) {
        log.info("📊 Génération résumé fiscal: {} à {} pour company {}", startDate, endDate, companyId);

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));

        List<TaxCalculation> allCalculations = taxCalculationRepository
            .findByCompanyAndPeriod(company, startDate, endDate);

        Map<TaxType, List<TaxCalculation>> byTaxType = allCalculations.stream()
            .collect(Collectors.groupingBy(TaxCalculation::getTaxType));

        List<TaxTypeSummary> taxTypeSummaries = byTaxType.entrySet().stream()
            .map(entry -> {
                TaxType taxType = entry.getKey();
                List<TaxCalculation> calculations = entry.getValue();

                BigDecimal totalBase = calculations.stream()
                    .map(TaxCalculation::getBaseAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalTax = calculations.stream()
                    .map(TaxCalculation::getTaxAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                long alertCount = calculations.stream()
                    .filter(TaxCalculation::getHasAlert)
                    .count();

                return TaxTypeSummary.builder()
                    .taxType(taxType)
                    .taxTypeName(taxType.getDisplayName())
                    .transactionCount(calculations.size())
                    .totalBaseAmount(totalBase)
                    .averageRate(taxType.getDefaultRate())
                    .totalTaxAmount(totalTax)
                    .alertCount((int) alertCount)
                    .accountNumber(taxType.getDefaultAccountNumber())
                    .build();
            })
            .sorted(Comparator.comparing(TaxTypeSummary::getTotalTaxAmount).reversed())
            .collect(Collectors.toList());

        BigDecimal grandTotalTax = taxTypeSummaries.stream()
            .map(TaxTypeSummary::getTotalTaxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalAlerts = taxTypeSummaries.stream()
            .mapToInt(TaxTypeSummary::getAlertCount)
            .sum();

        log.info("✅ Résumé fiscal généré: {} types de taxes, {} XAF total, {} alertes",
            taxTypeSummaries.size(), grandTotalTax, totalAlerts);

        return TaxSummaryReport.builder()
            .companyId(companyId)
            .companyName(company.getName())
            .startDate(startDate)
            .endDate(endDate)
            .totalTransactions(allCalculations.size())
            .grandTotalTaxAmount(grandTotalTax)
            .totalAlerts(totalAlerts)
            .taxTypeSummaries(taxTypeSummaries)
            .generatedAt(java.time.LocalDateTime.now())
            .build();
    }

    // ==================== MÉTHODES PRIVÉES ====================

    /**
     * Somme les montants de taxes pour un type donné
     */
    private BigDecimal sumByTaxType(List<TaxCalculation> calculations, TaxType taxType) {
        return calculations.stream()
            .filter(tc -> tc.getTaxType() == taxType)
            .map(TaxCalculation::getTaxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ==================== DTOs POUR RÉPONSES ====================

    /**
     * Rapport mensuel AIR
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class AIRMonthlyReport {
        private Long companyId;
        private String companyName;
        private int year;
        private int month;
        private String monthName;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDate dueDate;
        private int totalTransactions;
        private BigDecimal totalBaseAmount;
        private int transactionsWithNiu;
        private BigDecimal totalAirWithNiu;
        private int transactionsWithoutNiu;
        private BigDecimal totalAirWithoutNiu;
        private BigDecimal totalAirAmount;
        private BigDecimal penaltyCost;
        private List<AIRSupplierDetail> supplierDetails;
        private java.time.LocalDateTime generatedAt;
    }

    /**
     * Détail AIR par fournisseur
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class AIRSupplierDetail {
        private Long supplierId;
        private String supplierName;
        private String niuNumber;
        private boolean hasNiu;
        private int transactionCount;
        private BigDecimal totalBaseAmount;
        private BigDecimal airRate;
        private BigDecimal totalAirAmount;
        private BigDecimal penaltyCost;
    }

    /**
     * Rapport IRPP Loyer
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class IRPPRentReport {
        private Long companyId;
        private String companyName;
        private int year;
        private int month;
        private String monthName;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDate dueDate;
        private int totalPayments;
        private BigDecimal totalRentAmount;
        private BigDecimal totalIrppWithheld;
        private BigDecimal totalNetPaidToLandlords;
        private List<IRPPLandlordDetail> landlordDetails;
        private java.time.LocalDateTime generatedAt;
    }

    /**
     * Détail IRPP par bailleur
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class IRPPLandlordDetail {
        private Long landLordId;
        private String landLordName;
        private String taxId;
        private int paymentCount;
        private BigDecimal totalRentAmount;
        private BigDecimal irppRate;
        private BigDecimal irppWithheld;
        private BigDecimal netPaidToLandlord;
    }

    /**
     * Rapport d'alertes NIU
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class SupplierNIUAlertReport {
        private Long companyId;
        private String companyName;
        private LocalDate startDate;
        private LocalDate endDate;
        private int totalSuppliersWithoutNiu;
        private int totalTransactionsAffected;
        private BigDecimal totalPurchaseAmount;
        private BigDecimal totalAirPaidAt55Percent;
        private BigDecimal totalAirIfHadNiuAt22Percent;
        private BigDecimal totalPenaltyCost;
        private BigDecimal estimatedAnnualPenaltyCost;
        private List<SupplierNIUAlert> supplierAlerts;
        private java.time.LocalDateTime generatedAt;
    }

    /**
     * Alerte par fournisseur sans NIU
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class SupplierNIUAlert {
        private Long supplierId;
        private String supplierName;
        private String email;
        private String phone;
        private int transactionCount;
        private BigDecimal totalPurchaseAmount;
        private BigDecimal airPaidAt55Percent;
        private BigDecimal airIfHadNiuAt22Percent;
        private BigDecimal penaltyCost;
        private BigDecimal penaltyPercentageOfTotal;
        private LocalDate firstTransactionDate;
        private LocalDate lastTransactionDate;
        private String actionRequired;
    }

    /**
     * Calendrier fiscal annuel
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class TaxPaymentSchedule {
        private Long companyId;
        private String companyName;
        private int year;
        private List<MonthlyTaxDue> monthlySchedule;
        private BigDecimal annualTotalDue;
        private java.time.LocalDateTime generatedAt;
    }

    /**
     * Échéance mensuelle
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class MonthlyTaxDue {
        private int month;
        private String monthName;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private LocalDate dueDate;
        private BigDecimal vatDue;
        private BigDecimal airDue;
        private BigDecimal irppDue;
        private BigDecimal isDue;
        private BigDecimal totalDue;
    }

    /**
     * Résumé fiscal global
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class TaxSummaryReport {
        private Long companyId;
        private String companyName;
        private LocalDate startDate;
        private LocalDate endDate;
        private int totalTransactions;
        private BigDecimal grandTotalTaxAmount;
        private int totalAlerts;
        private List<TaxTypeSummary> taxTypeSummaries;
        private java.time.LocalDateTime generatedAt;
    }

    /**
     * Résumé par type de taxe
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class TaxTypeSummary {
        private TaxType taxType;
        private String taxTypeName;
        private int transactionCount;
        private BigDecimal totalBaseAmount;
        private BigDecimal averageRate;
        private BigDecimal totalTaxAmount;
        private int alertCount;
        private String accountNumber;
    }
}
