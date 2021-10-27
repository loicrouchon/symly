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
    void shouldFail_whenMainDirectoryDoesNotExist() {
        //given
        given(env)
                .withHome("home/doesnotexist");
        //when/then
        whenRunningCommand("status", "--to", "to/dir", "/home/user/some/file")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(msg.mainDirectoryDoesNotExist(env.home().toString()))
                .withFileTreeDiff(FileTree.Diff.empty());
    }

    @Test
    void shouldFail_whenTargetDirectoryDoesNotExist() {
        //given
        given(env);
        //when/then
        whenRunningCommand("status", "--to", "to/dir", "/home/user/some/file")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(msg.targetDirectoryDoesNotExist("to/dir"))
                .withFileTreeDiff(FileTree.Diff.empty());
    }

    @Test
    void shouldProvideCorrectDefaults() {
        //given
        given(env)
                .withDirectories("to/dir");
        //when/then
        whenRunningCommand("status", "--to", "to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.checkingLinks("home/user", List.of("to/dir")))
                .withFileTreeDiff(FileTree.Diff.empty());
    }

    @Test
    void shouldParseArguments_whenArgumentsArePassed() {
        //given
        given(env)
                .withDirectories("to/dir", "to/other-dir", "main/dir");
        //when/then
        whenRunningCommand("status", "--dir", "main/dir", "--to", "to/dir", "to/other-dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.checkingLinks("main/dir", List.of("to/dir", "to/other-dir")))
                .withFileTreeDiff(FileTree.Diff.empty());
    }

    @RequiredArgsConstructor
    private static class StatusMessageFactory {

        @NonNull
        private final Env env;

        public String missingTargetDirectories() {
            return "Missing required option: '--to=<repositories>'";
        }

        public String targetDirectoryDoesNotExist(String path) {
            return String.format("Argument <repositories> (%s): must be an existing directory", env.path(path));
        }

        public String mainDirectoryDoesNotExist(String path) {
            return String.format("Argument <main-directory> (%s): must be an existing directory", env.path(path));
        }

        public String checkingLinks(String mainDirectory, List<String> repositories) {
            return String.format(
                    "Checking links status from %s to [%s]",
                    env.path(mainDirectory),
                    repositories.stream()
                            .map(env::path)
                            .map(Path::toString)
                            .collect(Collectors.joining(", "))
            );
        }
    }
}