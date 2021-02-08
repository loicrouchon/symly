package org.linky.env;

import lombok.Getter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class IntegrationTest {

    @Getter
    private Env env;

    @BeforeEach
    public final void setUpTemporaryEnvironment() {
        env = Env.of();
    }

    @AfterEach
    public final void tearDownTemporaryEnvironment() {
        if (env != null) {
            env.delete();
            env = null;
        }
    }
}
