import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Releaser {

    private static final Color ERROR = Color.RED;
    private static final Color ACTION = Color.MAGENTA;
    private static final Color INFO = Color.YELLOW;
    private static final Color CHOICE = Color.CYAN;
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
            GitRepository repository = new GitRepository("origin", "main", "release");
            Releaser releaser = new Releaser(io, repository, new Application());
            releaser.preReleaseChecks();
            releaser.release();
        } catch (ReleaseException e) {
            io.eprintln(e.getMessage());
        } catch (Exception e) {
            io.eprintln(e.getMessage(), e);
        }
    }

    private String pad(String message) {
        return String.format(PADDING, message);
    }

    private String pad(String message, Color color) {
        return color.str(String.format(PADDING, message));
    }

    private void preReleaseChecks() {
        io.println(ACTION.str("Fetching remote..."));
        // TODO repo.fetchRemote();
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
        // TODO
        //        Command status = repo.status();
        //        if (!status.stdout().isEmpty()) {
        //            throw new ReleaseException(
        //                    """
        //            There are uncommitted changes, commit them before releasing:
        //            %s"""
        //                            .formatted(status.stdOutAsString()));
        //        }
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
                bumpVersion(nextBaseVersion);
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
                new Choice("1", "Latest release branch is %s%n".formatted(INFO.str(latestReleaseBranch))),
                new Choice("2", "Create release branch %s%n".formatted(INFO.str(incrementedVersionBranchName))));
        String choice =
                io.readChoice(choices, "Re-use (%s) or Bump (%s)? ".formatted(CHOICE.str("1"), CHOICE.str("2")));
        switch (choice) {
            case "1" -> repo.switchToBranch(latestReleaseBranch);
            case "2" -> repo.createBranchAndSwitch(incrementedVersionBranchName);
            default -> throw new IllegalStateException("");
        }
    }

    private void handleNonExistingReleaseBranch(Version nextBaseVersion) {
        Version nextVersion = nextBaseVersion.subversion("0");
        String branch = repo.releaseBranch(nextVersion);
        repo.createBranchAndSwitch(branch);
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
        io.printf("%s %s%n", pad("Bumping version to: ", ACTION), Color.GREEN.str(version));
        application.updateVersion(version);
        repo.commitAll("release: Bump version to %s".formatted(version));
    }

    private static class Application {
        private static Pattern VERSION_PATTERN = Pattern.compile("^version\s*=\s*([^\s]+)\s*$");

        private final Path path = Path.of("gradle.properties");

        private String content;

        public Application() {
            refresh();
        }

        public void refresh() {
            try {
                this.content = Files.readString(path);
            } catch (IOException e) {
                throw new RuntimeException("Unable to read Gradle property file " + path.toAbsolutePath(), e);
            }
        }

        public Version version() {
            return content.lines()
                    .flatMap(line -> {
                        Matcher matcher = VERSION_PATTERN.matcher(line);
                        if (matcher.matches()) {
                            return Stream.of(matcher.group(1));
                        }
                        return Stream.empty();
                    })
                    .findFirst()
                    .map(Version::parse)
                    .orElseThrow();
        }

        public void updateVersion(Version newVersion) {
            content = content.lines()
                    .map(line -> {
                        Matcher matcher = VERSION_PATTERN.matcher(line);
                        if (matcher.matches()) {
                            return "version = " + newVersion;
                        }
                        return line;
                    })
                    .collect(Collectors.joining("\n"));
            try {
                Files.writeString(path, content);
            } catch (IOException e) {
                throw new RuntimeException("Unable to write to Gradle property file " + path.toAbsolutePath(), e);
            }
            updateFilesAfterVersionUpdate();
        }

        private void updateFilesAfterVersionUpdate() {
            test();
        }

        public void test() {
            Command.exec(new ProcessBuilder()
                    .command("./gradlew", "clean", "build", "updateDocSnippets")
                    .inheritIO());
        }
    }

    private static record Version(String version, String suffix) {

        private static final String VERSION_SEPARATOR = ".";
        private static final String SUFFIX_SEPARATOR = "-";
        private static final String VERSION_SPLIT = "\\.";
        private static final Pattern VERSION_PATTERN = Pattern.compile("^[0-9]+(:?\\.[0-9]+)*(:?-[a-zA-Z0-9]+)?$");

        public Version subversion(String subversion) {
            return new Version(version + VERSION_SEPARATOR + subversion, suffix);
        }

        public boolean isSubVersion(Version baseVersion) {
            return Objects.equals(version, baseVersion.version) || version.startsWith(baseVersion.version + ".");
        }

        private Version increment() {
            String[] parts = version.split(VERSION_SPLIT);
            parts[parts.length - 1] = Integer.toString(Integer.parseInt(parts[parts.length - 1]) + 1);
            return new Version(String.join(VERSION_SEPARATOR, parts), suffix);
        }

        @Override
        public String toString() {
            if (suffix != null) {
                return version + SUFFIX_SEPARATOR + suffix;
            }
            return version;
        }

        public static Version parse(String fullVersion) {
            fullVersion = fullVersion.trim();
            if (!VERSION_PATTERN.matcher(fullVersion).matches()) {
                throw new ReleaseException(
                        """
                        Invalid version number: %s
                        Version numbers must follow the following pattern: %s"""
                                .formatted(fullVersion, VERSION_PATTERN.pattern()));
            }
            int suffixIndex = fullVersion.indexOf(SUFFIX_SEPARATOR);
            if (suffixIndex >= 0) {
                return new Version(fullVersion.substring(0, suffixIndex), fullVersion.substring(suffixIndex + 1));
            }
            return new Version(fullVersion, null);
        }

        public static int compare(Version a, Version b) {
            return compare(a.version, b.version);
        }

        public static int compare(String a, String b) {
            String[] av = a.split(VERSION_SPLIT);
            String[] bv = b.split(VERSION_SPLIT);
            for (int i = 0; i < Math.min(av.length, bv.length); i++) {
                if (!Objects.equals(av[i], bv[i])) {
                    return Integer.compare(Integer.parseInt(av[i]), Integer.parseInt(bv[i]));
                }
            }
            return Integer.compare(av.length, bv.length);
        }
    }

    private static class IO {

        final Console console = System.console();

        public IO printf(String message, Object... args) {
            console.printf(message, args);
            return this;
        }

        public IO println(Object message) {
            return printf("%s%n", message);
        }

        public IO eprintln(Object message) {
            color(ERROR);
            println(message);
            resetColor();
            return this;
        }

        public IO eprintln(Object message, Throwable t) {
            color(ERROR);
            println(message);
            t.printStackTrace(console.writer());
            resetColor();
            return this;
        }

        public IO color(Color color) {
            console.printf(color.code);
            return this;
        }

        public IO resetColor() {
            console.printf(Color.DEFAULT.code);
            return this;
        }

        public Optional<String> readLine(String message, Object... args) {
            String input = console.readLine("> " + message, args);
            if (input.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(input);
        }

        public String readChoice(List<Choice> choices, String message, Object... args) {
            Set<String> validChoices = choices.stream().map(Choice::key).collect(Collectors.toSet());
            String selection;
            do {
                choices.forEach(choice -> printf("  %s) %s", CHOICE.str(choice.key()), choice.message()));
                selection = console.readLine("> " + message, args);
            } while (!validChoices.contains(selection));
            return selection;
        }
    }

    private static record Choice(String key, String message) {}

    private static enum Color {
        RED("\033[31m"),
        GREEN("\033[32m"),
        YELLOW("\033[33m"),
        BLUE("\033[34m"),
        MAGENTA("\033[35m"),
        CYAN("\033[36m"),
        DEFAULT("\033[0m");
        final String code;

        Color(String code) {
            this.code = code;
        }

        public String str(Object value) {
            return "%s%s%s".formatted(code, value, DEFAULT.code);
        }

        public static String color(Color color, Object value) {
            return color.str(value);
        }
    }

    private static class GitRepository {

        private final String remote;
        private final String mainBranch;
        private final String releaseBranchPrefix;
        private final Pattern releaseBranchPattern;
        private String currentBranch;

        public GitRepository(String remote, String mainBranch, String releaseBranchPrefix) {
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

        public void switchToBranch(String branch) {
            // Command.exec("git", "switch", branch);
            // currentBranch = null;
            // TODO
            currentBranch = "release/0.8";
        }

        public void createBranchAndSwitch(String branch) {
            // Command.exec("git", "branch", branch);
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

        public void commitAll(String message) {
            // TODO
            //            Command.exec("git", "add", ".");
            //            Command.exec("git", "commit", "-m", message);
        }

        public void push() {
            // TODO
            //            Command.exec("git", "push", "--set-upstream", remote, currentBranch());
        }

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
            return Command.exec("git", "tag", "-l", "v%s.*".formatted(baseVersion))
                    .stdout();
        }
    }

    private static record Command(List<String> command, int exitCode, List<String> stdout) {

        public boolean successful() {
            return exitCode == 0;
        }

        public Command throwIfFailed() {
            if (!successful()) {
                throw new IllegalStateException("""
            Command %s failed with exit code %d
            %s"""
                        .formatted(command, exitCode, stdOutAsString()));
            }
            return this;
        }

        public String stdOutAsString() {
            return String.join("\n", stdout);
        }

        public void print(PrintStream pw) {
            pw.printf("Exit with %d%n", exitCode);
            stdout.forEach(pw::println);
        }

        public static String output(String... command) {
            ProcessBuilder pb = new ProcessBuilder().command(command).redirectErrorStream(true);
            return exec(pb).stdOutAsString();
        }

        public static Command exec(String... command) {
            return exec(new ProcessBuilder().command(command));
        }

        public static Command exec(ProcessBuilder pb) {
            List<String> command = pb.command();
            try {
                Process process = pb.start();
                boolean finished = process.waitFor(60, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroy();
                }
                int exitValue = process.exitValue();
                try (InputStream stdOut = process.getInputStream();
                        InputStreamReader isr = new InputStreamReader(stdOut);
                        BufferedReader br = new BufferedReader(isr); ) {
                    return new Command(command, exitValue, br.lines().toList()).throwIfFailed();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while executing: " + command, e);
            } catch (IOException e) {
                throw new RuntimeException("Command execution failed: " + command, e);
            }
        }
    }

    private static class ReleaseException extends RuntimeException {

        public ReleaseException(String message) {
            super(message);
        }
    }
}
