package org.linky.cli;

import java.io.PrintWriter;
import java.io.StringWriter;

class TestConsole extends CliConsole {

    private final StringWriter out;
    private final StringWriter err;

    public TestConsole() {
        this(new StringWriter(), new StringWriter());
    }

    private TestConsole(StringWriter out, StringWriter err) {
        super(new PrintWriter(out), new PrintWriter(err));
        this.out = out;
        this.err = err;
    }

    public String stdOut() {
        return out.toString();
    }

    public String stdErr() {
        return err.toString();
    }
}
