package org.symly.validation;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

@RequiredArgsConstructor
public class Validator {

    @NonNull
    private final CommandSpec spec;

    private void validate(Constraint validator) {
        validator.violation().ifPresent(this::throwViolation);
    }

    public void validate(Constraint... constraints) {
        for (Constraint constraint : constraints) {
            validate(constraint);
        }
    }

    public void validate(Iterable<Constraint> constraints) {
        constraints.forEach(this::validate);
    }

    private void throwViolation(String violation) {
        throw violation(violation);
    }

    public ParameterException violation(String message, Object... args) {
        return new ParameterException(spec.commandLine(), message.formatted(args));
    }
}
