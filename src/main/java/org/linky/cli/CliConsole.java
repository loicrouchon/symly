package org.linky.cli;

import java.io.PrintWriter;

class CliConsole {

    private static final CliConsole CLI_CONSOLE = new CliConsole();

    private final PrintWriter out;

    private CliConsole() {
        out = new PrintWriter(System.out);
    }

    public void printf(String format, Object... args) {
        out.printf(format, args);
        out.flush();
    }

    public PrintWriter writer() {
        return out;
    }

    public static CliConsole console() {
        return CLI_CONSOLE;
    }
}
