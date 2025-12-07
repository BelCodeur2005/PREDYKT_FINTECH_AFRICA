package com.predykt.accounting.dto.request;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO pour mettre à jour une facture fournisseur (mode DRAFT uniquement)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillUpdateRequest {

    private String supplierInvoiceNumber;

    private LocalDate issueDate;

    private LocalDate dueDate;

    private String referenceNumber;

    private String description;

    private String notes;

    @Valid
    private List<BillLineRequest> lines;  // Si null, ne pas modifier les lignes
}
