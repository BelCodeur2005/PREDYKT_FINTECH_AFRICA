// TransactionResponse.java
package com.predykt.accounting.dto.response;

import com.predykt.accounting.domain.enums.TransactionCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private LocalDate transactionDate;
    private BigDecimal amount;
    private String description;
    private TransactionCategory category;
private BigDecimal categoryConfidence;
    private Boolean isReconciled;
    private String bankReference;
    private String thirdPartyName;
    private LocalDate importedAt;
}