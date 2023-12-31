package releaser;

import static releaser.Color.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ReleaseCommand {
    private static final String PADDING = "%-32s";

    private final IO io;
    private final GitRepository repo;
    private final Application application;

    public ReleaseCommand(IO io, GitRepository repo, Application application) {
        this.io = io;
        this.repo = repo;
        this.application = application;
    }

    private String pad(String message) {
        return String.format(PADDING, message);
    }

    void preReleaseChecks() {
        io.println(ACTION.str("Fetching all branches and tags from remote..."));
        repo.fetchRemote();
        pull();
        io.printf(
                        "%s %s - %s%n",
                        pad("Current branch:"), INFO.str(repo.headOfLocalBranch()), INFO.str(repo.currentBranch()))
                .printf(
                        "%s %s - %s%n",
                        pad("Remote branch:"), INFO.str(repo.headOfRemoteBranch()), INFO.str(repo.remoteBranch()));
        String branchType;
        if (repo.isMainBranch()) {
            branchType = "main";
        } else if (repo.isReleaseBranch()) {
            branchType = "release";
        } else {
            branchType = "?";
        }
        io.printf("%s %s%n", pad("Branch type:"), INFO.str(branchType));
        if (repo.isMainBranch()) {
            if (!Objects.equals(repo.headOfLocalBranch(), repo.headOfRemoteBranch())) {
                throw new ReleaseException(
                        """
                    Release from the main branch can only be triggered if the main branch commits \
                    have been sync with the remote.
                    Please pull and push to the remote first.""");
            }
        } else if (!repo.isReleaseBranch()) {
            throw new ReleaseException(
                    """
            Release can only be triggered from the main branch or from a release branch.
            Current branch is %s
            Use:
                git switch %s"""
                            .formatted(repo.currentBranch(), repo.mainBranch()));
        }
        checkForUncommittedChanges();
    }

    private void checkForUncommittedChanges() {
        io.println(ACTION.str("Testing application..."));
        application.test();
        io.println(ACTION.str("Checking for uncommitted changes"));
        Command status = repo.status();
        if (!status.stdout().isEmpty()) {
            String choice = io.readChoice(
                    List.of(new Choice("y", "Proceed with uncommitted changes%n"), new Choice("n", "Abort release%n")),
                    """
                        There are uncommitted changes. Would you like to continue the release?

                        %s%n""",
                    COMMENT.str(status.stdOutAsString()));
            if (!Objects.equals(choice, "y")) {
                throw new ReleaseException(
                        """
                There are uncommitted changes, commit them before releasing:
                %s"""
                                .formatted(status.stdOutAsString()));
            }
        }
    }

    void release() {
        preReleaseChecks();
        if (repo.isMainBranch()) {
            Version baseVersion = application.version();
            io.printf("%s %s%n", pad("Current base version:"), INFO.str(baseVersion));
            Version nextBaseVersion = io.readLine(
                            "Enter next base version (%s to keep %s, %s to abort) ",
                            CHOICE.str("[Enter]"), INFO.str(baseVersion), ERROR.str("[ctrl+c]"))
                    .map(Version::parse)
                    .orElse(baseVersion);
            if (!Objects.equals(baseVersion, nextBaseVersion)) {
                bumpVersion(nextBaseVersion.suffix("SNAPSHOT"));
                repo.push();
            } else {
                io.printf("%s %s%n", pad("Using base version:"), INFO.str(nextBaseVersion));
            }
            Optional<Version> latestReleaseBranchVersion = repo.latestReleaseBranchVersion(nextBaseVersion);
            latestReleaseBranchVersion.ifPresentOrElse(
                    this::handleExistingCompatibleReleaseBranch, () -> handleNonExistingReleaseBranch(nextBaseVersion));
            performReleaseFromReleaseBranch();
        } else if (repo.isReleaseBranch()) {
            performReleaseFromReleaseBranch();
        } else {
            throw new ReleaseException(
                    """
        Release can only be triggered from the main branch or from a release branch.
        Current branch is %s
        Use:
            git switch %s"""
                            .formatted(repo.currentBranch(), repo.mainBranch()));
        }
    }

    private void handleExistingCompatibleReleaseBranch(Version version) {
        String latestReleaseBranch = repo.releaseBranch(version);
        Version incrementedVersion = version.increment();
        String incrementedVersionBranchName = repo.releaseBranch(incrementedVersion);
        io.printf("Latest release branch is %s%n", INFO.str(latestReleaseBranch));
        List<Choice> choices = List.of(
                new Choice(
                        "1",
                        "Continue with %s, next version will be %s%n"
                                .formatted(INFO.str(latestReleaseBranch), INFO.str(next(version)))),
                new Choice(
                        "2",
                        "Start with %s (bump), next version will be %s%n"
                                .formatted(
                                        INFO.str(incrementedVersionBranchName), INFO.str(next(incrementedVersion)))));
        String choice =
                io.readChoice(choices, "Re-use (%s) or Bump (%s)? ".formatted(CHOICE.str("1"), CHOICE.str("2")));
        switch (choice) {
            case "1" -> {
                repo.switchToBranch(latestReleaseBranch);
                io.printf("Merging %s into %s", INFO.str(repo.mainBranch()), INFO.str(latestReleaseBranch));
                repo.mergeBranch(repo.mainBranch());
            }
            case "2" -> repo.createBranchAndSwitch(incrementedVersionBranchName);
            default -> throw new IllegalStateException("Unreachable statement");
        }
        pull();
    }

    private void pull() {
        io.printf(ACTION.str("Pulling branch %s...%n".formatted(repo.currentBranch())));
        if (repo.remoteBranchExists()) {
            repo.pull();
        }
    }

    private void handleNonExistingReleaseBranch(Version nextBaseVersion) {
        Version nextVersion = nextBaseVersion.withoutSuffix();
        String branch = repo.releaseBranch(nextVersion);
        repo.createBranchAndSwitch(branch);
    }

    private Version next(Version baseVersion) {
        return repo.latestTaggedVersionForBaseVersion(baseVersion)
                .map(Version::increment)
                .orElse(baseVersion.subversion("0"));
    }

    private void performReleaseFromReleaseBranch() {
        Version baseVersion = repo.releaseBranchNumber();
        io.printf("%s %s%n", pad("Release branch base version:"), INFO.str(baseVersion));
        Optional<Version> latestVersion = repo.latestTaggedVersionForBaseVersion(baseVersion);
        latestVersion.ifPresent(version ->
                io.printf("%s %s%n", pad("Latest tag for release %s is:".formatted(baseVersion)), INFO.str(version)));
        Version nextVersion = latestVersion.map(Version::increment).orElse(baseVersion.subversion("0"));
        bumpVersion(nextVersion);
        Command.exec(new ProcessBuilder()
                .command("tools/releaser/src/main/resources/jreleaser-dry-run.sh", nextVersion.toString())
                .inheritIO());
        io.printf("Push the release to the main repository?%n");
        String choice = io.readChoice(
                List.of(
                        new Choice("y", "Proceed with release%n".formatted()),
                        new Choice("n", "Abort release%n".formatted())),
                "Proceed (%s) or Abort (%s) the release? ".formatted(CHOICE.str("y"), CHOICE.str("n")));
        if (Objects.equals(choice, "y")) {
            repo.push();
            io.printf("Release pushed to the remote. Release pipeline should start automatically.");
        }
    }

    private void bumpVersion(Version version) {
        io.printf("%s %s%n", ACTION.str(String.format(PADDING, "Bumping version to: ")), GREEN.str(version));
        application.updateVersion(version);
        repo.commitAll("release: Bump version to %s".formatted(version));
    }
}
