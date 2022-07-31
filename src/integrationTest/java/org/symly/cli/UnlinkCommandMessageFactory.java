package org.symly.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.env.Env;

@RequiredArgsConstructor
class UnlinkCommandMessageFactory {

    @NonNull
    private final Env env;

    public String mainDirectoryIsNotDefined() {
        return "Main directory is not defined";
    }

    public String repositoriesAreNotDefined() {
        return "Repositories are not defined";
    }

    public String mainDirectoryDoesNotExist(String path) {
        return String.format("Main directory (%s) is not an existing directory", env.path(path));
    }

    public String targetDirectoryDoesNotExist(String path) {
        return String.format("Repository (%s) is not an existing directory", env.path(path));
    }

    public String unlink(String to, List<String> from) {
        return String.format(
                "Removing links in %s to [%s]",
                env.path(to), from.stream().map(env::path).map(Path::toString).collect(Collectors.joining(", ")));
    }

    public String actionUnlink(String from, String to) {
        return action("unlink", from, to);
    }

    public String action(String action, String from, String to) {
        return String.format("%-12s %s -> %s", action + ":", from, env.path(to));
    }
}
