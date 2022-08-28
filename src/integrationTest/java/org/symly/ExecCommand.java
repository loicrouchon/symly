package org.symly;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.symly.ExecCommand.ExampleDefinition;

public class ExecCommand {

    interface FSEntry {
        void create(Path parent) throws IOException;

        static FSEntry parse(String entry) {
            String[] parts = entry.split(":");
            assertLength(parts, "L".equals(parts[0]) ? 3 : 2);
            return switch (parts[0]) {
                case "D" -> new DirectoryEntry(chrootPath(parts[1]));
                case "F" -> new FileEntry(chrootPath(parts[1]));
                case "L" -> new LinkEntry(chrootPath(parts[1]), Path.of(parts[2]));
                default -> throw new IllegalArgumentException("Unsupported entry " + entry);
            };
        }

        private static void assertLength(String[] parts, int length) {
            if (parts.length != length) {
                throw new IllegalArgumentException(
                        String.format("Unsupported entry %s. Expecting length %d", Arrays.toString(parts), length));
            }
        }
    }

    record DirectoryEntry(Path name) implements FSEntry {
        DirectoryEntry {
            if (name.isAbsolute()) {
                throw new IllegalArgumentException("Only relative path are supported: " + name);
            }
        }

        @Override
        public void create(Path parent) throws IOException {
            Files.createDirectories(parent.resolve(name));
        }

        @Override
        public String toString() {
            return "D:" + name;
        }
    }

    record FileEntry(Path name) implements FSEntry {
        FileEntry {
            if (name.isAbsolute()) {
                throw new IllegalArgumentException("Only relative path are supported: " + name);
            }
        }

        @Override
        public void create(Path parent) throws IOException {
            Path path = parent.resolve(name);
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        }

        @Override
        public String toString() {
            return "F:" + name;
        }
    }

    record LinkEntry(Path name, Path target) implements FSEntry {
        LinkEntry {
            if (name.isAbsolute()) {
                throw new IllegalArgumentException("Only relative path are supported: " + name);
            }
            if (target.isAbsolute()) {
                throw new IllegalArgumentException("Only relative path are supported: " + target);
            }
        }

        @Override
        public void create(Path parent) throws IOException {
            Path path = parent.resolve(name);
            Files.createDirectories(path.getParent());
            Files.createLink(path, parent.resolve(target));
        }

        @Override
        public String toString() {
            return "L:" + name + ":" + target;
        }
    }

    record ExampleDefinition(Path name, List<FSEntry> fsTree, Path cwd, List<String> command) {

        ExampleDefinition {
            if (cwd.isAbsolute()) {
                throw new IllegalArgumentException("Only relative path are supported: " + cwd);
            }
        }

        public static ExampleDefinition read(Path path) throws IOException {
            Properties properties = new Properties();
            try (var is = new FileInputStream(path.toFile())) {
                properties.load(is);
                List<FSEntry> tree = Arrays.stream(
                                properties.getProperty("filetree").split(" "))
                        .map(FSEntry::parse)
                        .toList();
                Path cwd = chrootPath(properties.getProperty("cwd"));
                List<String> command = List.of(properties.getProperty("command").split(" "));
                return new ExampleDefinition(path, tree, cwd, command);
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 1) {
            usage();
            System.exit(1);
        }
        try (FsRoot fsRoot = FsRoot.create()) {
            ExampleDefinition example = ExampleDefinition.read(Path.of(args[0]));
            System.out.println(example);
            execute(fsRoot, example);
        }
    }

    private static void usage() {
        System.out.println(
                """
    Usage: java ExecCommand.java EXAMPLE
        - EXAMPLE: the path to the example file to execute""");
    }

