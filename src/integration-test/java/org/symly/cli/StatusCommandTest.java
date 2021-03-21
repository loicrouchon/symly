package org.symly.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.symly.env.Env;
import org.symly.env.IntegrationTest;
import org.symly.files.FileTree;

@SuppressWarnings("java:S2699")
class StatusCommandTest extends IntegrationTest {

    private final StatusMessageFactory msg = new StatusMessageFactory(env);

    @Test
    void shouldFail_whenRequiredArgsAreMissing() {
        //given
        given(env);
        //when/then
        whenRunningCommand("status")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(msg.missingTargetDirectories())
                .withFileTreeDiff(FileTree.Diff.empty());
    }

    @Test
    void shouldFail_whenSourceDirectoryDoesNotExist() {
        //given
        given(env)
                .withHome("home/doesnotexist");
        //when/then
        whenRunningCommand("status", "-r", "to/dir", "/home/user/some/file")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(msg.sourceDirectoryDoesNotExist(env.home().toString()))
                .withFileTreeDiff(FileTree.Diff.empty());
    }

    @Test
    void shouldFail_whenTargetDirectoryDoesNotExist() {
        //given
        given(env);
        //when/then
        whenRunningCommand("status", "-r", "to/dir", "/home/user/some/file")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(msg.targetDirectoryDoesNotExist("to/dir"))
                .withFileTreeDiff(FileTree.Diff.empty());
    }

    @Test
    void shouldProvideCorrectDefaults() {
        //given
        given(env)
                .withDirectories("from/dir");
        //when/then
        whenRunningCommand("status", "-r", "from/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.checkingLinks(List.of("from/dir"), "home/user"))
                .withFileTreeDiff(FileTree.Diff.empty());
    }

    @Test
    void shouldParseArguments_whenArgumentsArePassed() {
        //given
        given(env)
                .withDirectories("from/dir", "from/other-dir", "to/dir");
        //when/then
        whenRunningCommand("status", "-s", "to/dir", "-r", "from/dir", "from/other-dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.checkingLinks(List.of("from/dir", "from/other-dir"), "to/dir"))
                .withFileTreeDiff(FileTree.Diff.empty());
    }

    @RequiredArgsConstructor
    private static class StatusMessageFactory {

        @NonNull
        private final Env env;

        public String missingTargetDirectories() {
            return "Missing required option: '--repositories=<repositories>'";
        }

        public String targetDirectoryDoesNotExist(String path) {
            return String.format("Argument <repositories> (%s): must be an existing directory", env.path(path));
        }

        public String sourceDirectoryDoesNotExist(String path) {
            return String.format("Argument <source-directory> (%s): must be an existing directory", env.path(path));
        }

        public String checkingLinks(List<String> from, String to) {
            return String.format(
                    "Checking links status from [%s] to %s",
                    from.stream()
                            .map(env::path)
                            .map(Path::toString)
                            .collect(Collectors.joining(", ")),
                    env.path(to));
        }
    }
}