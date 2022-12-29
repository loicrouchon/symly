import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class GitRepository {

    protected final String remote;
    protected final String mainBranch;
    protected final String releaseBranchPrefix;
    protected final Pattern releaseBranchPattern;
    protected String currentBranch;

    protected GitRepository(String remote, String mainBranch, String releaseBranchPrefix) {
        this.remote = remote;
        this.mainBranch = mainBranch;
        this.releaseBranchPrefix = releaseBranchPrefix;
        this.releaseBranchPattern = Pattern.compile("^" + releaseBranchPrefix + "/([0-9]+(:?\\.[0-9]+)*)$");
    }

    public String mainBranch() {
        return mainBranch;
    }

    public String currentBranch() {
        if (currentBranch == null) {
            currentBranch = Command.output("git", "rev-parse", "--verify", "--abbrev-ref", "HEAD");
        }
        return currentBranch;
    }

    public String remoteBranch() {
        return remote + "/" + currentBranch();
    }

    public boolean isMainBranch() {
        return Objects.equals(mainBranch, currentBranch());
    }

    public String releaseBranch(Version version) {
        return "%s/%s".formatted(releaseBranchPrefix, version);
    }

    public boolean isReleaseBranch() {
        return isReleaseBranch(currentBranch());
    }

    private boolean isReleaseBranch(String branch) {
        return releaseBranchPattern.matcher(branch).matches();
    }

    public Version releaseBranchNumber() {
        String branch = currentBranch();
        return releaseBranchNumber(branch);
    }

    private Version releaseBranchNumber(String branch) {
        Matcher matcher = releaseBranchPattern.matcher(branch);
        if (!matcher.matches()) {
            throw new IllegalStateException("%s is not a release branch".formatted(branch));
        }
        return Version.parse(matcher.group(1));
    }

    public abstract void switchToBranch(String branch);

    public void createBranchAndSwitch(String branch) {
        Command.exec("git", "branch", branch);
        switchToBranch(branch);
    }

    public void fetchRemote() {
        Command.exec("git", "fetch", "--tags", remote);
    }

    public String headOfLocalBranch() {
        return headCommit(currentBranch());
    }

    public String headOfRemoteBranch() {
        return headCommit(remoteBranch());
    }

    public String headCommit(String branchName) {
        return Command.output("git", "rev-parse", "--verify", "--short", branchName);
    }

    public Command status() {
        return Command.exec("git", "status", "--porcelain");
    }

    public abstract void commitAll(String message);

    public void pull() {
        Command.exec("git", "pull", "--set-upstream", remote, currentBranch());
    }

    public abstract void push();

    public Optional<Version> latestReleaseBranchVersion(Version baseVersion) {
        Command command = Command.exec("git", "branch", "-r", "--list", remote + "/*");
        return command.stdout().stream()
                .map(branch -> branch.trim().replaceFirst("^" + remote + "/", ""))
                .filter(this::isReleaseBranch)
                .map(this::releaseBranchNumber)
                .filter(version -> version.isSubVersion(baseVersion))
                .max(Version::compare);
    }

    public List<String> tags(Version baseVersion) {
        return Command.exec("git", "tag", "-l", "v%s.*".formatted(baseVersion)).stdout();
    }

    public static GitRepository create(String remote, String mainBranch, String releaseBranchPrefix) {
        return new ReadWriteGitRepository(remote, mainBranch, releaseBranchPrefix);
    }

    public static GitRepository createDryRun(String remote, String mainBranch, String releaseBranchPrefix) {
        return new ReadOnlyGitRepository(remote, mainBranch, releaseBranchPrefix);
    }
}

class ReadWriteGitRepository extends GitRepository {

    public ReadWriteGitRepository(String remote, String mainBranch, String releaseBranchPrefix) {
        super(remote, mainBranch, releaseBranchPrefix);
    }

    @Override
    public void switchToBranch(String branch) {
        Command.exec("git", "switch", branch);
        currentBranch = null;
    }

    @Override
    public void commitAll(String message) {
        Command.exec("git", "add", ".");
        Command.exec("git", "commit", "-m", message);
    }

    @Override
    public void push() {
        Command.exec("git", "push", "--set-upstream", remote, currentBranch());
    }
}

class ReadOnlyGitRepository extends GitRepository {

    public ReadOnlyGitRepository(String remote, String mainBranch, String releaseBranchPrefix) {
        super(remote, mainBranch, releaseBranchPrefix);
    }

    @Override
    public void switchToBranch(String branch) {
        currentBranch = branch;
    }

    @Override
    public void commitAll(String message) {
        // NO-OP
    }

    @Override
    public void push() {
        // NO-OP
    }
}
