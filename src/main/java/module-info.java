module org.symly {
    requires static lombok;

    requires info.picocli;

    opens org.symly.cli to info.picocli;
}