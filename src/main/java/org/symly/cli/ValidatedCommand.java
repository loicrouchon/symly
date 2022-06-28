package org.symly.cli;

import static picocli.CommandLine.Command;

import java.util.Collection;
import java.util.Collections;
import org.symly.cli.validation.Constraint;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command
abstract class ValidatedCommand implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public final void run() {
        constraints().forEach(validator -> validator.violation().ifPresent(this::throwViolation));
        execute();
    }

    protected Collection<Constraint> constraints() {
        return Collections.emptyList();
    }

    private void throwViolation(String violation) {
        throw new CommandLine.ParameterException(spec.commandLine(), violation);
    }

    protected abstract void execute();
}
