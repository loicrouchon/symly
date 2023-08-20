package org.symly.cli;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.symly.env.IntegrationTest;
import org.symly.files.FileTree;

@SuppressWarnings({
    "java:S100", // Method names should comply with a naming convention (test method names)
    "java:S1192", // String literals should not be duplicated
})
class StatusCommandTest extends IntegrationTest {

    private final StatusCommandMessageFactory msg = new StatusCommandMessageFactory(env);

    private final ContextInputMessageFactory ctxMsg = new ContextInputMessageFactory(env);

    @Test
    void shouldFail_whenMainDirectoryIsNotDefined() {
        // given
        given(env);
        // when/then
        whenRunningCommand("status")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(ctxMsg.mainDirectoryIsNotDefined())
                .withFileTreeDiff(FileTree.Diff.empty());
    }

    @Test
    void shouldFail_whenRepositoriesAreNotDefined() {
        // given
        given(env);
        // when/then
        whenRunningCommand("status", "--dir", "~")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(ctxMsg.repositoriesAreNotDefined())
                .withFileTreeDiff(FileTree.Diff.empty());
    }

    @Test
    void shouldFail_whenMainDirectoryDoesNotExist() {
        // given
        given(env).withHome("home/doesnotexist");
        // when/then
        whenRunningCommand("status", "--dir", "~", "--repositories", "to/dir", "/home/user/some/file")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(ctxMsg.mainDirectoryDoesNotExist(env.home().toString()))
                .withFileTreeDiff(FileTree.Diff.empty());
    }

    @Test
    void shouldFail_whenRepositoryDoesNotExist() {
        // given
        given(env);
        // when/then
        whenRunningCommand("status", "--dir", "~", "--repositories", "to/dir", "/home/user/some/file")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(ctxMsg.repositoryDoesNotExist("to/dir"))
                .withFileTreeDiff(FileTree.Diff.empty());
    }

    @Test
    void shouldProvideCorrectDefaults() {
        // given
        given(env).withLayout("D to/dir");
        // when/then
        whenRunningCommand("status", "-v", "--dir", "~", "--repositories", "to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.checkingLinks("home/user", List.of("to/dir")))
                .withFileTreeDiff(FileTree.Diff.empty());
    }

    @Test
    void shouldParseArguments_whenArgumentsArePassed() {
        // given
        given(env)
                .withLayout("""
            D main/dir
            D to/dir
            D to/other-dir
            """);
        // when/then
        whenRunningCommand("status", "-v", "--dir", "main/dir", "--repositories", "to/dir", "to/other-dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.checkingLinks("main/dir", List.of("to/dir", "to/other-dir")))
                .withFileTreeDiff(FileTree.Diff.empty());
    }
}
