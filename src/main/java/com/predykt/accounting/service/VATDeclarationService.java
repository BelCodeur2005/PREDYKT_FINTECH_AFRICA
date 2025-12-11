package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.*;
import com.predykt.accounting.domain.enums.VATAccountType;
import com.predykt.accounting.domain.enums.VATDeclarationType;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.exception.ValidationException;
import com.predykt.accounting.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Service de déclaration de TVA (CA3 mensuel / CA12 annuel)
 * Génère automatiquement les déclarations à partir du grand livre
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VATDeclarationService {

    private final CompanyRepository companyRepository;
    private final VATDeclarationRepository vatDeclarationRepository;
    private final GeneralLedgerRepository generalLedgerRepository;
    private final VATTransactionRepository vatTransactionRepository;
    private final TaxCalculationRepository taxCalculationRepository;

    /**
     * Génère une déclaration de TVA mensuelle (CA3)
     */
    @Transactional
    public VATDeclaration generateMonthlyDeclaration(Long companyId, int year, int month) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        // Vérifier que l'entreprise est assujettie à la TVA
        if (!Boolean.TRUE.equals(company.getIsVatRegistered())) {
            throw new ValidationException("L'entreprise n'est pas assujettie à la TVA");
        }

        YearMonth period = YearMonth.of(year, month);
        String fiscalPeriod = String.format("%04d-%02d", year, month);

        // Vérifier si une déclaration existe déjà
        Optional<VATDeclaration> existing = vatDeclarationRepository
            .findByCompanyAndFiscalPeriodAndDeclarationType(company, fiscalPeriod, VATDeclarationType.CA3_MONTHLY);

        if (existing.isPresent()) {
            log.warn("Une déclaration existe déjà pour la période {}", fiscalPeriod);
            return existing.get();
        }

        LocalDate startDate = period.atDay(1);
        LocalDate endDate = period.atEndOfMonth();

        log.info("📋 Génération déclaration TVA CA3 pour {} - Période: {}", company.getName(), fiscalPeriod);

        // Créer la déclaration
        VATDeclaration declaration = VATDeclaration.builder()
            .company(company)
            .declarationType(VATDeclarationType.CA3_MONTHLY)
            .fiscalPeriod(fiscalPeriod)
            .startDate(startDate)
            .endDate(endDate)
            .build();

        // Calculer TVA collectée (comptes 443x)
        declaration.setVatCollectedSales(calculateVATByAccount(company, startDate, endDate, "4431"));
        declaration.setVatCollectedServices(calculateVATByAccount(company, startDate, endDate, "4432"));
        declaration.setVatCollectedWorks(calculateVATByAccount(company, startDate, endDate, "4433"));

        // Calculer TVA déductible (comptes 445x)
        declaration.setVatDeductibleFixedAssets(calculateVATByAccount(company, startDate, endDate, "4451"));
        declaration.setVatDeductiblePurchases(calculateVATByAccount(company, startDate, endDate, "4452"));
        declaration.setVatDeductibleTransport(calculateVATByAccount(company, startDate, endDate, "4453"));
        declaration.setVatDeductibleServices(calculateVATByAccount(company, startDate, endDate, "4454"));

        // Récupérer le crédit de TVA du mois précédent
        YearMonth previousMonth = period.minusMonths(1);
        String previousPeriod = String.format("%04d-%02d", previousMonth.getYear(), previousMonth.getMonthValue());
        Optional<VATDeclaration> previousDeclaration = vatDeclarationRepository
            .findByCompanyAndFiscalPeriodAndDeclarationType(company, previousPeriod, VATDeclarationType.CA3_MONTHLY);

        if (previousDeclaration.isPresent()) {
            declaration.setPreviousVatCredit(previousDeclaration.get().getVatCreditToCarryForward());
            log.info("📊 Crédit TVA reporté du mois précédent: {} XAF",
                declaration.getPreviousVatCredit());
        }

        // Recalculer tous les totaux
        declaration.recalculateTotals();

        VATDeclaration saved = vatDeclarationRepository.save(declaration);

        log.info("✅ Déclaration TVA générée - TVA collectée: {} XAF - TVA déductible: {} XAF - Solde: {} XAF",
            saved.getTotalVatCollected(),
            saved.getTotalVatDeductible(),
            saved.getVatToPay());

        if (saved.hasVatCredit()) {
            log.info("💰 Crédit de TVA à reporter: {} XAF", saved.getVatCreditToCarryForward());
        }

        return saved;
    }

    /**
     * Calcule le solde d'un compte TVA pour une période
     *
     * ✅ VERSION PHASE 3 OPTIMISÉE (3 niveaux de priorité):
     * 1. TaxCalculation (Phase 2) - Source de vérité principale
     * 2. VATTransaction - Ancien système avec récupérabilité
     * 3. GeneralLedger - Fallback ultime
     */
    private BigDecimal calculateVATByAccount(Company company, LocalDate startDate, LocalDate endDate, String accountNumber) {
        // ========== PRIORITÉ 1: TaxCalculation (Phase 2) ==========
        // Utiliser les calculs de taxes enregistrés lors de la création des factures/bills
        BigDecimal fromTaxCalculations = calculateVATFromTaxCalculations(company, startDate, endDate, accountNumber);
        if (fromTaxCalculations != null && fromTaxCalculations.compareTo(BigDecimal.ZERO) > 0) {
            log.debug("📊 TVA depuis TaxCalculation (Phase 2): {} XAF pour compte {}", fromTaxCalculations, accountNumber);
            return fromTaxCalculations;
        }

        // ========== PRIORITÉ 2: VATTransaction (ancien système) ==========
        // Si pas de TaxCalculation, utiliser VATTransaction (avec récupérabilité)
        BigDecimal fromVatTransactions = calculateVATFromTransactions(company, startDate, endDate, accountNumber);
        if (fromVatTransactions != null && fromVatTransactions.compareTo(BigDecimal.ZERO) > 0) {
            log.debug("📊 TVA depuis VATTransaction: {} XAF pour compte {}", fromVatTransactions, accountNumber);
            return fromVatTransactions;
        }

        // ========== PRIORITÉ 3: GeneralLedger (fallback) ==========
        log.debug("⚠️ Fallback sur GeneralLedger pour compte {}", accountNumber);
        List<GeneralLedger> entries = generalLedgerRepository
            .findByCompanyAndAccountNumberAndEntryDateBetween(company, accountNumber, startDate, endDate);

        BigDecimal total = BigDecimal.ZERO;

        for (GeneralLedger entry : entries) {
            // Pour les comptes de TVA collectée (443x), on prend le crédit
            // Pour les comptes de TVA déductible (445x), on prend le débit
            if (accountNumber.startsWith("443")) {
                // TVA collectée = crédit
                total = total.add(entry.getCreditAmount() != null ? entry.getCreditAmount() : BigDecimal.ZERO);
            } else if (accountNumber.startsWith("445")) {
                // TVA déductible = débit
                total = total.add(entry.getDebitAmount() != null ? entry.getDebitAmount() : BigDecimal.ZERO);
            }
        }

        return total;
    }

    /**
     * ✅ NOUVEAU (Phase 3): Calcule la TVA à partir de la table tax_calculations
     * Source de vérité principale car enregistrée lors de la création des factures/bills
     */
    private BigDecimal calculateVATFromTaxCalculations(Company company, LocalDate startDate, LocalDate endDate, String accountNumber) {
        // Récupérer toutes les TaxCalculations de type VAT pour la période
        List<TaxCalculation> taxCalculations = taxCalculationRepository
            .findByCompanyAndCalculationDateBetween(company, startDate, endDate);

        BigDecimal total = BigDecimal.ZERO;

        for (TaxCalculation taxCalc : taxCalculations) {
            // Filtrer uniquement les calculs de TVA correspondant au compte
            if (taxCalc.getTaxType() == com.predykt.accounting.domain.enums.TaxType.VAT &&
                taxCalc.getAccountNumber() != null &&
                taxCalc.getAccountNumber().equals(accountNumber)) {

                total = total.add(taxCalc.getTaxAmount() != null ? taxCalc.getTaxAmount() : BigDecimal.ZERO);

                log.trace("  → TaxCalculation #{}: {} XAF ({})",
                    taxCalc.getId(), taxCalc.getTaxAmount(),
                    taxCalc.getInvoice() != null ? "Invoice" : "Bill");
            }
        }

        return total;
    }

    /**
     * Calcule la TVA à partir des transactions enregistrées (avec récupérabilité)
     * IMPORTANT : Utilise uniquement la TVA RÉCUPÉRABLE pour les comptes déductibles
     */
    private BigDecimal calculateVATFromTransactions(Company company, LocalDate startDate, LocalDate endDate, String accountNumber) {
        // Mapper le numéro de compte au VATAccountType
        String vatAccountType = accountNumber;

        BigDecimal amount = vatTransactionRepository.sumRecoverableVatByAccountType(
            company, startDate, endDate, vatAccountType
        );

        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            log.debug("✅ TVA récupérable pour compte {} : {} XAF (période {} à {})",
                accountNumber, amount, startDate, endDate);
        }

        return amount != null ? amount : BigDecimal.ZERO;
    }

    /**
     * Récupère une déclaration par ID
     */
    @Transactional(readOnly = true)
    public VATDeclaration getDeclaration(Long declarationId) {
        return vatDeclarationRepository.findById(declarationId)
            .orElseThrow(() -> new ResourceNotFoundException("Déclaration non trouvée"));
    }

    /**
     * Récupère toutes les déclarations d'une entreprise
     */
    @Transactional(readOnly = true)
    public List<VATDeclaration> getAllDeclarations(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        return vatDeclarationRepository.findByCompanyOrderByFiscalPeriodDesc(company);
    }

    /**
     * Récupère les déclarations par statut
     */
    @Transactional(readOnly = true)
    public List<VATDeclaration> getDeclarationsByStatus(Long companyId, String status) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée"));

        return vatDeclarationRepository.findByCompanyAndStatus(company, status);
    }

    /**
     * Valide une déclaration
     */
    @Transactional
    public VATDeclaration validateDeclaration(Long declarationId) {
        VATDeclaration declaration = getDeclaration(declarationId);

        if (!declaration.isEditable()) {
            throw new ValidationException("Cette déclaration n'est pas modifiable");
        }

        declaration.validate();
        VATDeclaration saved = vatDeclarationRepository.save(declaration);

        log.info("✅ Déclaration validée - Période: {} - Montant: {} XAF",
            saved.getFiscalPeriod(), saved.getVatToPay());

        return saved;
    }

    /**
     * Soumet une déclaration à l'administration fiscale
     */
    @Transactional
    public VATDeclaration submitDeclaration(Long declarationId, String referenceNumber) {
        VATDeclaration declaration = getDeclaration(declarationId);
        declaration.markAsSubmitted(referenceNumber);
        VATDeclaration saved = vatDeclarationRepository.save(declaration);

        log.info("📤 Déclaration soumise - Période: {} - Référence: {}",
            saved.getFiscalPeriod(), referenceNumber);

        return saved;
    }

    /**
     * Marque une déclaration comme payée
     */
    @Transactional
    public VATDeclaration markDeclarationAsPaid(Long declarationId) {
        VATDeclaration declaration = getDeclaration(declarationId);
        declaration.markAsPaid();
        VATDeclaration saved = vatDeclarationRepository.save(declaration);

        log.info("💰 Déclaration payée - Période: {} - Montant: {} XAF",
            saved.getFiscalPeriod(), saved.getVatToPay());

        return saved;
    }

    /**
     * Supprime une déclaration (uniquement si DRAFT)
     */
    @Transactional
    public void deleteDeclaration(Long declarationId) {
        VATDeclaration declaration = getDeclaration(declarationId);

        if (!declaration.isEditable()) {
            throw new ValidationException("Seules les déclarations en brouillon peuvent être supprimées");
        }

        vatDeclarationRepository.delete(declaration);
        log.info("🗑️ Déclaration supprimée - Période: {}", declaration.getFiscalPeriod());
    }

    /**
     * Génère un rapport récapitulatif de la déclaration
     */
    @Transactional(readOnly = true)
    public String generateDeclarationReport(Long declarationId) {
        VATDeclaration decl = getDeclaration(declarationId);

        StringBuilder report = new StringBuilder();
        report.append("═══════════════════════════════════════════════════════\n");
        report.append("        DÉCLARATION DE TVA - CA3 MENSUEL\n");
        report.append("═══════════════════════════════════════════════════════\n\n");
        report.append(String.format("Entreprise: %s\n", decl.getCompany().getName()));
        report.append(String.format("Période fiscale: %s\n", decl.getFiscalPeriod()));
        report.append(String.format("Du %s au %s\n", decl.getStartDate(), decl.getEndDate()));
        report.append(String.format("Statut: %s\n\n", decl.getStatus()));

        report.append("═══════════════════════════════════════════════════════\n");
        report.append("SECTION 1: TVA COLLECTÉE\n");
        report.append("═══════════════════════════════════════════════════════\n");
        report.append(String.format("TVA sur ventes (4431)       : %,15.2f XAF\n", decl.getVatCollectedSales()));
        report.append(String.format("TVA sur services (4432)     : %,15.2f XAF\n", decl.getVatCollectedServices()));
        report.append(String.format("TVA sur travaux (4433)      : %,15.2f XAF\n", decl.getVatCollectedWorks()));
        report.append("───────────────────────────────────────────────────────\n");
        report.append(String.format("TOTAL TVA COLLECTÉE         : %,15.2f XAF\n\n", decl.getTotalVatCollected()));

        report.append("═══════════════════════════════════════════════════════\n");
        report.append("SECTION 2: TVA DÉDUCTIBLE\n");
        report.append("═══════════════════════════════════════════════════════\n");
        report.append(String.format("TVA immobilisations (4451)  : %,15.2f XAF\n", decl.getVatDeductibleFixedAssets()));
        report.append(String.format("TVA achats (4452)           : %,15.2f XAF\n", decl.getVatDeductiblePurchases()));
        report.append(String.format("TVA transport (4453)        : %,15.2f XAF\n", decl.getVatDeductibleTransport()));
        report.append(String.format("TVA services (4454)         : %,15.2f XAF\n", decl.getVatDeductibleServices()));
        report.append("───────────────────────────────────────────────────────\n");
        report.append(String.format("TOTAL TVA DÉDUCTIBLE        : %,15.2f XAF\n\n", decl.getTotalVatDeductible()));

        report.append("═══════════════════════════════════════════════════════\n");
        report.append("SECTION 3: SOLDE DE TVA\n");
        report.append("═══════════════════════════════════════════════════════\n");
        report.append(String.format("Crédit mois précédent       : %,15.2f XAF\n", decl.getPreviousVatCredit()));

        BigDecimal netVat = decl.getTotalVatCollected().subtract(decl.getTotalVatDeductible());
        report.append(String.format("TVA nette (collectée - déd) : %,15.2f XAF\n", netVat));
        report.append("───────────────────────────────────────────────────────\n");

        if (decl.hasVatCredit()) {
            report.append(String.format("CRÉDIT DE TVA À REPORTER    : %,15.2f XAF\n", decl.getVatCreditToCarryForward()));
        } else {
            report.append(String.format("TVA À PAYER                 : %,15.2f XAF\n", decl.getVatToPay()));
        }

        report.append("═══════════════════════════════════════════════════════\n");

        if (decl.getSubmissionDate() != null) {
            report.append(String.format("\nDate de soumission: %s\n", decl.getSubmissionDate()));
            report.append(String.format("Référence: %s\n", decl.getReferenceNumber()));
        }

        if (decl.getPaymentDate() != null) {
            report.append(String.format("Date de paiement: %s\n", decl.getPaymentDate()));
        }

        report.append("\nDate d'édition: ").append(LocalDate.now()).append("\n");
        report.append("═══════════════════════════════════════════════════════\n");

        return report.toString();
    }
}
