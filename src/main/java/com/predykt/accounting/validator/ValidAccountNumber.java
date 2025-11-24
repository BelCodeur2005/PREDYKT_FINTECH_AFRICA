// Annotation
package com.predykt.accounting.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AccountNumberValidator.class)
@Documented
public @interface ValidAccountNumber {
    String message() default "Numéro de compte OHADA invalide";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}