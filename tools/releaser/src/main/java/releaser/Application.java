package releaser;

public abstract class Application {

    protected String version;

    protected Application() {
        refresh();
    }

    public void refresh() {
        this.version = Command.output("./mvnw", "help:evaluate", "-Dexpression=project.version", "-q", "-DforceStdout")
                .trim();
    }

    public Version version() {
        return Version.parse(version);
    }

    public void updateVersion(Version newVersion) {
        version = newVersion.toString();
        writeVersion();
        updateFilesAfterVersionUpdate();
    }

    protected abstract void writeVersion();

    private void updateFilesAfterVersionUpdate() {
        test();
    }

    public void test() {
        Command.exec(
                new ProcessBuilder().command("make", "codegen", "build-local").inheritIO());
    }

    public static Application create() {
        return new ReadWriteApplication();
    }

    public static Application createDryRun() {
        return new ReadOnlyApplication();
    }
}

class ReadWriteApplication extends Application {

    @Override
    protected void writeVersion() {
        Command.exec(new ProcessBuilder()
                .command("./mvnw", "versions:set", "-DnewVersion=" + version)
                .inheritIO());
    }
}

class ReadOnlyApplication extends Application {
    @Override
    protected void writeVersion() {
        // NO-OP
    }
}
