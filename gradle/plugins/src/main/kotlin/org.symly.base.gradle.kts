plugins {
    id("java")
}

version = providers.fileContents(rootProject.layout.projectDirectory.file("gradle/version.txt")).asText.getOrElse("").trim()

tasks.withType<AbstractArchiveTask>().configureEach {
    // Enable reproducible builds
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val installGitHooks by tasks.registering(Copy::class) {
    from("gradle/plugins/src/main/resources/git/hooks")
    into(".git/hooks")
}
tasks.processResources.get().dependsOn(installGitHooks)
