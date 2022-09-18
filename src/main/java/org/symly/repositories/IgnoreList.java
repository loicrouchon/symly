package org.symly.repositories;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.symly.cli.SymlyExecutionException;
import org.symly.files.FileSystemReader;

/**
 * Parses <strong>.symlyignore</strong> file in order to produce a collection of {@link IgnoreRule}. Such rules can be
 * used to ignore particular files inside repositories.
 */
class IgnoreList {

    static final String SYMLY_IGNORE = ".symlyignore";

    private static final Pattern COMMENT = Pattern.compile("#.*$");

    private static final Collection<Conversion> PATTERN_CONVERSIONS = List.of(
            // escape dots
            new Conversion("\\.", "\\\\."),
            // wildcard support
            new Conversion("\\*", ".*"),
            // trimming leading/trailing whitespaces
            new Conversion("^\s+", ""),
            new Conversion("\s+$", ""));

    static Collection<IgnoreRule> read(FileSystemReader fsReader, Path path) {
        Path ignoreList = path.resolve(SYMLY_IGNORE);
        if (fsReader.exists(ignoreList)) {
            try (Stream<String> lines = fsReader.lines(ignoreList)) {
                return parse(lines);
            } catch (IOException e) {
                throw new SymlyExecutionException("Unable to analyze repository structure %s".formatted(path), e);
            }
        }
        return Collections.emptyList();
    }

    static List<IgnoreRule> parse(Stream<String> lines) {
        return lines.map(line -> COMMENT.matcher(line).replaceAll(""))
                .filter(line -> !line.isBlank())
                .map(IgnoreList::toPattern)
                .toList();
    }

    private static IgnoreRule toPattern(String line) {
        for (Conversion conversion : PATTERN_CONVERSIONS) {
            line = conversion.convert(line);
        }
        return new IgnoreRule(Pattern.compile("^" + line + "$"));
    }
}

@RequiredArgsConstructor
class Conversion {

    private final Pattern pattern;
    private final String replacement;

    public Conversion(String regex, String replacement) {
        this.pattern = Pattern.compile(regex);
        this.replacement = replacement;
    }

    public String convert(String str) {
        return pattern.matcher(str).replaceAll(replacement);
    }
}
