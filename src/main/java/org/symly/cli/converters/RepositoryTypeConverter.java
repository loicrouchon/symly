package org.symly.cli.converters;

import java.nio.file.Path;
import org.symly.links.Repository;
import picocli.CommandLine.ITypeConverter;

public class RepositoryTypeConverter implements ITypeConverter<Repository> {

    public Repository convert(String value) {
        Path path = Path.of(value);
        return Repository.of(path);
    }
}
