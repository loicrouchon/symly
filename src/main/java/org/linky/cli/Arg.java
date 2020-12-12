package org.linky.cli;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;

@RequiredArgsConstructor(staticName = "of")
public class Arg {

    private final CommandLine.Model.CommandSpec spec;
    private final String name;

    public <T> void validate(Function<T, Optional<String>> validator, T value) {
        validator.apply(value).ifPresent(error -> {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    String.format("%s: %s (%s)", name, error, value));
        });
    }

    public <T> void validate(Function<T, Optional<String>> validator, Collection<T> values) {
        values.forEach(value -> validate(validator, value));
    }
}
