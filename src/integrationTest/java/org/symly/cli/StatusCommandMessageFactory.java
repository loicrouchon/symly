package org.symly.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.env.Env;

@RequiredArgsConstructor
class StatusCommandMessageFactory {

    private static final String MISSING_REQUIRED_OPTION_TO = "Missing required option: '--to=<repositories>'";
    @NonNull
    private final Env env;

    public String missingTargetDirectories() {
        return MISSING_REQUIRED_OPTION_TO;
    }

    public String targetDirectoryDoesNotExist(String path) {
        return String.format("Argument <repositories> (%s): must be an existing directory", env.path(path));
    }

    public String mainDirectoryDoesNotExist(String path) {
        return String.format("Argument <main-directory> (%s): must be an existing directory", env.path(path));
    }

    public String checkingLinks(String mainDirectory, List<String> repositories) {
        return String.format(
            "Checking links status from %s to [%s]",
            env.path(mainDirectory),
            repositories.stream()
                .map(env::path)
                .map(Path::toString)
                .collect(Collectors.joining(", "))
        );
    }
}
