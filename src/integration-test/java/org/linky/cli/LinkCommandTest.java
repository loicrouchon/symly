package org.linky.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.linky.env.Env;
import org.linky.env.IntegrationTest;

@SuppressWarnings("java:S2699")
class LinkCommandTest extends IntegrationTest {

    private LinkMessageFactory msg;

    @BeforeEach
    void initializeMessageFactory() {
        msg = new LinkMessageFactory(getEnv());
    }

    @Test
    void shouldFail_whenRequiredArgsAreMissing() {
        //given
        givenCleanEnv();
        //when/then
        whenRunningCommand("link")
                .thenItShould()
                .fail()
                .withErrorMessage(msg.missingSources())
                .andLayout()
                .isEmpty();
    }

    @Test
    void shouldFail_whenDestinationDirectoryDoesNotExist() {
        //given
        givenCleanEnv()
                .withHome("home/doesnotexist");
        //when/then
        whenRunningCommand("link", "-s", "to/dir", "/home/user/some/file")
                .thenItShould()
                .fail()
                .withErrorMessage(msg.destinationDoesNotExist(home().toString()))
                .andLayout()
                .isEmpty();
    }

    @Test
    void shouldFail_whenSourceDirectoryDoesNotExist() {
        //given
        givenCleanEnv();
        //when/then
        whenRunningCommand("link", "-s", "to/dir", "/home/user/some/file")
                .thenItShould()
                .fail()
                .withErrorMessage(msg.sourceDoesNotExist("to/dir"))
                .andLayout()
                .isEmpty();
    }

    @Test
    void shouldProvideCorrectDefaults() {
        //given
        givenCleanEnv()
                .withDirectories("from/dir");
        //when/then
        whenRunningCommand("link", "-s", "from/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.creatingLinks(List.of("from/dir"), home().toString()))
                .andLayout()
                .isEmpty();
    }

    @Test
    void shouldParseArguments_whenArgumentsArePassed() {
        //given
        givenCleanEnv()
                .withDirectories("from/dir", "from/other-dir", "to/dir");
        //when/then
        whenRunningCommand("link", "-s", "from/dir", "from/other-dir", "-d", "to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.creatingLinks(List.of("from/dir", "from/other-dir"), "to/dir"))
                .andLayout()
                .isEmpty();
    }

    @Test
    void shouldLinkFile_whenDestinationFileDoesNotExist() {
        //given
        givenCleanEnv()
                .withFiles(
                        "home/user/from/dir/file",
                        "home/user/from/dir/nested/file"
                        );

        //when/then
        whenRunningCommand("link", "-s", "home/user/from/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.createLink("home/user/file", "home/user/from/dir/file"))
                .withMessage(msg.createLink("home/user/nested/file", "home/user/from/dir/nested/file"))
                .andLayout()
                .containsExactly(
                        "home/user/file -> home/user/from/dir/file",
                        "home/user/from/dir/file",
                        "home/user/from/dir/nested/file",
                        "home/user/nested/file -> home/user/from/dir/nested/file"
                );
    }

    @Test
    void shouldLinkLink_whenDestinationLinkDoesNotExist() {
        //given
        givenCleanEnv()
                .withFiles("opt/file")
                .withSymbolicLink("home/user/from/dir/link", "opt/file")
                .withSymbolicLink("home/user/from/dir/nested/link", "opt/file");

        //when/then
        whenRunningCommand("link", "-s", "home/user/from/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.createLink("home/user/link", "home/user/from/dir/link"))
                .withMessage(msg.createLink("home/user/nested/link", "home/user/from/dir/nested/link"))
                .andLayout()
                .containsExactly(
                        "home/user/from/dir/link -> opt/file",
                        "home/user/from/dir/nested/link -> opt/file",
                        "home/user/link -> home/user/from/dir/link",
                        "home/user/nested/link -> home/user/from/dir/nested/link",
                        "opt/file"
                );
    }

    @Test
    void shouldNotLinkDirectory_whenDirectorySymlinkDoesNotExist() {
        //given
        givenCleanEnv()
                .withDirectories("home/user/from/dir/sub/dir");

        //when/then
        whenRunningCommand("link", "-s", "home/user/from/dir")
                .thenItShould()
                .succeed()
                .andLayout()
                .isEmpty();
    }

    @Test
    void shouldLinkDirectory_whenDirectorySymlinkExists() {
        //given
        givenCleanEnv()
                .withFiles("home/user/from/dir/sub/dir/.symlink");

        //when/then
        whenRunningCommand("link", "-s", "home/user/from/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.createLink("home/user/sub/dir", "home/user/from/dir/sub/dir"))
                .andLayout()
                .containsExactly(
                        "home/user/from/dir/sub/dir/.symlink",
                        "home/user/sub/dir -> home/user/from/dir/sub/dir"
                );
    }

    @RequiredArgsConstructor
    public static class LinkMessageFactory {

        @NonNull
        private final Env env;

        public String missingSources() {
            return "Missing required option: '--sources=<sources>'";
        }

        public String destinationDoesNotExist(String path) {
            return String.format("Argument <destination> (%s): must be an existing directory", path);
        }

        public String sourceDoesNotExist(String path) {
            return String.format("Argument <sources> (%s): must be an existing directory", path);
        }

        public String creatingLinks(List<String> from, String to) {
            return String.format(
                    "Creating links from [%s] to %s",
                    from.stream()
                            .map(env::path)
                            .map(Path::toString)
                            .collect(Collectors.joining(", ")),
                    env.path(to));
        }

        public String createLink(String from, String to) {
            return String.format("[CREATE    ] %s -> %s", env.path(from), env.path(to));
        }
    }
}