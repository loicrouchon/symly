package org.symly.cli;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;

@RequiredArgsConstructor
public class VersionProvider implements CommandLine.IVersionProvider {

    @NonNull
    private final Config config;

    @Override
    public String[] getVersion() {
        return new String[] {
            "%s version %s".formatted(config.applicationName(), config.applicationVersion()),
        };
    }
}
