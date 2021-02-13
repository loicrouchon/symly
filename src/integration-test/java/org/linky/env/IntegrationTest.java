package org.linky.env;

import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;

public abstract class IntegrationTest {

    protected final Env env = Env.of();

    @AfterEach
    final void tearDownTemporaryEnvironment() {
        env.delete();
    }

    protected static Env given(Env env) {
        return env;
    }

    protected Execution whenRunningCommand(String... command) {
        return env.run(command);
    }

    protected Path path(String path) {
        return env.path(path);
    }

    protected Path home() {
        return env.home();
    }
}
