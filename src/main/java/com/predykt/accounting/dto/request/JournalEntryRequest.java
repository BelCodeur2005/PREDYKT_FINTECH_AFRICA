// src/main/java/com/predykt/accounting/dto/request/JournalEntryRequest.java
package com.predykt.accounting.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

