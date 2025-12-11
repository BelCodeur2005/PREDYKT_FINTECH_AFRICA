package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.*;
import com.predykt.accounting.domain.enums.BillStatus;
import com.predykt.accounting.domain.enums.BillType;
import com.predykt.accounting.dto.request.BillCreateRequest;
import com.predykt.accounting.dto.request.BillLineRequest;
import com.predykt.accounting.dto.request.BillUpdateRequest;
import com.predykt.accounting.dto.response.BillLineResponse;
import com.predykt.accounting.dto.response.BillResponse;
import com.predykt.accounting.exception.ResourceNotFoundException;
import com.predykt.accounting.exception.ValidationException;
import com.predykt.accounting.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de gestion des factures fournisseurs (Bills)
 * Conforme OHADA + Fiscalité Cameroun (AIR, IRPP Loyer)
 *
 * Fonctionnalités:
 * - CRUD complet des factures fournisseurs
 * - Génération automatique numéro facture (FA-YYYY-NNNN)
 * - Calcul automatique AIR 2.2% (NIU) ou 5.5% (pas de NIU)
 * - Calcul automatique IRPP Loyer 15% pour les loueurs
 * - Génération écritures comptables lors de la validation
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BillService {

    private final BillRepository billRepository;
    private final BillLineRepository billLineRepository;
    private final SupplierRepository supplierRepository;
    private final CompanyRepository companyRepository;
    private final ChartOfAccountsRepository chartOfAccountsRepository;
    private final GeneralLedgerRepository generalLedgerRepository;
    private final TaxCalculationRepository taxCalculationRepository;
    private final TaxService taxService;
    private final JdbcTemplate jdbcTemplate;

    // Constantes OHADA
    private static final String BILL_PREFIX = "FA";
    private static final String VAT_DEDUCTIBLE_ACCOUNT = "4452";  // TVA déductible
    private static final String PURCHASE_ACCOUNT_DEFAULT = "601";  // Achats de marchandises

    /**
     * Créer une nouvelle facture fournisseur (statut DRAFT)
     */
    public BillResponse createBill(Long companyId, BillCreateRequest request) {
        log.info("🆕 Création facture fournisseur pour entreprise {} - Fournisseur {}",
            companyId, request.getSupplierId());

        // 1. Valider entreprise
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée: " + companyId));

        // 2. Valider fournisseur
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
            .orElseThrow(() -> new ResourceNotFoundException("Fournisseur non trouvé: " + request.getSupplierId()));

        if (!supplier.getCompany().getId().equals(companyId)) {
            throw new ValidationException("Le fournisseur n'appartient pas à cette entreprise");
        }

        if (!supplier.getIsActive()) {
            throw new ValidationException("Le fournisseur est inactif");
        }

        // 3. Générer numéro de facture
        String billNumber = generateBillNumber(company);

        // 4. Créer la facture
        Bill bill = Bill.builder()
            .company(company)
            .supplier(supplier)
            .billNumber(billNumber)
            .supplierInvoiceNumber(request.getSupplierInvoiceNumber())
            .billType(request.getBillType() != null ? request.getBillType() : BillType.PURCHASE)
            .issueDate(request.getIssueDate())
            .dueDate(request.getDueDate())
            .referenceNumber(request.getReferenceNumber())
            .description(request.getDescription())
            .notes(request.getNotes())
            .status(BillStatus.DRAFT)
            .supplierNiu(supplier.getNiuNumber())
            .supplierHasNiu(supplier.getHasNiu())
            .build();

        // 5. Ajouter les lignes de facture
        int lineNumber = 1;
        for (BillLineRequest lineReq : request.getLines()) {
            BillLine line = createBillLine(lineReq, lineNumber++, company);
            bill.addLine(line);
        }

        // 6. Calculer les totaux + AIR + IRPP
        calculateBillAmounts(bill);

        // 7. Sauvegarder
        bill = billRepository.save(bill);

        log.info("✅ Facture fournisseur créée: {} - Montant TTC: {} XAF (AIR: {} XAF)",
            bill.getBillNumber(), bill.getTotalTtc(), bill.getAirAmount());

        return toResponse(bill);
    }

    /**
     * Obtenir une facture par ID
     */
    @Transactional(readOnly = true)
    public BillResponse getBill(Long companyId, Long billId) {
        Bill bill = findBillByIdAndCompany(companyId, billId);
        return toResponse(bill);
    }

    /**
     * Lister toutes les factures fournisseurs
     */
    @Transactional(readOnly = true)
    public List<BillResponse> getAllBills(Long companyId, BillStatus status) {
        Company company = findCompanyOrThrow(companyId);

        List<Bill> bills;
        if (status != null) {
            bills = billRepository.findByCompanyAndStatusOrderByIssueDateDesc(company, status);
        } else {
            bills = billRepository.findByCompanyOrderByIssueDateDesc(company);
        }

        return bills.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Lister les factures d'un fournisseur
     */
    @Transactional(readOnly = true)
    public List<BillResponse> getBillsBySupplier(Long companyId, Long supplierId) {
        Company company = findCompanyOrThrow(companyId);
        Supplier supplier = supplierRepository.findById(supplierId)
            .orElseThrow(() -> new ResourceNotFoundException("Fournisseur non trouvé: " + supplierId));

        List<Bill> bills = billRepository.findByCompanyAndSupplierOrderByIssueDateDesc(company, supplier);
        return bills.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Obtenir les factures en retard
     */
    @Transactional(readOnly = true)
    public List<BillResponse> getOverdueBills(Long companyId) {
        Company company = findCompanyOrThrow(companyId);
        List<Bill> bills = billRepository.findOverdueBills(company, LocalDate.now());
        return bills.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Mettre à jour une facture (uniquement en mode DRAFT)
     */
    public BillResponse updateBill(Long companyId, Long billId, BillUpdateRequest request) {
        log.info("📝 Mise à jour facture fournisseur {} pour entreprise {}", billId, companyId);

        Bill bill = findBillByIdAndCompany(companyId, billId);

        // Vérifier que la facture est modifiable
        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new ValidationException("Seules les factures en statut DRAFT peuvent être modifiées");
        }

        // Mettre à jour les champs
        if (request.getSupplierInvoiceNumber() != null) bill.setSupplierInvoiceNumber(request.getSupplierInvoiceNumber());
        if (request.getIssueDate() != null) bill.setIssueDate(request.getIssueDate());
        if (request.getDueDate() != null) bill.setDueDate(request.getDueDate());
        if (request.getReferenceNumber() != null) bill.setReferenceNumber(request.getReferenceNumber());
        if (request.getDescription() != null) bill.setDescription(request.getDescription());
        if (request.getNotes() != null) bill.setNotes(request.getNotes());

        // Mettre à jour les lignes si fournies
        if (request.getLines() != null) {
            bill.getLines().clear();
            billLineRepository.deleteByBill(bill);

            int lineNumber = 1;
            for (BillLineRequest lineReq : request.getLines()) {
                BillLine line = createBillLine(lineReq, lineNumber++, bill.getCompany());
                bill.addLine(line);
            }

            calculateBillAmounts(bill);
        }

        bill = billRepository.save(bill);
        log.info("✅ Facture fournisseur {} mise à jour", bill.getBillNumber());

        return toResponse(bill);
    }

    /**
     * VALIDER une facture fournisseur → Génère l'écriture comptable
     */
    public BillResponse validateBill(Long companyId, Long billId) {
        log.info("✅ VALIDATION facture fournisseur {} pour entreprise {}", billId, companyId);

        Bill bill = findBillByIdAndCompany(companyId, billId);

        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new ValidationException("Seules les factures en statut DRAFT peuvent être validées");
        }

        if (bill.getLines().isEmpty()) {
            throw new ValidationException("Impossible de valider une facture sans lignes");
        }

        // Changer le statut
        bill.setStatus(BillStatus.ISSUED);

        // 🔥 GÉNÉRER L'ÉCRITURE COMPTABLE
        GeneralLedger entry = generateAccountingEntry(bill);
        bill.setGeneralLedger(entry);

        bill = billRepository.save(bill);

        log.info("✅ Facture fournisseur {} validée - Écriture {} générée",
            bill.getBillNumber(), entry.getId());

        return toResponse(bill);
    }

    /**
     * Annuler une facture fournisseur
     */
    public BillResponse cancelBill(Long companyId, Long billId) {
        log.info("❌ Annulation facture fournisseur {} pour entreprise {}", billId, companyId);

        Bill bill = findBillByIdAndCompany(companyId, billId);

        if (bill.getStatus() == BillStatus.PAID) {
            throw new ValidationException("Impossible d'annuler une facture déjà payée");
        }

        if (bill.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            throw new ValidationException("Impossible d'annuler une facture partiellement payée");
        }

        bill.setStatus(BillStatus.CANCELLED);
        bill = billRepository.save(bill);

        log.info("✅ Facture fournisseur {} annulée", bill.getBillNumber());
        return toResponse(bill);
    }

    /**
     * Supprimer une facture (uniquement DRAFT)
     */
    public void deleteBill(Long companyId, Long billId) {
        log.warn("🗑️ Suppression facture fournisseur {} pour entreprise {}", billId, companyId);

        Bill bill = findBillByIdAndCompany(companyId, billId);

        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new ValidationException("Seules les factures en statut DRAFT peuvent être supprimées");
        }

        billRepository.delete(bill);
        log.info("✅ Facture fournisseur {} supprimée", bill.getBillNumber());
    }

    // ==================== Méthodes privées ====================

    /**
     * Calculer tous les montants de la facture + AIR + IRPP Loyer
     * Utilise TaxService pour calculs conformes et configurables
     */
    private void calculateBillAmounts(Bill bill) {
        // 1. Calculer totaux HT et TVA à partir des lignes
        BigDecimal totalHt = bill.getLines().stream()
            .map(BillLine::getTotalHt)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal vatDeductible = bill.getLines().stream()
            .map(BillLine::getVatAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        bill.setTotalHt(totalHt);
        bill.setVatDeductible(vatDeductible);

        // 2. Calculer AIR et IRPP via TaxService (conforme + alertes automatiques)
        try {
            List<com.predykt.accounting.domain.entity.TaxCalculation> taxCalculations =
                taxService.calculateAllTaxesForTransaction(
                    bill.getCompany(),
                    totalHt,
                    "PURCHASE",
                    bill.getSupplier(),
                    bill.getLines().isEmpty() ? null : bill.getLines().get(0).getAccountNumber(),
                    bill.getIssueDate()
                );

            // Associer les TaxCalculation à la Bill et sauvegarder (traçabilité)
            taxCalculations.forEach(taxCalc -> {
                taxCalc.setBill(bill);
                taxCalculationRepository.save(taxCalc);
                log.debug("💾 TaxCalculation sauvegardée: {} - {} XAF", taxCalc.getTaxType(), taxCalc.getTaxAmount());
            });

            // Extraire AIR (2.2% avec NIU ou 5.5% sans NIU)
            taxCalculations.stream()
                .filter(tax -> tax.getTaxType().name().startsWith("AIR"))
                .findFirst()
                .ifPresent(airCalc -> {
                    bill.setAirAmount(airCalc.getTaxAmount());
                    bill.setAirRate(airCalc.getTaxRate());

                    // ✅ Alertes automatiques si fournisseur sans NIU
                    if (airCalc.getHasAlert() != null && airCalc.getHasAlert()) {
                        log.warn("⚠️ {}", airCalc.getAlertMessage());
                    }
                });

            // Extraire IRPP Loyer (15% si loueur)
            taxCalculations.stream()
                .filter(tax -> tax.getTaxType() == com.predykt.accounting.domain.enums.TaxType.IRPP_RENT)
                .findFirst()
                .ifPresent(irppCalc -> {
                    bill.setIrppRentAmount(irppCalc.getTaxAmount());
                    log.info("📝 IRPP Loyer appliqué: {} XAF (15%)", irppCalc.getTaxAmount());
                });

            log.info("✅ {} TaxCalculation(s) créées et associées à la facture {}",
                taxCalculations.size(), bill.getBillNumber());

        } catch (Exception e) {
            // Fallback sur calcul manuel en cas d'erreur
            log.error("Erreur calcul taxes via TaxService, fallback sur calcul manuel", e);
            calculateTaxesFallback(bill, totalHt);
        }

        // 3. Calculer TTC
        BigDecimal totalTtc = totalHt.add(vatDeductible);
        bill.setTotalTtc(totalTtc);

        // 4. Initialiser amount_due
        bill.setAmountDue(totalTtc.subtract(bill.getAmountPaid()));
    }

    /**
     * Calcul manuel des taxes en fallback (sécurité)
     */
    private void calculateTaxesFallback(Bill bill, BigDecimal totalHt) {
        // AIR : 2.2% avec NIU, 5.5% sans NIU
        BigDecimal airRate = bill.getSupplierHasNiu() ? new BigDecimal("2.2") : new BigDecimal("5.5");
        BigDecimal airAmount = totalHt.multiply(airRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        bill.setAirRate(airRate);
        bill.setAirAmount(airAmount);

        // IRPP Loyer : 15% si loueur
        if (bill.getBillType() == com.predykt.accounting.domain.enums.BillType.RENT ||
            (bill.getSupplier() != null && bill.getSupplier().isRentSupplier())) {
            BigDecimal irppAmount = totalHt.multiply(new BigDecimal("15"))
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            bill.setIrppRentAmount(irppAmount);
        }

        if (!bill.getSupplierHasNiu()) {
            log.warn("⚠️ Fournisseur {} SANS NIU → AIR majoré à 5.5% (au lieu de 2.2%)",
                bill.getSupplier().getName());
        }
    }

    /**
     * Générer l'écriture comptable pour une facture fournisseur validée
     *
     * Exemple: Facture 500 000 XAF HT + TVA 19.25% + AIR 2.2%
     *
     * Journal AC (Achats):
     * DÉBIT  | 601   (Achats marchandises)   | 500 000 | Charge
     * DÉBIT  | 4452  (TVA déductible)        |  96 250 | TVA récupérable
     * DÉBIT  | 4421  (AIR à récupérer)       |  11 000 | Précompte IR
     * CRÉDIT | 4011001 (Fournisseur XYZ)     | 585 250 | Dette fournisseur
     */
    private GeneralLedger generateAccountingEntry(Bill bill) {
        log.info("🔄 Génération écriture comptable pour facture fournisseur {}", bill.getBillNumber());

        Company company = bill.getCompany();
        Supplier supplier = bill.getSupplier();

        // Récupérer le compte auxiliaire du fournisseur
        String supplierAccountNumber = supplier.getAuxiliaryAccountNumber();
        if (supplierAccountNumber == null) {
            throw new ValidationException("Le fournisseur n'a pas de compte auxiliaire");
        }

        ChartOfAccounts supplierAccount = chartOfAccountsRepository.findByCompanyAndAccountNumber(company, supplierAccountNumber)
            .orElseThrow(() -> new ValidationException("Compte fournisseur non trouvé: " + supplierAccountNumber));

        // Ligne 1: DÉBIT Achats (601) = HT
        ChartOfAccounts purchaseAccount = chartOfAccountsRepository.findByCompanyAndAccountNumber(company, PURCHASE_ACCOUNT_DEFAULT)
            .orElseThrow(() -> new ValidationException("Compte achats non trouvé: " + PURCHASE_ACCOUNT_DEFAULT));

        GeneralLedger purchaseEntry = GeneralLedger.builder()
            .company(company)
            .entryDate(bill.getIssueDate())
            .journalCode("AC")  // Journal des achats
            .reference(bill.getBillNumber())
            .account(purchaseAccount)
            .description("Achat - " + supplier.getName() + " - " + bill.getDescription())
            .debitAmount(bill.getTotalHt())
            .creditAmount(BigDecimal.ZERO)
            .supplier(supplier)
            .build();
        GeneralLedger savedEntry = generalLedgerRepository.save(purchaseEntry);

        // Ligne 2: DÉBIT TVA déductible (4452)
        if (bill.getVatDeductible().compareTo(BigDecimal.ZERO) > 0) {
            ChartOfAccounts vatAccount = chartOfAccountsRepository.findByCompanyAndAccountNumber(company, VAT_DEDUCTIBLE_ACCOUNT)
                .orElseThrow(() -> new ValidationException("Compte TVA déductible non trouvé: " + VAT_DEDUCTIBLE_ACCOUNT));

            GeneralLedger vatEntry = GeneralLedger.builder()
                .company(company)
                .entryDate(bill.getIssueDate())
                .journalCode("AC")
                .reference(bill.getBillNumber())
                .account(vatAccount)
                .description("TVA déductible sur facture " + bill.getBillNumber())
                .debitAmount(bill.getVatDeductible())
                .creditAmount(BigDecimal.ZERO)
                .supplier(supplier)
                .build();
            generalLedgerRepository.save(vatEntry);
        }

        // Ligne 3: DÉBIT AIR à récupérer - Récupération compte depuis TaxService
        if (bill.getAirAmount().compareTo(BigDecimal.ZERO) > 0) {
            // Récupérer le compte AIR depuis la configuration fiscale
            String airAccountNumber = getAIRAccountNumber(company, bill.getSupplierHasNiu());

            ChartOfAccounts airAccount = chartOfAccountsRepository.findByCompanyAndAccountNumber(company, airAccountNumber)
                .orElseThrow(() -> new ValidationException("Compte AIR non trouvé: " + airAccountNumber));

            GeneralLedger airEntry = GeneralLedger.builder()
                .company(company)
                .entryDate(bill.getIssueDate())
                .journalCode("AC")
                .reference(bill.getBillNumber())
                .account(airAccount)  // Depuis config
                .description(String.format("AIR %.2f%% - %s", bill.getAirRate(), bill.getBillNumber()))
                .debitAmount(bill.getAirAmount())
                .creditAmount(BigDecimal.ZERO)
                .supplier(supplier)
                .build();
            generalLedgerRepository.save(airEntry);
        }

        // Ligne 4: DÉBIT IRPP Loyer (si applicable) - Récupération compte depuis TaxService
        if (bill.getIrppRentAmount().compareTo(BigDecimal.ZERO) > 0) {
            // Récupérer le compte IRPP depuis la configuration fiscale
            String irppAccountNumber = getIRPPRentAccountNumber(company);

            ChartOfAccounts irppAccount = chartOfAccountsRepository.findByCompanyAndAccountNumber(company, irppAccountNumber)
                .orElseThrow(() -> new ValidationException("Compte IRPP Loyer non trouvé: " + irppAccountNumber));

            GeneralLedger irppEntry = GeneralLedger.builder()
                .company(company)
                .entryDate(bill.getIssueDate())
                .journalCode("AC")
                .reference(bill.getBillNumber())
                .account(irppAccount)  // Depuis config
                .description("IRPP Loyer 15% - " + bill.getBillNumber())
                .debitAmount(bill.getIrppRentAmount())
                .creditAmount(BigDecimal.ZERO)
                .supplier(supplier)
                .build();
            generalLedgerRepository.save(irppEntry);
        }

        // Ligne 5: CRÉDIT Fournisseur (4011001) = TTC
        GeneralLedger supplierEntry = GeneralLedger.builder()
            .company(company)
            .entryDate(bill.getIssueDate())
            .journalCode("AC")
            .reference(bill.getBillNumber())
            .account(supplierAccount)
            .description("Dette fournisseur " + supplier.getName() + " - " + bill.getBillNumber())
            .debitAmount(BigDecimal.ZERO)
            .creditAmount(bill.getTotalTtc())
            .supplier(supplier)
            .build();
        generalLedgerRepository.save(supplierEntry);

        log.info("✅ Écriture comptable générée: DÉBIT {} + TVA + AIR/IRPP / CRÉDIT {} = {} XAF",
            PURCHASE_ACCOUNT_DEFAULT, supplierAccountNumber, bill.getTotalTtc());

        return savedEntry;
    }

    /**
     * Créer une ligne de facture fournisseur
     */
    private BillLine createBillLine(BillLineRequest request, int lineNumber, Company company) {
        // Récupérer le taux de TVA depuis la configuration fiscale si non fourni
        BigDecimal vatRate = request.getVatRate();
        if (vatRate == null) {
            vatRate = getDefaultVATRate(company);
        }

        BillLine line = BillLine.builder()
            .lineNumber(lineNumber)
            .productCode(request.getProductCode())
            .description(request.getDescription())
            .quantity(request.getQuantity())
            .unit(request.getUnit() != null ? request.getUnit() : "Unité")
            .unitPrice(request.getUnitPrice())
            .discountPercentage(request.getDiscountPercentage() != null ? request.getDiscountPercentage() : BigDecimal.ZERO)
            .vatRate(vatRate)
            .accountNumber(request.getAccountNumber())
            .build();

        line.calculateAmounts();
        return line;
    }

    /**
     * Générer le numéro de facture fournisseur (FA-2025-0001)
     */
    private String generateBillNumber(Company company) {
        int year = LocalDate.now().getYear();
        Long sequence = jdbcTemplate.queryForObject(
            "SELECT nextval('seq_bill_number')",
            Long.class
        );
        return String.format("%s-%d-%04d", BILL_PREFIX, year, sequence);
    }

    /**
     * Convertir Bill en BillResponse
     */
    private BillResponse toResponse(Bill bill) {
        List<BillLineResponse> lineResponses = bill.getLines().stream()
            .map(this::toLineResponse)
            .collect(Collectors.toList());

        return BillResponse.builder()
            .id(bill.getId())
            .companyId(bill.getCompany().getId())
            .supplierId(bill.getSupplier().getId())
            .supplierName(bill.getSupplier().getName())
            .supplierNiu(bill.getSupplierNiu())
            .billNumber(bill.getBillNumber())
            .supplierInvoiceNumber(bill.getSupplierInvoiceNumber())
            .billType(bill.getBillType())
            .issueDate(bill.getIssueDate())
            .dueDate(bill.getDueDate())
            .paymentDate(bill.getPaymentDate())
            .totalHt(bill.getTotalHt())
            .vatDeductible(bill.getVatDeductible())
            .airAmount(bill.getAirAmount())
            .irppRentAmount(bill.getIrppRentAmount())
            .totalTtc(bill.getTotalTtc())
            .amountPaid(bill.getAmountPaid())
            .amountDue(bill.getAmountDue())
            .status(bill.getStatus())
            .isReconciled(bill.getIsReconciled())
            .reconciliationDate(bill.getReconciliationDate())
            .referenceNumber(bill.getReferenceNumber())
            .description(bill.getDescription())
            .notes(bill.getNotes())
            .supplierHasNiu(bill.getSupplierHasNiu())
            .airRate(bill.getAirRate())
            .lines(lineResponses)
            .daysOverdue(bill.getDaysOverdue())
            .agingCategory(bill.getAgingCategory())
            .generalLedgerId(bill.getGeneralLedger() != null ? bill.getGeneralLedger().getId() : null)
            .createdAt(bill.getCreatedAt())
            .createdBy(bill.getCreatedBy())
            .updatedAt(bill.getUpdatedAt())
            .updatedBy(bill.getUpdatedBy())
            .build();
    }

    private BillLineResponse toLineResponse(BillLine line) {
        return BillLineResponse.builder()
            .id(line.getId())
            .lineNumber(line.getLineNumber())
            .productCode(line.getProductCode())
            .description(line.getDescription())
            .quantity(line.getQuantity())
            .unit(line.getUnit())
            .unitPrice(line.getUnitPrice())
            .discountPercentage(line.getDiscountPercentage())
            .subtotal(line.getSubtotal())
            .discountAmount(line.getDiscountAmount())
            .totalHt(line.getTotalHt())
            .vatRate(line.getVatRate())
            .vatAmount(line.getVatAmount())
            .totalTtc(line.getTotalTtc())
            .accountNumber(line.getAccountNumber())
            .build();
    }

    private Bill findBillByIdAndCompany(Long companyId, Long billId) {
        Bill bill = billRepository.findById(billId)
            .orElseThrow(() -> new ResourceNotFoundException("Facture fournisseur non trouvée: " + billId));

        if (!bill.getCompany().getId().equals(companyId)) {
            throw new ValidationException("Cette facture n'appartient pas à cette entreprise");
        }

        return bill;
    }

    private Company findCompanyOrThrow(Long companyId) {
        return companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée: " + companyId));
    }

    /**
     * Récupère le numéro de compte AIR depuis la configuration fiscale
     */
    private String getAIRAccountNumber(Company company, boolean hasNiu) {
        com.predykt.accounting.domain.enums.TaxType airType = hasNiu
            ? com.predykt.accounting.domain.enums.TaxType.AIR_WITH_NIU
            : com.predykt.accounting.domain.enums.TaxType.AIR_WITHOUT_NIU;

        return taxService.getTaxConfigurations(company.getId()).stream()
            .filter(config -> config.getTaxType() == airType)
            .map(config -> config.getAccountNumber())
            .findFirst()
            .orElse("4421");  // Fallback sur compte OHADA standard (AIR à récupérer)
    }

    /**
     * Récupère le numéro de compte IRPP Loyer depuis la configuration fiscale
     */
    private String getIRPPRentAccountNumber(Company company) {
        return taxService.getTaxConfigurations(company.getId()).stream()
            .filter(config -> config.getTaxType() == com.predykt.accounting.domain.enums.TaxType.IRPP_RENT)
            .map(config -> config.getAccountNumber())
            .findFirst()
            .orElse("4422");  // Fallback sur compte OHADA standard (IRPP Loyer retenu)
    }

    /**
     * Récupère le taux de TVA par défaut depuis la configuration fiscale
     */
    private BigDecimal getDefaultVATRate(Company company) {
        return taxService.getTaxConfigurations(company.getId()).stream()
            .filter(config -> config.getTaxType() == com.predykt.accounting.domain.enums.TaxType.VAT)
            .filter(config -> config.getIsActive())
            .map(config -> config.getTaxRate())
            .findFirst()
            .orElse(new BigDecimal("19.25"));  // Fallback sur taux Cameroun standard
    }
}
