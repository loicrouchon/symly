package org.symly.cli;

import picocli.CommandLine;

@CommandLine.Command(
        hidden = true,
        name = "man-page",
        subcommands = picocli.codegen.docgen.manpage.ManPageGenerator.class
)
class ManPageGenerator {
}
