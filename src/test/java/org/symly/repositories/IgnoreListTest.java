package org.symly.repositories;

import static org.symly.testing.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class IgnoreListTest {

    @Test
    void parseIgnoreList_shouldIgnore_whiteSpaces() {
        // given//when
        var ignoreRules = parse("", " ", "\t", "  \t", "\t  ");
        // then
        assertThat(ignoreRules).isEmpty();
    }

    @Test
    void parseIgnoreList_shouldIgnore_commentedLines() {
        // given/when
        var ignoreRules = parse("# this is a comment", "  # this is a comment, but not at the beginning of the line");
        // then
        assertThat(ignoreRules).isEmpty();
    }

    @Test
    void parseIgnoreList_shouldParse_simplePattern() {
        // given/when
        var ignoreRules = parse("some-file");
        // then
        assertThat(ignoreRules).hasSize(1).first().isEqualTo(IgnoreRule.ofRegex("^\\Qsome-file\\E$"));
    }

    @Test
    void parseIgnoreList_shouldParse_patternWithTrailingComment() {
        // given/when
        var ignoreRules = parse("some-file # comment");
        // then
        assertThat(ignoreRules).hasSize(1).first().isEqualTo(IgnoreRule.ofRegex("^\\Qsome-file\\E$"));
    }

    @Test
    void parseIgnoreList_shouldParse_patternWithDots() {
        // given/when
        var ignoreRules = parse("file.ext");
        // then
        assertThat(ignoreRules).hasSize(1).first().isEqualTo(IgnoreRule.ofRegex("^\\Qfile.ext\\E$"));
    }

    @Test
    void parseIgnoreList_shouldParse_patternWithWildcardPrefix() {
        // given/when
        var ignoreRules = parse("*.ext");
        // then
        assertThat(ignoreRules).hasSize(1).first().isEqualTo(IgnoreRule.ofRegex("^.*\\Q.ext\\E$"));
    }

    @Test
    void parseIgnoreList_shouldParse_patternWithWildcardSuffix() {
        // given/when
        var ignoreRules = parse("file-*");
        // then
        assertThat(ignoreRules).hasSize(1).first().isEqualTo(IgnoreRule.ofRegex("^\\Qfile-\\E.*$"));
    }

    @Test
    void parseIgnoreList_shouldParse_patternContainingEscapeSequence() {
        // given/when
        var ignoreRules = parse("fi\\E.\\Qle");
        // then
        assertThat(ignoreRules).hasSize(1).first().isEqualTo(IgnoreRule.ofRegex("^\\Qfi\\E\\\\E\\Q.\\Qle\\E$"));
    }

    private List<IgnoreRule> parse(String... lines) {
        return IgnoreList.parse(Arrays.stream(lines));
    }
}
