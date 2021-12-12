package org.symly.cli;

import java.io.PrintWriter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public
class CliConsole {

    private final PrintWriter out;
    private final PrintWriter err;

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

    public void flush() {
        err.flush();
        out.flush();
    }
}
