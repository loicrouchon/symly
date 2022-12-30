package org.symly.repositories;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.symly.cli.SymlyExecutionException;
import org.symly.files.FileSystemReader;

/**
 * Parses <strong>.symlyignore</strong> file in order to produce a collection of {@link IgnoreRule}. Such rules can be
 * used to ignore particular files inside repositories.
 */
class IgnoreList {

    static final String SYMLY_IGNORE = ".symlyignore";

    private static final Pattern COMMENT = Pattern.compile("#.*$");
    private static final Pattern ASTERISK_PATTERN = Pattern.compile("\\*");

    private static final Collection<UnaryOperator<String>> PATTERN_CONVERSIONS = List.of(
            // trimming leading/trailing whitespaces
            new RegexConversion("^\\s+", ""),
            new RegexConversion("\\s+$", ""),
            // wildcard support
            IgnoreList::processWildcards);

    private static String processWildcards(String str) {
        String matchAnythingPattern = ".*";
        String quoted = ASTERISK_PATTERN
                .splitAsStream(str)
                .map(s -> s.isEmpty() ? s : Pattern.quote(s))
                .collect(Collectors.joining(matchAnythingPattern));
        if (str.endsWith("*")) {
            // For prefix matches, there is no problem, however
            // split does not create an empty cell for suffix matches
            // hence this code to manually handle it
            return quoted + matchAnythingPattern;
        }
        return quoted;
    }

    private static final List<IgnoreRule> TOP_LEVEL_DEFAULT_IGNORE_RULES =
            List.of(toPattern(ContextConfig.SYMLY_CONFIG));

    private IgnoreList() {}

    static Collection<IgnoreRule> readTopLevel(FileSystemReader fsReader, Path path) {
        return readIt(path, fsReader, TOP_LEVEL_DEFAULT_IGNORE_RULES);
    }

    static Collection<IgnoreRule> read(FileSystemReader fsReader, Path path) {
        return readIt(path, fsReader, Collections.emptyList());
    }

    private static List<IgnoreRule> readIt(Path path, FileSystemReader fsReader, List<IgnoreRule> defaultValue) {
        Path ignoreList = path.resolve(SYMLY_IGNORE);
        if (fsReader.exists(ignoreList)) {
            try (Stream<String> lines = fsReader.lines(ignoreList)) {
                return parse(lines);
            } catch (IOException e) {
                throw new SymlyExecutionException("Unable to analyze repository structure %s".formatted(path), e);
            }
        }
        return defaultValue;
    }

    static List<IgnoreRule> parse(Stream<String> lines) {
        return lines.map(line -> COMMENT.matcher(line).replaceAll(""))
                .filter(line -> !line.isBlank())
                .map(IgnoreList::toPattern)
                .toList();
    }

    private static IgnoreRule toPattern(String line) {
        for (UnaryOperator<String> conversion : PATTERN_CONVERSIONS) {
            line = conversion.apply(line);
        }
        return new IgnoreRule(Pattern.compile("^" + line + "$"));
    }
}

class RegexConversion implements UnaryOperator<String> {

    private final Pattern pattern;
    private final String replacement;

    public RegexConversion(String regex, String replacement) {
        this.pattern = Pattern.compile(regex);
        this.replacement = replacement;
    }

    @Override
    public String apply(String str) {
        return pattern.matcher(str).replaceAll(replacement);
    }
}
