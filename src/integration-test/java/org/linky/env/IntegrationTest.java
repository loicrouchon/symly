package org.linky.env;

import java.util.ArrayList;
import java.util.Collection;
import org.junit.jupiter.api.AfterEach;

public abstract class IntegrationTest {

    private final Collection<Env> envs = new ArrayList<>();

    public Env env() {
        Env env = Env.of();
        envs.add(env);
        return env;
    }

    @AfterEach
    public final void tearDownTemporaryEnvironment() {
        envs.forEach(Env::delete);
        envs.clear();
    }
}
