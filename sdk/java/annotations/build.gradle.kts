plugins {
    java
}

group = "org.janus"
version = "0.0.1-SNAPSHOT"
description = "Java SDK Annotations"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
    }
}

repositories {
    mavenCentral()
}
