package uk.ac.warwick.tabula.validators;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy=WithinYearsValidatorForReadableInstant.class)
public @interface WithinYears {

	int maxFuture() default Integer.MAX_VALUE;

	int maxPast() default Integer.MAX_VALUE;

	Class<?>[] groups() default { };

	Class<? extends Payload>[] payload() default {};

	String message() default "{uk.ac.warwick.tabula.validators.WithinYears.message}";

}
