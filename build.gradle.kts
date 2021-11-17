plugins {
    application
    `jvm-test-suite`
    idea
    jacoco
    id("org.asciidoctor.jvm.convert") version "3.3.2"
    id("nebula.ospackage") version "9.0.0"
}

val appModuleName = "org.${project.name}"
val appMainClassName = "org.${project.name}.cli.Main"
application {
    mainModule.set(appModuleName)
    mainClass.set(appMainClassName)
    applicationDefaultJvmArgs = listOf(
        // the cli is short lived, so let's enable a fast JVM startup
        "-XX:TieredStopAtLevel=1"
    )
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.processResources {
    filesMatching("**/application.properties") {
        expand(mapOf("version" to project.version))
    }
}

repositories {
    mavenCentral()
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter(libs.versions.junit.get())

            dependencies {
                compileOnly(libs.lombok)
                // annotationProcessor(libs.lombok)
                implementation(libs.assertj)
                implementation(libs.bundles.mockito)
            }
        }

        val integrationTest by registering(JvmTestSuite::class) {
            useJUnitJupiter(libs.versions.junit.get())

            dependencies {
                compileOnly(libs.lombok)
                // annotationProcessor(libs.lombok)
                implementation(libs.assertj)
            }

            targets {
                all {
                    testTask.configure {
                        systemProperty("symly.runtime.classpath", sourceSets.main.get().runtimeClasspath.asPath)
                        shouldRunAfter(test)
                    }
                }
            }
        }
        tasks.check.get().dependsOn(integrationTest)
    }
}

dependencies {
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testAnnotationProcessor(libs.lombok)
    configurations["integrationTestAnnotationProcessor"](libs.lombok)

    implementation(libs.picocli.core)
    annotationProcessor(libs.picocli.codegen)
}

tasks.jacocoTestReport {
    dependsOn(testing.suites.named("test"), testing.suites.named("integrationTest"))
    additionalSourceDirs.from(files(sourceSets.main.get().allSource.srcDirs))
    sourceDirectories.from(files(sourceSets.main.get().allSource.srcDirs))
    classDirectories.from(files(sourceSets.main.get().output))
    executionData.from(
        files(
            "${buildDir}/jacoco/test.exec",
            "${buildDir}/jacoco/integrationTest.exec",
        )
    )

    reports {
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/all"))
    }
}

tasks.register<JavaExec>("generateManpageAsciiDoc") {
    dependsOn(tasks.installDist)
    inputs.dir("${buildDir}/install/${project.name}/")
    outputs.dir("${buildDir}/docs/manpage/adoc")
    classpath("${buildDir}/install/${project.name}/lib/*", configurations.annotationProcessor)
    mainClass.set("picocli.codegen.docgen.manpage.ManPageGenerator")
    args(
        "org.${project.name}.cli.MainCommand",
        "--factory=org.${project.name}.cli.BeanFactory",
        "--outdir=${buildDir}/docs/manpage/adoc"
    )
}

tasks.asciidoctor {
    dependsOn(tasks.named("generateManpageAsciiDoc"))
    inputs.dir("${buildDir}/docs/manpage/adoc")
    outputs.dirs(
        "${buildDir}/docs/manpage/html5",
        "${buildDir}/docs/manpage/manpage"
    )
    sourceDir(file("${buildDir}/docs/manpage/adoc"))
    setOutputDir(file("${buildDir}/docs/manpage/"))
    logDocuments = true
    outputOptions {
        backends("manpage", "html5")
    }
}

tasks.register<Exec>("generateManpage") {
    dependsOn(tasks.asciidoctor)
    inputs.dir("${buildDir}/docs/manpage/manpage")
    outputs.dir("${buildDir}/docs/manpage/gz")
    workingDir("${buildDir}/docs")
    commandLine(
        "/bin/sh",
        "-c",
        "rm -rf manpage/gz && cp -R manpage/manpage manpage/gz && gzip -9 manpage/gz/*"
    )
}

tasks.register<JavaExec>("generateShellCompletions") {
    dependsOn(tasks.installDist)
    inputs.dir("${buildDir}/install/${project.name}/")
    outputs.file("${buildDir}/shell/completions/${project.name}")
    classpath("${buildDir}/install/${project.name}/lib/*")
    mainClass.set("picocli.AutoComplete")
    args(
        "org.${project.name}.cli.MainCommand",
        "--factory=org.${project.name}.cli.BeanFactory",
        "--completionScript=${buildDir}/shell/completions/${project.name}"
    )
    doFirst {
        file("${buildDir}/shell/completions/${project.name}").delete()
    }
}

ospackage {
    packageName = "symly"
    packageDescription = "Manages symbolic links."
    url = "https://github.com/loicrouchon/symly"

    release = "1"
    os = org.redline_rpm.header.Os.LINUX
    user = "root"

    license = "ASL 2.0"
    from("LICENSE", closureOf<CopySpec> {
        into("usr/share/doc/${project.name}")
        rename("LICENSE", "copyright")
        fileType = org.redline_rpm.payload.Directive.LICENSE
    })
    from("src/main/packaging/linux/${project.name}", closureOf<CopySpec> {
        into("usr/bin")
    })
    from("${buildDir}/install/${project.name}/bin/symly", closureOf<CopySpec> {
        into("usr/share/${project.name}/bin")
    })
    from("${buildDir}/install/${project.name}/lib", closureOf<CopySpec> {
        into("usr/share/${project.name}/lib")
    })
    from("${buildDir}/docs/manpage/gz", closureOf<CopySpec> {
        into("usr/man/man1")
        fileType = org.redline_rpm.payload.Directive.DOC
    })
//    requires https ://github.com/remkop/picocli/issues/1346
//    from("${buildDir}/shell/completions") {
//        into "/usr/share/bash-completion/completions"
//    }
}

val buildDebPackage = tasks.register<com.netflix.gradle.plugins.deb.Deb>("buildDebPackage") {
    dependsOn(
        tasks.named("installDist"),
        tasks.named("generateManpage"),
        tasks.named("generateShellCompletions")
    )
    shouldRunAfter(tasks.named("integrationTest"))
    maintainer = "Loic Rouchon"
    setArch("amd64")
    license = "ASL 2.0"
    requires("openjdk-17-jre-headless", "").or("java17-runtime-headless", null)
}

val buildRpmPackage = tasks.register<com.netflix.gradle.plugins.rpm.Rpm>("buildRpmPackage") {
    dependsOn(
        tasks.named("installDist"),
        tasks.named("generateManpage"),
        tasks.named("generateShellCompletions")
    )
    shouldRunAfter(tasks.named("integrationTest"))
    packager = "Loïc Rouchon"
    setArch("x86_64")
    requires("java-latest-openjdk-headless", "")
    addParentDirs = false
}
tasks.assemble.get().dependsOn(buildDebPackage, buildRpmPackage)