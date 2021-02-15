package org.linky.cli;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.linky.env.Env;
import org.linky.env.IntegrationTest;
import org.linky.files.FileTree.Diff;

@SuppressWarnings("java:S2699")
class AddCommandTest extends IntegrationTest {

    private final AddMessageFactory msg = new AddMessageFactory(env);

    @Test
    void shouldFail_whenRequiredArgsAreMissing() {
        //given
        given(env);
        //when/then
        whenRunningCommand("add")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(msg.missingArguments())
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldFail_whenDirectoryDoesNotExist() {
        //given
        given(env)
                .withHome("home/doesnotexist");
        //when/then
        whenRunningCommand("add", "-t", "to/dir", "/home/user/some/file")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(msg.argumentFromIsNotADirectory("home/doesnotexist"))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldParseArguments_whenArgumentsArePassed() {
        //given
        given(env)
                .withDirectories("from/dir", "to/dir")
                .withFiles("from/dir/some/file");
        //when/then
        whenRunningCommand("add", "-f", "from/dir", "-t", "to/dir", "from/dir/some/file")
                .thenItShould()
                .succeed()
                .withMessage(msg.init("some/file", "from/dir", "to/dir"))
                .withMessage(msg.moved("from/dir/some/file", "to/dir/some/file"));
    }

    @RequiredArgsConstructor
    private static class AddMessageFactory {

        @NonNull
        private final Env env;

        public String missingArguments() {
            return "Missing required options and parameters: '--to=<to>', '<file>'";
        }

        public String argumentFromIsNotADirectory(String path) {
            return String.format("Argument <from> (%s): must be an existing directory", env.path(path));
        }

        public String init(String name, String from, String to) {
            return String.format("Moving %s from %s to %s and creating link", name, env.path(from), env.path(to));
        }

        public String moved(String from, String to) {
            return String.format("[MOVED] %s -> %s", from, env.path(to));
        }
    }
}