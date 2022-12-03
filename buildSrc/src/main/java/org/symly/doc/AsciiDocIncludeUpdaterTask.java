package org.symly.doc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/**
 * <p>Execute after all tests are executed to process includes from asciidoc files.</p>
 * <p>This is unfortunately necessary as GitHub does not process asciidoc includes online and would otherwise only
 * display a link to the content.</p>
 */
public class AsciiDocIncludeUpdaterTask extends DefaultTask {

    @TaskAction
    public void run() {
        try (Stream<String> paths = Stream.of("README.adoc", "docs/")) {
            paths.map(Path::of)
                    .flatMap(AsciiDocIncludeUpdaterTask::walk)
                    .filter(path -> path.getFileName().toString().endsWith(".adoc"))
                    .sorted()
                    .map(AsciiDocFile::new)
                    .forEach(AsciiDocFile::processIncludes);
        }
    }

    private static Stream<Path> walk(Path path) {
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Path %s does not exists".formatted(path.toAbsolutePath()));
        }
        if (Files.isDirectory(path)) {
            try {
                return Files.walk(path).filter(Files::isRegularFile);
            } catch (IOException e) {
                throw new AsciiDocIncludeException(e);
            }
        }
        return Stream.of(path);
    }
}

class AsciiDocFile {

    private static final String INCLUDE_START = "// include::";
    private static final String BRACKETS = "[]";
    private static final String INCLUDE_END = "// end::include";

    private final Path path;

    AsciiDocFile(Path path) {
        this.path = path;
    }

    public void processIncludes() {
        try {
            String content = Files.readString(path);
            int index = 0;
            while ((index = content.indexOf(INCLUDE_START, index)) >= 0) {
                int bracketIndex = mandatoryIndex(content, BRACKETS, index);
                String includeId = content.substring(index + INCLUDE_START.length(), bracketIndex);
                if (includeId.indexOf('\n') >= 0) {
                    throw new IllegalStateException("include id at %s should not contain line breaks but was:%n%n%s%n"
                            .formatted(location(content, index), includeId));
                }
                int startIndex = bracketIndex + BRACKETS.length();
                int endIndex = mandatoryIndex(content, INCLUDE_END, startIndex);
                String value = readIncludeContent(content, startIndex, includeId);
                content = applyInclude(content, startIndex, endIndex, value);
                index = endIndex;
            }
            Files.writeString(path, content);
        } catch (IOException e) {
            throw new AsciiDocIncludeException("Unable to process Asciidoc file includes. %s".formatted(e.getMessage()));
        }
    }

    private String readIncludeContent(String content, int index, String includeId) throws IOException {
        Path includePath = Path.of(includeId);
        try {
            return Files.readString(includePath);
        } catch (IOException e) {
            throw new AsciiDocIncludeException("Unable to read content for include %s at %s. %s"
                    .formatted(includePath, location(content, index), e));
        }
    }

    private String applyInclude(String content, int startIndex, int endIndex, String value) {
        return content.substring(0, startIndex) + "\n" + value + "\n" + content.substring(endIndex);
    }

    private int mandatoryIndex(String content, String substring, int indexFrom) {
        int index = content.indexOf(substring, indexFrom);
        if (index < 0) {
            throw new IllegalStateException("Expecting %s to be found after %s but none could be found"
                    .formatted(substring, location(content, index)));
        }
        return index;
    }

    private String location(String content, int index) {
        return "%s:%d".formatted(path, lineNumber(content, index));
    }

    private static long lineNumber(String content, int index) {
        return content.substring(0, index).chars().filter(c -> c == '\n').count();
    }
}

class AsciiDocIncludeException extends RuntimeException {

    public AsciiDocIncludeException(String message) {
        super(message);
    }

    public AsciiDocIncludeException(Exception e) {
        super(e);
    }
}
