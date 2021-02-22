package org.linky.cli;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;

@RequiredArgsConstructor
public class VersionProvider implements CommandLine.IVersionProvider {

    @NonNull
    private final Config config;

    public VersionProvider() {
        this(new Config());
    }

    @Override
    public String[] getVersion() {
        return new String[]{
                String.format("%s version %s", config.applicationName(), config.applicationVersion()),
        };
    }
}
