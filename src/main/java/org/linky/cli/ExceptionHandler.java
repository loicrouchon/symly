package org.linky.cli;

import lombok.RequiredArgsConstructor;
import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

@RequiredArgsConstructor
class ExceptionHandler implements IExecutionExceptionHandler {

    private final CliConsole console;

    @Override
    public int handleExecutionException(Exception ex, CommandLine commandLine, ParseResult parseResult) {
        console.eprintf("%n");
        if (ex instanceof LinkyExecutionException) {
            console.eprintf("%s%n", ex.getMessage());
            return 1;
        }
        console.eprintf("%s:%s%n", ex.getClass(), ex.getMessage());
        ex.printStackTrace(console.ewriter());
        return 0;
    }
}
