package org.linky;

import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

public interface Result<S, E> {

    void accept(Consumer<S> successConsumer, Consumer<E> errorConsumer);

    S orThrow(Supplier<? extends RuntimeException> exception);

    @SuppressWarnings("unchecked")
    static <E> Success<Void, E> success() {
        return (Success<Void, E>) Success.EMPTY;
    }

    static <S, E> Success<S, E> success(S value) {
        return new Success<>(value);
    }

    @SuppressWarnings("unchecked")
    static <S> Error<S, Void> error() {
        return (Error<S, Void>) Error.EMPTY;
    }

    static <S, E> Error<S, E> error(E value) {
        return new Error<>(value);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Success<S, E> implements Result<S, E> {

        private static final Success<Void, ?> EMPTY = new Success<>(null);

        private final S value;

        @Override
        public void accept(Consumer<S> successConsumer, Consumer<E> errorConsumer) {
            successConsumer.accept(value);
        }

        @Override
        public S orThrow(Supplier<? extends RuntimeException> exception) {
            return value;
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Error<S, E> implements Result<S, E> {

        private static final Error<?, Void> EMPTY = new Error<>(null);

        private final E value;

        @Override
        public void accept(Consumer<S> successConsumer, Consumer<E> errorConsumer) {
            errorConsumer.accept(value);
        }

        @Override
        public S orThrow(Supplier<? extends RuntimeException> exception) {
            throw exception.get();
        }
    }
}
