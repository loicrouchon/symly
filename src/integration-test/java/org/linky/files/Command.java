package org.linky.files;

import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.linky.cli.Execution;

public class Command {

    public static Execution run(
            Path workingDirectory,
            Map<String, String> systemProperties,
            String[] args,
            long timeout) {
        try {
            Process process = process(workingDirectory, systemProperties, args);
            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                fail("Process did not finish in time");
            }
            return new Execution(process);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Process process(Path workingDirectory, Map<String, String> systemProperties, String[] args)
            throws IOException {
        List<String> command = command(systemProperties, args);
        return new ProcessBuilder()
                .directory(workingDirectory.toFile())
                .command(command)
                .start();
    }

    private static List<String> command(Map<String, String> systemProperties, String[] args) {
        String java = javaBinary();
        String classpath = classpath();
        String mainClass = "org.linky.cli.Main";
        List<String> command = new ArrayList<>();
        command.add(java);
        systemProperties.forEach((key, value) -> command.add(String.format("-D%s=%s", key, value)));
        command.add("-cp");
        command.add(classpath);
        command.add(mainClass);
        command.addAll(Arrays.asList(args));
        return command;
    }

    private static String javaBinary() {
        return String.format("%s/bin/java", System.getProperty("java.home"));
    }

    private static String classpath() {
        return Path.of("build/libs/*").toAbsolutePath().toString();
    }
}
