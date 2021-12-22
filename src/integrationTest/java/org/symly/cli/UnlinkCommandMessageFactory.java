package org.symly.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.env.Env;

@RequiredArgsConstructor
class UnlinkCommandMessageFactory {

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

    public String unlink(String to, List<String> from) {
        return String.format(
                "Removing links in %s to [%s]",
                env.path(to),
                from.stream()
                        .map(env::path)
                        .map(Path::toString)
                        .collect(Collectors.joining(", "))
        );
    }

    public String actionUnlink(String from, String to) {
        return action("DELETE", from, to);
    }

    public String action(String action, String from, String to) {
        return String.format("%s %s -> %s", padAction(action), env.path(from), env.path(to));
    }

    private String padAction(String action) {
        return String.format("%-10s", action);
    }
}
