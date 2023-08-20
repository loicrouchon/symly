package org.symly.doc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.symly.files.RIOException;

/**
 * <p>Represents an AsciiDoc snippet to be included in AsciiDoc files.</p>
 * <p>Snippets will be saved in folder {@code docs/snippets} and can be referenced
 * from Asciidoc files using the following syntax:</p>
 * <pre>{@code
 * // include::docs/snippets/<ID>.adoc[]
 * // end::include
 * }</pre>
 * <p>Where {@code <ID>} is the name of the snippet.</p>
 * <p>Processing of those AsciiDoc includes is performed as part of the Gradle build and
 * any changes in those should be committed to the version control system.</p>
 */
class AsciiDocSnippet {

    /**
     * Saves a snippet in {@code "docs/snippets/" + id + ".adoc"} with the given content.
     * @param id the id of the snippet
     * @param content the content of the snippet
     */
    public static void save(String id, String content) {
        String snippet = """
            ----
            %s
            ----""".formatted(content);
        String snippetFileName = "docs/snippets/%s.adoc".formatted(id);
        try {
            Files.writeString(Path.of(snippetFileName), snippet);
        } catch (IOException e) {
            throw new RIOException(e);
        }
    }
}
