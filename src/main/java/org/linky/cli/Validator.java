package org.linky.cli;

import java.util.Arrays;
import java.util.Optional;

interface Validator<T> {

    Optional<String> validate(T value);

    @SafeVarargs
    static <T> Validator<T> combine(Validator<T>... validators) {
        return value -> Arrays.stream(validators)
                .flatMap(validator -> validator.validate(value).stream())
                .findFirst();
    }
}
