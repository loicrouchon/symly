import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

abstract class Application {

    protected static final Pattern VERSION_PATTERN = Pattern.compile("^version\\s*=\\s*(\\S+)\\s*$");

    protected final Path path = Path.of("gradle.properties");

    protected String content;

    protected Application() {
        refresh();
    }

    public void refresh() {
        try {
            this.content = Files.readString(path);
        } catch (IOException e) {
            throw new ReleaseFailure("Unable to read Gradle property file " + path.toAbsolutePath(), e);
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
        writeApplicationProperties();
        updateFilesAfterVersionUpdate();
    }

    protected abstract void writeApplicationProperties();

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
    protected void writeApplicationProperties() {
        try {
            Files.writeString(path, content);
        } catch (IOException e) {
            throw new ReleaseFailure("Unable to write to Gradle property file " + path.toAbsolutePath(), e);
        }
    }
}

class ReadOnlyApplication extends Application {

    @Override
    protected void writeApplicationProperties() {
        // NO-OP
    }
}
