package releaser;

import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

abstract class Git {

    private final Path dir;

    protected Git(Path dir) {
        this.dir = dir;
    }

    public Path dir() {
        return dir;
    }

    public void clone(String remote) {
        gitExecCommand("git", "clone", remote, ".");
    }

    public void fetchTags(String remote) {
        gitExec("git", "fetch", "--tags", remote);
    }

    public String currentBranch() {
        return gitExecCommand("git", "branch", "--show-current").stdOutAsString();
    }

    public Stream<String> branches(String pattern) {
        return gitExecCommand("git", "branch", "--all", "--list", pattern).stdout().stream()
                .map(branch -> branch.trim().replaceFirst("^remotes/", ""));
    }

    public boolean branchExists(String branch) {
        return branches(branch).anyMatch(b -> Objects.equals(b, branch));
    }

    public Command createBranch(String branch) {
        return gitExecCommand("git", "branch", branch);
    }

    public abstract void switchToBranch(String branch);

    public String headCommit(String branchName) {
        return gitExecCommand("git", "rev-parse", "--verify", "--short", branchName)
                .stdOutAsString();
    }

    public Command tags(Version baseVersion) {
        return gitExecCommand("git", "tag", "-l", "v%s.*".formatted(baseVersion));
    }

    public Command status() {
        return gitExecCommand("git", "status", "--porcelain");
    }

    public abstract void commitAll(String message);

    public void pull(String remote, String branch) {
        gitExec("git", "pull", "--set-upstream", remote, branch);
    }

    public abstract void push(String repository);

    public abstract void push(String repository, String branch);

    protected Command gitExecCommand(String... command) {
        return Command.exec(new ProcessBuilder(command).directory(dir.toFile()));
    }

    protected void gitExec(String... command) {
        Command.exec(new ProcessBuilder(command).directory(dir.toFile()).inheritIO());
    }

    static class ReadWriteGit extends Git {

        protected ReadWriteGit(Path dir) {
            super(dir);
        }

        @Override
        public void switchToBranch(String branch) {
            gitExec("git", "switch", branch);
        }

        @Override
        public void commitAll(String message) {
            gitExec("git", "add", ".");
            gitExec("git", "commit", "-m", message);
        }

        @Override
        public void push(String repository) {
            gitExec("git", "push", repository);
        }

        @Override
        public void push(String repository, String branch) {
            gitExec("git", "push", "--set-upstream", repository, branch);
        }
    }

    static class ReadOnlyGit extends Git {

        protected ReadOnlyGit(Path dir) {
            super(dir);
        }

        @Override
        public void switchToBranch(String branch) {
            // NO-OP
        }

        @Override
        public void commitAll(String message) {
            // NO-OP
        }

        @Override
        public void push(String repository) {
            // NO-OP
        }

        @Override
        public void push(String repository, String branch) {
            // NO-OP
        }
    }
}
