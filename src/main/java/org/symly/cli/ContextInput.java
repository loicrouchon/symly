package org.symly.cli;

import static java.util.function.Predicate.not;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.cli.validation.Constraint;
import org.symly.cli.validation.Validator;
import org.symly.files.FileSystemReader;
import org.symly.repositories.*;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@RequiredArgsConstructor
public class ContextInput {

    @NonNull
    private final FileSystemReader fsReader;

    @Option(
            names = {"-d", "--dir", "--directory"},
            paramLabel = "<main-directory>",
            description = "Main directory in which links will be created")
    Path mainDirectoryPath;

    @Option(
            names = {"-r", "--repositories"},
            paramLabel = "<repositories>",
            description =
                    """
        Repositories containing files to link in the main directory. \
        Repositories are to be listed by decreasing priority as the first ones will \
        override the content of the later ones.""",
            arity = "0..*")
    List<Path> repositoriesPaths;

    @Option(
            names = {"--max-depth"},
            paramLabel = "<max-depth>",
            description = "Depth of the lookup for orphans deletion")
    Integer maxDepth;

    private Validator validator;

    @Spec
    @SuppressWarnings("unused") // called by picocli
    void initializeValidator(CommandSpec spec) {
        validator = new Validator(spec);
    }

    public Context context() {
        ContextConfig contextConfig = ContextConfig.read(fsReader);
        MainDirectory mainDirectory = mainDirectory(contextConfig);
        Repositories repositories = repositories(contextConfig);
        int orphanMaxDepth = orphanMaxDepth(contextConfig);
        return new Context(mainDirectory, repositories, orphanMaxDepth);
    }

    private MainDirectory mainDirectory(ContextConfig contextConfig) {
        MainDirectory mainDirectory = Optional.ofNullable(mainDirectoryPath)
                .or(contextConfig::directory)
                .map(MainDirectory::of)
                .orElse(null);
        validator.validate(
                Constraint.of("Main directory is not defined", () -> mainDirectory != null),
                Constraint.of(
                        String.format("Main directory (%s) is not an existing directory", mainDirectory),
                        () -> fsReader.isADirectory(mainDirectory)));
        return mainDirectory;
    }

    private Repositories repositories(ContextConfig contextConfig) {
        Repositories repositories = Repositories.of(
                fsReader,
                Optional.ofNullable(repositoriesPaths)
                        .filter(not(Collection::isEmpty))
                        .orElseGet(contextConfig::repositories)
                        .stream()
                        .map(Repository::of)
                        .toList());
        Collection<Constraint> constraints = new ArrayList<>();
        constraints.add(Constraint.of(
                "Repositories are not defined",
                () -> !repositories.repositories().isEmpty()));
        repositories
                .repositories()
                .forEach(repository -> constraints.add(Constraint.of(
                        String.format("Repository (%s) is not an existing directory", repository.toPath()),
                        () -> fsReader.isADirectory(repository))));
        validator.validate(constraints);
        return repositories;
    }

    private int orphanMaxDepth(ContextConfig contextConfig) {
        int orphanMaxDepth = Optional.ofNullable(maxDepth).orElseGet(contextConfig::orphanMaxDepth);
        validator.validate(Constraint.of(
                String.format("Orphan lookup max-depth (%s) must be a positive integer", orphanMaxDepth),
                () -> orphanMaxDepth >= 0));
        return orphanMaxDepth;
    }
}
