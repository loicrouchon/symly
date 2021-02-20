package org.linky.cli.converters;

import java.nio.file.Path;
import org.linky.links.TargetDirectory;
import picocli.CommandLine.ITypeConverter;

public class TargetDirectoryTypeConverter implements ITypeConverter<TargetDirectory> {

    public TargetDirectory convert(String value) {
        Path path = Path.of(value);
        return TargetDirectory.of(path);
    }
}
