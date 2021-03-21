package org.symly;

import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

public interface Result<S, E> {

    void accept(Consumer<S> successConsumer, Consumer<E> errorConsumer);

    static <S, E> Success<S, E> success(S value) {
        return new Success<>(value);
    }

    static <S, E> Error<S, E> error(E value) {
        return new Error<>(value);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Success<S, E> implements Result<S, E> {

        private final S value;

        @Override
        public void accept(Consumer<S> successConsumer, Consumer<E> errorConsumer) {
            successConsumer.accept(value);
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Error<S, E> implements Result<S, E> {

        private final E value;

        @Override
        public void accept(Consumer<S> successConsumer, Consumer<E> errorConsumer) {
            errorConsumer.accept(value);
        }
    }
}
