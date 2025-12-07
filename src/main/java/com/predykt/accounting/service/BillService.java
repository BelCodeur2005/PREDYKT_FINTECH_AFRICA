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
    private final GeneralLedgerRepository generalLedgerRepository;
    private final JdbcTemplate jdbcTemplate;

    // Constantes OHADA + Fiscalité Cameroun
    private static final String BILL_PREFIX = "FA";
    private static final String VAT_DEDUCTIBLE_ACCOUNT = "4452";  // TVA déductible
    private static final String AIR_ACCOUNT = "4421";  // AIR à récupérer
    private static final String IRPP_RENT_ACCOUNT = "4422";  // IRPP Loyer retenu
    private static final String PURCHASE_ACCOUNT_DEFAULT = "601";  // Achats de marchandises
    private static final BigDecimal AIR_RATE_WITH_NIU = new BigDecimal("2.2");
    private static final BigDecimal AIR_RATE_WITHOUT_NIU = new BigDecimal("5.5");
    private static final BigDecimal IRPP_RENT_RATE = new BigDecimal("15.0");

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
            BillLine line = createBillLine(lineReq, lineNumber++);
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
                BillLine line = createBillLine(lineReq, lineNumber++);
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

        // 2. Calculer AIR (Acompte sur Impôt sur le Revenu)
        BigDecimal airRate = bill.getSupplierHasNiu() ? AIR_RATE_WITH_NIU : AIR_RATE_WITHOUT_NIU;
        BigDecimal airAmount = totalHt.multiply(airRate)
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        bill.setAirRate(airRate);
        bill.setAirAmount(airAmount);

        // 3. Calculer IRPP Loyer 15% (si le fournisseur est un loueur)
        BigDecimal irppRentAmount = BigDecimal.ZERO;
        if (bill.getBillType() == BillType.RENT ||
            (bill.getSupplier() != null && bill.getSupplier().isRentSupplier())) {
            irppRentAmount = totalHt.multiply(IRPP_RENT_RATE)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        bill.setIrppRentAmount(irppRentAmount);

        // 4. Calculer TTC
        BigDecimal totalTtc = totalHt.add(vatDeductible);
        bill.setTotalTtc(totalTtc);

        // 5. Initialiser amount_due
        bill.setAmountDue(totalTtc.subtract(bill.getAmountPaid()));

        // Log pour info
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
        String supplierAccount = supplier.getAuxiliaryAccountNumber();
        if (supplierAccount == null) {
            throw new ValidationException("Le fournisseur n'a pas de compte auxiliaire");
        }

        // Ligne 1: DÉBIT Achats (601) = HT
        GeneralLedger purchaseEntry = GeneralLedger.builder()
            .company(company)
            .entryDate(bill.getIssueDate())
            .journalCode("AC")  // Journal des achats
            .pieceNumber(bill.getBillNumber())
            .accountNumber(PURCHASE_ACCOUNT_DEFAULT)
            .description("Achat - " + supplier.getName() + " - " + bill.getDescription())
            .debitAmount(bill.getTotalHt())
            .creditAmount(BigDecimal.ZERO)
            .supplier(supplier)
            .build();
        GeneralLedger savedEntry = generalLedgerRepository.save(purchaseEntry);

        // Ligne 2: DÉBIT TVA déductible (4452)
        if (bill.getVatDeductible().compareTo(BigDecimal.ZERO) > 0) {
            GeneralLedger vatEntry = GeneralLedger.builder()
                .company(company)
                .entryDate(bill.getIssueDate())
                .journalCode("AC")
                .pieceNumber(bill.getBillNumber())
                .accountNumber(VAT_DEDUCTIBLE_ACCOUNT)
                .description("TVA déductible sur facture " + bill.getBillNumber())
                .debitAmount(bill.getVatDeductible())
                .creditAmount(BigDecimal.ZERO)
                .supplier(supplier)
                .build();
            generalLedgerRepository.save(vatEntry);
        }

        // Ligne 3: DÉBIT AIR à récupérer (4421)
        if (bill.getAirAmount().compareTo(BigDecimal.ZERO) > 0) {
            GeneralLedger airEntry = GeneralLedger.builder()
                .company(company)
                .entryDate(bill.getIssueDate())
                .journalCode("AC")
                .pieceNumber(bill.getBillNumber())
                .accountNumber(AIR_ACCOUNT)
                .description("AIR " + bill.getAirRate() + "% - " + bill.getBillNumber())
                .debitAmount(bill.getAirAmount())
                .creditAmount(BigDecimal.ZERO)
                .supplier(supplier)
                .build();
            generalLedgerRepository.save(airEntry);
        }

        // Ligne 4: DÉBIT IRPP Loyer (si applicable)
        if (bill.getIrppRentAmount().compareTo(BigDecimal.ZERO) > 0) {
            GeneralLedger irppEntry = GeneralLedger.builder()
                .company(company)
                .entryDate(bill.getIssueDate())
                .journalCode("AC")
                .pieceNumber(bill.getBillNumber())
                .accountNumber(IRPP_RENT_ACCOUNT)
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
            .pieceNumber(bill.getBillNumber())
            .accountNumber(supplierAccount)
            .description("Dette fournisseur " + supplier.getName() + " - " + bill.getBillNumber())
            .debitAmount(BigDecimal.ZERO)
            .creditAmount(bill.getTotalTtc())
            .supplier(supplier)
            .build();
        generalLedgerRepository.save(supplierEntry);

        log.info("✅ Écriture comptable générée: DÉBIT {} + {} + {} / CRÉDIT {} = {} XAF",
            PURCHASE_ACCOUNT_DEFAULT, VAT_DEDUCTIBLE_ACCOUNT, AIR_ACCOUNT,
            supplierAccount, bill.getTotalTtc());

        return savedEntry;
    }

    /**
     * Créer une ligne de facture fournisseur
     */
    private BillLine createBillLine(BillLineRequest request, int lineNumber) {
        BillLine line = BillLine.builder()
            .lineNumber(lineNumber)
            .productCode(request.getProductCode())
            .description(request.getDescription())
            .quantity(request.getQuantity())
            .unit(request.getUnit() != null ? request.getUnit() : "Unité")
            .unitPrice(request.getUnitPrice())
            .discountPercentage(request.getDiscountPercentage() != null ? request.getDiscountPercentage() : BigDecimal.ZERO)
            .vatRate(request.getVatRate() != null ? request.getVatRate() : new BigDecimal("19.25"))
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
}
