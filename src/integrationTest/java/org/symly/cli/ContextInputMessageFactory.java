package org.symly.cli;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.env.Env;

@RequiredArgsConstructor
class ContextInputMessageFactory {

    @NonNull
    protected final Env env;

    public String mainDirectoryIsNotDefined() {
        return "Main directory is not defined";
    }

    public String repositoriesAreNotDefined() {
        return "Repositories are not defined";
    }

    public String mainDirectoryDoesNotExist(String path) {
        return String.format("Main directory (%s) is not an existing directory", env.path(path));
    }

    public String repositoryDoesNotExist(String path) {
        return String.format("Repository (%s) is not an existing directory", env.path(path));
    }

    public String maxDepthMustBePositive(int maxDepth) {
        return String.format("Orphan lookup max-depth (%d) must be a positive integer", maxDepth);
    }
}
