package org.linky.files;

import java.util.Properties;
import lombok.Getter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class TemporaryFolderTest {

    @Getter
    private Env env;

    private Properties propertiesBackup;

    @BeforeEach
    public final void setUpTemporaryEnvironment() {
        propertiesBackup = (Properties) System.getProperties().clone();
        env = Env.of();
    }

    @AfterEach
    public final void tearDownTemporaryEnvironment() {
        restoreSystemProperties();
        deleteWorkingDir();
    }

    private void deleteWorkingDir() {
        if (env != null) {
            env.delete();
            env = null;
        }
    }

    private void restoreSystemProperties() {
        if (propertiesBackup != null) {
            System.setProperties(propertiesBackup);
            propertiesBackup = null;
        }
    }
}
