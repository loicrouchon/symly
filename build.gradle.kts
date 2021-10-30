plugins {
    application
    `jvm-test-suite`
//    `idea`
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
    modularity.inferModulePath.set(true)
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

val lombokVersion = "1.18.22"
val junitVersion = "5.8.1"
val assertjVersion = "3.21.0"
val mockitoVersion = "4.0.0"

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter(junitVersion)

            dependencies {
                compileOnly("org.projectlombok:lombok:${lombokVersion}")
                // annotationProcessor "org.projectlombok:lombok:${lombokVersion}"

                implementation("org.assertj:assertj-core:${assertjVersion}")

                implementation("org.mockito:mockito-core:${mockitoVersion}")
                implementation("org.mockito:mockito-junit-jupiter:${mockitoVersion}")
            }
        }

        val integrationTest by registering(JvmTestSuite::class) {
            useJUnitJupiter(junitVersion)

            dependencies {
                compileOnly("org.projectlombok:lombok:${lombokVersion}")
                // annotationProcessor "org.projectlombok:lombok:${lombokVersion}"

                implementation("org.assertj:assertj-core:${assertjVersion}")
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
    }
}

tasks.named("check") {
    dependsOn(testing.suites.named("integrationTest"))
}

dependencies {
    compileOnly("org.projectlombok:lombok:${lombokVersion}")
    annotationProcessor("org.projectlombok:lombok:${lombokVersion}")
    testAnnotationProcessor("org.projectlombok:lombok:${lombokVersion}")
    configurations["integrationTestAnnotationProcessor"]("org.projectlombok:lombok:${lombokVersion}")

    val picocliVersion = "4.6.1"
    implementation("info.picocli:picocli:${picocliVersion}")
    annotationProcessor("info.picocli:picocli-codegen:${picocliVersion}")
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

tasks.register<Exec>("buildNativeImage") {
    dependsOn(tasks.installDist)
    inputs.dir("${buildDir}/install/${project.name}/lib")
    outputs.file("${buildDir}/bin/${project.name}")
    workingDir("${buildDir}/bin")
    commandLine(
        nativeImageBin(),
        "--verbose",
        "--no-fallback",
        "-H:Name=${project.name}",
        "-H:-AddAllCharsets",
        "-H:-UseServiceLoaderFeature",
        "-J-Drx.unsafe-disable=true",
        "-H:+RemoveUnusedSymbols",
        "-DremoveUnusedAutoconfig=true",
        "--initialize-at-build-time=${appModuleName},picocli",
        "-H:+PrintImageElementSizes",
        // Native image size debug flags
//            "-H:DashboardDump=img-dump",
//            "-H:+DashboardAll",
//            "-H:+DashboardHeap",
//            "-H:+DashboardCode",
//            "-H:+DashboardPointsTo",
//            "-H:+DashboardPretty",
        // Native image stdout debug
//            "-H:+PrintUniverse",
//            "-H:+PrintHeapHistogram",
//            "-H:+PrintAnalysisCallTree",
//            "-H:+PrintImageObjectTree",
//            "-H:+PrintHeapHistogram",
//            "-H:+PrintMethodHistogram",
//            "-H:+PrintImageHeapPartitionSizes",
        "--module-path",
        "${buildDir}/install/${project.name}/lib/",
        "--module",
        "${appModuleName}/${appMainClassName}",
        "-Dpicocli.converters.excludes=java.sql.*,java.time.*"
    )
}

fun nativeImageBin(): String {
    val env = System.getenv("GRAALVM_HOME") ?: System.getenv("JAVA_HOME")
    if (env != null) {
        return "${env}/bin/native-image"
    }
    return "native-image"
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

    from("${buildDir}/bin/${project.name}", closureOf<CopySpec> {
        into("usr/bin/")
        fileMode = 755
    })
    from("${buildDir}/docs/manpage/gz", closureOf<CopySpec> {
        into("usr/man/man1")
        fileType = org.redline_rpm.payload.Directive.DOC
    })
// requires https://github.com/remkop/picocli/issues/1346
//    from("${buildDir}/shell/completions") {
//        into "/usr/share/bash-completion/completions"
//    }
}

tasks.register<com.netflix.gradle.plugins.deb.Deb>("buildDebPackage") {
    dependsOn(tasks.named("generateManpage"), tasks.named("generateShellCompletions"))
    maintainer = "Loic Rouchon"
    setArch("amd64")
    license = "ASL 2.0"
}

tasks.register<com.netflix.gradle.plugins.rpm.Rpm>("buildRpmPackage") {
    dependsOn(tasks.named("generateManpage"), tasks.named("generateShellCompletions"))
    packager = "Loïc Rouchon"
    setArch("x86_64")
    addParentDirs = false
}
