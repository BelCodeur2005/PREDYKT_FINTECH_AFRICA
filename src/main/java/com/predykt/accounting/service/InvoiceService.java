package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.*;
import com.predykt.accounting.domain.enums.InvoiceStatus;
import com.predykt.accounting.domain.enums.InvoiceType;
import com.predykt.accounting.dto.request.InvoiceCreateRequest;
import com.predykt.accounting.dto.request.InvoiceLineRequest;
import com.predykt.accounting.dto.request.InvoiceUpdateRequest;
import com.predykt.accounting.dto.response.InvoiceLineResponse;
import com.predykt.accounting.dto.response.InvoiceResponse;
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
    private final JdbcTemplate jdbcTemplate;

    // Constantes
    private static final String INVOICE_PREFIX = "FV";
    private static final String VAT_COLLECTED_ACCOUNT = "4431";  // TVA collectée
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
            InvoiceLine line = createInvoiceLine(lineReq, lineNumber++);
            invoice.addLine(line);
        }

        // 6. Calculer les totaux
        invoice.calculateTotals();

        // 7. Sauvegarder
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
        if (request.getIssueDate() != null) invoice.setIssueDate(request.getIssueDate());
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
                InvoiceLine line = createInvoiceLine(lineReq, lineNumber++);
                invoice.addLine(line);
            }

            // Recalculer les totaux
            invoice.calculateTotals();
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
        String customerAccount = customer.getAuxiliaryAccountNumber();
        if (customerAccount == null) {
            throw new ValidationException("Le client n'a pas de compte auxiliaire. Veuillez réinitialiser le client.");
        }

        // Créer l'écriture principale
        GeneralLedger entry = GeneralLedger.builder()
            .company(company)
            .entryDate(invoice.getIssueDate())
            .journalCode("VE")  // Journal des ventes
            .pieceNumber(invoice.getInvoiceNumber())
            .accountNumber(customerAccount)  // DÉBIT: Client
            .description("Facture client " + customer.getName() + " - " + invoice.getInvoiceNumber())
            .debitAmount(invoice.getTotalTtc())  // Débit = TTC (créance totale)
            .creditAmount(BigDecimal.ZERO)
            .customer(customer)
            .build();

        entry = generalLedgerRepository.save(entry);
        final Long parentEntryId = entry.getId();

        // Ligne 2: CRÉDIT Ventes (701) = HT
        GeneralLedger salesEntry = GeneralLedger.builder()
            .company(company)
            .entryDate(invoice.getIssueDate())
            .journalCode("VE")
            .pieceNumber(invoice.getInvoiceNumber())
            .accountNumber(SALES_ACCOUNT_DEFAULT)  // CRÉDIT: Ventes
            .description("Vente - " + invoice.getDescription())
            .debitAmount(BigDecimal.ZERO)
            .creditAmount(invoice.getTotalHt())  // Crédit = HT
            .customer(customer)
            .build();
        generalLedgerRepository.save(salesEntry);

        // Ligne 3: CRÉDIT TVA collectée (4431) = Montant TVA
        if (invoice.getVatAmount().compareTo(BigDecimal.ZERO) > 0) {
            GeneralLedger vatEntry = GeneralLedger.builder()
                .company(company)
                .entryDate(invoice.getIssueDate())
                .journalCode("VE")
                .pieceNumber(invoice.getInvoiceNumber())
                .accountNumber(VAT_COLLECTED_ACCOUNT)  // CRÉDIT: TVA collectée
                .description("TVA 19.25% sur facture " + invoice.getInvoiceNumber())
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(invoice.getVatAmount())
                .customer(customer)
                .build();
            generalLedgerRepository.save(vatEntry);
        }

        log.info("✅ Écriture comptable générée: DÉBIT {} {} XAF / CRÉDIT {} + {} = {} XAF",
            customerAccount, invoice.getTotalTtc(),
            SALES_ACCOUNT_DEFAULT, VAT_COLLECTED_ACCOUNT, invoice.getTotalTtc());

        return entry;
    }

    /**
     * Créer une ligne de facture à partir d'une requête
     */
    private InvoiceLine createInvoiceLine(InvoiceLineRequest request, int lineNumber) {
        InvoiceLine line = InvoiceLine.builder()
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

        // Les montants sont calculés automatiquement par @PrePersist
        line.calculateAmounts();

        return line;
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
            .agingCategory(invoice.getAgingCategory())
            .generalLedgerId(invoice.getGeneralLedger() != null ? invoice.getGeneralLedger().getId() : null)
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
}
