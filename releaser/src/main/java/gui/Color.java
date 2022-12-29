package gui;

public enum Color {
    RED("\033[31m"),
    GREEN("\033[32m"),
    YELLOW("\033[33m"),
    BLUE("\033[34m"),
    MAGENTA("\033[35m"),
    CYAN("\033[36m"),
    GRAY("\033[90m"),
    DEFAULT("\033[0m");

    public static final Color ERROR = Color.RED;
    public static final Color ACTION = Color.MAGENTA;
    public static final Color INFO = Color.YELLOW;
    public static final Color CHOICE = Color.CYAN;
    public static final Color COMMENT = Color.GRAY;

    final String code;

    Color(String code) {
        this.code = code;
    }

    public String str(Object value) {
        return "%s%s%s".formatted(code, value, DEFAULT.code);
    }
}
