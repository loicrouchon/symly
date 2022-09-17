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
    void shouldLinkFile_whenTargetFileDoesNotExist() {
        // given
        given(env)
                .withWorkingDir("home/user")
                .withLayout(
                        """
            F home/user/repository/.config/fish/config.fish
            F home/user/repository/.config/starship.toml
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
                .withMessage(msg.linkActionCreate(".gitconfig", "home/user/repository/.gitconfig"))
                .withFileTreeDiff(
                        Diff.ofChanges(
                                """
                +L home/user/.config/fish/config.fish -> home/user/repository/.config/fish/config.fish
                +L home/user/.config/starship.toml -> home/user/repository/.config/starship.toml
                +L home/user/.gitconfig -> home/user/repository/.gitconfig
                """))
                .execution();

        System.out.println(executionReport);
    }

    @Test
    void displayHelpExample() {
        // given
        given(env).withWorkingDir("home/user");
        // when/then
        var executionReport = whenRunningCommand().thenItShould().succeed().executionReport();

        Documentation.updateSnippets(
                "locally-built-symly-default-output",
                executionReport.symlyExecution().replaceFirst("symly", "./build/install/symly/bin/symly"));
    }
}
