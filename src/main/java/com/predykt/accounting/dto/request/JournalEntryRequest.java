// src/main/java/com/predykt/accounting/dto/request/JournalEntryRequest.java
package com.predykt.accounting.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class JournalEntryRequest {
    
    @NotNull(message = "La date d'écriture est obligatoire")
    private LocalDate entryDate;
    
    @NotBlank(message = "La référence est obligatoire")
    private String reference;
    
    private String journalCode;
    
    @NotEmpty(message = "Au moins une ligne d'écriture est requise")
    @Valid
    private List<JournalEntryLineRequest> lines;
}

// JournalEntryLineRequest.java
package com.predykt.accounting.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class JournalEntryLineRequest {
    
    @NotBlank(message = "Le numéro de compte est obligatoire")
    private String accountNumber;
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Le montant débit doit être positif ou nul")
    private BigDecimal debitAmount = BigDecimal.ZERO;
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Le montant crédit doit être positif ou nul")
    private BigDecimal creditAmount = BigDecimal.ZERO;
    
    private String description;
}