package org.linky.env;

import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class IntegrationTest {

    private Env env;

    @BeforeEach
    final void setUpTemporaryEnvironment() {
        env = Env.of();
    }

    @AfterEach
    final void tearDownTemporaryEnvironment() {
        if (env != null) {
            env.delete();
            env = null;
        }
    }

    protected Env givenCleanEnv() {
        return getEnv();
    }

    protected Env getEnv() {
        return env;
    }

    protected Path path(String path) {
        return env.path(path);
    }

    protected Path home() {
        return env.home();
    }

    protected Execution whenRunningCommand(String... command) {
        return env.run(command);
    }
}
