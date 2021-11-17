package org.symly.cli;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;
import org.symly.files.FileSystemWriterImpl;
import picocli.CommandLine;

class BeanFactory implements CommandLine.IFactory {

    private final Map<Class<?>, Supplier<?>> constructors = Map.ofEntries(
            bean(Config.class, Config::new),
            bean(CliConsole.class, () -> new CliConsole(new PrintWriter(System.out), new PrintWriter(System.err))),
            bean(FileSystemReader.class, FileSystemReader::new),
            bean(FileSystemWriter.class, FileSystemWriterImpl::new),
            bean(VersionProvider.class, () -> new VersionProvider(create(Config.class))),
            bean(MainCommand.class, () -> new MainCommand(create(Config.class), create(CliConsole.class))),
            bean(ExceptionHandler.class, () -> new ExceptionHandler(create(Config.class), create(CliConsole.class))),
            bean(
                    LinkCommand.class,
                    () -> new LinkCommand(create(CliConsole.class), create(FileSystemReader.class),
                            create(FileSystemWriter.class))),
            bean(
                    StatusCommand.class,
                    () -> new StatusCommand(create(CliConsole.class), create(FileSystemReader.class)))
    );

    private final Map<Class<?>, Object> beans = new HashMap<>();

    private final CommandLine.IFactory parentFactory = CommandLine.defaultFactory();

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

    private static <K> Map.Entry<Class<K>, Supplier<K>> bean(Class<K> cls, Supplier<K> constructor) {
        return Map.entry(cls, constructor);
    }
}
