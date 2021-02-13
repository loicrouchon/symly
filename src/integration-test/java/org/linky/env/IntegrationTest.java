package org.linky.env;

import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;

public abstract class IntegrationTest {

    private Env env;

    protected Env givenCleanEnv() {
        if (env == null) {
            env = Env.of();
            return env;
        }
        throw new IllegalStateException("Only one temporary environment can be created per test.");
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

    @AfterEach
    final void tearDownTemporaryEnvironment() {
        if (env != null) {
            env.delete();
            env = null;
        }
    }
}
