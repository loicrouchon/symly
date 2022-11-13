package org.symly.cli;

import java.util.Arrays;
import java.util.Objects;
import picocli.CommandLine;

public class VersionProvider implements CommandLine.IVersionProvider {

    private final Config config;

    public VersionProvider(Config config) {
        this.config = Objects.requireNonNull(config);
    }

    @Override
    public String[] getVersion() {
        String symlyVersion = "%s version %s".formatted(config.applicationName(), config.applicationVersion());
        if (config.verbose()) {
            return new String[] {
                symlyVersion,
                "",
                "-- JVM information --",
                "%s %s".formatted(property("java.runtime.name"), property("java.version")),
                "%s %s %s"
                        .formatted(
                                property("java.vendor"),
                                property("java.vendor.version", "java.runtime.version"),
                                property("java.version.date")),
                "%s".formatted(property("java.home"))
            };
        } else {
            return new String[] {symlyVersion};
        }
    }

    private String property(String... names) {
        return Arrays.stream(names)
                .map(System::getProperty)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("");
    }
}
