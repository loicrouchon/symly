package releaser;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

class GitRepository {

    private final Git git;
    protected final BranchingModel branchingModel;
    protected String currentBranch;

    protected GitRepository(Git git, BranchingModel branchingModel) {
        this.git = git;
        this.branchingModel = branchingModel;
    }

    public String mainBranch() {
        return branchingModel.mainBranch();
    }

    public String currentBranch() {
        if (currentBranch == null) {
            currentBranch = git.currentBranch();
        }
        return currentBranch;
    }

    public String remoteBranch() {
        return branchingModel.remote() + "/" + currentBranch();
    }

    public boolean remoteBranchExists() {
        return git.branchExists(remoteBranch());
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

    public void switchToBranch(String branch) {
        git.switchToBranch(branch);
        currentBranch = null;
    }

    public void createBranchAndSwitch(String branch) {
        if (git.branchExists(branch)) {
            throw new ReleaseException(
                    "Branch %s already exists. Please ensure it can be deleted and resume when done".formatted(branch));
        }
        git.createBranch(branch);
        switchToBranch(branch);
    }

    public void fetchRemote() {
        git.fetchTags(branchingModel.remote());
    }

    public String headOfLocalBranch() {
        return headCommit(currentBranch());
    }

    public String headOfRemoteBranch() {
        return headCommit(remoteBranch());
    }

    public String headCommit(String branchName) {
        return git.headCommit(branchName);
    }

    public Command status() {
        return git.status();
    }

    public void pull() {
        git.pull(branchingModel.remote(), currentBranch());
    }

    public void commitAll(String message) {
        git.commitAll(message);
    }

    public void push() {
        git.push(branchingModel.remote(), currentBranch());
    }

    public Optional<Version> latestReleaseBranchVersion(Version baseVersion) {
        return git.branches(branchingModel.remote() + "/*")
                .map(branch -> branch.replaceFirst("^" + branchingModel.remote() + "/", ""))
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
        return git.tags(baseVersion).stdout();
    }

    public static GitRepository create(BranchingModel branchingModel) {
        return new GitRepository(new Git.ReadWriteGit(Path.of(".")), branchingModel);
    }

    public static GitRepository createDryRun(BranchingModel branchingModel) {
        return new GitRepository(new Git.ReadOnlyGit(Path.of(".")), branchingModel);
    }
}
