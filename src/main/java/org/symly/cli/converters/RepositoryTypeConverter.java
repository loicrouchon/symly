package org.symly.cli.converters;

import java.nio.file.Path;
import org.symly.repositories.Repository;
import picocli.CommandLine.ITypeConverter;

public class RepositoryTypeConverter implements ITypeConverter<Repository> {

    public Repository convert(String value) {
        Path path = PathAdapter.convert(value);
        return Repository.of(path);
    }
}
