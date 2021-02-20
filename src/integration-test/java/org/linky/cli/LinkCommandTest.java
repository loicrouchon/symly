package org.linky.cli;

import java.util.List;
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
                .withErrorMessage(msg.missingTargets())
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldFail_whenDestinationDirectoryDoesNotExist() {
        //given
        given(env).withHome("home/doesnotexist");
        //when/then
        whenRunningCommand("link", "-t", "to/dir", "/home/user/some/file")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(msg.destinationDoesNotExist(env.home().toString()))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldFail_whenTargetDirectoryDoesNotExist() {
        //given
        given(env);
        //when/then
        whenRunningCommand("link", "-t", "to/dir", "/home/user/some/file")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(msg.targetDoesNotExist("to/dir"))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldProvideCorrectDefaults() {
        //given
        given(env)
                .withDirectories("from/dir");
        //when/then
        whenRunningCommand("link", "-t", "from/dir")
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
        whenRunningCommand("link", "-t", "from/dir", "from/other-dir", "-d", "to/dir")
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
        whenRunningCommand("link", "-t", "home/user/from/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionCreate("home/user/file", "home/user/from/dir/file"))
                .withMessage(msg.linkActionCreate("home/user/nested/file", "home/user/from/dir/nested/file"))
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
        whenRunningCommand("link", "-t", "home/user/from/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionCreate("home/user/link", "home/user/from/dir/link"))
                .withMessage(msg.linkActionCreate("home/user/nested/link", "home/user/from/dir/nested/link"))
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
        whenRunningCommand("link", "-t", "home/user/from/dir")
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
        whenRunningCommand("link", "-t", "home/user/from/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionCreate("home/user/sub/dir", "home/user/from/dir/sub/dir"))
                .withFileTreeDiff(Diff.empty().withNewPaths(
                        "home/user/sub/dir -> home/user/from/dir/sub/dir"
                ));
    }

    @Test
    void shouldNotLinkFile_whenLinkDestinationIsAnExistingFile() {
        //given
        given(env)
                .withFiles(
                        "home/user/file",
                        "home/user/from/dir/file"
                );
        //when/then
        whenRunningCommand("link", "-t", "home/user/from/dir")
                .thenItShould()
                .failWithError()
                .withErrorMessages(msg.cannotCreateLinkError("home/user/file", "home/user/from/dir/file"))
                .withMessage(msg.linkActionConflict("home/user/file", "home/user/from/dir/file"))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldNotLinkFile_whenLinkDestinationIsAnExistingDirectory() {
        //given
        given(env)
                .withDirectories("home/user/file")
                .withFiles("home/user/from/dir/file");
        //when/then
        whenRunningCommand("link", "-t", "home/user/from/dir")
                .thenItShould()
                .failWithError()
                .withErrorMessages(msg.cannotCreateLinkError("home/user/file", "home/user/from/dir/file"))
                .withMessage(msg.linkActionConflict("home/user/file", "home/user/from/dir/file"))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldUpdateLink_whenLinkDestinationIsAnExistingLink() {
        //given
        given(env)
                .withSymbolicLink("home/user/file", "home/user/other-file")
                .withFiles("home/user/from/dir/file");
        //when/then
        whenRunningCommand("link", "-t", "home/user/from/dir")
                .thenItShould()
                .succeed()
                .withMessages(msg.linkActionUpdate("home/user/file", "home/user/from/dir/file",
                        "home/user/other-file"))
                .withFileTreeDiff(Diff.empty()
                        .withNewPaths("home/user/file -> home/user/from/dir/file")
                        .withRemovedPaths("home/user/file -> home/user/other-file")
                );
    }

    @Test
    void shouldNotUpdateLinkFile_whenLinkIsAlreadyUpToDate() {
        //given
        given(env)
                .withSymbolicLink("home/user/file", "home/user/from/dir/file")
                .withFiles("home/user/from/dir/file");
        //when/then
        whenRunningCommand("link", "-t", "home/user/from/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionUpToDate("home/user/file", "home/user/from/dir/file"))
                .withFileTreeDiff(Diff.empty());
    }

    @RequiredArgsConstructor
    private static class LinkMessageFactory {

        @NonNull
        private final Env env;

        public String missingTargets() {
            return "Missing required option: '--targets=<targets>'";
        }

        public String destinationDoesNotExist(String path) {
            return String.format("Argument <destination> (%s): must be an existing directory", path);
        }

        public String targetDoesNotExist(String path) {
            return String.format("Argument <targets> (%s): must be an existing directory", path);
        }

        public String creatingLinks(List<String> from, String to) {
            return String.format(
                    "Creating links from [%s] to %s",
                    String.join(", ", from),
                    env.path(to));
        }

        public String linkActionCreate(String from, String to) {
            return action("[CREATE    ]", from, to);
        }

        public String linkActionConflict(String from, String to) {
            return action("[CONFLICT  ]", from, to);
        }

        public List<String> linkActionUpdate(String from, String to, String previousTo) {
            return List.of(
                    action("[UPDATE    ]", from, to),
                    String.format("> Previous link target was %s", env.path(previousTo))
            );
        }

        public String linkActionUpToDate(String from, String to) {
            return action("[UP_TO_DATE]", from, to);
        }

        public String action(String action, String from, String to) {
            return String.format("%s %s -> %s", action, env.path(from), env.path(to));
        }

        public List<String> cannotCreateLinkError(String from, String to) {
            return List.of(
                    String.format("Unable to create link %s -> %s", env.path(from), env.path(to)),
                    String.format(
                            "> Regular file %s already exist. To overwrite it, use the --replace-file option.",
                            env.path(from))
            );
        }
    }
}