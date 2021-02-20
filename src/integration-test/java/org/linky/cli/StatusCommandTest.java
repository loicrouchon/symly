package org.linky.cli;

import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.linky.env.Env;
import org.linky.env.IntegrationTest;
import org.linky.files.FileTree;

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
                .withErrorMessage(msg.missingTargets())
                .withFileTreeDiff(FileTree.Diff.empty());
    }

    @Test
    void shouldFail_whenDestinationDirectoryDoesNotExist() {
        //given
        given(env)
                .withHome("home/doesnotexist");
        //when/then
        whenRunningCommand("status", "-t", "to/dir", "/home/user/some/file")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(msg.destinationDoesNotExist(env.home().toString()))
                .withFileTreeDiff(FileTree.Diff.empty());
    }

    @Test
    void shouldFail_whenSourceDirectoryDoesNotExist() {
        //given
        given(env);
        //when/then
        whenRunningCommand("status", "-t", "to/dir", "/home/user/some/file")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(msg.targetsDoesNotExist("to/dir"))
                .withFileTreeDiff(FileTree.Diff.empty());
    }

    @Test
    void shouldProvideCorrectDefaults() {
        //given
        given(env)
                .withDirectories("from/dir");
        //when/then
        whenRunningCommand("status", "-t", "from/dir")
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
        whenRunningCommand("status", "-t", "from/dir", "from/other-dir", "-d", "to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.checkingLinks(List.of("from/dir", "from/other-dir"), "to/dir"))
                .withFileTreeDiff(FileTree.Diff.empty());
    }

    @RequiredArgsConstructor
    private static class StatusMessageFactory {

        @NonNull
        private final Env env;

        public String missingTargets() {
            return "Missing required option: '--targets=<targets>'";
        }

        public String destinationDoesNotExist(String path) {
            return String.format("Argument <destination> (%s): must be an existing directory", path);
        }

        public String targetsDoesNotExist(String path) {
            return String.format("Argument <targets> (%s): must be an existing directory", path);
        }

        public String checkingLinks(List<String> from, String to) {
            return String.format(
                    "Checking links status from [%s] to %s",
                    String.join(", ", from),
                    env.path(to));
        }
    }
}