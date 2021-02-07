package org.linky.cli;

import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

@RequiredArgsConstructor
class TestCommand<T> {

    private final TestConsole console = new TestConsole();
    private final Function<CliConsole, T> commandBuilder;

    public Execution<T> run(String... args) {
        T command = commandBuilder.apply(console);
        TestCommandFactory factory = new TestCommandFactory(Map.of(command.getClass(), command));
        int exitCode = Main.runCommand(factory, console, args);
        return new Execution<>(command, exitCode, console.stdOut(), console.stdErr());
    }

    @RequiredArgsConstructor
    private static class TestCommandFactory implements IFactory {

        private final IFactory defaultFactory = CommandLine.defaultFactory();

        private final Map<Class<?>, Object> commands;

        @Override
        public <K> K create(Class<K> cls) throws Exception {
            Object command = commands.get(cls);
            if (command != null) {
                return cls.cast(command);
            }
            return defaultFactory.create(cls);
        }
    }
}
