package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.*;
import com.predykt.accounting.domain.enums.TaxType;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service central de gestion fiscale camerounaise
 * Implémente les 5 taxes essentielles pour le MVP:
 * - TVA 19,25%
 * - Acompte IS (IMF) 2,2%
 * - AIR (Précompte) 2,2% / 5,5%
 * - IRPP Loyer 15%
 * - CNPS ~20%
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaxService {

    private final CompanyRepository companyRepository;
    private final TaxConfigurationRepository taxConfigRepository;
    private final TaxCalculationRepository taxCalculationRepository;
    private final SupplierRepository supplierRepository;
    private final ChartOfAccountsRepository chartRepository;

    /**
     * Initialise les configurations fiscales par défaut pour une entreprise
     */
    @Transactional
    public void initializeDefaultTaxConfigurations(Company company) {
        log.info("🔧 Initialisation des configurations fiscales pour {}", company.getName());

        // Créer les configurations pour toutes les taxes
        for (TaxType taxType : TaxType.values()) {
            if (!taxConfigRepository.existsByCompanyAndTaxType(company, taxType)) {
                TaxConfiguration config = TaxConfiguration.createDefault(company, taxType);

                // Désactiver la TVA si l'entreprise n'est pas assujettie
                if (taxType == TaxType.VAT && !Boolean.TRUE.equals(company.getIsVatRegistered())) {
                    config.setIsActive(false);
                    log.info("⚠️ TVA désactivée (entreprise non assujettie)");
                }

                taxConfigRepository.save(config);
                log.info("✅ Configuration créée: {} - {}%", taxType.getDisplayName(), taxType.getDefaultRate());
            }
        }
    }

    /**
     * Calcule toutes les taxes applicables pour une transaction
     */
    @Transactional
    public List<TaxCalculation> calculateAllTaxesForTransaction(
            Company company,
            BigDecimal amount,
            String transactionType,  // "SALE" ou "PURCHASE"
            Supplier supplier,
            String accountNumber,
            LocalDate transactionDate
    ) {
        log.info("💰 Calcul des taxes pour transaction: {} XAF - Type: {}", amount, transactionType);

        List<TaxCalculation> calculations = new ArrayList<>();

        // Récupérer les configurations fiscales actives
        List<TaxConfiguration> configs;
        if ("SALE".equalsIgnoreCase(transactionType)) {
            configs = taxConfigRepository.findActiveSalesTaxes(company);
        } else {
            configs = taxConfigRepository.findActivePurchaseTaxes(company);
        }

        for (TaxConfiguration config : configs) {
            TaxCalculation calculation = null;

            switch (config.getTaxType()) {
                case VAT -> calculation = calculateVAT(company, amount, config, transactionDate);
                case IS_ADVANCE -> calculation = calculateISAdvance(company, amount, config, transactionDate);
                case AIR_WITH_NIU, AIR_WITHOUT_NIU -> {
                    if (supplier != null) {
                        calculation = calculateAIR(company, amount, supplier, transactionDate);
                    }
                }
                case IRPP_RENT -> {
                    if (supplier != null && supplier.isRentSupplier()) {
                        calculation = calculateIRPPRent(company, amount, supplier, config, transactionDate);
                    }
                }
                case CNPS -> {
                    // CNPS calculé séparément sur les salaires
                    if ("SALARY".equalsIgnoreCase(accountNumber)) {
                        calculation = calculateCNPS(company, amount, config, transactionDate);
                    }
                }
            }

            if (calculation != null) {
                calculations.add(taxCalculationRepository.save(calculation));
            }
        }

        log.info("✅ {} taxes calculées", calculations.size());
        return calculations;
    }

    /**
     * Calcule la TVA (19,25%)
     */
    private TaxCalculation calculateVAT(Company company, BigDecimal amountHT, TaxConfiguration config, LocalDate date) {
        BigDecimal taxAmount = config.calculateTaxAmount(amountHT);

        return TaxCalculation.builder()
            .company(company)
            .taxType(TaxType.VAT)
            .calculationDate(date)
            .baseAmount(amountHT)
            .taxRate(config.getTaxRate())
            .taxAmount(taxAmount)
            .accountNumber(config.getAccountNumber())
            .status("CALCULATED")
            .notes("TVA calculée automatiquement")
            .build();
    }

    /**
     * Calcule l'Acompte IS (IMF) - 2,2% sur le CA
     */
    private TaxCalculation calculateISAdvance(Company company, BigDecimal salesAmount, TaxConfiguration config, LocalDate date) {
        BigDecimal taxAmount = config.calculateTaxAmount(salesAmount);

        return TaxCalculation.builder()
            .company(company)
            .taxType(TaxType.IS_ADVANCE)
            .calculationDate(date)
            .baseAmount(salesAmount)
            .taxRate(config.getTaxRate())
            .taxAmount(taxAmount)
            .accountNumber(config.getAccountNumber())
            .status("CALCULATED")
            .notes("Acompte IS (IMF) mensuel - Échéance: 15 du mois suivant")
            .build();
    }

    /**
     * Calcule l'AIR (Précompte) - 2,2% avec NIU ou 5,5% sans NIU
     */
    private TaxCalculation calculateAIR(Company company, BigDecimal purchaseAmount, Supplier supplier, LocalDate date) {
        boolean hasNiu = supplier.hasValidNiu();
        TaxType airType = TaxType.getAIRType(hasNiu);
        BigDecimal taxRate = supplier.getApplicableAirRate();

        BigDecimal taxAmount = purchaseAmount
            .multiply(taxRate)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        TaxCalculation calculation = TaxCalculation.builder()
            .company(company)
            .supplier(supplier)
            .taxType(airType)
            .calculationDate(date)
            .baseAmount(purchaseAmount)
            .taxRate(taxRate)
            .taxAmount(taxAmount)
            .accountNumber("4478")
            .status("CALCULATED")
            .build();

        // Ajouter alerte si NIU manquant
        if (!hasNiu) {
            calculation.addAlert(
                String.format("⚠️ ALERTE: Fournisseur '%s' sans NIU - Taux majoré à 5,5%% (surcoût: %s XAF). " +
                              "Veuillez régulariser le dossier fournisseur.",
                    supplier.getName(),
                    calculation.calculatePenaltyCost())
            );
            log.warn("⚠️ AIR sans NIU pour fournisseur: {} - Pénalité: {} XAF",
                supplier.getName(), calculation.calculatePenaltyCost());
        }

        return calculation;
    }

    /**
     * Calcule l'IRPP Loyer - 15% sur les loyers
     */
    private TaxCalculation calculateIRPPRent(Company company, BigDecimal rentAmount, Supplier supplier, TaxConfiguration config, LocalDate date) {
        BigDecimal taxAmount = config.calculateTaxAmount(rentAmount);
        BigDecimal amountToLandlord = rentAmount.subtract(taxAmount);  // 85% au bailleur

        return TaxCalculation.builder()
            .company(company)
            .supplier(supplier)
            .taxType(TaxType.IRPP_RENT)
            .calculationDate(date)
            .baseAmount(rentAmount)
            .taxRate(config.getTaxRate())
            .taxAmount(taxAmount)
            .accountNumber(config.getAccountNumber())
            .status("CALCULATED")
            .notes(String.format("IRPP Loyer - 85%% au bailleur (%s XAF), 15%% à l'État (%s XAF)",
                amountToLandlord, taxAmount))
            .build();
    }

    /**
     * Calcule la CNPS (~20% sur les salaires)
     */
    private TaxCalculation calculateCNPS(Company company, BigDecimal salaryAmount, TaxConfiguration config, LocalDate date) {
        BigDecimal taxAmount = config.calculateTaxAmount(salaryAmount);

        return TaxCalculation.builder()
            .company(company)
            .taxType(TaxType.CNPS)
            .calculationDate(date)
            .baseAmount(salaryAmount)
            .taxRate(config.getTaxRate())
            .taxAmount(taxAmount)
            .accountNumber(config.getAccountNumber())
            .status("CALCULATED")
            .notes("CNPS - Estimation pour provision (taux indicatif 20%)")
            .build();
    }

    /**
     * Récupère le résumé fiscal mensuel par type de taxe
     */
    @Transactional(readOnly = true)
    public Map<TaxType, BigDecimal> getMonthlySummary(Long companyId, int year, int month) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        log.info("📊 Résumé fiscal mensuel: {}/{} pour {}", month, year, company.getName());

        Map<TaxType, BigDecimal> summary = new HashMap<>();

        for (TaxType taxType : TaxType.values()) {
            BigDecimal total = taxCalculationRepository.sumTaxAmountByTypeAndPeriod(
                company, taxType, startDate, endDate
            );
            if (total != null && total.compareTo(BigDecimal.ZERO) > 0) {
                summary.put(taxType, total);
            }
        }

        return summary;
    }

    /**
     * Récupère toutes les alertes fiscales actives
     */
    @Transactional(readOnly = true)
    public List<TaxCalculation> getActiveAlerts(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        return taxCalculationRepository.findCalculationsWithAlerts(company);
    }

    /**
     * Compte le nombre de fournisseurs sans NIU (alertes potentielles)
     */
    @Transactional(readOnly = true)
    public Long countSuppliersWithoutNiu(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        return supplierRepository.countSuppliersWithoutNiu(company);
    }

    /**
     * Active ou désactive une taxe
     */
    @Transactional
    public void toggleTax(Long companyId, TaxType taxType, boolean active) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        TaxConfiguration config = taxConfigRepository.findByCompanyAndTaxType(company, taxType)
            .orElseThrow(() -> new ResourceNotFoundException("Configuration fiscale non trouvée"));

        config.setIsActive(active);
        taxConfigRepository.save(config);

        log.info("🔄 Taxe {} {} pour {}", taxType.getDisplayName(),
            active ? "activée" : "désactivée", company.getName());
    }

    /**
     * Met à jour le taux d'une taxe
     */
    @Transactional
    public void updateTaxRate(Long companyId, TaxType taxType, BigDecimal newRate) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        TaxConfiguration config = taxConfigRepository.findByCompanyAndTaxType(company, taxType)
            .orElseThrow(() -> new ResourceNotFoundException("Configuration fiscale non trouvée"));

        BigDecimal oldRate = config.getTaxRate();
        config.setTaxRate(newRate);
        taxConfigRepository.save(config);

        log.info("📝 Taux {} modifié: {}% → {}% pour {}",
            taxType.getDisplayName(), oldRate, newRate, company.getName());
    }

    /**
     * Récupère l'historique des calculs pour une période
     */
    @Transactional(readOnly = true)
    public List<TaxCalculation> getCalculationHistory(Long companyId, LocalDate startDate, LocalDate endDate) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        return taxCalculationRepository.findByCompanyAndPeriod(company, startDate, endDate);
    }

    /**
     * Récupère toutes les configurations fiscales d'une entreprise
     */
    @Transactional(readOnly = true)
    public List<TaxConfiguration> getTaxConfigurations(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        return taxConfigRepository.findByCompany(company);
    }
}
