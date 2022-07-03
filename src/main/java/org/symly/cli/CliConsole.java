package org.symly.cli;

import java.io.PrintWriter;
import java.lang.System.Logger.Level;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CliConsole {

    private final PrintWriter out;
    private final PrintWriter err;

    private Level currentLevel = Level.INFO;

    public void enableVerboseMode() {
        currentLevel = Level.DEBUG;
    }

    public void printf(String format, Object... args) {
        printf(Level.INFO, format, args);
    }

    public void printf(Level level, String format, Object... args) {
        if (shouldPrintForLevel(level)) {
            out.printf(format, args);
            out.flush();
        }
    }

    public void eprintf(String format, Object... args) {
        eprintf(Level.ERROR, format, args);
    }

    public void eprintf(Level level, String format, Object... args) {
        if (level.compareTo(Level.WARNING) < 0) {
            throw new IllegalArgumentException("Only WARN+ messages should be written on the standard error output");
        }
        if (shouldPrintForLevel(level)) {
            err.printf(format, args);
            err.flush();
        }
    }

    private boolean shouldPrintForLevel(Level level) {
        return level.compareTo(currentLevel) >= 0;
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
