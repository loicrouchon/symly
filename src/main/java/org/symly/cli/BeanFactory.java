package org.symly.cli;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;
import org.symly.files.FileSystemWriterImpl;
import org.symly.repositories.LinksFinder;
import picocli.CommandLine;

public class BeanFactory implements CommandLine.IFactory {

    private final Map<Class<?>, Supplier<?>> constructors = new HashMap<>();

    private final Map<Class<?>, Object> beans = new HashMap<>();

    private final CommandLine.IFactory parentFactory = CommandLine.defaultFactory();

    public BeanFactory() {
        register(Config.class, Config::new);
        register(CliConsole.class, () -> new CliConsole(printWriter(System.out), printWriter(System.err)));
        register(FileSystemReader.class, FileSystemReader.RealFileSystemReader::new);
        register(FileSystemWriter.class, FileSystemWriterImpl::new);
        register(LinksFinder.class, () -> new LinksFinder(create(FileSystemReader.class)));
        register(VersionProvider.class, () -> new VersionProvider(create(Config.class)));
        register(MainCommand.class, () -> new MainCommand(create(Config.class), create(CliConsole.class)));
        register(ExceptionHandler.class, () -> new ExceptionHandler(create(Config.class), create(CliConsole.class)));
        register(
                LinkCommand.class,
                () -> new LinkCommand(
                        create(CliConsole.class),
                        create(FileSystemReader.class),
                        create(FileSystemWriter.class),
                        create(LinksFinder.class)));
        register(
                UnlinkCommand.class,
                () -> new UnlinkCommand(
                        create(CliConsole.class),
                        create(FileSystemReader.class),
                        create(FileSystemWriter.class),
                        create(LinksFinder.class)));
        register(
                StatusCommand.class,
                () -> new StatusCommand(
                        create(CliConsole.class), create(FileSystemReader.class), create(LinksFinder.class)));
        register(ContextInput.class, () -> new ContextInput(create(FileSystemReader.class)));
    }

    private static PrintWriter printWriter(PrintStream outputStream) {
        return new PrintWriter(outputStream, true, StandardCharsets.UTF_8);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K> K create(Class<K> cls) {
        if (!beans.containsKey(cls)) {
            loadBean(cls);
        }
        return (K) beans.get(cls);
    }

    private void loadBean(Class<?> cls) {
        if (constructors.containsKey(cls)) {
            beans.put(cls, constructors.get(cls).get());
        } else {
            try {
                beans.put(cls, parentFactory.create(cls));
            } catch (Exception e) {
                throw new IllegalStateException("Unable to create bean " + cls, e);
            }
        }
    }

    public <K> void register(Class<K> cls, Supplier<K> constructor) {
        constructors.put(cls, constructor);
    }
}
