package org.linky.cli;

import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

class ExceptionHandler implements IExecutionExceptionHandler {

    @Override
    public int handleExecutionException(Exception ex, CommandLine commandLine, ParseResult parseResult) {
        CliConsole console = CliConsole.console();
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
