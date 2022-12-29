package releaser;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class Releaser {

    public static void main(String[] args) {
        IO io = new IO();
        try {
            new Cmd(io).parse(new LinkedList<>(List.of(args)));
        } catch (ReleaseException e) {
            io.eprintln(e.getMessage());
        } catch (Exception e) {
            io.eprintln(e.getMessage(), e);
        }
    }

    static void version(Cmd cmd) {
        cmd.io().printf("%s%n", cmd.application().version());
    }

    static void check(Cmd context) {
        new ConsistencyChecker().check(context.gitRepository(), context.application());
    }

    static void release(Cmd context) {
        String performRealRelease = context.io()
                .readChoice(
                        List.of(new Choice("1", "Real release%n"), new Choice("2", "Dry-run release%n")),
                        "Perform a real release?%n");
        if (!Objects.equals(performRealRelease, "1")) {
            context.enableDryRun();
        }
        new ReleaseCommand(context.io(), context.gitRepository(), context.application()).release();
    }
}

class Cmd {

    private final IO io;
    private boolean dryRun;
    private Consumer<Cmd> command;

    Cmd(IO io) {
        this.io = io;
    }

    public IO io() {
        return io;
    }

    public Application application() {
        if (dryRun) {
            return Application.createDryRun();
        }
        return Application.create();
    }

    public GitRepository gitRepository() {
        BranchingModel branchingModel = BranchingModel.of("origin", "main", "release");
        if (dryRun) {
            return GitRepository.createDryRun(branchingModel);
        }
        return GitRepository.create(branchingModel);
    }

    public void enableDryRun() {
        dryRun = true;
    }

    public void setCommand(Consumer<Cmd> newCommand) {
        if (command != null) {
            throw usage();
        }
        command = newCommand;
    }

    public void execute() {
        command.accept(this);
    }

    public void parse(Deque<String> args) {
        while (!args.isEmpty()) {
            String arg = args.pop();
            switch (arg) {
                case "check" -> setCommand(Releaser::check);
                case "version" -> setCommand(Releaser::version);
                case "release" -> setCommand(Releaser::release);
                default -> throw usage();
            }
        }
        execute();
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
}
