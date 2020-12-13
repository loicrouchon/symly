package org.linky.cli;

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;

@RequiredArgsConstructor(staticName = "of")
public class Arg {

    private final CommandLine.Model.CommandSpec spec;
    private final String name;

    public <T> void validate(T value, Validator<T> validator) {
        validator.validate(value).ifPresent(this::throwViolation);
    }

    public <T> void validate(Collection<T> values, Validator<T> validator) {
        values.forEach(value -> validate(value, validator));
    }

    public void throwViolation(String violation) {
        throw new CommandLine.ParameterException(
                spec.commandLine(),
                String.format("Argument <%s>: %s", name, violation));
    }
}
