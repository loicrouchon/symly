package org.symly.cli;

import java.nio.file.Path;
import org.symly.repositories.PathAdapter;
import picocli.CommandLine.ITypeConverter;

public class PathTypeConverter implements ITypeConverter<Path> {

    public Path convert(String value) {
        return PathAdapter.convert(value);
    }
}
