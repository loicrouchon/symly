package org.symly.cli.converters;

import java.nio.file.Path;
import org.symly.repositories.MainDirectory;
import picocli.CommandLine.ITypeConverter;

public class MainDirectoryTypeConverter implements ITypeConverter<MainDirectory> {

    public MainDirectory convert(String value) {
        Path path = PathAdapter.convert(value);
        return MainDirectory.of(path);
    }
}
