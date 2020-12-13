package org.linky.cli;

import static java.util.function.Predicate.not;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.linky.Result;
import org.linky.cli.validation.Constraint;
import org.linky.files.FileSystemReader;
import org.linky.files.FileSystemWriter;
import org.linky.files.FileSystemWriterImpl;
import org.linky.links.Action;
import org.linky.links.Configuration;
import org.linky.links.Link;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
        name = "add",
        description = "Adds a file to a source by moving it and linking itreplaces the original source by a link to the move destination"
)
@RequiredArgsConstructor
class AddCommand extends ValidatedCommand {

    @Option(
            names = {"-f", "--from"},
            description = "From directory in the file to move must be located and replaced by a link",
            required = true,
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS
    )
    Path from;

    @Option(
            names = {"-t", "--to"},
            description = "To directory in the file will be moved",
            required = true
    )
    Path to;

    @Parameters(index = "0", description = "File or directory to be moved from <from> to <to>")
    Path file;

    private final CliConsole console;
    private final FileSystemReader fsReader;
    private final FileSystemWriter fsWriter;

    private Path name;
    private Path destinationFile;

    AddCommand() {
        this(CliConsole.console(), new FileSystemReader(), new FileSystemWriterImpl());
    }

    @Override
    protected Collection<Constraint> constraints() {
        name = from.toAbsolutePath().relativize(file.toAbsolutePath()).normalize();
        destinationFile = to.resolve(name).toAbsolutePath().normalize();
        return List.of(
                Constraint.ofArg("from", from, "must be an existing directory", fsReader::isDirectory),
                Constraint.ofArg("from", from, "must not be a symbolic link", not(fsReader::isSymbolicLink)),
                Constraint.ofArg("to", to, "must be an existing directory", fsReader::isDirectory),
                Constraint.ofArg("file", file, "must exists", fsReader::exists),
                Constraint.of(
                        String.format("<file> (%s) must be a subpath of <from> (%s)", file, from),
                        () -> file.startsWith(from)),
                Constraint.of(
                        String.format("<file> (%s) must not exist in <to> (%s)", name, to),
                        () -> !fsReader.exists(destinationFile))
        );
    }

    @Override
    protected void execute() {
        console.printf(
                "Moving %s from %s to %s and creating link%n",
                name,
                from.toAbsolutePath().normalize(),
                to.toAbsolutePath().normalize());
        add(file, destinationFile);
    }

    private void add(Path originalFile, Path destinationFile) {
        Path destinationDirectory = destinationFile.getParent();
        if (!fsReader.exists(destinationDirectory)) {
            createParentDirectory(destinationDirectory);
        }
        moveFile(originalFile, destinationFile);
        if (fsReader.isDirectory(destinationFile)) {
            createSymlinkMarker(destinationFile);
        }
        createLink(originalFile, destinationFile);
    }

    private void createParentDirectory(Path destinationDirectory) {
        try {
            fsWriter.createDirectories(destinationDirectory);
        } catch (IOException e) {
            throw new LinkyExecutionException(String.format(
                    "Unable to create destination directory %s%n> %s%n", destinationDirectory,
                    e.getMessage()));
        }
    }

    private void moveFile(Path originalFile, Path destinationFile) {
        try {
            fsWriter.move(originalFile, destinationFile);
        } catch (IOException e) {
            throw new LinkyExecutionException(String.format(
                    "Unable to move %s to %s%n> %s%n", originalFile, destinationFile, e.getMessage()));
        }
    }

    private void createSymlinkMarker(Path destinationFile) {
        Path symlinkMarker = Configuration.symlinkMarker(destinationFile);
        try {
            fsWriter.createEmptyFile(symlinkMarker);
        } catch (IOException e) {
            throw new LinkyExecutionException(String.format(
                    "Unable to create directory symlink marker %s%n> %s%n", symlinkMarker, e.getMessage()));
        }
    }

    private void createLink(Path originalFile, Path destinationFile) {
        Link link = Link.of(originalFile, destinationFile);
        Result<Path, Action.Code> linkResult = link
                .status(fsReader)
                .toAction()
                .apply(fsWriter);
        linkResult.accept(
                previousPath -> console.printf("[MOVED] %s%n", link),
                errorCode -> {
                    throw new LinkyExecutionException(
                            String.format("Unable to create link %s%n> %s%n", link, errorCode.getDetails()));
                }
        );
    }
}
