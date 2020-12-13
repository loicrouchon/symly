package org.linky.cli;

import java.util.Map;
import picocli.CommandLine;
import picocli.CommandLine.IDefaultValueProvider;

public class EnvironmentVariableDefaultsProvider implements IDefaultValueProvider {

    private static final Map<String, String> OPTIONS_TO_ENV_VAR = Map.of(
            "--destination", "HOME",
            "--from", "HOME"
    );

    @Override
    public String defaultValue(CommandLine.Model.ArgSpec argSpec) {
        if (argSpec.isOption()) {
            String optionName = ((CommandLine.Model.OptionSpec) argSpec).longestName();
            String envVarName = OPTIONS_TO_ENV_VAR.get(optionName);
            if (envVarName != null) {
                return System.getenv(envVarName);
            }
        }
        return null;
    }
}
