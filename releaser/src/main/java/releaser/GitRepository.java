package releaser;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

abstract class GitRepository {

    protected final BranchingModel branchingModel;
    protected String currentBranch;

    protected GitRepository(BranchingModel branchingModel) {
        this.branchingModel = branchingModel;
    }

    public String mainBranch() {
        return branchingModel.mainBranch();
    }

    public String currentBranch() {
        if (currentBranch == null) {
            currentBranch = Command.output("git", "rev-parse", "--verify", "--abbrev-ref", "HEAD");
        }
        return currentBranch;
    }

    public String remoteBranch() {
        return branchingModel.remote() + "/" + currentBranch();
    }

    public boolean isMainBranch() {
        return Objects.equals(branchingModel.mainBranch(), currentBranch());
    }

    public String releaseBranch(Version version) {
        return "%s/%s".formatted(branchingModel.releaseBranchPrefix(), version);
    }

    public boolean isReleaseBranch() {
        return isReleaseBranch(currentBranch());
    }

    private boolean isReleaseBranch(String branch) {
        return branchingModel.isAReleaseBranch(branch);
    }

    public Version releaseBranchNumber() {
        String branch = currentBranch();
        return releaseBranchNumber(branch);
    }

    private Version releaseBranchNumber(String branch) {
        return branchingModel.releaseBranchNumber(branch);
    }

    public abstract void switchToBranch(String branch);

    public void createBranchAndSwitch(String branch) {
        Command.exec("git", "branch", branch);
        switchToBranch(branch);
    }

    public void fetchRemote() {
        Command.exec("git", "fetch", "--tags", branchingModel.remote());
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
        Command.exec("git", "pull", "--set-upstream", branchingModel.remote(), currentBranch());
    }

    public abstract void push();

    public Optional<Version> latestReleaseBranchVersion(Version baseVersion) {
        Command command = Command.exec("git", "branch", "-r", "--list", branchingModel.remote() + "/*");
        return command.stdout().stream()
                .map(branch -> branch.trim().replaceFirst("^" + branchingModel.remote() + "/", ""))
                .filter(this::isReleaseBranch)
                .map(this::releaseBranchNumber)
                .filter(version -> version.isSubVersion(baseVersion))
                .max(Version::compare);
    }

    public Optional<Version> latestTaggedVersionForBaseVersion(Version baseVersion) {
        return tags(baseVersion).stream()
                .map(tag -> tag.replaceFirst("^v", ""))
                .max(Version::compare)
                .map(Version::parse);
    }

    private List<String> tags(Version baseVersion) {
        return Command.exec("git", "tag", "-l", "v%s.*".formatted(baseVersion)).stdout();
    }

    public static GitRepository create(BranchingModel branchingModel) {
        return new ReadWriteGitRepository(branchingModel);
    }

    public static GitRepository createDryRun(BranchingModel branchingModel) {
        return new ReadOnlyGitRepository(branchingModel);
    }
}

class ReadWriteGitRepository extends GitRepository {

    public ReadWriteGitRepository(BranchingModel branchingModel) {
        super(branchingModel);
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
        Command.exec("git", "push", "--set-upstream", branchingModel.remote(), currentBranch());
    }
}

class ReadOnlyGitRepository extends GitRepository {

    public ReadOnlyGitRepository(BranchingModel branchingModel) {
        super(branchingModel);
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
