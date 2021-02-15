package org.linky.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.linky.env.Env;
import org.linky.env.IntegrationTest;
import org.linky.files.FileTree.Diff;

@SuppressWarnings("java:S2699")
class LinkCommandTest extends IntegrationTest {

    private final LinkMessageFactory msg = new LinkMessageFactory(env);

    @Test
    void shouldFail_whenRequiredArgsAreMissing() {
        //given
        given(env);
        //when/then
        whenRunningCommand("link")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(msg.missingSources())
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldFail_whenDestinationDirectoryDoesNotExist() {
        //given
        given(env).withHome("home/doesnotexist");
        //when/then
        whenRunningCommand("link", "-s", "to/dir", "/home/user/some/file")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(msg.destinationDoesNotExist(env.home().toString()))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldFail_whenSourceDirectoryDoesNotExist() {
        //given
        given(env);
        //when/then
        whenRunningCommand("link", "-s", "to/dir", "/home/user/some/file")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(msg.sourceDoesNotExist("to/dir"))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldProvideCorrectDefaults() {
        //given
        given(env)
                .withDirectories("from/dir");
        //when/then
        whenRunningCommand("link", "-s", "from/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.creatingLinks(List.of("from/dir"), env.home().toString()))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldParseArguments_whenArgumentsArePassed() {
        //given
        given(env)
                .withDirectories("from/dir", "from/other-dir", "to/dir");
        //when/then
        whenRunningCommand("link", "-s", "from/dir", "from/other-dir", "-d", "to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.creatingLinks(List.of("from/dir", "from/other-dir"), "to/dir"))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldLinkFile_whenDestinationFileDoesNotExist() {
        //given
        given(env)
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
                .withFileTreeDiff(Diff.empty().withNewPaths(
                        "home/user/file -> home/user/from/dir/file",
                        "home/user/nested/file -> home/user/from/dir/nested/file"
                ));
    }

    @Test
    void shouldLinkLink_whenDestinationLinkDoesNotExist() {
        //given
        given(env)
                .withFiles("opt/file")
                .withSymbolicLink("home/user/from/dir/link", "opt/file")
                .withSymbolicLink("home/user/from/dir/nested/link", "opt/file");
        //when/then
        whenRunningCommand("link", "-s", "home/user/from/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.createLink("home/user/link", "home/user/from/dir/link"))
                .withMessage(msg.createLink("home/user/nested/link", "home/user/from/dir/nested/link"))
                .withFileTreeDiff(Diff.empty().withNewPaths(
                        "home/user/link -> home/user/from/dir/link",
                        "home/user/nested/link -> home/user/from/dir/nested/link"
                ));
    }

    @Test
    void shouldNotLinkDirectory_whenDirectorySymlinkDoesNotExist() {
        //given
        given(env)
                .withDirectories("home/user/from/dir/sub/dir");
        //when/then
        whenRunningCommand("link", "-s", "home/user/from/dir")
                .thenItShould()
                .succeed()
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldLinkDirectory_whenDirectorySymlinkExists() {
        //given
        given(env)
                .withFiles("home/user/from/dir/sub/dir/.symlink");
        //when/then
        whenRunningCommand("link", "-s", "home/user/from/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.createLink("home/user/sub/dir", "home/user/from/dir/sub/dir"))
                .withFileTreeDiff(Diff.empty().withNewPaths(
                        "home/user/sub/dir -> home/user/from/dir/sub/dir"
                ));
    }

    @Test
    void shouldNotLinkFile_whenDestinationFileAlreadyExist() {
        //given
        given(env)
                .withFiles(
                        "home/user/file",
                        "home/user/from/dir/file"
                );
        //when/then
        whenRunningCommand("link", "-s", "home/user/from/dir")
                .thenItShould()
                .failWithError()
                .withErrorMessages(msg.cannotCreateLink("home/user/file", "home/user/from/dir/file"))
                .withMessage(msg.createLinkConflict("home/user/file", "home/user/from/dir/file"))
                .withFileTreeDiff(Diff.empty());
    }

    @RequiredArgsConstructor
    private static class LinkMessageFactory {

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

        public String createLinkConflict(String from, String to) {
            return String.format("[CONFLICT  ] %s -> %s", env.path(from), env.path(to));
        }

        public List<String> cannotCreateLink(String from, String to) {
            return List.of(
                    String.format("Unable to create link %s -> %s", env.path(from), env.path(to)),
                    String.format(
                            "> Regular file %s already exist. To overwrite it, use the --replace-file option.",
                            env.path(from))
            );
        }
    }
}