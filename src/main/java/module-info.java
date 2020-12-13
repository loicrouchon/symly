module linky {
    requires java.base;
    requires static lombok;

    requires info.picocli;

    opens org.linky.cli to info.picocli;
}