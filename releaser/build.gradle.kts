plugins {
    java
    idea
    id("com.diffplug.spotless") version "6.7.2"
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
