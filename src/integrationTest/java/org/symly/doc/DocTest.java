package org.symly.doc;

import org.junit.jupiter.api.Test;
import org.symly.cli.LinkCommandMessageFactory;
import org.symly.cli.StatusCommandMessageFactory;
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
class DocTest extends IntegrationTest {

    private final LinkCommandMessageFactory linkMsgs = new LinkCommandMessageFactory(env);
    private final StatusCommandMessageFactory statusMsgs = new StatusCommandMessageFactory(env);

    @Test
    void displayHelpExample() {
        // given
        given(env).withWorkingDir("home/user");
        // when/then
        var executionReport = whenRunningCommand().thenItShould().succeed().executionReport();

        AsciiDocSnippet.save("symly-default-output", executionReport.symlyExecution());
        AsciiDocSnippet.save(
                "locally-built-symly-default-output",
                executionReport.symlyExecution().replaceFirst("symly", "./build/install/symly/bin/symly"));
    }

    @Test
    void displayLinkHelpExample() {
        // given
        given(env).withWorkingDir("home/user");
        // when/then
        var executionReport =
                whenRunningCommand("link", "--help").thenItShould().succeed().executionReport();

        AsciiDocSnippet.save("symly-link-help", executionReport.symlyExecution());
    }

    @Test
    void displayLinkBasicExample() {
        // given
        given(env)
                .withWorkingDir("home/user")
                .withLayout(
                        """
    F home/user/repository/.config/fish/config.fish
    F home/user/repository/.config/starship.toml
    F home/user/repository/.bashrc
    F home/user/repository/.gitconfig
    """);
        // when/then
        var executionReport = whenRunningCommand("link", "--dir", "~", "--repositories", "repository")
                .thenItShould()
                .succeed()
                .withMessage(linkMsgs.linkActionCreate(
                        ".config/fish/config.fish", "home/user/repository/.config/fish/config.fish"))
                .withMessage(linkMsgs.linkActionCreate(
                        ".config/starship.toml", "home/user/repository/.config/starship.toml"))
                .withMessage(linkMsgs.linkActionCreate(".bashrc", "home/user/repository/.bashrc"))
                .withMessage(linkMsgs.linkActionCreate(".gitconfig", "home/user/repository/.gitconfig"))
                .withFileTreeDiff(
                        Diff.ofChanges(
                                """
    +L home/user/.config/fish/config.fish -> home/user/repository/.config/fish/config.fish
    +L home/user/.config/starship.toml -> home/user/repository/.config/starship.toml
    +L home/user/.bashrc -> home/user/repository/.bashrc
    +L home/user/.gitconfig -> home/user/repository/.gitconfig
    """))
                .executionReport();

        AsciiDocSnippet.save("symly-link-basic-example", executionReport.toString());
    }

    @Test
    void displayLinkMultipleRepositoriesExample() {
        // given
        given(env)
                .withWorkingDir("home/user")
                .withLayout(
                        """
F home/user/repositories/custom/.bashrc
F home/user/repositories/defaults/.config/starship.toml
F home/user/repositories/defaults/.gitconfig
""");
        // when/then
        var executionReport = whenRunningCommand(
                        "link", "--dir", "~", "--repositories", "repositories/defaults", "repositories/custom")
                .thenItShould()
                .succeed()
                .withMessage(linkMsgs.linkActionCreate(
                        ".config/starship.toml", "home/user/repositories/defaults/.config/starship.toml"))
                .withMessage(linkMsgs.linkActionCreate(".bashrc", "home/user/repositories/custom/.bashrc"))
                .withMessage(linkMsgs.linkActionCreate(".gitconfig", "home/user/repositories/defaults/.gitconfig"))
                .withFileTreeDiff(
                        Diff.ofChanges(
                                """
+L home/user/.config/starship.toml -> home/user/repositories/defaults/.config/starship.toml
+L home/user/.bashrc -> home/user/repositories/custom/.bashrc
+L home/user/.gitconfig -> home/user/repositories/defaults/.gitconfig
"""))
                .executionReport();

        String firstExecFileTreeBefore = executionReport.fileTreeBefore();
        String firstExecSymlyExecution = executionReport.symlyExecution();

        given(env).withLayout("""
                F home/user/repositories/custom/.gitconfig
                """);

        executionReport = whenRunningCommand(
                        "link", "--dir", "~", "--repositories", "repositories/defaults", "repositories/custom")
                .thenItShould()
                .succeed()
                .withMessage(linkMsgs.linkActionDelete(".gitconfig", "home/user/repositories/defaults/.gitconfig"))
                .withMessage(linkMsgs.linkActionCreate(".gitconfig", "home/user/repositories/custom/.gitconfig"))
                .withFileTreeDiff(
                        Diff.ofChanges(
                                """
-L home/user/.gitconfig -> home/user/repositories/defaults/.gitconfig
+L home/user/.gitconfig -> home/user/repositories/custom/.gitconfig
"""))
                .executionReport();

        AsciiDocSnippet.save(
                "symly-link-multiple-repositories-example",
                commands(
                        firstExecFileTreeBefore,
                        firstExecSymlyExecution,
                        "$ touch repositories/custom/.gitconfig",
                        executionReport.symlyExecution()));
    }

    @Test
    void displayLinkDirectoryLinkingExample() {
        // given
        given(env)
                .withWorkingDir("home/user")
                .withLayout(
                        """
F home/user/repository/.config/fish/config.fish
F home/user/repository/.config/fish/.symlink
""");
        // when/then
        var executionReport = whenRunningCommand("link", "--dir", "~", "--repositories", "repository")
                .thenItShould()
                .succeed()
                .withMessage(linkMsgs.linkActionCreate(".config/fish", "home/user/repository/.config/fish"))
                .withFileTreeDiff(
                        Diff.ofChanges("""
+L home/user/.config/fish -> home/user/repository/.config/fish
"""))
                .executionReport();

        AsciiDocSnippet.save(
                "symly-link-directory-linking-example",
                commands(executionReport.fileTreeBefore(), executionReport.symlyExecution()));
    }

    @Test
    void displayStatusHelpExample() {
        // given
        given(env).withWorkingDir("home/user");
        // when/then
        var executionReport =
                whenRunningCommand("status", "--help").thenItShould().succeed().executionReport();

        AsciiDocSnippet.save("symly-status-help", executionReport.symlyExecution());
    }

    @Test
    void displayStatusBasicExample() {
        // given
        given(env)
                .withWorkingDir("home/user")
                .withLayout(
                        """
L home/user/.gitconfig -> home/user/repository/.gitconfig
L home/user/.zshrc -> home/user/repository/.zshrc
F home/user/repository/.bashrc
F home/user/repository/.gitconfig
""");
        // when/then
        var executionReport = whenRunningCommand("status", "--dir", "~", "--repositories", "repository")
                .thenItShould()
                .succeed()
                .withMessage(statusMsgs.missingLink(".bashrc", "home/user/repository/.bashrc"))
                .withMessage(statusMsgs.orphanLink(".zshrc"))
                .executionReport();

        AsciiDocSnippet.save(
                "symly-status-basic-example",
                commands(executionReport.fileTreeBefore(), executionReport.symlyExecution()));
    }

    @Test
    void displayUnlinkHelpExample() {
        // given
        given(env).withWorkingDir("home/user");
        // when/then
        var executionReport =
                whenRunningCommand("unlink", "--help").thenItShould().succeed().executionReport();

        AsciiDocSnippet.save("symly-unlink-help", executionReport.symlyExecution());
    }

    private String commands(String... commands) {
        return String.join("\n\n", commands);
    }
}