    private static void execute(FsRoot fsRoot, ExampleDefinition example) throws IOException, InterruptedException {
        System.out.printf("# Creating temporary root %s%n", fsRoot);
        fsRoot.createTree(example);
        System.out.println("# File fsTree (before)");
        System.out.println(fsRoot.fileTreeAsString());
        ProcessBuilder processBuilder = fsRoot.exec(example).redirectErrorStream(true);
        System.out.printf("# Executing command %s in %s %n", processBuilder.command(), processBuilder.directory());
        Process process = processBuilder.start();
        process.waitFor(5, TimeUnit.SECONDS);
        try (var isr = new InputStreamReader(process.getInputStream());
                var br = new BufferedReader(isr)) {
            br.lines().map(line -> line.replace(fsRoot.root() + "/", "/")).forEach(System.out::println);
        }
        System.out.println("# File fsTree (after)");
        System.out.println(fsRoot.fileTreeAsString());
    }

    private static Path chrootPath(String path) {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Path must be absolute");
        }
        return Path.of(path.substring(1));
    }
}

class FsRoot implements AutoCloseable {

    private final Path root;

    private FsRoot(Path root) {
        this.root = root;
    }

    public Path root() {
        return root;
    }

    public ProcessBuilder exec(ExampleDefinition example) {
        return new ProcessBuilder(command(example.command()))
                .directory(cwd(example.cwd()).toFile());
    }

    private Path cwd(Path cwd) {
        return root.resolve(cwd);
    }

    private String[] command(List<String> command) {
        return command.stream()
                .map(arg -> {
                    if (arg.startsWith("/")) {
                        return root.resolve(arg.substring(1)).toString();
                    }
                    return arg;
                })
                .toArray(String[]::new);
    }

    public void createTree(ExampleDefinition example) throws IOException {
        for (var fsEntry : example.fsTree()) {
            fsEntry.create(root);
        }
    }

    String fileTreeAsString() throws IOException {
        try (var paths = Files.walk(root)) {
            List<String> lines = paths.filter(p -> !Objects.equals(p, root))
                    .sorted(Comparator.comparing(Path::toString))
                    .map(this::printFsEntry)
                    .toList();
            return treeify(lines);
        }
    }

    private static String treeify(List<String> lines) {
        char[][] chars = new char[lines.size()][];
        for (int i = 0; i < lines.size(); i++) {
            chars[i] = lines.get(i).toCharArray();
        }
        boolean[] opened =
                new boolean[lines.stream().mapToInt(String::length).max().orElse(0)];
        for (int l = chars.length - 1; l >= 0; l--) {
            for (int c = 0; c < chars[l].length - 1; c++) {
                char next = chars[l][c + 1];
                if (chars[l][c] == '|') {
                    if (next != '-' && !opened[c]) {
                        chars[l][c] = ' ';
                    } else if (next == '-') {
                        if (!opened[c]) {
                            chars[l][c] = '\\';
                            opened[c] = true;
                        }
                        while (c < opened.length - 1) {
                            c++;
                            opened[c] = false;
                        }
                        break;
                    }
                }
            }
        }
        return Arrays.stream(chars).map(String::new).collect(Collectors.joining("\n"));
    }

    private String printFsEntry(Path p) {
        Path entry = root.relativize(p);
        int nameCount = entry.getNameCount();
        StringBuilder sb = new StringBuilder();
        sb.append("|   ".repeat(Math.max(0, nameCount - 2)));
        if (nameCount > 1) {
            sb.append("|-- ");
        }
        sb.append(entry.getFileName());
        if (Files.isSymbolicLink(p)) {
            try {
                sb.append(" -> /").append(root.relativize(Files.readSymbolicLink(p)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (Files.isDirectory(p)) {
            sb.append("/");
        }
        return sb.toString();
    }

    @Override
    public void close() throws IOException {
        try (var files = Files.walk(root)) {
            files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    @Override
    public String toString() {
        return root.toString();
    }

    public static FsRoot create() throws IOException {
        Path tempDirectory = Files.createTempDirectory("doc-gen");
        try {
            Path root = tempDirectory.toRealPath();
            return new FsRoot(root);
        } catch (IOException e) {
            tempDirectory.toFile().delete();
            throw e;
        }
    }
}
