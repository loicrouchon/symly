plugins {
    id("org.symly.base")
    id("org.symly.doc")
    id("application")
    id("jvm-test-suite")
    id("idea")
    id("jacoco")
    id("com.diffplug.spotless") version "6.15.0"
    id("org.asciidoctor.jvm.convert") version "4.0.0-alpha.1"
    id("com.netflix.nebula.ospackage") version "11.0.0"
    id("net.ltgt.errorprone") version "3.0.1"
}

group = "org.symly"

val appModuleName = "org.${project.name}"
val appMainClassName = "org.${project.name}.cli.Main"
application {
    mainModule.set(appModuleName)
    mainClass.set(appMainClassName)
    applicationDefaultJvmArgs = listOf(
        // the cli is short-lived, so let's enable a fast JVM startup
        "-XX:+NeverActAsServerClassMachine",
    )
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

spotless {
    java {
        palantirJavaFormat()
    }
    format("xml") {
        target("src/**/*.xml")
        targetExclude("src/docs/**")
        eclipseWtp(com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep.XML)
            .configFile(".metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.wst.xml.core.prefs")
    }
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("**/application.properties") {
        expand(props)
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
                implementation(libs.assertj)
            }
        }

        val integrationTest by registering(JvmTestSuite::class) {
            useJUnitJupiter(libs.versions.junit.get())

            dependencies {
                implementation(project())
                implementation(libs.picocli.core)
                implementation(libs.assertj)
            }

            targets {
                all {
                    testTask.configure {
                        systemProperty("symly.runtime.classpath", sourceSets.main.get().runtimeClasspath.asPath)
                        systemProperty("symly.testing.opaque-testing", properties["opaqueTesting"].toString())
                        shouldRunAfter(test)
                    }
                }
            }
        }
        tasks.check.get().dependsOn(integrationTest)
    }
}

dependencies {
    errorprone(libs.errorprone)
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
tasks.check.get().dependsOn(tasks.jacocoTestReport.get())

val generateManualStructure = tasks.register<JavaExec>("generateManualStructure") {
    classpath(sourceSets.main.get().runtimeClasspath, configurations.annotationProcessor)
    mainClass.set("picocli.codegen.docgen.manpage.ManPageGenerator")
    args(
        "org.${project.name}.cli.MainCommand",
        "--factory=org.${project.name}.cli.BeanFactory",
        "--outdir=${file("src/docs/resources/manpage/adoc")}"
    )
    outputs.dir("src/docs/resources/manpage/adoc")
}

fun generateManual(
    name: String,
    backend: String,
    output: String
): TaskProvider<org.asciidoctor.gradle.jvm.AsciidoctorTask> {
    return tasks.register<org.asciidoctor.gradle.jvm.AsciidoctorTask>("generate${name.capitalize()}Manual") {
        inputs.files(generateManualStructure)
        sourceDir("src/docs/resources/manpage/adoc")
        setOutputDir(output)
        attributes(mapOf("reproducible" to true))
        outputOptions {
            backends(backend)
        }
    }
}

val generateManpageManual = generateManual("manpage", "manpage", "src/docs/resources/manpage/manpage")
val generateHtmlManual = generateManual("html", "html5", "${buildDir}/docs/manpage/html")

val compressManpageManual = tasks.register<Exec>("compressManpageManual") {
    val manpage = "src/docs/resources/manpage/manpage"
    val gz = "${buildDir}/docs/manpage/gz"
    inputs.files(generateManpageManual)
    outputs.dir(gz)
    workingDir(".")
    commandLine(
        "/bin/sh",
        "-c",
        "rm -rf ${gz} && mkdir -p ${gz} && cp -R ${manpage}/* ${gz} && gzip -n -9 ${gz}/*"
    )
}

val generateShellCompletions = tasks.register<JavaExec>("generateShellCompletions") {
    classpath(sourceSets.main.get().runtimeClasspath)
    mainClass.set("picocli.AutoComplete")
    args(
        "org.${project.name}.cli.MainCommand",
        "--factory=org.${project.name}.cli.BeanFactory",
        "--completionScript=${buildDir}/shell/completions/${project.name}",
        "--force"
    )
    outputs.file("${buildDir}/shell/completions/${project.name}")
}

distributions {
    main {
        contents {
            from("LICENSE")
            from(generateManpageManual) {
                into("doc/manpage")
            }
            from(generateHtmlManual) {
                into("doc/html")
            }
        }
    }
}

val prepareHomebrewBottle = tasks.register<Sync>("prepareHomebrewBottle") {
    inputs.files(tasks.installDist, compressManpageManual)
    val standardDistPath = tasks.installDist.get().outputs.files.singleFile.toPath()
    from("LICENSE")
    from("src/packaging/homebrew")
    from(standardDistPath.resolve("bin/symly")) {
        into("libexec/bin")
    }
    from(standardDistPath.resolve("lib")) {
        into("libexec/lib")
    }
    from(compressManpageManual) {
        into("share/man/man1")
    }
    from(generateHtmlManual) {
        into("share/doc/${project.name}")
    }
    into("${buildDir}/distributions-preparation/homebrew")
}

val buildHomebrewBottle = tasks.register<Zip>("buildHomebrewBottle") {
    from(prepareHomebrewBottle)
    destinationDirectory.set(file("${buildDir}/distributions/"))
    archiveFileName.set("${project.name}-${project.version}-homebrew-bottle.zip")
}
tasks.assemble.get().dependsOn(buildHomebrewBottle)

val rpmSpec = tasks.register<Copy>("rpmSpec") {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    from("src/packaging/fedora/${project.name}.spec", closureOf<CopySpec> {
        expand(props)
    })
    into(file("${buildDir}/distributions/"))
}
tasks.assemble.get().dependsOn(rpmSpec)

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
    from("src/packaging/linux")
    from("${buildDir}/install/${project.name}/bin/symly", closureOf<CopySpec> {
        into("usr/share/${project.name}/bin")
    })
    from("${buildDir}/install/${project.name}/lib", closureOf<CopySpec> {
        into("usr/share/${project.name}/lib")
    })
    from(compressManpageManual, closureOf<CopySpec> {
        into("usr/man/man1")
        fileType = org.redline_rpm.payload.Directive.DOC
    })
//    requires https ://github.com/remkop/picocli/issues/1346
//    from(generateShellCompletions, closureOf<CopySpec> {
//        into "/usr/share/bash-completion/completions"
//    })
}

val buildDebPackage = tasks.register<com.netflix.gradle.plugins.deb.Deb>("buildDebPackage") {
    dependsOn(tasks.installDist)
    shouldRunAfter(tasks.named("integrationTest"))
    maintainer = "Loic Rouchon"
    license = "ASL 2.0"
    requires("openjdk-17-jre-headless", "").or("java17-runtime-headless", null)
}

val buildRpmPackage = tasks.register<com.netflix.gradle.plugins.rpm.Rpm>("buildRpmPackage") {
    dependsOn(tasks.installDist)
    shouldRunAfter(tasks.named("integrationTest"))
    packager = "Loïc Rouchon"
    requires("java-latest-openjdk-headless", "")
    addParentDirs = false
}
tasks.assemble.get().dependsOn(buildDebPackage, buildRpmPackage)
