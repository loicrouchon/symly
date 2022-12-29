package gui;

import static gui.Color.CHOICE;
import static gui.Color.ERROR;

import java.io.Console;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class IO {

    final Console console = System.console();

    public IO printf(String message, Object... args) {
        console.printf(message, args);
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
        t.printStackTrace(console.writer());
        resetColor();
        return this;
    }

    public IO color(Color color) {
        console.printf(color.code);
        return this;
    }

    public IO resetColor() {
        console.printf(Color.DEFAULT.code);
        return this;
    }

    public Optional<String> readLine(String message, Object... args) {
        String input = console.readLine("> " + message, args);
        if (input.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(input);
    }

    public String readChoice(List<Choice> choices, String message, Object... args) {
        Set<String> validChoices = choices.stream().map(Choice::key).collect(Collectors.toSet());
        String selection;
        do {
            choices.forEach(choice -> printf("  %s) %s", CHOICE.str(choice.key()), choice.message().formatted()));
            selection = console.readLine("> " + message, args);
        } while (!validChoices.contains(selection));
        return selection;
    }
}
