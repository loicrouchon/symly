package releaser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

public record Command(List<String> command, int exitCode, List<String> stdout) {

    public boolean successful() {
        return exitCode == 0;
    }

    public Command throwIfFailed() {
        if (!successful()) {
            throw new IllegalStateException(
                    """
                Command %s failed with exit code %d
                %s"""
                            .formatted(command, exitCode, stdOutAsString()));
        }
        return this;
    }

    public String stdOutAsString() {
        return String.join("\n", stdout);
    }

    public static String output(String... command) {
        ProcessBuilder pb = new ProcessBuilder().command(command).redirectErrorStream(true);
        return exec(pb).stdOutAsString();
    }

    public static Command exec(String... command) {
        return exec(new ProcessBuilder().command(command).redirectErrorStream(true));
    }

    public static Command exec(ProcessBuilder pb) {
        List<String> command = pb.command();
        try {
            Process process = pb.start();
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroy();
            }
            int exitValue = process.exitValue();
            try (InputStream stdOut = process.getInputStream();
                    InputStreamReader isr = new InputStreamReader(stdOut);
                    BufferedReader br = new BufferedReader(isr)) {
                return new Command(command, exitValue, br.lines().toList()).throwIfFailed();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ReleaseFailure("Interrupted while executing: " + command, e);
        } catch (IOException e) {
            throw new ReleaseFailure("Command execution failed: " + command, e);
        }
    }
}
