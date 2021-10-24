package org.symly.cli;

import lombok.RequiredArgsConstructor;
import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

@RequiredArgsConstructor
class ExceptionHandler implements IExecutionExceptionHandler {

    private final Config config;
    private final CliConsole console;

    @Override
    public int handleExecutionException(Exception e, CommandLine commandLine, ParseResult parseResult) {
        console.eprintf("%n");
        if (e.getMessage() != null) {
            console.eprintf("%s%n", e.getMessage());
        }
        if (e instanceof SymlyExecutionException) {
            if (config.verbose()) {
                e.printStackTrace(console.ewriter());
            }
            return 1;
        }
        e.printStackTrace(console.ewriter());
        return 3;
    }
}
