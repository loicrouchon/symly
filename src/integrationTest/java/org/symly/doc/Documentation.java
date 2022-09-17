package org.symly.doc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.files.RIOException;

class Documentation {

    private static final List<AsciiDocFile> ADOC_FILES = Stream.of("README.adoc", "docs/")
            .map(Path::of)
            .flatMap(Documentation::walk)
            .filter(path -> path.getFileName().toString().endsWith(".adoc"))
            .sorted()
            .map(AsciiDocFile::new)
            .toList();

    private static Stream<Path> walk(Path path) {
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Path %s does not exists".formatted(path.toAbsolutePath()));
        }
        if (Files.isDirectory(path)) {
            try {
                return Files.walk(path).filter(Files::isRegularFile);
            } catch (IOException e) {
                throw new RIOException(e);
            }
        }
        return Stream.of(path);
    }

    public static void updateSnippets(String id, String value) {
        ADOC_FILES.forEach(file -> file.updateSnippet(id, value));
    }

    public static void updateSnippets(String id, String... commands) {
        String value = String.join("\n\n", commands);
        ADOC_FILES.forEach(file -> file.updateSnippet(id, value));
    }
}

@RequiredArgsConstructor
class AsciiDocFile {

    @NonNull
    private final Path path;

    private String content;

    public static final String BLOCK_DELIMITER = "\n----\n";

    public void updateSnippet(String id, String value) {
        try {
            String content = content();
            int index = 0;
            String snippetTag = "// include::inline[name=\"%s\"".formatted(id);
            while ((index = content.indexOf(snippetTag, index)) >= 0) {
                int blockStartIndex = content.indexOf("\n", content.indexOf(BLOCK_DELIMITER, index));
                checkIndex(blockStartIndex, id, content, index);
                int blockEndIndex = content.indexOf(BLOCK_DELIMITER, blockStartIndex + BLOCK_DELIMITER.length());
                checkIndex(blockEndIndex, id, content, index);
                writeContent(blockStartIndex, blockEndIndex, value);
                index = blockEndIndex;
            }
        } catch (IOException e) {
            throw new RIOException(e);
        }
    }

    private String content() throws IOException {
        if (content == null) {
            try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
                content = lines.collect(Collectors.joining("\n"));
                if (content.charAt(content.length() - 1) != '\n') {
                    content += "\n";
                }
            }
        }
        return content;
    }

    private void writeContent(int blockStartIndex, int blockEndIndex, String value) throws IOException {
        content = content.substring(0, blockStartIndex + BLOCK_DELIMITER.length())
                + value
                + content.substring(blockEndIndex);
        Files.writeString(path, content);
    }

    private void checkIndex(int index, String id, String content, int snippetDeclarationIndex) {
        if (index < 0) {
            long linesCount = content.substring(0, snippetDeclarationIndex)
                    .chars()
                    .filter(c -> c == '\n')
                    .count();
            throw new IllegalStateException(
                    "Block delimiter not found for snippet %s in file %s:%d".formatted(id, path, linesCount));
        }
    }
}
