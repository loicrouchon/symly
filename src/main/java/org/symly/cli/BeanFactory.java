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
        registerBean(Config.class, Config::new);
        registerBean(CliConsole.class, () -> new CliConsole(printWriter(System.out), printWriter(System.err)));
        registerBean(FileSystemReader.class, FileSystemReader.RealFileSystemReader::new);
        registerBean(FileSystemWriter.class, FileSystemWriterImpl::new);
        registerBean(LinksFinder.class, () -> new LinksFinder(create(FileSystemReader.class)));
        registerBean(VersionProvider.class, () -> new VersionProvider(create(Config.class)));
        registerBean(MainCommand.class, () -> new MainCommand(create(Config.class), create(CliConsole.class)));
        registerBean(
                ExceptionHandler.class, () -> new ExceptionHandler(create(Config.class), create(CliConsole.class)));
        registerBean(
                LinkCommand.class,
                () -> new LinkCommand(
                        create(CliConsole.class),
                        create(FileSystemReader.class),
                        create(FileSystemWriter.class),
                        create(LinksFinder.class)));
        registerBean(
                UnlinkCommand.class,
                () -> new UnlinkCommand(
                        create(CliConsole.class),
                        create(FileSystemReader.class),
                        create(FileSystemWriter.class),
                        create(LinksFinder.class)));
        registerBean(
                StatusCommand.class, () -> new StatusCommand(create(CliConsole.class), create(FileSystemReader.class)));
        registerBean(ContextInput.class, () -> new ContextInput(create(FileSystemReader.class)));
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

    public <K> void registerBean(Class<K> cls, Supplier<K> constructor) {
        constructors.put(cls, constructor);
    }
}
