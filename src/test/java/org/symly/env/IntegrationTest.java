package org.symly.env;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;

@Tag("integration-test")
public abstract class IntegrationTest {

    protected final Env env = Env.of();

    protected static Env given(Env env) {
        return env;
    }

    protected Execution whenRunningCommand(String... command) {
        return env.run(command);
    }

    @AfterEach
    public final void tearDownTemporaryEnvironment() {
        env.delete();
    }
}
