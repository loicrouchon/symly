package org.symly.cli;

import picocli.AutoComplete.GenerateCompletion;
import picocli.CommandLine.Command;

@Command(
        hidden = true,
        name = "shell",
        subcommands = GenerateCompletion.class
)
public class Shell {

}
