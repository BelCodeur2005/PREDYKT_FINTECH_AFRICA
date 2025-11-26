package com.predykt.accounting.service;

import com.predykt.accounting.domain.entity.Cabinet;
import com.predykt.accounting.domain.entity.CabinetInvoice;
import com.predykt.accounting.domain.enums.InvoiceStatus;
import com.predykt.accounting.repository.CabinetInvoiceRepository;
import com.predykt.accounting.repository.CabinetRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

/**
 * Service pour la gestion des factures de cabinet (MODE CABINET)
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CabinetInvoiceService {

    private final CabinetInvoiceRepository invoiceRepository;
    private final CabinetRepository cabinetRepository;

    /**
     * Crée une nouvelle facture
     */
    public CabinetInvoice createInvoice(CabinetInvoice invoice) {
        log.info("Création d'une facture pour le cabinet {}", invoice.getCabinet().getId());

        // Valider le cabinet
        Cabinet cabinet = cabinetRepository.findById(invoice.getCabinet().getId())
            .orElseThrow(() -> new EntityNotFoundException(
                "Cabinet non trouvé avec l'ID: " + invoice.getCabinet().getId()));

        // Générer le numéro de facture si non fourni
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isEmpty()) {
            invoice.setInvoiceNumber(generateInvoiceNumber(cabinet.getId()));
        } else {
            // Vérifier l'unicité du numéro de facture
            if (invoiceRepository.existsByInvoiceNumber(invoice.getInvoiceNumber())) {
                throw new IllegalArgumentException("Une facture avec ce numéro existe déjà");
            }
        }

        invoice.setCabinet(cabinet);

        // Calculer le TTC si non fourni
        if (invoice.getAmountTtc() == null) {
            invoice.calculateTtc();
        }

        return invoiceRepository.save(invoice);
    }

    /**
     * Met à jour une facture
     */
    public CabinetInvoice updateInvoice(Long invoiceId, CabinetInvoice updatedInvoice) {
        log.info("Mise à jour de la facture ID: {}", invoiceId);

        CabinetInvoice existing = getInvoiceById(invoiceId);

        // Ne pas permettre la modification d'une facture payée
        if (existing.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Impossible de modifier une facture déjà payée");
        }

        existing.setInvoiceDate(updatedInvoice.getInvoiceDate());
        existing.setDueDate(updatedInvoice.getDueDate());
        existing.setAmountHt(updatedInvoice.getAmountHt());
        existing.setVatAmount(updatedInvoice.getVatAmount());
        existing.setPeriodStart(updatedInvoice.getPeriodStart());
        existing.setPeriodEnd(updatedInvoice.getPeriodEnd());
        existing.setDescription(updatedInvoice.getDescription());
        existing.setNotes(updatedInvoice.getNotes());

        // Recalculer le TTC
        existing.calculateTtc();

        return invoiceRepository.save(existing);
    }

    /**
     * Récupère une facture par ID
     */
    @Transactional(readOnly = true)
    public CabinetInvoice getInvoiceById(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new EntityNotFoundException("Facture non trouvée avec l'ID: " + invoiceId));
    }

    /**
     * Récupère une facture par numéro
     */
    @Transactional(readOnly = true)
    public CabinetInvoice getInvoiceByNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber)
            .orElseThrow(() -> new EntityNotFoundException("Facture non trouvée avec le numéro: " + invoiceNumber));
    }

    /**
     * Récupère toutes les factures d'un cabinet
     */
    @Transactional(readOnly = true)
    public List<CabinetInvoice> getCabinetInvoices(Long cabinetId) {
        return invoiceRepository.findByCabinetId(cabinetId);
    }

    /**
     * Récupère les factures d'un cabinet par statut
     */
    @Transactional(readOnly = true)
    public List<CabinetInvoice> getInvoicesByStatus(Long cabinetId, InvoiceStatus status) {
        return invoiceRepository.findByCabinetIdAndStatus(cabinetId, status);
    }

    /**
     * Récupère les factures en retard d'un cabinet
     */
    @Transactional(readOnly = true)
    public List<CabinetInvoice> getOverdueInvoices(Long cabinetId) {
        return invoiceRepository.findOverdueInvoicesByCabinetId(cabinetId, LocalDate.now());
    }

    /**
     * Récupère les factures pour une période
     */
    @Transactional(readOnly = true)
    public List<CabinetInvoice> getInvoicesByPeriod(Long cabinetId, LocalDate startDate, LocalDate endDate) {
        return invoiceRepository.findByCabinetIdAndPeriod(cabinetId, startDate, endDate);
    }

    /**
     * Marque une facture comme payée
     */
    public CabinetInvoice markAsPaid(Long invoiceId, String paymentMethod) {
        log.info("Marquage de la facture {} comme payée via {}", invoiceId, paymentMethod);

        CabinetInvoice invoice = getInvoiceById(invoiceId);

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Cette facture est déjà payée");
        }

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Impossible de marquer une facture annulée comme payée");
        }

        invoice.markAsPaid(paymentMethod);
        return invoiceRepository.save(invoice);
    }

    /**
     * Annule une facture
     */
    public CabinetInvoice cancelInvoice(Long invoiceId) {
        log.info("Annulation de la facture {}", invoiceId);

        CabinetInvoice invoice = getInvoiceById(invoiceId);

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Impossible d'annuler une facture déjà payée");
        }

        invoice.cancel();
        return invoiceRepository.save(invoice);
    }

    /**
     * Calcule le chiffre d'affaires total d'un cabinet
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalRevenue(Long cabinetId) {
        return invoiceRepository.calculateTotalRevenueByCabinetId(cabinetId);
    }

    /**
     * Calcule le CA d'un cabinet pour une période
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateRevenueByPeriod(Long cabinetId, LocalDate startDate, LocalDate endDate) {
        return invoiceRepository.calculateRevenueByCabinetIdAndPeriod(cabinetId, startDate, endDate);
    }

    /**
     * Calcule le montant des impayés d'un cabinet
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateOutstandingAmount(Long cabinetId) {
        return invoiceRepository.calculateOutstandingAmountByCabinetId(cabinetId);
    }

    /**
     * Calcule le montant des factures en retard
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateOverdueAmount(Long cabinetId) {
        return invoiceRepository.calculateOverdueAmountByCabinetId(cabinetId, LocalDate.now());
    }

    /**
     * Compte les factures par statut
     */
    @Transactional(readOnly = true)
    public Long countInvoicesByStatus(Long cabinetId, InvoiceStatus status) {
        return invoiceRepository.countByCabinetIdAndStatus(cabinetId, status);
    }

    /**
     * Génère un numéro de facture unique
     */
    private String generateInvoiceNumber(Long cabinetId) {
        int year = Year.now().getValue();
        String prefix = String.format("CAB%03d-%d-", cabinetId, year);

        String lastNumber = invoiceRepository.findLastInvoiceNumberByCabinetIdAndPrefix(cabinetId, prefix)
            .orElse(prefix + "0000");

        // Extraire le numéro séquentiel
        String sequentialPart = lastNumber.substring(lastNumber.lastIndexOf('-') + 1);
        int nextSequence = Integer.parseInt(sequentialPart) + 1;

        return String.format("%s%04d", prefix, nextSequence);
    }

    /**
     * Crée une facture à partir du temps suivi
     */
    public CabinetInvoice createInvoiceFromTimeTracking(Long cabinetId, Long companyId,
                                                        LocalDate startDate, LocalDate endDate,
                                                        BigDecimal vatRate) {
        log.info("Création de facture depuis le suivi du temps pour le cabinet {} et l'entreprise {} " +
                "du {} au {}", cabinetId, companyId, startDate, endDate);

        Cabinet cabinet = cabinetRepository.findById(cabinetId)
            .orElseThrow(() -> new EntityNotFoundException("Cabinet non trouvé avec l'ID: " + cabinetId));

        // Note: Ce service nécessiterait une injection de TimeTrackingService pour calculer le montant
        // Pour l'instant, on retourne une facture vide que l'appelant devra remplir

        CabinetInvoice invoice = CabinetInvoice.builder()
            .cabinet(cabinet)
            .invoiceDate(LocalDate.now())
            .dueDate(LocalDate.now().plusDays(30))
            .periodStart(startDate)
            .periodEnd(endDate)
            .status(InvoiceStatus.PENDING)
            .build();

        if (vatRate != null) {
            invoice.calculateVat(vatRate);
        }

        return invoice;
    }

    /**
     * Supprime une facture
     */
    public void deleteInvoice(Long invoiceId) {
        log.info("Suppression de la facture ID: {}", invoiceId);

        CabinetInvoice invoice = getInvoiceById(invoiceId);

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Impossible de supprimer une facture déjà payée");
        }

        invoiceRepository.delete(invoice);
    }
}
