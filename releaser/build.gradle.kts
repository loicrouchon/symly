plugins {
    application
    idea
    id("com.diffplug.spotless") version "6.7.2"
}

val appMainClassName = "releaser.Releaser"
application {
    mainClass.set(appMainClassName)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

spotless {
    java {
        palantirJavaFormat()
    }
}
