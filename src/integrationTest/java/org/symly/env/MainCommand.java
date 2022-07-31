package org.symly.env;

import static org.assertj.core.api.Fail.fail;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.cli.BeanFactory;
import org.symly.cli.CliConsole;
import org.symly.cli.Main;
import org.symly.files.FileTree;
import org.symly.repositories.PathAdapter;

@SuppressWarnings({"java:S5960" // Assertions should not be used in production code (this is test code)
})
@RequiredArgsConstructor
public class MainCommand {

    @NonNull
    private final Path rootDir;

    @NonNull
    private final Path workingDir;

    @NonNull
    private final Path home;

    public Execution run(String[] args) {
        FileTree rootFileTreeSnapshot = FileTree.fromPath(rootDir);
        try (SystemProperties sysProps = new SystemProperties();
                PrintChannel stdOut = new PrintChannel();
                PrintChannel stdErr = new PrintChannel()) {
            sysProps.set("user.home", home);
            sysProps.set(PathAdapter.SYMLY_CWD_PROPERTY, workingDir.toAbsolutePath());
            BeanFactory beanFactory = new BeanFactory();
            beanFactory.registerBean(
                    CliConsole.class, () -> new CliConsole(stdOut.printWriter(), stdErr.printWriter()));
            int exitCode = Main.runCommand(beanFactory, args);
            return Execution.of(rootFileTreeSnapshot, rootDir, workingDir, exitCode, stdOut.reader(), stdErr.reader());
        } catch (Exception e) {
            fail("Command execution failed", e);
            throw new IllegalStateException("unreachable");
        }
    }

    private static class PrintChannel implements AutoCloseable {

        private final StringWriter channel = new StringWriter();
        private final PrintWriter printWriter = new PrintWriter(channel);
        private StringReader reader;

        public PrintWriter printWriter() {
            return printWriter;
        }

        public Reader reader() throws IOException {
            if (reader == null) {
                reader = new StringReader(channel.toString());
                closeWriter();
            }
            return reader;
        }

        @Override
        public void close() throws Exception {
            try {
                reader().close();
            } finally {
                closeWriter();
            }
        }

        private void closeWriter() throws IOException {
            try {
                printWriter.close();
            } finally {
                channel.close();
            }
        }
    }

    private static class SystemProperties implements AutoCloseable {

        private final Map<String, String> originalProperties = new HashMap<>();

        public void set(String key, Path value) {

            originalProperties.put(key, System.getProperty(key));
            setSystemProperty(key, value);
        }

        private void setSystemProperty(String key, Object value) {
            if (value != null) {
                System.setProperty(key, value.toString());
            } else {
                System.clearProperty(key);
            }
        }

        @Override
        public void close() {
            originalProperties.forEach(this::setSystemProperty);
        }
    }
}
