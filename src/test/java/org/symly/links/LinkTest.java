package org.symly.links;

import static org.symly.testing.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LinkTest {

    @Test
    void toString_shouldDisplayLink() {
        assertThat(Link.of(Path.of("from"), Path.of("to"))).hasToString("from -> to");
    }
}
