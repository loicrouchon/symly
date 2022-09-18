package org.symly.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.env.Env;

@RequiredArgsConstructor
class StatusCommandMessageFactory {

    @NonNull
    private final Env env;

    public String checkingLinks(String mainDirectory, List<String> repositories) {
        return "Checking links status from %s to [%s]"
                .formatted(
                        env.path(mainDirectory),
                        repositories.stream().map(env::path).map(Path::toString).collect(Collectors.joining(", ")));
    }
}
