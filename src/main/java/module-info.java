module org.symly {
    requires info.picocli;

    opens org.symly.cli to
            info.picocli;
}
