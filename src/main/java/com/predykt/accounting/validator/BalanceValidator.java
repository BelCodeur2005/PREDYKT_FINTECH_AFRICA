// ============================================
// BalanceValidator.java
// ============================================
package com.predykt.accounting.validator;

import com.predykt.accounting.dto.request.JournalEntryRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class BalanceValidator implements ConstraintValidator<BalancedEntry, JournalEntryRequest> {
    
    @Override
    public boolean isValid(JournalEntryRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getLines() == null || request.getLines().isEmpty()) {
            return false;
        }
        
        BigDecimal totalDebit = request.getLines().stream()
            .map(line -> line.getDebitAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCredit = request.getLines().stream()
            .map(line -> line.getCreditAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalDebit.compareTo(totalCredit) == 0;
    }
}

