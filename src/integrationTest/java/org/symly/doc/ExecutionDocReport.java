package org.symly.doc;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.symly.env.Execution;
import org.symly.files.FileRef;
import org.symly.files.FileTree;

public class ExecutionDocReport {

    @NonNull
    private final Execution execution;

    private final Path workingDir;

    public ExecutionDocReport(@NonNull Execution execution) {
        this.execution = execution;
        workingDir = execution.rootDir().relativize(execution.workingDir());
    }

    public String symlyExecution() {
        return maskChRoot(
            workingDir,
            "$ %s%n%s".formatted(String.join(" ", execution.command()), String.join("\n", execution.stdOut())));
    }

    public FileTree currentFileTree() {
        return execution.currentFileTree();
    }

    public String fileTreeBefore() {
        return fsTreeAsString(execution.snapshot(), workingDir);
    }

    public String fileTreeAfter() {
        return fsTreeAsString(execution.currentFileTree(), workingDir);
    }

    public String fsTreeAsString(FileTree fileTree, Path root) {
        List<String> entries = fileTree.layout()
                .filter(fileRef -> isSubDirectory(root, fileRef))
                .map(fileRef1 -> fsEntry(root, fileRef1))
                .toList();
        return maskChRoot(root, "$ tree %s%n%s:%n%s".formatted(root, root, treeify(entries)));
    }

    private static boolean isSubDirectory(Path root, FileRef fileRef) {
        Path path = fileRef.name();
        return path.startsWith(root) && !path.equals(root);
    }

    private static String fsEntry(Path root, FileRef fileRef) {
        Path entry = root.relativize(fileRef.name());
        int nameCount = entry.getNameCount() + 1;
        StringBuilder sb = new StringBuilder();
        sb.append("|   ".repeat(Math.max(0, nameCount - 2)));
        if (nameCount > 1) {
            sb.append("|-- ");
        }
        sb.append(entry.getFileName());
        if (fileRef instanceof FileRef.LinkFileRef linkFileRef) {
            sb.append(" -> /").append(linkFileRef.target());
        } else if (fileRef instanceof FileRef.DirectoryRef) {
            sb.append("/");
        }
        return sb.toString();
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

    @Override
    public String toString() {
        return "%s%n%n%s%n%n%s".formatted(fileTreeBefore(), symlyExecution(), fileTreeAfter());
    }

    private String maskChRoot(Path root, String str) {
        return str
                // hide the absolute real path of the temporary chroot in which the command was executed
                .replaceAll(execution.rootDir().toString(), "")
                // make the virtual 'root' name look like an absolute one
                .replaceAll("(^|\n| )" + root, "$1/" + root);
    }
}
