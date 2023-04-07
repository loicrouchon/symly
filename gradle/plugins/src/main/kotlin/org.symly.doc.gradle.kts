import org.symly.doc.AsciiDocIncludeUpdaterTask

plugins {
    id("java")
}

val updateDocSnippets = tasks.register<AsciiDocIncludeUpdaterTask>("updateDocSnippets") {
    dependsOn(testing.suites.named("integrationTest"))
}
tasks.check.get().dependsOn(updateDocSnippets)
