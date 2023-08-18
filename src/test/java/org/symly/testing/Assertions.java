package org.symly.testing;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Assertions {

    public static BooleanContainer assertThat(boolean value) {
        return new BooleanContainer(value);
    }

    public static <T> OptionalContainer<T> assertThat(
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<T> value) {
        return new OptionalContainer<>(value);
    }

    public static <T> ListContainer<T> assertThat(Stream<? extends T> value) {
        return new ListContainer<>(value.toList());
    }

    public static <T> ListContainer<T> assertThat(Collection<? extends T> value) {
        return new ListContainer<>(value);
    }

    @SafeVarargs
    public static <T> ListContainer<T> assertThat(T... value) {
        return new ListContainer<>(List.of(value));
    }

    public static <T> Container<T> assertThat(T value) {
        return new Container<>(value);
    }

    public static <T> CodeContainer<T> assertThatCode(Callable<T> code) {
        return new CodeContainer<>(code);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static class Container<T> {

        protected final T value;
        protected String failMessage;

        public Container(T value) {
            this.value = value;
        }

        public Container<T> withFailMessage(String message, Object... args) {
            failMessage = message.formatted(args);
            return this;
        }

        protected void failWithMessage(String message) {
            fail(Objects.requireNonNullElse(failMessage, message));
        }

        public Container<T> isNotNull() {
            assertNotNull(value, failMessage);
            return this;
        }

        public Container<T> isEqualTo(T expected) {
            assertEquals(expected, value, failMessage);
            return this;
        }

        public Container<T> hasToString(String expected) {
            assertNotNull(value, failMessage);
            assertEquals(expected, value.toString(), failMessage);
            return this;
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static class BooleanContainer extends Container<Boolean> {

        public BooleanContainer(Boolean value) {
            super(value);
        }

        public BooleanContainer isTrue() {
            assertTrue(value, failMessage);
            return this;
        }

        public BooleanContainer isFalse() {
            assertFalse(value, failMessage);
            return this;
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static class OptionalContainer<T> extends Container<Optional<T>> {

        public OptionalContainer(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<T> value) {
            super(value);
        }

        public OptionalContainer<T> isEmpty() {
            if (value.isPresent()) {
                failWithMessage("Expecting %s to be empty".formatted(value));
            }
            return this;
        }

        public OptionalContainer<T> isPresent() {
            if (value.isEmpty()) {
                failWithMessage("Expecting %s to have a value".formatted(value));
            }
            return this;
        }

        public OptionalContainer<T> hasValue(T expected) {
            isPresent();
            @SuppressWarnings("OptionalGetWithoutIsPresent")
            T actual = value.get();
            assertEquals(expected, actual);
            return this;
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static class ListContainer<T> extends Container<Collection<? extends T>> {

        public ListContainer(Collection<? extends T> value) {
            super(value);
        }

        @Override
        public ListContainer<T> withFailMessage(String message, Object... args) {
            super.withFailMessage(message, args);
            return this;
        }

        public ListContainer<T> isEmpty() {
            return hasSize(0);
        }

        public ListContainer<T> hasSize(int size) {
            assertEquals(value.size(), size, failMessage);
            return this;
        }

        @SafeVarargs
        public final ListContainer<T> contains(T... elements) {
            containsAll(value, List.of(elements));
            return this;
        }

        public ListContainer<T> doesNotContain(T element) {
            if (value.contains(element)) {
                failWithMessage("""
        Expecting:
        %s

        Not to be found in:
        %s"""
                        .formatted(element, value));
            }
            return this;
        }

        private void containsAll(Collection<? extends T> a, Collection<? extends T> b) {
            for (T element : b) {
                if (!a.contains(element)) {
                    failWithMessage(
                            """
        Element:
        \t - %s

        Not found in collection:
        %s
        """
                                    .formatted(
                                            element,
                                            a.stream()
                                                    .map(Objects::toString)
                                                    .collect(Collectors.joining("\n\t - ", "\t - ", ""))));
                }
            }
        }

        @SafeVarargs
        public final ListContainer<T> containsExactly(T... elements) {
            assertEquals(List.copyOf(value), List.of(elements));
            return this;
        }

        @SafeVarargs
        public final ListContainer<T> containsExactlyInAnyOrder(T... elements) {
            return containsExactlyInAnyOrderElementsOf(List.of(elements));
        }

        public Container<T> first() {
            if (value.isEmpty()) {
                failWithMessage("Expecting collection not to be empty");
            }
            return assertThat(value.iterator().next());
        }

        public ListContainer<T> containsExactlyInAnyOrderElementsOf(Collection<? extends T> elements) {
            hasSize(elements.size());
            containsAll(value, elements);
            containsAll(elements, value);
            return this;
        }
    }

    public static class CodeContainer<T> {
        private final Callable<T> code;

        public CodeContainer(Callable<T> code) {
            this.code = code;
        }

        public <E extends Throwable> ThrowableContainer<E> throwsThrowableOfType(Class<E> cls) {
            try {
                code.call();
            } catch (Throwable t) {
                if (cls.isInstance(t)) {
                    return new ThrowableContainer<>(cls.cast(t));
                } else {
                    fail(
                            """
                    Expected a throwable of type %s to be thrown, but %s was thrown instead:

                    %s"""
                                    .formatted(cls, t.getClass(), t));
                }
            }
            fail("Expected a throwable of type %s to be thrown, but nothing was thrown".formatted(cls));
            throw new IllegalStateException("unreachable");
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static class ThrowableContainer<T extends Throwable> extends Container<T> {

        public ThrowableContainer(T value) {
            super(value);
        }

        public ThrowableContainer<T> hasMessage(String message) {
            assertEquals(value.getMessage(), message);
            return this;
        }

        public ThrowableContainer<T> hasMessageStartingWith(String message) {
            String actualMessage = value.getMessage();
            if (!actualMessage.startsWith(message)) {
                failWithMessage(
                        """
                    Expected message:
                    %s

                    to start with:
                    %s"""
                                .formatted(actualMessage, message));
            }
            return this;
        }
    }
}
