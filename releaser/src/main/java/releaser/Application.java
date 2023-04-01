package releaser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

abstract class Application {

    protected final Path path = Path.of("gradle/version.txt");

    protected String version;

    protected Application() {
        refresh();
    }

    public void refresh() {
        try {
            this.version = Files.readString(path).trim();
        } catch (IOException e) {
            throw new ReleaseFailure("Unable to read application version " + path.toAbsolutePath(), e);
        }
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
        Command.exec(new ProcessBuilder()
                .command("./gradlew", "clean", "build", "updateDocSnippets")
                .inheritIO());
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
        try {
            Files.writeString(path, version);
        } catch (IOException e) {
            throw new ReleaseFailure("Unable to write to Gradle property file " + path.toAbsolutePath(), e);
        }
    }
}

class ReadOnlyApplication extends Application {
    @Override
    protected void writeVersion() {
        // NO-OP
    }
}
