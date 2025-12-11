package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.*;
import com.predykt.accounting.domain.enums.InvoiceStatus;
import com.predykt.accounting.domain.enums.InvoiceType;
import com.predykt.accounting.dto.request.InvoiceCreateRequest;
import com.predykt.accounting.dto.request.InvoiceLineRequest;
import com.predykt.accounting.dto.request.InvoiceUpdateRequest;
import com.predykt.accounting.dto.response.InvoiceLineResponse;
import com.predykt.accounting.dto.response.InvoicePaymentSummaryResponse;
import com.predykt.accounting.dto.response.InvoiceResponse;
import com.predykt.accounting.dto.response.PaymentResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de gestion des factures clients (Invoices)
 * Conforme OHADA avec génération automatique des écritures comptables
 *
 * Fonctionnalités:
 * - CRUD complet des factures
 * - Génération automatique numéro facture (FV-YYYY-NNNN)
 * - Calcul automatique montants HT/TVA/TTC
 * - Génération écritures comptables lors de la validation
 * - Gestion statuts et transitions
 * - Balance âgée automatique
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;
    private final ChartOfAccountsRepository chartOfAccountsRepository;
    private final GeneralLedgerRepository generalLedgerRepository;
    private final PaymentRepository paymentRepository;
    private final TaxCalculationRepository taxCalculationRepository;
    private final TaxService taxService;
    private final JdbcTemplate jdbcTemplate;

    // Constantes
    private static final String INVOICE_PREFIX = "FV";
    private static final String SALES_ACCOUNT_DEFAULT = "701";   // Ventes de marchandises

    /**
     * Créer une nouvelle facture (statut DRAFT)
     */
    public InvoiceResponse createInvoice(Long companyId, InvoiceCreateRequest request) {
        log.info("🆕 Création facture pour entreprise {} - Client {}", companyId, request.getCustomerId());

        // 1. Valider entreprise
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée: " + companyId));

        // 2. Valider client
        Customer customer = customerRepository.findById(request.getCustomerId())
            .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé: " + request.getCustomerId()));

        if (!customer.getCompany().getId().equals(companyId)) {
            throw new ValidationException("Le client n'appartient pas à cette entreprise");
        }

        if (!customer.getIsActive()) {
            throw new ValidationException("Le client est inactif");
        }

        // 3. Générer numéro de facture
        String invoiceNumber = generateInvoiceNumber(company);

        // 4. Créer la facture
        Invoice invoice = Invoice.builder()
            .company(company)
            .customer(customer)
            .invoiceNumber(invoiceNumber)
            .invoiceType(request.getInvoiceType() != null ? request.getInvoiceType() : InvoiceType.STANDARD)
            .issueDate(request.getIssueDate())
            .dueDate(request.getDueDate())
            .referenceNumber(request.getReferenceNumber())
            .description(request.getDescription())
            .notes(request.getNotes())
            .paymentTerms(request.getPaymentTerms())
            .isVatExempt(request.getIsVatExempt() != null ? request.getIsVatExempt() : false)
            .vatExemptionReason(request.getVatExemptionReason())
            .status(InvoiceStatus.DRAFT)
            .build();

        // 5. Ajouter les lignes de facture
        int lineNumber = 1;
        for (InvoiceLineRequest lineReq : request.getLines()) {
            InvoiceLine line = createInvoiceLine(lineReq, lineNumber++, company);
            invoice.addLine(line);
        }

        // 6. Calculer les totaux
        invoice.calculateTotals();

        // 7. Créer les TaxCalculation pour traçabilité
        createVATTaxCalculations(invoice);

        // 8. Sauvegarder
        invoice = invoiceRepository.save(invoice);

        log.info("✅ Facture créée: {} - Montant TTC: {} XAF", invoice.getInvoiceNumber(), invoice.getTotalTtc());

        return toResponse(invoice);
    }

    /**
     * Obtenir une facture par ID
     */
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Long companyId, Long invoiceId) {
        Invoice invoice = findInvoiceByIdAndCompany(companyId, invoiceId);
        return toResponse(invoice);
    }

    /**
     * Lister toutes les factures d'une entreprise
     */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoices(Long companyId, InvoiceStatus status) {
        Company company = findCompanyOrThrow(companyId);

        List<Invoice> invoices;
        if (status != null) {
            invoices = invoiceRepository.findByCompanyAndStatusOrderByIssueDateDesc(company, status);
        } else {
            invoices = invoiceRepository.findByCompanyOrderByIssueDateDesc(company);
        }

        return invoices.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Lister les factures d'un client
     */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoicesByCustomer(Long companyId, Long customerId) {
        Company company = findCompanyOrThrow(companyId);
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé: " + customerId));

        List<Invoice> invoices = invoiceRepository.findByCompanyAndCustomerOrderByIssueDateDesc(company, customer);
        return invoices.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Obtenir les factures en retard (Balance âgée)
     */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getOverdueInvoices(Long companyId) {
        Company company = findCompanyOrThrow(companyId);
        List<Invoice> invoices = invoiceRepository.findOverdueInvoices(company, LocalDate.now());
        return invoices.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Obtenir le résumé des paiements d'une facture (Option B - OHADA)
     *
     * Cette méthode retourne l'historique complet des paiements fractionnés
     * d'une facture avec statistiques :
     * - Liste de tous les paiements enregistrés (chacun avec sa propre écriture comptable)
     * - Montant total payé vs montant dû
     * - Pourcentage payé
     * - Nombre de paiements
     *
     * Conforme OHADA : Chaque paiement fractionné a été enregistré séparément
     * à sa date effective, créant une écriture comptable distincte.
     *
     * Exemple :
     * Facture 200 000 XAF - Client ABC
     * → Paiement 1 : 15/03 - 100 000 XAF (50%)
     * → Paiement 2 : 30/03 - 100 000 XAF (50%)
     * Total : 200 000 XAF (100%) - PAYÉE
     */
    @Transactional(readOnly = true)
    public InvoicePaymentSummaryResponse getInvoicePaymentSummary(Long companyId, Long invoiceId) {
        log.info("📊 Récupération résumé paiements pour facture {} - Entreprise {}", invoiceId, companyId);

        // Récupérer la facture
        Invoice invoice = findInvoiceByIdAndCompany(companyId, invoiceId);

        // Récupérer tous les paiements de cette facture (triés par date)
        List<Payment> payments = paymentRepository.findByInvoiceOrderByPaymentDateDesc(invoice);

        // Convertir en PaymentResponse
        List<PaymentResponse> paymentResponses = payments.stream()
            .map(this::toPaymentResponse)
            .collect(Collectors.toList());

        // Créer l'historique simplifié
        List<InvoicePaymentSummaryResponse.PaymentSummary> paymentHistory = payments.stream()
            .map(p -> InvoicePaymentSummaryResponse.PaymentSummary.builder()
                .paymentId(p.getId())
                .paymentNumber(p.getPaymentNumber())
                .paymentDate(p.getPaymentDate())
                .amount(p.getAmount())
                .paymentMethod(p.getPaymentMethod().name())
                .isReconciled(p.getIsReconciled())
                .description(p.getDescription())
                .build())
            .collect(Collectors.toList());

        // Construire la réponse
        InvoicePaymentSummaryResponse summary = InvoicePaymentSummaryResponse.builder()
            .invoiceId(invoice.getId())
            .invoiceNumber(invoice.getInvoiceNumber())
            .issueDate(invoice.getIssueDate())
            .dueDate(invoice.getDueDate())
            .status(invoice.getStatus())
            .customerId(invoice.getCustomer().getId())
            .customerName(invoice.getCustomer().getName())
            .totalTtc(invoice.getTotalTtc())
            .amountPaid(invoice.getAmountPaid())
            .amountDue(invoice.getAmountDue())
            .paymentPercentage(invoice.getPaymentPercentage())
            .paymentCount(invoice.getPaymentCount())
            .hasFractionalPayments(invoice.hasFractionalPayments())
            .isFullyPaid(invoice.getStatus() == InvoiceStatus.PAID)
            .isOverdue(invoice.isOverdue())
            .daysOverdue(invoice.getDaysOverdue())
            .payments(paymentResponses)
            .paymentHistory(paymentHistory)
            .build();

        log.info("✅ Résumé paiements - Facture {} : {} paiement(s) - {}% payé ({} / {} XAF)",
            invoice.getInvoiceNumber(),
            summary.getPaymentCount(),
            summary.getPaymentPercentage(),
            summary.getAmountPaid(),
            summary.getTotalTtc());

        return summary;
    }

    /**
     * Mettre à jour une facture (uniquement en mode DRAFT)
     */
    public InvoiceResponse updateInvoice(Long companyId, Long invoiceId, InvoiceUpdateRequest request) {
        log.info("📝 Mise à jour facture {} pour entreprise {}", invoiceId, companyId);

        Invoice invoice = findInvoiceByIdAndCompany(companyId, invoiceId);

        // Vérifier que la facture est modifiable
        if (!invoice.isEditable()) {
            throw new ValidationException("Seules les factures en statut DRAFT peuvent être modifiées");
        }

        // Mettre à jour les champs
        if (request.getDueDate() != null) invoice.setDueDate(request.getDueDate());
        if (request.getReferenceNumber() != null) invoice.setReferenceNumber(request.getReferenceNumber());
        if (request.getDescription() != null) invoice.setDescription(request.getDescription());
        if (request.getNotes() != null) invoice.setNotes(request.getNotes());
        if (request.getPaymentTerms() != null) invoice.setPaymentTerms(request.getPaymentTerms());

        // Mettre à jour les lignes si fournies
        if (request.getLines() != null) {
            // Supprimer les anciennes lignes
            invoice.getLines().clear();
            invoiceLineRepository.deleteByInvoice(invoice);

            // Ajouter les nouvelles lignes
            int lineNumber = 1;
            for (InvoiceLineRequest lineReq : request.getLines()) {
                InvoiceLine line = createInvoiceLine(lineReq, lineNumber++, invoice.getCompany());
                invoice.addLine(line);
            }

            // Recalculer les totaux
            invoice.calculateTotals();

            // Recréer les TaxCalculation pour traçabilité
            createVATTaxCalculations(invoice);
        }

        invoice = invoiceRepository.save(invoice);
        log.info("✅ Facture {} mise à jour", invoice.getInvoiceNumber());

        return toResponse(invoice);
    }

    /**
     * VALIDER une facture → Génère l'écriture comptable automatiquement
     * Cette action est IRRÉVERSIBLE
     */
    public InvoiceResponse validateInvoice(Long companyId, Long invoiceId) {
        log.info("✅ VALIDATION facture {} pour entreprise {}", invoiceId, companyId);

        Invoice invoice = findInvoiceByIdAndCompany(companyId, invoiceId);

        // Vérifier le statut
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new ValidationException("Seules les factures en statut DRAFT peuvent être validées");
        }

        // Vérifier qu'il y a des lignes
        if (invoice.getLines().isEmpty()) {
            throw new ValidationException("Impossible de valider une facture sans lignes");
        }

        // Changer le statut
        invoice.setStatus(InvoiceStatus.ISSUED);

        // 🔥 GÉNÉRER L'ÉCRITURE COMPTABLE AUTOMATIQUEMENT
        GeneralLedger entry = generateAccountingEntry(invoice);
        invoice.setGeneralLedger(entry);

        invoice = invoiceRepository.save(invoice);

        log.info("✅ Facture {} validée avec succès - Écriture comptable {} générée",
            invoice.getInvoiceNumber(), entry.getId());

        return toResponse(invoice);
    }

    /**
     * Annuler une facture (uniquement si non payée)
     */
    public InvoiceResponse cancelInvoice(Long companyId, Long invoiceId) {
        log.info("❌ Annulation facture {} pour entreprise {}", invoiceId, companyId);

        Invoice invoice = findInvoiceByIdAndCompany(companyId, invoiceId);

        if (!invoice.isCancellable()) {
            throw new ValidationException("Cette facture ne peut pas être annulée (déjà payée partiellement ou totalement)");
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice = invoiceRepository.save(invoice);

        log.info("✅ Facture {} annulée", invoice.getInvoiceNumber());
        return toResponse(invoice);
    }

    /**
     * Supprimer une facture (uniquement en mode DRAFT)
     */
    public void deleteInvoice(Long companyId, Long invoiceId) {
        log.warn("🗑️ Suppression facture {} pour entreprise {}", invoiceId, companyId);

        Invoice invoice = findInvoiceByIdAndCompany(companyId, invoiceId);

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new ValidationException("Seules les factures en statut DRAFT peuvent être supprimées");
        }

        invoiceRepository.delete(invoice);
        log.info("✅ Facture {} supprimée définitivement", invoice.getInvoiceNumber());
    }

    // ==================== Méthodes privées ====================

    /**
     * Générer l'écriture comptable pour une facture validée
     *
     * Exemple: Facture 1 000 000 XAF HT + TVA 19.25% = 1 192 500 XAF TTC
     *
     * Journal VE (Ventes):
     * DÉBIT  | 4111001 (Client - Restaurant)  | 1 192 500 | Créance client
     * CRÉDIT | 701     (Ventes marchandises)  | 1 000 000 | Chiffre d'affaires
     * CRÉDIT | 4431    (TVA collectée)        |   192 500 | TVA à reverser
     */
    private GeneralLedger generateAccountingEntry(Invoice invoice) {
        log.info("🔄 Génération écriture comptable pour facture {}", invoice.getInvoiceNumber());

        Company company = invoice.getCompany();
        Customer customer = invoice.getCustomer();

        // Récupérer le compte auxiliaire du client (4111001, 4111002...)
        String customerAccountNumber = customer.getAuxiliaryAccountNumber();
        if (customerAccountNumber == null) {
            throw new ValidationException("Le client n'a pas de compte auxiliaire. Veuillez réinitialiser le client.");
        }

        ChartOfAccounts customerAccount = chartOfAccountsRepository.findByCompanyAndAccountNumber(company, customerAccountNumber)
            .orElseThrow(() -> new ValidationException("Compte client non trouvé dans le plan comptable: " + customerAccountNumber));

        // Créer l'écriture principale
        GeneralLedger entry = GeneralLedger.builder()
            .company(company)
            .entryDate(invoice.getIssueDate())
            .journalCode("VE")  // Journal des ventes
            .reference(invoice.getInvoiceNumber())
            .account(customerAccount)  // DÉBIT: Client
            .description("Facture client " + customer.getName() + " - " + invoice.getInvoiceNumber())
            .debitAmount(invoice.getTotalTtc())  // Débit = TTC (créance totale)
            .creditAmount(BigDecimal.ZERO)
            .customer(customer)
            .build();

        entry = generalLedgerRepository.save(entry);
        final Long parentEntryId = entry.getId();

        // Ligne 2: CRÉDIT Ventes (701) = HT
        ChartOfAccounts salesAccount = chartOfAccountsRepository.findByCompanyAndAccountNumber(company, SALES_ACCOUNT_DEFAULT)
            .orElseThrow(() -> new ValidationException("Compte ventes non trouvé: " + SALES_ACCOUNT_DEFAULT));

        GeneralLedger salesEntry = GeneralLedger.builder()
            .company(company)
            .entryDate(invoice.getIssueDate())
            .journalCode("VE")
            .reference(invoice.getInvoiceNumber())
            .account(salesAccount)  // CRÉDIT: Ventes
            .description("Vente - " + invoice.getDescription())
            .debitAmount(BigDecimal.ZERO)
            .creditAmount(invoice.getTotalHt())  // Crédit = HT
            .customer(customer)
            .build();
        generalLedgerRepository.save(salesEntry);

        // Ligne 3: CRÉDIT TVA collectée - Récupération compte depuis TaxService
        if (invoice.getVatAmount().compareTo(BigDecimal.ZERO) > 0) {
            // Récupérer le compte TVA depuis la configuration fiscale
            String vatCollectedAccountNumber = getVATCollectedAccountNumber(company);

            ChartOfAccounts vatCollectedAccount = chartOfAccountsRepository.findByCompanyAndAccountNumber(company, vatCollectedAccountNumber)
                .orElseThrow(() -> new ValidationException("Compte TVA collectée non trouvé: " + vatCollectedAccountNumber));

            // Récupérer le taux de TVA depuis la configuration (pour la description)
            BigDecimal vatRate = invoice.getVatAmount()
                .divide(invoice.getTotalHt(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

            GeneralLedger vatEntry = GeneralLedger.builder()
                .company(company)
                .entryDate(invoice.getIssueDate())
                .journalCode("VE")
                .reference(invoice.getInvoiceNumber())
                .account(vatCollectedAccount)  // CRÉDIT: TVA collectée (depuis config)
                .description(String.format("TVA %.2f%% sur facture %s", vatRate, invoice.getInvoiceNumber()))
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(invoice.getVatAmount())
                .customer(customer)
                .build();
            generalLedgerRepository.save(vatEntry);
        }

        log.info("✅ Écriture comptable générée: DÉBIT {} {} XAF / CRÉDIT {} + TVA = {} XAF",
            customerAccount, invoice.getTotalTtc(), SALES_ACCOUNT_DEFAULT, invoice.getTotalTtc());

        return entry;
    }

    /**
     * Récupère le numéro de compte TVA collectée depuis la configuration fiscale
     */
    private String getVATCollectedAccountNumber(Company company) {
        return taxService.getTaxConfigurations(company.getId()).stream()
            .filter(config -> config.getTaxType() == com.predykt.accounting.domain.enums.TaxType.VAT)
            .map(config -> config.getAccountNumber())
            .findFirst()
            .orElse("4431");  // Fallback sur compte OHADA standard
    }

    /**
     * Créer une ligne de facture à partir d'une requête
     */
    private InvoiceLine createInvoiceLine(InvoiceLineRequest request, int lineNumber, Company company) {
        // Récupérer le taux de TVA depuis la configuration fiscale si non fourni
        BigDecimal vatRate = request.getVatRate();
        if (vatRate == null) {
            vatRate = getDefaultVATRate(company);
        }

        InvoiceLine line = InvoiceLine.builder()
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

        // Les montants sont calculés automatiquement par @PrePersist
        line.calculateAmounts();

        return line;
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

    /**
     * Générer le numéro de facture (FV-2025-0001)
     */
    private String generateInvoiceNumber(Company company) {
        int year = LocalDate.now().getYear();
        Long sequence = jdbcTemplate.queryForObject(
            "SELECT nextval('seq_invoice_number')",
            Long.class
        );
        return String.format("%s-%d-%04d", INVOICE_PREFIX, year, sequence);
    }

    /**
     * Convertir Invoice en InvoiceResponse
     */
    private InvoiceResponse toResponse(Invoice invoice) {
        List<InvoiceLineResponse> lineResponses = invoice.getLines().stream()
            .map(this::toLineResponse)
            .collect(Collectors.toList());

        return InvoiceResponse.builder()
            .id(invoice.getId())
            .companyId(invoice.getCompany().getId())
            .customerId(invoice.getCustomer().getId())
            .customerName(invoice.getCustomer().getName())
            .customerNiu(invoice.getCustomerNiu())
            .invoiceNumber(invoice.getInvoiceNumber())
            .invoiceType(invoice.getInvoiceType())
            .issueDate(invoice.getIssueDate())
            .dueDate(invoice.getDueDate())
            .paymentDate(invoice.getPaymentDate())
            .totalHt(invoice.getTotalHt())
            .vatAmount(invoice.getVatAmount())
            .totalTtc(invoice.getTotalTtc())
            .amountPaid(invoice.getAmountPaid())
            .amountDue(invoice.getAmountDue())
            .status(invoice.getStatus())
            .isReconciled(invoice.getIsReconciled())
            .reconciliationDate(invoice.getReconciliationDate())
            .referenceNumber(invoice.getReferenceNumber())
            .description(invoice.getDescription())
            .notes(invoice.getNotes())
            .paymentTerms(invoice.getPaymentTerms())
            .isVatExempt(invoice.getIsVatExempt())
            .vatExemptionReason(invoice.getVatExemptionReason())
            .customerHasNiu(invoice.getCustomerHasNiu())
            .lines(lineResponses)
            .daysOverdue(invoice.getDaysOverdue())
            .isOverdue(invoice.isOverdue())
            .agingCategory(invoice.getAgingCategory())
            .paymentPercentage(invoice.getPaymentPercentage())
            .paymentCount(invoice.getPaymentCount())
            .hasFractionalPayments(invoice.hasFractionalPayments())
            .createdAt(invoice.getCreatedAt())
            .updatedAt(invoice.getUpdatedAt())
            .build();
    }

    private InvoiceLineResponse toLineResponse(InvoiceLine line) {
        return InvoiceLineResponse.builder()
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

    private Invoice findInvoiceByIdAndCompany(Long companyId, Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Facture non trouvée: " + invoiceId));

        if (!invoice.getCompany().getId().equals(companyId)) {
            throw new ValidationException("Cette facture n'appartient pas à cette entreprise");
        }

        return invoice;
    }

    private Company findCompanyOrThrow(Long companyId) {
        return companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée: " + companyId));
    }

    /**
     * Convertir Payment en PaymentResponse (version simplifiée)
     */
    private PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
            .id(payment.getId())
            .companyId(payment.getCompany().getId())
            .paymentNumber(payment.getPaymentNumber())
            .paymentType(payment.getPaymentType())
            .invoiceId(payment.getInvoice() != null ? payment.getInvoice().getId() : null)
            .invoiceNumber(payment.getInvoice() != null ? payment.getInvoice().getInvoiceNumber() : null)
            .billId(payment.getBill() != null ? payment.getBill().getId() : null)
            .billNumber(payment.getBill() != null ? payment.getBill().getBillNumber() : null)
            .customerId(payment.getCustomer() != null ? payment.getCustomer().getId() : null)
            .customerName(payment.getCustomer() != null ? payment.getCustomer().getName() : null)
            .supplierId(payment.getSupplier() != null ? payment.getSupplier().getId() : null)
            .supplierName(payment.getSupplier() != null ? payment.getSupplier().getName() : null)
            .paymentDate(payment.getPaymentDate())
            .amount(payment.getAmount())
            .paymentMethod(payment.getPaymentMethod())
            .chequeNumber(payment.getChequeNumber())
            .mobileMoneyNumber(payment.getMobileMoneyNumber())
            .transactionReference(payment.getTransactionReference())
            .status(payment.getStatus())
            .isReconciled(payment.getIsReconciled())
            .reconciliationDate(payment.getReconciliationDate())
            .reconciledBy(payment.getReconciledBy())
            .description(payment.getDescription())
            .notes(payment.getNotes())
            .createdAt(payment.getCreatedAt())
            .createdBy(payment.getCreatedBy())
            .build();
    }

    /**
     * Créer et sauvegarder les TaxCalculation pour une facture client (TVA collectée)
     * Permet la traçabilité complète des taxes calculées
     */
    private void createVATTaxCalculations(Invoice invoice) {
        if (invoice.getVatAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return; // Pas de TVA, rien à tracer
        }

        try {
            // Calculer le taux de TVA effectif
            BigDecimal vatRate = invoice.getVatAmount()
                .divide(invoice.getTotalHt(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

            // Récupérer le compte TVA collectée depuis la configuration
            String vatAccountNumber = getVATCollectedAccountNumber(invoice.getCompany());

            // Créer la TaxCalculation pour la TVA collectée
            com.predykt.accounting.domain.entity.TaxCalculation taxCalc =
                com.predykt.accounting.domain.entity.TaxCalculation.builder()
                    .company(invoice.getCompany())
                    .invoice(invoice)
                    .taxType(com.predykt.accounting.domain.enums.TaxType.VAT)
                    .calculationDate(invoice.getIssueDate())
                    .baseAmount(invoice.getTotalHt())
                    .taxRate(vatRate)
                    .taxAmount(invoice.getVatAmount())
                    .accountNumber(vatAccountNumber)
                    .status("CALCULATED")
                    .notes("TVA collectée sur facture client " + invoice.getInvoiceNumber())
                    .hasAlert(false)
                    .build();

            taxCalculationRepository.save(taxCalc);
            log.debug("💾 TaxCalculation TVA sauvegardée: {} XAF ({}%) pour facture {}",
                invoice.getVatAmount(), vatRate, invoice.getInvoiceNumber());

        } catch (Exception e) {
            log.error("Erreur lors de la création de TaxCalculation pour la facture {}: {}",
                invoice.getInvoiceNumber(), e.getMessage());
            // Ne pas bloquer la création de la facture si la traçabilité échoue
        }
    }
}
