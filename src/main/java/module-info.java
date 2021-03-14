module symly {
    requires java.base;
    requires static lombok;

    requires info.picocli;

    opens org.symly.cli to info.picocli;
}