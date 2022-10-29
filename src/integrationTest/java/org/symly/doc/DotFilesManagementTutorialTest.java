package org.symly.doc;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.symly.cli.LinkCommandMessageFactory;
import org.symly.env.IntegrationTest;
import org.symly.files.FileTree.Diff;

@SuppressWarnings({
    // Method names should comply with a naming convention (test method names)
    "java:S100",
    // String literals should not be duplicated
    "java:S1192",
    // Tests should include assertions: basic assertions are actually performed by `thenItShould().succeed()`
    // On top of that, this is not really a test in itself, but more of hack to generate the doc from the program itself
    "java:S2699"
})
class DotFilesManagementTutorialTest extends IntegrationTest {

    private final LinkCommandMessageFactory linkMsgs = new LinkCommandMessageFactory(env);

    @Test
    void displayLinkMultipleRepositoriesExample() {
        // given
        Path mydotfilesPath = Path.of("home/user/mydotfiles");
        given(env)
                .withHome("home/user/")
                .withWorkingDir(mydotfilesPath.toString())
                .withLayout(
                        """
                    F home/user/mydotfiles/.bashrc
                    F home/user/mydotfiles/.config/starship.toml
                    F home/user/mydotfiles/.gitconfig
                    """);
        // when/then
        initialRepoLink(mydotfilesPath);

        given(env)
                .deleteFile("home/user/mydotfiles/.bashrc")
                .withLayout("F home/user/mydotfiles/.config/fish/config.fish");

        updatedRepoLink(mydotfilesPath);

        given(env)
                .withFileContent(
                        "home/user/mydotfiles/symly.config",
                        """
                directory = ~
                repositories = .
                """);

        verifyWithStatus();

        given(env)
                .deleteFile("home/user/mydotfiles/.config/fish/config.fish")
                .deleteFile("home/user/mydotfiles/.config/starship.toml")
                .deleteFile("home/user/mydotfiles/.gitconfig")
                .withLayout(
                        """
                F home/user/mydotfiles/defaults/.config/fish/config.fish
                F home/user/mydotfiles/defaults/.config/starship.toml
                F home/user/mydotfiles/defaults/.gitconfig
                D home/user/mydotfiles/work
                """)
                .withFileContent(
                        "home/user/mydotfiles/symly.config",
                        """
        directory = ~
        repositories = defaults, work
        """);

        goMultiRepo();

        given(env).withLayout("F home/user/mydotfiles/work/.gitconfig");
        multiRepoOverride();
    }

    private void initialRepoLink(Path mydotfilesPath) {
        var report = whenRunningCommand("link", "--dir", "~", "--repositories", ".")
                .thenItShould()
                .succeed()
                .withMessage(linkMsgs.linkActionCreate(".bashrc", "home/user/mydotfiles/.bashrc"))
                .withMessage(linkMsgs.linkActionCreate(
                        ".config/starship.toml", "home/user/mydotfiles/.config/starship.toml"))
                .withMessage(linkMsgs.linkActionCreate(".gitconfig", "home/user/mydotfiles/.gitconfig"))
                .withFileTreeDiff(
                        Diff.ofChanges(
                                """
+L home/user/.bashrc -> home/user/mydotfiles/.bashrc
+L home/user/.config/starship.toml -> home/user/mydotfiles/.config/starship.toml
+L home/user/.gitconfig -> home/user/mydotfiles/.gitconfig
"""))
                .executionReport();

        AsciiDocSnippet.save("dotfiles-management-tutorial-1-simple-repo-structure", report.fileTreeBefore());
        AsciiDocSnippet.save("dotfiles-management-tutorial-2-initial-repo-linking", report.symlyExecution());

        AsciiDocSnippet.save(
                "dotfiles-management-tutorial-3-initial-repo-linking-state",
                report.fsTreeAsString(
                        report.currentFileTree().subtree(fe -> !fe.name().startsWith(mydotfilesPath)),
                        Path.of("home/user")));
    }

    private void updatedRepoLink(Path mydotfilesPath) {
        ExecutionDocReport report = whenRunningCommand("link", "--dir", "~", "--repositories", ".")
                .thenItShould()
                .succeed()
                .withMessage(linkMsgs.linkActionDelete(".bashrc", "home/user/mydotfiles/.bashrc"))
                .withMessage(linkMsgs.linkActionCreate(
                        ".config/fish/config.fish", "home/user/mydotfiles/.config/fish/config.fish"))
                .withFileTreeDiff(
                        Diff.ofChanges(
                                """
-L home/user/.bashrc -> home/user/mydotfiles/.bashrc
+L home/user/.config/fish/config.fish -> home/user/mydotfiles/.config/fish/config.fish
"""))
                .executionReport();

        AsciiDocSnippet.save("dotfiles-management-tutorial-4-repo-update-linking", report.symlyExecution());
        AsciiDocSnippet.save(
                "dotfiles-management-tutorial-5-repo-update-linking-state",
                report.fsTreeAsString(
                        report.currentFileTree().subtree(fe -> !fe.name().startsWith(mydotfilesPath)),
                        Path.of("home/user")));
    }

    private void verifyWithStatus() {
        var report = whenRunningCommand("status", "-v")
                .thenItShould()
                .succeed()
                .withMessage("Everything is already up to date")
                .executionReport();
        AsciiDocSnippet.save("dotfiles-management-tutorial-6-repo-status", report.symlyExecution());
    }

    private void goMultiRepo() {
        var report = whenRunningCommand("link")
                .thenItShould()
                .succeed()
                .withMessages(linkMsgs.linkActionUpdate(
                        ".config/fish/config.fish",
                        "home/user/mydotfiles/defaults/.config/fish/config.fish",
                        "home/user/mydotfiles/.config/fish/config.fish"))
                .withMessages(linkMsgs.linkActionUpdate(
                        ".config/starship.toml",
                        "home/user/mydotfiles/defaults/.config/starship.toml",
                        "home/user/mydotfiles/.config/starship.toml"))
                .withMessages(linkMsgs.linkActionUpdate(
                        ".gitconfig", "home/user/mydotfiles/defaults/.gitconfig", "home/user/mydotfiles/.gitconfig"))
                .executionReport();
        AsciiDocSnippet.save("dotfiles-management-tutorial-7-multi-repo-setup", report.symlyExecution());
    }

    private void multiRepoOverride() {
        var report = whenRunningCommand("link")
                .thenItShould()
                .succeed()
                .withMessages(linkMsgs.linkActionUpdate(
                        ".gitconfig",
                        "home/user/mydotfiles/work/.gitconfig",
                        "home/user/mydotfiles/defaults/.gitconfig"))
                .executionReport();
        AsciiDocSnippet.save("dotfiles-management-tutorial-8-multi-repo-override", report.symlyExecution());
    }
}
