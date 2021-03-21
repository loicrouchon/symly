package org.symly.env;

import org.junit.jupiter.api.AfterEach;

public abstract class IntegrationTest {

    protected final Env env = Env.of();

    protected static Env given(Env env) {
        return env;
    }

    protected Execution whenRunningCommand(String... command) {
        return env.run(command);
    }

    @AfterEach
    final void tearDownTemporaryEnvironment() {
        env.delete();
    }
}
