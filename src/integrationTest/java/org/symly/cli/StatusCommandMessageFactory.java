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

    public String mainDirectoryIsNotDefined() {
        return "Main directory is not defined";
    }

    public String repositoriesAreNotDefined() {
        return "Repositories are not defined";
    }

    public String mainDirectoryDoesNotExist(String path) {
        return String.format("Main directory (%s) is not an existing directory", env.path(path));
    }

    public String repositoryDirectoryDoesNotExist(String path) {
        return String.format("Repository (%s) is not an existing directory", env.path(path));
    }

    public String checkingLinks(String mainDirectory, List<String> repositories) {
        return String.format(
                "Checking links status from %s to [%s]",
                env.path(mainDirectory),
                repositories.stream().map(env::path).map(Path::toString).collect(Collectors.joining(", ")));
    }
}
