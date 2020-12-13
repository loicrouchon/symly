package org.linky.cli;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class Execution<T> {

    private static final int VALIDATION_FAILED_CODE = 2;

    final T command;
    private final int exitCode;
    private final String out;
    private final String err;

    public void assertThatValidationFailedWithMessage(String message) {
        assertThat(err).contains(message);
        assertThat(out).isEmpty();
        assertThat(exitCode).isEqualTo(VALIDATION_FAILED_CODE);
    }
}
