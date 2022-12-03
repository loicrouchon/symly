package org.symly.validation;

import java.util.Optional;
import java.util.function.BooleanSupplier;

public interface Constraint {

    Optional<String> violation();

    static Constraint of(String constraint, BooleanSupplier validator) {
        return new SimpleConstraint(() -> constraint, validator);
    }
}
