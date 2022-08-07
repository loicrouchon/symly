package org.symly.repositories;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(ignoreRules).hasSize(1).first().isEqualTo(IgnoreRule.ofRegex("^some-file$"));
    }

    @Test
    void parseIgnoreList_shouldParse_patternWithTrailingComment() {
        // given/when
        var ignoreRules = parse("some-file # comment");
        // then
        assertThat(ignoreRules).hasSize(1).first().isEqualTo(IgnoreRule.ofRegex("^some-file$"));
    }

    @Test
    void parseIgnoreList_shouldParse_patternWithDots() {
        // given/when
        var ignoreRules = parse("file.ext");
        // then
        assertThat(ignoreRules).hasSize(1).first().isEqualTo(IgnoreRule.ofRegex("^file\\.ext$"));
    }

    @Test
    void parseIgnoreList_shouldParse_patternWithWildcard() {
        // given/when
        var ignoreRules = parse("*.ext");
        // then
        assertThat(ignoreRules).hasSize(1).first().isEqualTo(IgnoreRule.ofRegex("^.*\\.ext$"));
    }

    private List<IgnoreRule> parse(String... lines) {
        return IgnoreList.parse(Arrays.stream(lines));
    }
}
