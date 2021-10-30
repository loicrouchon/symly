rootProject.name = "symly"

enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("picocli", "4.6.1")
            version("junit", "5.8.1")
            version("mockito", "4.0.0")
            alias("picocli.core").to("info.picocli", "picocli").versionRef("picocli")
            alias("picocli.codegen").to("info.picocli", "picocli-codegen").versionRef("picocli")
            alias("lombok").to("org.projectlombok:lombok:1.18.22")
            alias("assertj").to("org.assertj:assertj-core:3.21.0")
            alias("mockito.core").to("org.mockito", "mockito-core").versionRef("mockito")
            alias("mockito.junit").to("org.mockito", "mockito-junit-jupiter").versionRef("mockito")
        }
    }
}
