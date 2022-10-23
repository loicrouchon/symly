package org.symly.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.env.Env;

@RequiredArgsConstructor
public class UnlinkCommandMessageFactory {

    @NonNull
    private final Env env;

    public String unlink(String to, List<String> from) {
        return "Removing links in %s to [%s]"
                .formatted(
                        env.path(to),
                        from.stream().map(env::path).map(Path::toString).collect(Collectors.joining(", ")));
    }

    public String actionUnlink(String from, String to) {
        return action("unlink", from, to);
    }

    public String action(String action, String from, String to) {
        return "%-12s %s -> %s".formatted(action + ":", from, env.path(to));
    }
}
