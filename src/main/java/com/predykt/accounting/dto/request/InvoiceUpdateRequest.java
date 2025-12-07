package com.predykt.accounting.dto.request;

import com.predykt.accounting.domain.enums.InvoiceStatus;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO pour mettre à jour une facture client (seulement si status = DRAFT)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceUpdateRequest {

    private LocalDate dueDate;

    private String referenceNumber;

    private String description;

    private String notes;

    private String paymentTerms;

    private Boolean isVatExempt;

    private String vatExemptionReason;

    @Valid
    private List<InvoiceLineRequest> lines;

    private InvoiceStatus status;  // Pour changer le statut (DRAFT → ISSUED)
}
