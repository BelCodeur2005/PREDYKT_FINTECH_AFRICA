package com.predykt.accounting.dto.request;

import com.predykt.accounting.domain.enums.BillType;
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
 * DTO pour créer une facture fournisseur
 * Conforme OHADA + Fiscalité Cameroun (AIR, IRPP Loyer)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillCreateRequest {

    @NotNull(message = "Le fournisseur est obligatoire")
    private Long supplierId;

    private String supplierInvoiceNumber;  // Numéro de facture du fournisseur

    @Builder.Default
    private BillType billType = BillType.PURCHASE;  // PURCHASE, RENT, SERVICES

    @NotNull(message = "La date d'émission est obligatoire")
    private LocalDate issueDate;

    @NotNull(message = "La date d'échéance est obligatoire")
    private LocalDate dueDate;

    private String referenceNumber;

    private String description;

    private String notes;

    @NotEmpty(message = "Au moins une ligne de facture est requise")
    @Valid
    @Builder.Default
    private List<BillLineRequest> lines = new ArrayList<>();
}
