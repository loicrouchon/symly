import static gui.Color.*;

import gui.Choice;
import gui.IO;
import java.util.*;
import java.util.function.Consumer;

public class Releaser {
    private static final String PADDING = "%-32s";

    private final IO io;
    private final GitRepository repo;
    private final Application application;

    public Releaser(IO io, GitRepository repo, Application application) {
        this.io = io;
        this.repo = repo;
        this.application = application;
    }

    public static void main(String[] args) {
        IO io = new IO();
        try {
            parse(io, new LinkedList<>(Arrays.asList(args)));
        } catch (ReleaseException e) {
            io.eprintln(e.getMessage());
        } catch (Exception e) {
            io.eprintln(e.getMessage(), e);
        }
    }

    private static void parse(IO io, Deque<String> args) {
        boolean dryRun = false;
        Consumer<Context> command = null;
        while (!args.isEmpty()) {
            String arg = args.pop();
            switch (arg) {
                case "-d", "--dry-run" -> dryRun = true;
                case "release" -> {
                    if (command == null) {
                        command = Releaser::release;
                    } else {
                        throw usage();
                    }
                }
                case "check" -> {
                    if (command == null) {
                        command = Releaser::check;
                    } else {
                        throw usage();
                    }
                }
                default -> throw usage();
            }
        }
        if (command == null) {
            throw usage();
        }
        command.accept(context(io, dryRun));
    }

    private static ReleaseException usage() {
        return new ReleaseException(
                """
    Usage: releaser [MODE] [-d|--dry-run]
      - MODE: release|check
         * release: starts a release (version bump and release trigger)
         * check: runs basic checks (branch type/version number consistency)
      -d, --dry-run: dry-run mode""");
    }

    private record Context(IO io, Application application, GitRepository repository) {}

    private static Context context(IO io, boolean dryRun) {
        GitRepository repository;
        Application application;
        if (dryRun) {
            application = Application.create();
            repository = GitRepository.create("origin", "main", "release");
        } else {
            application = Application.createDryRun();
            repository = GitRepository.createDryRun("origin", "main", "release");
        }
        return new Context(io, application, repository);
    }

    private static void release(Context context) {
        Releaser releaser = new Releaser(context.io, context.repository, context.application);
        releaser.preReleaseChecks();
        releaser.release();
    }

    private static void check(Context context) {
        ConsistencyChecker consistencyChecker = new ConsistencyChecker();
        consistencyChecker.check(context.repository, context.application);
    }

    private String pad(String message) {
        return String.format(PADDING, message);
    }

    private void preReleaseChecks() {
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
        } else if (repo.isReleaseBranch()) {
            // TODO check latest remote commit is present in local branch
        } else {
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

    private void release() {
        if (repo.isMainBranch()) {
            Version baseVersion = application.version();
            io.printf("%s %s%n", pad("Current base version:"), INFO.str(baseVersion));
            Version nextBaseVersion = io.readLine(
                            "Enter next base version (%s to keep %s, %s to abort) ",
                            CHOICE.str("[Enter]"), INFO.str(baseVersion), ERROR.str("[ctrl+c]"))
                    .map(Version::parse)
                    .orElse(baseVersion);
            if (!Objects.equals(baseVersion, nextBaseVersion)) {
                bumpVersion(nextBaseVersion.suffix("dev"));
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
                        "Start with %s, next version will be %s%n"
                                .formatted(
                                        INFO.str(incrementedVersionBranchName), INFO.str(next(incrementedVersion)))));
        String choice =
                io.readChoice(choices, "Re-use (%s) or Bump (%s)? ".formatted(CHOICE.str("1"), CHOICE.str("2")));
        switch (choice) {
            case "1" -> repo.switchToBranch(latestReleaseBranch);
            case "2" -> repo.createBranchAndSwitch(incrementedVersionBranchName);
            default -> throw new IllegalStateException("Unreachable statement");
        }
        pull();
    }

    private void pull() {
        io.printf(ACTION.str("Pulling branch %s...%n".formatted(repo.currentBranch())));
        repo.pull();
    }

    private void handleNonExistingReleaseBranch(Version nextBaseVersion) {
        Version nextVersion = nextBaseVersion.subversion("0").withoutSuffix();
        String branch = repo.releaseBranch(nextVersion);
        repo.createBranchAndSwitch(branch);
    }

    private Version next(Version baseVersion) {
        return repo.tags(baseVersion).stream()
                .map(tag -> tag.replaceFirst("^v", ""))
                .max(Version::compare)
                .map(Version::parse)
                .map(Version::increment)
                .orElse(baseVersion.subversion("0"));
    }

    private void performReleaseFromReleaseBranch() {
        Version baseVersion = repo.releaseBranchNumber();
        io.printf("%s %s%n", pad("Release branch base version:"), INFO.str(baseVersion));
        Optional<Version> latestVersion = repo.tags(baseVersion).stream()
                .map(tag -> tag.replaceFirst("^v", ""))
                .max(Version::compare)
                .map(Version::parse);
        latestVersion.ifPresent(version ->
                io.printf("%s %s%n", pad("Latest tag for release %s is:".formatted(baseVersion)), INFO.str(version)));
        Version nextVersion = latestVersion.map(Version::increment).orElse(baseVersion.subversion("0"));
        bumpVersion(nextVersion);
        Command.exec(new ProcessBuilder()
                .command("src/build-tools/jreleaser-dry-run.sh", nextVersion.toString())
                .inheritIO());
        io.printf("Push the release to the main repository?%n");
        String choice = io.readChoice(
                List.of(
                        new Choice("y", "Proceed with release%n".formatted()),
                        new Choice("n", "Abort release%n".formatted())),
                "Proceed (%s) or Abort (%s) the release? ".formatted(CHOICE.str("y"), CHOICE.str("n")));
        switch (choice) {
            case "y" -> {
                repo.push();
                io.printf("Release pushed to the remote. Release pipeline should start automatically.");
            }
            default -> {}
        }
    }

    private void bumpVersion(Version version) {
        io.printf("%s %s%n", ACTION.str(String.format(PADDING, "Bumping version to: ")), GREEN.str(version));
        application.updateVersion(version);
        repo.commitAll("release: Bump version to %s".formatted(version));
    }
}
