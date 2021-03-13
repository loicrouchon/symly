package org.symly.cli.converters;

import java.nio.file.Path;
import org.symly.links.SourceDirectory;
import picocli.CommandLine.ITypeConverter;

public class SourceDirectoryTypeConverter implements ITypeConverter<SourceDirectory> {

    public SourceDirectory convert(String value) {
        Path path = Path.of(value);
        return SourceDirectory.of(path);
    }
}
