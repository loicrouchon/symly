package org.symly.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.env.Env;

@RequiredArgsConstructor
public class StatusCommandMessageFactory {

    @NonNull
    private final Env env;

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
