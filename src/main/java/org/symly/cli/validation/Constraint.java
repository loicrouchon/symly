package org.symly.cli.validation;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public interface Constraint {

    Optional<String> violation();

    static Constraint of(String constraint, BooleanSupplier validator) {
        return new SimpleConstraint(() -> constraint, validator);
    }

    static <T> Constraint ofArg(String name, T value, String reason, Predicate<T> validator) {
        return new SimpleConstraint(
                () -> "Argument <%s> (%s): %s".formatted(name, value, reason), () -> validator.test(value));
    }

    static <T> Constraint ofArg(String name, Collection<T> values, String reason, Predicate<T> validator) {
        return new Constraints(values.stream()
                .map(value -> ofArg(name, value, reason, validator))
                .toList());
    }
}
