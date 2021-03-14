module symly {
    requires java.base;
    requires static lombok;

    requires info.picocli;
    requires info.picocli.codegen;

    opens org.symly.cli to info.picocli;
}