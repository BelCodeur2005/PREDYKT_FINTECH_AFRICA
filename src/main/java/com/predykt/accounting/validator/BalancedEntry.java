// Annotation
package com.predykt.accounting.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BalanceValidator.class)
@Documented
public @interface BalancedEntry {
    String message() default "L'écriture doit être équilibrée (Débit = Crédit)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
