package com.predykt.accounting.controller.cabinet;

import com.predykt.accounting.domain.entity.CabinetInvoice;
import com.predykt.accounting.domain.enums.InvoiceStatus;
import com.predykt.accounting.dto.CabinetInvoiceDTO;
import com.predykt.accounting.service.CabinetInvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller REST pour la gestion des factures de cabinet (MODE CABINET)
 */
@RestController
@RequestMapping("/api/cabinet-invoices")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('CABINET_MANAGER') or hasRole('ACCOUNTANT')")
public class CabinetInvoiceController {

    private final CabinetInvoiceService invoiceService;

    /**
     * Crée une nouvelle facture
     */
    @PostMapping
    public ResponseEntity<CabinetInvoice> createInvoice(@Valid @RequestBody CabinetInvoiceDTO dto) {
        log.info("Création d'une facture pour le cabinet {}", dto.getCabinetId());

        CabinetInvoice invoice = mapToEntity(dto);
        CabinetInvoice created = invoiceService.createInvoice(invoice);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Met à jour une facture
     */
    @PutMapping("/{id}")
    public ResponseEntity<CabinetInvoice> updateInvoice(
            @PathVariable Long id,
            @Valid @RequestBody CabinetInvoiceDTO dto) {
        log.info("Mise à jour de la facture ID: {}", id);

        CabinetInvoice invoice = mapToEntity(dto);
        CabinetInvoice updated = invoiceService.updateInvoice(id, invoice);

        return ResponseEntity.ok(updated);
    }

    /**
     * Récupère une facture par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CabinetInvoice> getInvoiceById(@PathVariable Long id) {
        CabinetInvoice invoice = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(invoice);
    }

    /**
     * Récupère une facture par numéro
     */
    @GetMapping("/by-number/{invoiceNumber}")
    public ResponseEntity<CabinetInvoice> getInvoiceByNumber(@PathVariable String invoiceNumber) {
        CabinetInvoice invoice = invoiceService.getInvoiceByNumber(invoiceNumber);
        return ResponseEntity.ok(invoice);
    }

    /**
     * Récupère toutes les factures d'un cabinet
     */
    @GetMapping("/cabinet/{cabinetId}")
    public ResponseEntity<List<CabinetInvoice>> getCabinetInvoices(@PathVariable Long cabinetId) {
        List<CabinetInvoice> invoices = invoiceService.getCabinetInvoices(cabinetId);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Récupère les factures d'un cabinet par statut
     */
    @GetMapping("/cabinet/{cabinetId}/status/{status}")
    public ResponseEntity<List<CabinetInvoice>> getInvoicesByStatus(
            @PathVariable Long cabinetId,
            @PathVariable InvoiceStatus status) {
        List<CabinetInvoice> invoices = invoiceService.getInvoicesByStatus(cabinetId, status);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Récupère les factures en retard d'un cabinet
     */
    @GetMapping("/cabinet/{cabinetId}/overdue")
    public ResponseEntity<List<CabinetInvoice>> getOverdueInvoices(@PathVariable Long cabinetId) {
        List<CabinetInvoice> invoices = invoiceService.getOverdueInvoices(cabinetId);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Récupère les factures pour une période
     */
    @GetMapping("/cabinet/{cabinetId}/period")
    public ResponseEntity<List<CabinetInvoice>> getInvoicesByPeriod(
            @PathVariable Long cabinetId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<CabinetInvoice> invoices = invoiceService.getInvoicesByPeriod(cabinetId, startDate, endDate);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Marque une facture comme payée
     */
    @PutMapping("/{id}/mark-paid")
    public ResponseEntity<CabinetInvoice> markAsPaid(
            @PathVariable Long id,
            @RequestParam String paymentMethod) {
        log.info("Marquage de la facture {} comme payée", id);

        CabinetInvoice updated = invoiceService.markAsPaid(id, paymentMethod);
        return ResponseEntity.ok(updated);
    }

    /**
     * Annule une facture
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<CabinetInvoice> cancelInvoice(@PathVariable Long id) {
        log.info("Annulation de la facture {}", id);

        CabinetInvoice updated = invoiceService.cancelInvoice(id);
        return ResponseEntity.ok(updated);
    }

    /**
     * Calcule le chiffre d'affaires total d'un cabinet
     */
    @GetMapping("/cabinet/{cabinetId}/revenue/total")
    public ResponseEntity<BigDecimal> calculateTotalRevenue(@PathVariable Long cabinetId) {
        BigDecimal revenue = invoiceService.calculateTotalRevenue(cabinetId);
        return ResponseEntity.ok(revenue);
    }

    /**
     * Calcule le CA d'un cabinet pour une période
     */
    @GetMapping("/cabinet/{cabinetId}/revenue/period")
    public ResponseEntity<BigDecimal> calculateRevenueByPeriod(
            @PathVariable Long cabinetId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        BigDecimal revenue = invoiceService.calculateRevenueByPeriod(cabinetId, startDate, endDate);
        return ResponseEntity.ok(revenue);
    }

    /**
     * Calcule le montant des impayés d'un cabinet
     */
    @GetMapping("/cabinet/{cabinetId}/outstanding")
    public ResponseEntity<BigDecimal> calculateOutstandingAmount(@PathVariable Long cabinetId) {
        BigDecimal amount = invoiceService.calculateOutstandingAmount(cabinetId);
        return ResponseEntity.ok(amount);
    }

    /**
     * Calcule le montant des factures en retard
     */
    @GetMapping("/cabinet/{cabinetId}/overdue/amount")
    public ResponseEntity<BigDecimal> calculateOverdueAmount(@PathVariable Long cabinetId) {
        BigDecimal amount = invoiceService.calculateOverdueAmount(cabinetId);
        return ResponseEntity.ok(amount);
    }

    /**
     * Compte les factures par statut
     */
    @GetMapping("/cabinet/{cabinetId}/count/{status}")
    public ResponseEntity<Long> countInvoicesByStatus(
            @PathVariable Long cabinetId,
            @PathVariable InvoiceStatus status) {
        Long count = invoiceService.countInvoicesByStatus(cabinetId, status);
        return ResponseEntity.ok(count);
    }

    /**
     * Supprime une facture
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CABINET_MANAGER')")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        log.info("Suppression de la facture ID: {}", id);

        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }

    // Méthodes utilitaires

    private CabinetInvoice mapToEntity(CabinetInvoiceDTO dto) {
        return CabinetInvoice.builder()
            .invoiceNumber(dto.getInvoiceNumber())
            .invoiceDate(dto.getInvoiceDate())
            .dueDate(dto.getDueDate())
            .amountHt(dto.getAmountHt())
            .vatAmount(dto.getVatAmount())
            .amountTtc(dto.getAmountTtc())
            .status(dto.getStatus())
            .periodStart(dto.getPeriodStart())
            .periodEnd(dto.getPeriodEnd())
            .description(dto.getDescription())
            .notes(dto.getNotes())
            .build();
    }
}
