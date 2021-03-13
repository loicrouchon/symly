package org.symly.cli;

import java.util.Map;
import picocli.CommandLine;
import picocli.CommandLine.IDefaultValueProvider;

public class EnvironmentVariableDefaultsProvider implements IDefaultValueProvider {

    public static final String USER_HOME = "user.home";

    private static final Map<String, String> OPTIONS_TO_VAR_NAME = Map.of(
            "--source-directory", USER_HOME,
            "--from", USER_HOME
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
