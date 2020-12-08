package org.linky.cli;

import java.io.PrintWriter;

class CliConsole {

    private static final CliConsole CLI_CONSOLE = new CliConsole();

    private final PrintWriter out;
    private final PrintWriter err;

    private CliConsole() {
        out = new PrintWriter(System.out);
        err = new PrintWriter(System.err);
    }

    public void printf(String format, Object... args) {
        out.printf(format, args);
        out.flush();
    }

    public void eprintf(String format, Object... args) {
        err.printf(format, args);
        err.flush();
    }

    public PrintWriter writer() {
        return out;
    }

    public PrintWriter ewriter() {
        return err;
    }

    public static CliConsole console() {
        return CLI_CONSOLE;
    }
}
