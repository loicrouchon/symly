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
        return maskChRoot("""
            $ %s
            %s"""
                .formatted(String.join(" ", execution.command()), String.join("\n", execution.stdOut())));
    }

    public String fileTreeBefore() {
        return fileTree(execution.snapshot());
    }

    public String fileTreeAfter() {
        return fileTree(execution.currentFileTree());
    }

    private String fileTree(FileTree currentFileTree) {
        List<String> entries = currentFileTree
                .layout()
                .filter(fileRef -> !workingDir.startsWith(fileRef.name()))
                .map(this::fsEntry)
                .toList();
        return maskChRoot("""
            $ tree %s
            %s""".formatted(workingDir, treeify(entries)));
    }

    private String fsEntry(FileRef fileRef) {
        Path entry = workingDir.relativize(fileRef.name());
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

    private String maskChRoot(String str) {
        return str
                // hide the absolute real path of the temporary chroot in which the command was executed
                .replaceAll(execution.rootDir().toString(), "")
                // make the virtual 'root' name look like an absolute one
                .replaceAll("(^| )" + workingDir, "$1/" + workingDir);
    }
}
