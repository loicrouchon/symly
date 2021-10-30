package org.symly.env;

import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.files.FileTree;

@RequiredArgsConstructor
public class Command {

    private static final String JAVA_BINARY = String.format("%s/bin/java", System.getProperty("java.home"));
    private static final List<String> JVM_OPTIONS = List.of(
            "-XX:TieredStopAtLevel=1",
            "-Xmx8m",
            "-XX:+ShowCodeDetailsInExceptionMessages"
        );
    private static final String CLASSPATH_SYSTEM_PROPERTY = "symly.runtime.classpath";
    private static final String MAIN_CLASS = "org.symly.cli.Main";

    @NonNull
    private final Path rootDir;
    @NonNull
    private final Path workingDir;
    @NonNull
    private final Map<String, String> systemProperties;

    public Execution run(String[] args, long timeout) {
        FileTree rootFileTreeSnapshot = FileTree.fromPath(rootDir);
        List<String> command = command(systemProperties, args);
        try {
            Process process = new ProcessBuilder()
                    .directory(workingDir.toFile())
                    .command(command)
                    .start();
            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                fail(commandFailureMessage("Command did not finish in time", command));
            }
            return Execution.of(rootFileTreeSnapshot, rootDir, workingDir, process);
        } catch (InterruptedException | IOException e) {
            fail(commandFailureMessage("Command execution failed with: " + e.getMessage(), command));
            throw new IllegalStateException("unreachable");
        }
    }

    private List<String> command(Map<String, String> systemProperties, String[] args) {
        List<String> command = new ArrayList<>();
        command.add(JAVA_BINARY);
        command.addAll(JVM_OPTIONS);
        systemProperties.forEach((key, value) -> command.add(String.format("-D%s=%s", key, value)));
        jacocoAgent().ifPresent(command::add);
        command.add("-cp");
        command.add(System.getProperty(CLASSPATH_SYSTEM_PROPERTY));
        command.add(MAIN_CLASS);
        command.addAll(Arrays.asList(args));
        return command;
    }

    private Optional<String> jacocoAgent() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        List<String> args = runtimeMXBean.getInputArguments();
        return args.stream()
                .filter(arg -> arg.startsWith("-javaagent") && arg.contains("jacoco"))
                .findFirst()
                .map(this::toAbsolutePath);
    }

    private String toAbsolutePath(String jacocoAgent) {
        Path buildPath = Paths.get("build").toAbsolutePath();
        return jacocoAgent
                .replace("-javaagent:build", "-javaagent:" + buildPath)
                .replace("=destfile=build", "=destfile=" + buildPath);
    }

    private String commandFailureMessage(String message, List<String> command) {
        return String.format(
                "%s%n\tworking directory:%n\t\t%s%n\tCommand:%n\t\t%s",
                message, workingDir, command);
    }
}
