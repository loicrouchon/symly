package org.symly.files;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.symly.cli.SymlyExecutionException;

public class IoMock {

    private final Map<Path, FSEntry> fsEntries = new HashMap<>();

    public void file(Path path) {
        file(path, "");
    }

    public void file(Path path, String content) {
        path = path.toAbsolutePath();
        if (fsEntries.containsKey(path)) {
            throw new IllegalArgumentException(String.format("File system entry %s is already defined", path));
        }
        directory(path.getParent());
        fsEntries.put(path, new File(path, content));
    }

    public void directory(Path path) {
        if (path != null) {
            path = path.toAbsolutePath();
            if (fsEntries.get(path) != null && !(fsEntries.get(path) instanceof Directory)) {
                throw new IllegalArgumentException(String.format("File system entry %s is already defined", path));
            }
            directory(path.getParent());
            fsEntries.put(path, new Directory(path));
        }
    }

    public void symlink(Path path, Path target) {
        path = path.toAbsolutePath();
        if (fsEntries.containsKey(path)) {
            throw new IllegalArgumentException(String.format("File system entry %s is already defined", path));
        }
        directory(path.getParent());
        fsEntries.put(path, new Symlink(path, target));
    }

    public FileSystemReader buildFileSystemReader() {
        return new FileSystemReaderStub(Map.copyOf(fsEntries));
    }
}

sealed interface FSEntry permits File, Directory, Symlink {}

record File(Path path, String content) implements FSEntry {}

record Directory(Path path) implements FSEntry {}

record Symlink(Path path, Path target) implements FSEntry {}

@RequiredArgsConstructor
class FileSystemReaderStub implements FileSystemReader {

    private final Map<Path, FSEntry> fsEntries;

    @Override
    public boolean exists(Path path) {
        return fsEntries.containsKey(path.toAbsolutePath());
    }

    @Override
    public boolean isDirectory(Path path) {
        return fsEntries.get(path.toAbsolutePath()) instanceof Directory;
    }

    @Override
    public boolean isSymbolicLink(Path path) {
        return fsEntries.get(path.toAbsolutePath()) instanceof Symlink;
    }

    @Override
    public Path readSymbolicLink(Path path) {
        try {
            Symlink link = getOfType(Symlink.class, path);
            if (link == null) {
                return null;
            }
            return link.target();
        } catch (IOException e) {
            throw new SymlyExecutionException(
                    String.format("Unable to read link %s real path: %s", path, e.getMessage()), e);
        }
    }

    public Stream<String> lines(Path path) throws IOException {
        File file = getOfType(File.class, path);
        return Arrays.stream(file.content().split("\n"));
    }

    private <T extends FSEntry> T getOfType(Class<T> fsEntryClass, Path path) throws IOException {
        FSEntry fsEntry = fsEntries.get(path.toAbsolutePath());
        if (fsEntry == null) {
            throw new IOException(String.format("File system entry %s does not exist", path));
        }
        if (fsEntryClass.isInstance(fsEntry)) {
            return fsEntryClass.cast(fsEntry);
        }
        throw new IOException(String.format(
                "Path %s is not a %s but is a %s",
                path, fsEntryClass.getSimpleName(), fsEntry.getClass().getSimpleName()));
    }

    public Stream<Path> walk(Path path) {
        Path absolutePath = path.toAbsolutePath();
        return fsEntries.keySet().stream().filter(p -> p.startsWith(absolutePath));
    }

    public void walkFileTree(Path start, Set<FileVisitOption> options, int maxDepth, FileVisitor<? super Path> visitor)
            throws IOException {
        throw new IOException("This operation is not stubbed");
    }
}
