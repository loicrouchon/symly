plugins {
    id("java")
    id("idea")
    id("com.diffplug.spotless") version "6.15.0"
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
