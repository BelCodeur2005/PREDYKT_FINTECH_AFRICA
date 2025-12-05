package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.FixedAsset;
import com.predykt.accounting.domain.enums.AssetCategory;
import com.predykt.accounting.domain.enums.DepreciationMethod;
import com.predykt.accounting.dto.response.DepreciationScheduleResponse;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.repository.CompanyRepository;
import com.predykt.accounting.repository.FixedAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service pour le calcul des amortissements conforme OHADA et fiscalité camerounaise
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DepreciationService {

    private final FixedAssetRepository fixedAssetRepository;
    private final CompanyRepository companyRepository;

    /**
     * Générer le tableau d'amortissements complet pour un exercice fiscal
     */
    @Transactional(readOnly = true)
    public DepreciationScheduleResponse generateDepreciationSchedule(Long companyId, Integer fiscalYear) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        log.info("Génération du tableau d'amortissements pour l'entreprise {} - Exercice {}",
                 companyId, fiscalYear);

        LocalDate fiscalYearStart = LocalDate.of(fiscalYear, 1, 1);
        LocalDate fiscalYearEnd = LocalDate.of(fiscalYear, 12, 31);

        // Récupérer toutes les immobilisations concernées par l'exercice
        List<FixedAsset> assets = fixedAssetRepository.findForDepreciationSchedule(
            company, fiscalYearStart, fiscalYearEnd);

        // Calculer les détails de chaque immobilisation
        List<DepreciationScheduleResponse.DepreciationItem> items = assets.stream()
            .map(asset -> calculateDepreciationItem(asset, fiscalYear))
            .sorted(Comparator.comparing((DepreciationScheduleResponse.DepreciationItem item) -> item.getCategory())
                .thenComparing(DepreciationScheduleResponse.DepreciationItem::getAcquisitionDate))
            .collect(Collectors.toList());

        // Calculer les totaux par catégorie
        List<DepreciationScheduleResponse.CategorySummary> categorySummaries =
            calculateCategorySummaries(items);

        // Calculer le résumé global
        DepreciationScheduleResponse.DepreciationSummary summary = calculateSummary(items, assets);

        // Récupérer les mouvements de l'exercice
        List<DepreciationScheduleResponse.AssetMovement> acquisitions =
            getAcquisitions(company, fiscalYearStart, fiscalYearEnd);
        List<DepreciationScheduleResponse.AssetMovement> disposals =
            getDisposals(company, fiscalYearStart, fiscalYearEnd);

        // Générer l'analyse
        DepreciationScheduleResponse.DepreciationAnalysis analysis =
            generateAnalysis(items, assets, fiscalYear);

        return DepreciationScheduleResponse.builder()
            .companyId(companyId)
            .companyName(company.getName())
            .fiscalYear(fiscalYear)
            .fiscalYearStart(fiscalYearStart)
            .fiscalYearEnd(fiscalYearEnd)
            .items(items)
            .categorySummaries(categorySummaries)
            .summary(summary)
            .acquisitions(acquisitions)
            .disposals(disposals)
            .analysis(analysis)
            .build();
    }

    /**
     * Calculer les détails d'amortissement pour une immobilisation
     */
    private DepreciationScheduleResponse.DepreciationItem calculateDepreciationItem(
            FixedAsset asset, Integer fiscalYear) {

        LocalDate fiscalYearStart = LocalDate.of(fiscalYear, 1, 1);
        LocalDate fiscalYearEnd = LocalDate.of(fiscalYear, 12, 31);

        BigDecimal depreciableAmount = asset.getDepreciableAmount();

        // Calculer l'amortissement cumulé de l'exercice précédent
        BigDecimal previousDepreciation = calculateAccumulatedDepreciation(asset, fiscalYear - 1);

        // Calculer la dotation de l'exercice en cours
        BigDecimal currentDepreciation = calculateAnnualDepreciation(asset, fiscalYear);

        // Amortissements cumulés totaux
        BigDecimal accumulatedDepreciation = previousDepreciation.add(currentDepreciation);

        // VNC (Valeur Nette Comptable)
        BigDecimal netBookValue = (asset.getTotalCost() != null ? asset.getTotalCost() : asset.getAcquisitionCost())
            .subtract(accumulatedDepreciation);

        // Vérifier si c'est un prorata temporis
        boolean isProrata = isAcquiredDuringYear(asset, fiscalYear);
        int monthsInService = calculateMonthsInService(asset, fiscalYear);

        // Calcul plus-value/moins-value si cédé
        BigDecimal disposalGainLoss = null;
        if (asset.getDisposalDate() != null && asset.getDisposalAmount() != null) {
            disposalGainLoss = asset.getDisposalAmount().subtract(netBookValue);
        }

        return DepreciationScheduleResponse.DepreciationItem.builder()
            .id(asset.getId())
            .assetNumber(asset.getAssetNumber())
            .assetName(asset.getAssetName())
            .category(asset.getCategory())
            .categoryName(asset.getCategory().getDisplayName())
            .accountNumber(asset.getAccountNumber())
            .acquisitionDate(asset.getAcquisitionDate())
            .acquisitionCost(asset.getAcquisitionCost())
            .installationCost(asset.getInstallationCost())
            .totalCost(asset.getTotalCost() != null ? asset.getTotalCost() : asset.getAcquisitionCost())
            .depreciationMethod(asset.getDepreciationMethod())
            .depreciationMethodName(asset.getDepreciationMethod().getDisplayName())
            .usefulLifeYears(asset.getUsefulLifeYears())
            .depreciationRate(asset.getDepreciationRate())
            .residualValue(asset.getResidualValue())
            .depreciableAmount(depreciableAmount)
            .previousAccumulatedDepreciation(previousDepreciation)
            .currentYearDepreciation(currentDepreciation)
            .accumulatedDepreciation(accumulatedDepreciation)
            .netBookValue(netBookValue)
            .isProrata(isProrata)
            .monthsInService(monthsInService)
            .disposalDate(asset.getDisposalDate())
            .disposalAmount(asset.getDisposalAmount())
            .disposalGainLoss(disposalGainLoss)
            .isFullyDepreciated(asset.getIsFullyDepreciated())
            .isDisposed(asset.isDisposed())
            .build();
    }

    /**
     * Calculer la dotation annuelle d'amortissement pour un exercice donné
     */
    public BigDecimal calculateAnnualDepreciation(FixedAsset asset, Integer fiscalYear) {
        if (!asset.isDepreciable()) {
            return BigDecimal.ZERO;
        }

        LocalDate fiscalYearStart = LocalDate.of(fiscalYear, 1, 1);
        LocalDate fiscalYearEnd = LocalDate.of(fiscalYear, 12, 31);

        // Si acquis après la fin de l'exercice, pas d'amortissement
        if (asset.getAcquisitionDate().isAfter(fiscalYearEnd)) {
            return BigDecimal.ZERO;
        }

        // Si cédé avant le début de l'exercice, pas d'amortissement
        if (asset.getDisposalDate() != null && asset.getDisposalDate().isBefore(fiscalYearStart)) {
            return BigDecimal.ZERO;
        }

        BigDecimal depreciableAmount = asset.getDepreciableAmount();

        if (asset.getDepreciationMethod() == DepreciationMethod.LINEAR) {
            return calculateLinearDepreciation(asset, fiscalYear, depreciableAmount);
        } else if (asset.getDepreciationMethod() == DepreciationMethod.DECLINING_BALANCE) {
            return calculateDecliningBalanceDepreciation(asset, fiscalYear, depreciableAmount);
        }

        // Méthode non supportée, retour à linéaire
        return calculateLinearDepreciation(asset, fiscalYear, depreciableAmount);
    }

    /**
     * Amortissement linéaire
     */
    private BigDecimal calculateLinearDepreciation(FixedAsset asset, Integer fiscalYear,
                                                   BigDecimal depreciableAmount) {
        if (asset.getUsefulLifeYears() == null || asset.getUsefulLifeYears() == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal annualDepreciation = depreciableAmount
            .divide(BigDecimal.valueOf(asset.getUsefulLifeYears()), 2, RoundingMode.HALF_UP);

        // Appliquer le prorata temporis si acquisition en cours d'année
        if (isAcquiredDuringYear(asset, fiscalYear)) {
            int monthsInService = calculateMonthsInService(asset, fiscalYear);
            annualDepreciation = annualDepreciation
                .multiply(BigDecimal.valueOf(monthsInService))
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        }

        // Limiter à la valeur résiduelle
        BigDecimal accumulatedSoFar = calculateAccumulatedDepreciation(asset, fiscalYear - 1);
        BigDecimal remainingValue = depreciableAmount.subtract(accumulatedSoFar);

        return annualDepreciation.min(remainingValue).max(BigDecimal.ZERO);
    }

    /**
     * Amortissement dégressif (coefficients fiscaux Cameroun)
     */
    private BigDecimal calculateDecliningBalanceDepreciation(FixedAsset asset, Integer fiscalYear,
                                                             BigDecimal depreciableAmount) {
        if (asset.getUsefulLifeYears() == null || asset.getUsefulLifeYears() == 0) {
            return BigDecimal.ZERO;
        }

        // Coefficient dégressif selon la durée de vie
        BigDecimal coefficient = asset.getDepreciationMethod()
            .getDecliningBalanceCoefficient(asset.getUsefulLifeYears());

        BigDecimal rate = BigDecimal.ONE
            .divide(BigDecimal.valueOf(asset.getUsefulLifeYears()), 4, RoundingMode.HALF_UP)
            .multiply(coefficient);

        // VNC de début d'exercice
        BigDecimal beginningBookValue = (asset.getTotalCost() != null ? asset.getTotalCost() : asset.getAcquisitionCost())
            .subtract(calculateAccumulatedDepreciation(asset, fiscalYear - 1));

        BigDecimal decliningDepreciation = beginningBookValue.multiply(rate);

        // Calculer aussi le linéaire sur la durée restante
        int yearsElapsed = fiscalYear - asset.getAcquisitionDate().getYear();
        int remainingYears = Math.max(asset.getUsefulLifeYears() - yearsElapsed, 1);

        BigDecimal linearDepreciation = beginningBookValue
            .divide(BigDecimal.valueOf(remainingYears), 2, RoundingMode.HALF_UP);

        // Prendre le maximum (règle du dégressif)
        BigDecimal annualDepreciation = decliningDepreciation.max(linearDepreciation);

        // Appliquer le prorata temporis si acquisition en cours d'année
        if (isAcquiredDuringYear(asset, fiscalYear)) {
            int monthsInService = calculateMonthsInService(asset, fiscalYear);
            annualDepreciation = annualDepreciation
                .multiply(BigDecimal.valueOf(monthsInService))
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        }

        return annualDepreciation.min(beginningBookValue).max(BigDecimal.ZERO);
    }

    /**
     * Calculer l'amortissement cumulé jusqu'à une année donnée
     */
    public BigDecimal calculateAccumulatedDepreciation(FixedAsset asset, Integer upToYear) {
        if (!asset.isDepreciable()) {
            return BigDecimal.ZERO;
        }

        int startYear = asset.getAcquisitionDate().getYear();
        BigDecimal accumulated = BigDecimal.ZERO;

        for (int year = startYear; year <= upToYear; year++) {
            accumulated = accumulated.add(calculateAnnualDepreciation(asset, year));
        }

        return accumulated;
    }

    /**
     * Vérifier si l'immobilisation a été acquise durant l'exercice fiscal
     */
    private boolean isAcquiredDuringYear(FixedAsset asset, Integer fiscalYear) {
        return asset.getAcquisitionDate().getYear() == fiscalYear;
    }

    /**
     * Calculer le nombre de mois en service durant l'exercice
     */
    private int calculateMonthsInService(FixedAsset asset, Integer fiscalYear) {
        LocalDate fiscalYearStart = LocalDate.of(fiscalYear, 1, 1);
        LocalDate fiscalYearEnd = LocalDate.of(fiscalYear, 12, 31);

        LocalDate serviceStart = asset.getAcquisitionDate().isAfter(fiscalYearStart)
            ? asset.getAcquisitionDate() : fiscalYearStart;

        LocalDate serviceEnd = fiscalYearEnd;
        if (asset.getDisposalDate() != null && asset.getDisposalDate().isBefore(fiscalYearEnd)) {
            serviceEnd = asset.getDisposalDate();
        }

        long months = ChronoUnit.MONTHS.between(
            serviceStart.withDayOfMonth(1),
            serviceEnd.withDayOfMonth(1).plusMonths(1)
        );

        return (int) Math.max(1, Math.min(months, 12));
    }

    /**
     * Calculer les totaux par catégorie
     */
    private List<DepreciationScheduleResponse.CategorySummary> calculateCategorySummaries(
            List<DepreciationScheduleResponse.DepreciationItem> items) {

        Map<AssetCategory, List<DepreciationScheduleResponse.DepreciationItem>> byCategory =
            items.stream().collect(Collectors.groupingBy(DepreciationScheduleResponse.DepreciationItem::getCategory));

        return byCategory.entrySet().stream()
            .map(entry -> {
                AssetCategory category = entry.getKey();
                List<DepreciationScheduleResponse.DepreciationItem> categoryItems = entry.getValue();

                return DepreciationScheduleResponse.CategorySummary.builder()
                    .category(category)
                    .categoryName(category.getDisplayName())
                    .accountPrefix(category.getAccountPrefix())
                    .assetCount(categoryItems.size())
                    .totalAcquisitionCost(sumField(categoryItems, DepreciationScheduleResponse.DepreciationItem::getTotalCost))
                    .totalPreviousDepreciation(sumField(categoryItems, DepreciationScheduleResponse.DepreciationItem::getPreviousAccumulatedDepreciation))
                    .totalCurrentDepreciation(sumField(categoryItems, DepreciationScheduleResponse.DepreciationItem::getCurrentYearDepreciation))
                    .totalAccumulatedDepreciation(sumField(categoryItems, DepreciationScheduleResponse.DepreciationItem::getAccumulatedDepreciation))
                    .totalNetBookValue(sumField(categoryItems, DepreciationScheduleResponse.DepreciationItem::getNetBookValue))
                    .build();
            })
            .sorted(Comparator.comparing(DepreciationScheduleResponse.CategorySummary::getCategory))
            .collect(Collectors.toList());
    }

    /**
     * Calculer le résumé global
     */
    private DepreciationScheduleResponse.DepreciationSummary calculateSummary(
            List<DepreciationScheduleResponse.DepreciationItem> items,
            List<FixedAsset> assets) {

        long activeCount = items.stream().filter(item -> !item.getIsDisposed()).count();
        long disposedCount = items.stream().filter(DepreciationScheduleResponse.DepreciationItem::getIsDisposed).count();
        long fullyDepreciatedCount = items.stream().filter(DepreciationScheduleResponse.DepreciationItem::getIsFullyDepreciated).count();

        Map<String, BigDecimal> byMethod = items.stream()
            .collect(Collectors.groupingBy(
                item -> item.getDepreciationMethod().name(),
                Collectors.reducing(BigDecimal.ZERO,
                    DepreciationScheduleResponse.DepreciationItem::getCurrentYearDepreciation,
                    BigDecimal::add)
            ));

        return DepreciationScheduleResponse.DepreciationSummary.builder()
            .totalAssetCount(items.size())
            .activeAssetCount((int) activeCount)
            .disposedAssetCount((int) disposedCount)
            .fullyDepreciatedCount((int) fullyDepreciatedCount)
            .totalGrossValue(sumField(items, DepreciationScheduleResponse.DepreciationItem::getTotalCost))
            .totalPreviousDepreciation(sumField(items, DepreciationScheduleResponse.DepreciationItem::getPreviousAccumulatedDepreciation))
            .totalCurrentDepreciation(sumField(items, DepreciationScheduleResponse.DepreciationItem::getCurrentYearDepreciation))
            .totalAccumulatedDepreciation(sumField(items, DepreciationScheduleResponse.DepreciationItem::getAccumulatedDepreciation))
            .totalNetBookValue(sumField(items, DepreciationScheduleResponse.DepreciationItem::getNetBookValue))
            .depreciationByMethod(byMethod)
            .build();
    }

    /**
     * Récupérer les acquisitions de l'exercice
     */
    private List<DepreciationScheduleResponse.AssetMovement> getAcquisitions(
            Company company, LocalDate start, LocalDate end) {

        return fixedAssetRepository.findAcquisitionsDuringYear(company, start, end).stream()
            .map(asset -> DepreciationScheduleResponse.AssetMovement.builder()
                .assetId(asset.getId())
                .assetNumber(asset.getAssetNumber())
                .assetName(asset.getAssetName())
                .category(asset.getCategory())
                .movementDate(asset.getAcquisitionDate())
                .amount(asset.getTotalCost() != null ? asset.getTotalCost() : asset.getAcquisitionCost())
                .description("Acquisition - " + asset.getSupplierName())
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Récupérer les cessions de l'exercice
     */
    private List<DepreciationScheduleResponse.AssetMovement> getDisposals(
            Company company, LocalDate start, LocalDate end) {

        return fixedAssetRepository.findDisposalsDuringYear(company, start, end).stream()
            .map(asset -> {
                BigDecimal vnc = (asset.getTotalCost() != null ? asset.getTotalCost() : asset.getAcquisitionCost())
                    .subtract(calculateAccumulatedDepreciation(asset, asset.getDisposalDate().getYear()));

                BigDecimal gainLoss = asset.getDisposalAmount() != null
                    ? asset.getDisposalAmount().subtract(vnc)
                    : BigDecimal.ZERO;

                return DepreciationScheduleResponse.AssetMovement.builder()
                    .assetId(asset.getId())
                    .assetNumber(asset.getAssetNumber())
                    .assetName(asset.getAssetName())
                    .category(asset.getCategory())
                    .movementDate(asset.getDisposalDate())
                    .amount(asset.getDisposalAmount())
                    .netBookValue(vnc)
                    .gainLoss(gainLoss)
                    .description("Cession - " + asset.getDisposalReason())
                    .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * Générer l'analyse et les recommandations
     */
    private DepreciationScheduleResponse.DepreciationAnalysis generateAnalysis(
            List<DepreciationScheduleResponse.DepreciationItem> items,
            List<FixedAsset> assets,
            Integer fiscalYear) {

        List<String> alerts = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        // Immobilisations totalement amorties
        List<DepreciationScheduleResponse.DepreciationItem> fullyDepreciated = items.stream()
            .filter(DepreciationScheduleResponse.DepreciationItem::getIsFullyDepreciated)
            .collect(Collectors.toList());

        if (!fullyDepreciated.isEmpty()) {
            alerts.add(String.format("%d immobilisation(s) totalement amortie(s)", fullyDepreciated.size()));
            recommendations.add("Envisager le renouvellement des immobilisations totalement amorties");
        }

        // Immobilisations anciennes (au-delà de la durée de vie)
        List<DepreciationScheduleResponse.DepreciationItem> oldAssets = items.stream()
            .filter(item -> !item.getIsDisposed() &&
                           ChronoUnit.YEARS.between(item.getAcquisitionDate(), LocalDate.now()) > item.getUsefulLifeYears())
            .collect(Collectors.toList());

        if (!oldAssets.isEmpty()) {
            alerts.add(String.format("%d immobilisation(s) dépassent leur durée de vie utile", oldAssets.size()));
        }

        return DepreciationScheduleResponse.DepreciationAnalysis.builder()
            .alerts(alerts)
            .recommendations(recommendations)
            .fullyDepreciatedAssets(fullyDepreciated)
            .oldAssets(oldAssets)
            .build();
    }

    /**
     * Utilitaire pour sommer un champ BigDecimal
     */
    private <T> BigDecimal sumField(List<T> items, java.util.function.Function<T, BigDecimal> extractor) {
        return items.stream()
            .map(extractor)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Obtenir les immobilisations amortissables pour un mois donné
     * Utilisé pour générer les dotations mensuelles
     */
    public List<FixedAsset> getDepreciableAssets(Company company, Integer year, Integer month) {
        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());

        return fixedAssetRepository.findDepreciableForFiscalYear(company, periodStart, periodEnd);
    }

    /**
     * Calculer la dotation aux amortissements pour un mois donné
     */
    public BigDecimal calculateMonthlyDepreciation(FixedAsset asset, Integer year, Integer month) {
        if (!asset.isDepreciable()) {
            return BigDecimal.ZERO;
        }

        // Dotation annuelle / 12
        BigDecimal annualDepreciation = calculateAnnualDepreciation(asset, year);
        return annualDepreciation.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
    }
}
