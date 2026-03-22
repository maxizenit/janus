plugins {
    id("java-library")
}

group = "org.janus"
version = "0.0.1-SNAPSHOT"
description = "Java SDK Core"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":sdk:java:annotations"))

    api("org.jspecify:jspecify:${libs.versions.jspecify.get()}")
}