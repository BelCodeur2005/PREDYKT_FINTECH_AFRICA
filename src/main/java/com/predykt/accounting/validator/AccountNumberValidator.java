// ============================================
// AccountNumberValidator.java
// ============================================
package com.predykt.accounting.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AccountNumberValidator implements ConstraintValidator<ValidAccountNumber, String> {
    
    @Override
    public boolean isValid(String accountNumber, ConstraintValidatorContext context) {
        if (accountNumber == null || accountNumber.isEmpty()) {
            return false;
        }
        
        // Validation OHADA: commence par 1-9, longueur 1-7
        return accountNumber.matches("^[1-9]\\d{0,6}$");
    }
}

