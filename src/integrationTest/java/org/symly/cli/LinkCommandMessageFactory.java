package org.symly.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.env.Env;

@RequiredArgsConstructor
class LinkCommandMessageFactory {

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

    public String maxDepthMustBePositive(int maxDepth) {
        return String.format("Argument <max-depth> (%d): must be a positive integer", maxDepth);
    }

    public String creatingLinks(String to, List<String> from) {
        return String.format(
                "Creating links in %s to [%s]",
                env.path(to),
                from.stream()
                        .map(env::path)
                        .map(Path::toString)
                        .collect(Collectors.joining(", "))
        );
    }

    public String linkActionCreate(String from, String to) {
        return action("CREATE", from, to);
    }

    public String linkActionConflict(String from, String to) {
        return action("CONFLICT", from, to);
    }

    public List<String> linkActionUpdate(String from, String to, String previousTo) {
        return List.of(
                action("UPDATE", from, to),
                String.format("> Previous link target was %s", env.path(previousTo))
        );
    }

    public String linkActionUpToDate(String from, String to) {
        return action("UP_TO_DATE", from, to);
    }

    public String linkActionDelete(String from, String to) {
        return action("DELETE", from, to);
    }

    public String linkActionDelete(String from) {
        return String.format("%s %s", padAction("DELETE"), env.path(from));
    }

    public String action(String action, String from, String to) {
        return String.format("%s %s -> %s", padAction(action), env.path(from), env.path(to));
    }

    private String padAction(String action) {
        return String.format("%-10s", action);
    }

    public List<String> cannotCreateLinkError(String from, String to) {
        return List.of(
                String.format("Unable to create link %s -> %s", env.path(from), env.path(to)),
                String.format(
                        "> Regular file %s already exist. To overwrite it, use the --replace-file option.",
                        env.path(from))
        );
    }
}
