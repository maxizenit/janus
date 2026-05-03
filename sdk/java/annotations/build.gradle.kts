plugins {
    id("java-library")
}

group = "org.janus"
version = "1.2.0-SNAPSHOT"
description = "Java SDK Annotations"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
    }
}

repositories {
    mavenCentral()
}

tasks.withType<Jar> {
    archiveBaseName.set("janus-sdk-annotations")
}
