package com.braindribbler.recipe.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = DomainValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDomain {
    String message() default "Registration from this email domain is not permitted.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
