package org.symly.doc;

import org.junit.jupiter.api.Test;
import org.symly.cli.LinkCommandMessageFactory;
import org.symly.env.IntegrationTest;
import org.symly.files.FileTree.Diff;

@SuppressWarnings({
    "java:S100", // Method names should comply with a naming convention (test method names)
    "java:S1192", // String literals should not be duplicated
})
class DocTest extends IntegrationTest {

    private final LinkCommandMessageFactory msg = new LinkCommandMessageFactory(env);

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
                .withMessage(msg.linkActionCreate(
                        ".config/fish/config.fish", "home/user/repository/.config/fish/config.fish"))
                .withMessage(
                        msg.linkActionCreate(".config/starship.toml", "home/user/repository/.config/starship.toml"))
                .withMessage(msg.linkActionCreate(".bashrc", "home/user/repository/.bashrc"))
                .withMessage(msg.linkActionCreate(".gitconfig", "home/user/repository/.gitconfig"))
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
                .withMessage(msg.linkActionCreate(
                        ".config/starship.toml", "home/user/repositories/defaults/.config/starship.toml"))
                .withMessage(msg.linkActionCreate(".bashrc", "home/user/repositories/custom/.bashrc"))
                .withMessage(msg.linkActionCreate(".gitconfig", "home/user/repositories/defaults/.gitconfig"))
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
                .withMessage(msg.linkActionDelete(".gitconfig", "home/user/repositories/defaults/.gitconfig"))
                .withMessage(msg.linkActionCreate(".gitconfig", "home/user/repositories/custom/.gitconfig"))
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
                .withMessage(msg.linkActionCreate(".config/fish", "home/user/repository/.config/fish"))
                .withFileTreeDiff(
                        Diff.ofChanges("""
+L home/user/.config/fish -> home/user/repository/.config/fish
"""))
                .executionReport();

        AsciiDocSnippet.save(
                "symly-link-directory-linking-example",
                commands(executionReport.fileTreeBefore(), executionReport.symlyExecution()));
    }

    private String commands(String... commands) {
        return String.join("\n\n", commands);
    }
}
