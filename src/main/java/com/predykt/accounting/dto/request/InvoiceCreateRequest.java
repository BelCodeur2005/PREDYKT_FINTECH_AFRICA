package com.predykt.accounting.dto.request;

import com.predykt.accounting.domain.enums.InvoiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO pour créer une facture client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCreateRequest {

    @NotNull(message = "Le client est obligatoire")
    private Long customerId;

    private InvoiceType invoiceType;  // Par défaut: STANDARD

    @NotNull(message = "La date d'émission est obligatoire")
    private LocalDate issueDate;

    @NotNull(message = "La date d'échéance est obligatoire")
    private LocalDate dueDate;

    private String referenceNumber;  // Bon de commande, etc.

    private String description;

    private String notes;

    private String paymentTerms;

    private Boolean isVatExempt;  // Exonération TVA (export, zones franches)

    private String vatExemptionReason;

    @NotEmpty(message = "Au moins une ligne de facture est requise")
    @Valid
    @Builder.Default
    private List<InvoiceLineRequest> lines = new ArrayList<>();
}
