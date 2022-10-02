package org.symly;

import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

public sealed interface Result<S, E> permits Result.Success, Result.ErrorResult {

    void accept(Consumer<S> successConsumer, Consumer<E> errorConsumer);

    static <E> Success<Void, E> success() {
        return new Success<>(null);
    }

    static <S, E> Success<S, E> success(S value) {
        return new Success<>(value);
    }

    static <S, E> ErrorResult<S, E> error(E value) {
        return new ErrorResult<>(value);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class Success<S, E> implements Result<S, E> {

        private final S value;

        @Override
        public void accept(Consumer<S> successConsumer, Consumer<E> errorConsumer) {
            successConsumer.accept(value);
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class ErrorResult<S, E> implements Result<S, E> {

        private final E value;

        @Override
        public void accept(Consumer<S> successConsumer, Consumer<E> errorConsumer) {
            errorConsumer.accept(value);
        }
    }
}
