package org.symly.cli;

import static picocli.CommandLine.Command;

import java.util.Collection;
import org.symly.cli.validation.Constraint;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command
abstract class ValidatedCommand implements Runnable {

    @Spec
    CommandSpec spec;

    protected void validate(Collection<Constraint> constraints) {
        constraints.forEach(validator -> validator.violation().ifPresent(this::throwViolation));
    }

    private void throwViolation(String violation) {
        throw new CommandLine.ParameterException(spec.commandLine(), violation);
    }
}
