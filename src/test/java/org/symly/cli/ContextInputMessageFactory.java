package org.symly.cli;

import java.util.Objects;
import org.symly.env.Env;

class ContextInputMessageFactory {

    protected final Env env;

    ContextInputMessageFactory(Env env) {
        this.env = Objects.requireNonNull(env);
    }

    public String mainDirectoryIsNotDefined() {
        return "Main directory is not defined";
    }

    public String repositoriesAreNotDefined() {
        return "Repositories are not defined";
    }

    public String mainDirectoryDoesNotExist(String path) {
        return "Main directory (%s) is not an existing directory".formatted(env.path(path));
    }

    public String repositoryDoesNotExist(String path) {
        return "Repository (%s) is not an existing directory".formatted(env.path(path));
    }

    public String maxDepthMustBePositive(int maxDepth) {
        return "Orphan lookup max-depth (%d) must be a positive integer".formatted(maxDepth);
    }
}
