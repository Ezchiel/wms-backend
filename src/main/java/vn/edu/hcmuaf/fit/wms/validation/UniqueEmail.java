package vn.edu.hcmuaf.fit.wms.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UniqueEmailValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueEmail {
    String message() default "Email đã tồn tại trong hệ thống";

    // two essential configurations for a Constraint in Spring
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
