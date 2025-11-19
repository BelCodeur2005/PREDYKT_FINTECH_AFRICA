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