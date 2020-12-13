package org.linky.cli;

import static picocli.CommandLine.Spec;

import java.io.IOException;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.linky.Result;
import org.linky.files.FilesMutatorServiceImpl;
import org.linky.files.FilesReaderService;
import org.linky.links.Action;
import org.linky.links.Configuration;
import org.linky.links.Link;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
        name = "add",
        description = "Adds a file to a source by moving it and linking itreplaces the original source by a link to the move destination"
)
@RequiredArgsConstructor
public class AddCommand implements Runnable {

    @Spec
    CommandSpec spec;

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

    private final FilesReaderService filesReaderService;
    private final Validators validators;

    public AddCommand() {
        filesReaderService = new FilesReaderService();
        validators = new Validators(filesReaderService);
    }

    @Override
    public void run() {
        Path name = from.relativize(file).normalize();
        Path destinationFile = to.resolve(name).toAbsolutePath().normalize();
        Arg.of(spec, "from").validate(from, validators::directoryExists/*, isNotSymbolicLink*/);
        Arg.of(spec, "to").validate(to, validators::directoryExists);
        Arg.of(spec, "path").validate(file, Validator.combine(
                validators::exists,
                p -> validators.isSubPathOf(p, from),
                p -> validators.doesNotExists(destinationFile)
        ));
        CliConsole console = CliConsole.console();
        console.printf(
                "Moving %s from %s to %s and creating link%n",
                name,
                from.toAbsolutePath().normalize(),
                to.toAbsolutePath().normalize());
        add(console, file, destinationFile);
    }

    private void add(CliConsole console, Path originalFile, Path destinationFile) {
        FilesMutatorServiceImpl filesMutatorService = new FilesMutatorServiceImpl();
        Path destinationDirectory = destinationFile.getParent();
        if (!filesReaderService.exists(destinationDirectory)) {
            try {
                filesMutatorService.createDirectories(destinationDirectory);
            } catch (IOException e) {
                throw new LinkyExecutionException(String.format(
                        "Unable to create destination directory %s%n> %s%n", destinationDirectory,
                        e.getMessage()));
            }
        }
        try {
            filesMutatorService.move(originalFile, destinationFile);
        } catch (IOException e) {
            throw new LinkyExecutionException(String.format(
                    "Unable to move %s to %s%n> %s%n", originalFile, destinationFile, e.getMessage()));
        }
        if (filesReaderService.isDirectory(destinationFile)) {
            Path symlinkMarker = Configuration.symlinkMarker(destinationFile);
            try {
                filesMutatorService.createEmptyFile(symlinkMarker);
            } catch (IOException e) {
                throw new LinkyExecutionException(String.format(
                        "Unable to create directory symlink marker %s%n> %s%n", symlinkMarker, e.getMessage()));
            }
        }
        Link link = new Link(originalFile, destinationFile);
        Result<Path, Action.Code> linkResult = link
                .status(filesReaderService)
                .toAction()
                .apply(filesMutatorService);
        linkResult.accept(
                previousPath -> console.printf("[MOVED] %s%n", link),
                errorCode -> {
                    throw new LinkyExecutionException(
                            String.format("Unable to create link %s%n> %s%n", link, errorCode.getDetails()));
                }
        );
    }
}
