package org.symly.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.symly.env.Env;

public class StatusCommandMessageFactory {

    private final Env env;

    public StatusCommandMessageFactory(Env env) {
        this.env = Objects.requireNonNull(env);
    }

    public String checkingLinks(String mainDirectory, List<String> repositories) {
        return "Checking links status from %s to [%s]"
                .formatted(
                        env.path(mainDirectory),
                        repositories.stream().map(env::path).map(Path::toString).collect(Collectors.joining(", ")));
    }

    public String missingLink(String from, String to) {
        return status("missing", from, to);
    }

    public String orphanLink(String from) {
        return status("orphan", from);
    }

    private String status(String status, String from, String to) {
        return "%-12s %s -> %s".formatted(status + ":", from, env.path(to));
    }

    private String status(String status, String from) {
        return "%-12s %s".formatted(status + ":", from);
    }
}
