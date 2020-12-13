package org.linky.cli;

import java.util.Map;
import picocli.CommandLine;
import picocli.CommandLine.IDefaultValueProvider;

class EnvironmentVariableDefaultsProvider implements IDefaultValueProvider {

    private static final Map<String, String> OPTIONS_TO_VAR_NAME = Map.of(
            "--destination", "HOME",
            "--from", "HOME"
    );

    @Override
    public String defaultValue(CommandLine.Model.ArgSpec argSpec) {
        if (argSpec.isOption()) {
            String optionName = ((CommandLine.Model.OptionSpec) argSpec).longestName();
            String varName = OPTIONS_TO_VAR_NAME.get(optionName);
            if (varName != null) {
                String systemProperty = System.getProperty(varName);
                if (systemProperty != null) {
                    return systemProperty;
                }
                return System.getenv(varName);
            }
        }
        return null;
    }
}
