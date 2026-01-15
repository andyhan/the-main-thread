package com.secure.text.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
@Constraint(validatedBy = SafeTextValidator.class)
@Documented
public @interface SafeText {

    String message() default "Input contains invalid or dangerous Unicode characters";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}