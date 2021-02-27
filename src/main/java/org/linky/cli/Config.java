package org.linky.cli;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {

    private static final String APPLICATION_PROPERTIES = "/application.properties";

    private Properties properties;

    public String applicationName() {
        return property("application.name");
    }

    public String applicationVersion() {
        return property("application.version");
    }

    private String property(String property) {
        if (properties == null) {
            properties = load();
        }
        return properties.getProperty(property);
    }

    private static Properties load() {
        InputStream is = VersionProvider.class.getResourceAsStream(APPLICATION_PROPERTIES);
        try {
            Properties properties = new Properties();
            properties.load(is);
            return properties;
        } catch (IOException e) {
            throw new LinkyExecutionException("Unable to load application version", e);
        }
    }
}
