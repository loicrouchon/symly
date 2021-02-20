package org.linky.cli;

import java.nio.file.Path;
import org.linky.links.TargetDirectory;
import picocli.CommandLine;

class TargetDirectoryTypeConverter implements CommandLine.ITypeConverter<TargetDirectory> {

    public TargetDirectory convert(String value) {
        Path path = Path.of(value);
        return TargetDirectory.of(path);
    }
}
