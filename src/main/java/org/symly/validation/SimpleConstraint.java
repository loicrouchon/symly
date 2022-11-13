package org.symly.validation;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

class SimpleConstraint implements Constraint {

    private final Supplier<String> reason;
    private final BooleanSupplier validator;

    SimpleConstraint(Supplier<String> reason, BooleanSupplier validator) {
        this.reason = reason;
        this.validator = validator;
    }

    @Override
    public Optional<String> violation() {
        if (!validator.getAsBoolean()) {
            return Optional.of(reason.get());
        }
        return Optional.empty();
    }
}
