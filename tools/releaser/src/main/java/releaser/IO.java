package releaser;

import static releaser.Color.CHOICE;
import static releaser.Color.DEFAULT;
import static releaser.Color.ERROR;

import java.io.Console;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class IO {

    private final Console console = System.console();
    private final PrintWriter writer;

    @SuppressWarnings({"java:S106" // Need to write to stdout
    })
    public IO() {
        if (console != null) {
            writer = console.writer();
        } else {
            writer = new PrintWriter(System.out);
        }
    }

    public IO printf(String message, Object... args) {
        writer.printf(message, args);
        writer.flush();
        return this;
    }

    public IO println(Object message) {
        return printf("%s%n", message);
    }

    public IO eprintln(Object message) {
        color(ERROR);
        println(message);
        resetColor();
        return this;
    }

    public IO eprintln(Object message, Throwable t) {
        color(ERROR);
        println(message);
        t.printStackTrace(writer);
        resetColor();
        return this;
    }

    public IO color(Color color) {
        return printf(color.code);
    }

    public IO resetColor() {
        return printf(DEFAULT.code);
    }

    public Optional<String> readLine(String message, Object... args) {
        String input = readLineFromConsole(message, args);
        if (input.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(input);
    }

    public String readChoice(List<Choice> choices, String message, Object... args) {
        Set<String> validChoices = choices.stream().map(Choice::key).collect(Collectors.toSet());
        String selection;
        do {
            choices.forEach(choice -> printf(
                    "  %s) %s", CHOICE.str(choice.key()), choice.message().formatted()));
            selection = readLineFromConsole(message, args);
        } while (!validChoices.contains(selection));
        return selection;
    }

    private String readLineFromConsole(String message, Object[] args) {
        if (console == null) {
            throw new ReleaseFailure("Unable to read input from Console");
        }
        return console.readLine("> " + message, args);
    }
}
