package org.symly.cli;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class Config {

    private static final String APPLICATION_PROPERTIES = "/application.properties";

    private final Map<String, String> properties;

    public Config() {
        this.properties = load();
    }

    public String applicationName() {
        return property("application.name");
    }

    public String applicationVersion() {
        return property("application.version");
    }

    private String property(String property) {
        return properties.get(property);
    }

    private static Map<String, String> load() {
        try (InputStream is = Config.class.getResourceAsStream(APPLICATION_PROPERTIES)) {
            Properties properties = new Properties();
            properties.load(is);
            return properties.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> asString(e.getKey()),
                            e -> asString(e.getValue())));
        } catch (IOException e) {
            throw new SymlyExecutionException("Unable to load application version", e);
        }
    }

    private static String asString(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }
}
