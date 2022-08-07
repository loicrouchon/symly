package org.symly.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.env.Env;

@RequiredArgsConstructor
class LinkCommandMessageFactory {

    @NonNull
    private final Env env;

    public String creatingLinks(String to, List<String> from) {
        return String.format(
                "Creating links in %s to [%s]",
                env.path(to), from.stream().map(env::path).map(Path::toString).collect(Collectors.joining(", ")));
    }

    public String linkActionCreate(String from, String to) {
        return action("added", from, to);
    }

    public String linkActionConflict(String from, String to) {
        return action("!conflict", from, to);
    }

    public List<String> linkActionUpdate(String from, String to, String previousTo) {
        return List.of(action("deleted", from, previousTo), action("added", from, to));
    }

    public String linkActionUpToDate(String from, String to) {
        return action("up-to-date", from, to);
    }

    public String linkActionDelete(String from, String to) {
        return action("deleted", from, to);
    }

    public String linkActionDelete(String from) {
        return action("deleted", from);
    }

    public String action(String action, String from) {
        return String.format("%-12s %s", action + ":", from);
    }

    public String action(String action, String from, String to) {
        return String.format("%-12s %s -> %s", action + ":", from, env.path(to));
    }

    public List<String> cannotCreateLinkError(String from, String to) {
        Path absFrom = env.path(env.home().resolve(from).toString());
        return List.of(
                String.format("Unable to create link %s -> %s", absFrom, env.path(to)),
                String.format(
                        "> Regular file %s already exist. To overwrite it, use the -f (--force) option.", absFrom));
    }

    public String everythingUpToDate() {
        return "Everything is already up to date";
    }
}
