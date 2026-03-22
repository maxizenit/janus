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
    api("jakarta.validation:jakarta.validation-api:${libs.versions.jakartaValidation.get()}")
    api("org.hibernate.validator:hibernate-validator:${libs.versions.hibernateValidator.get()}")
}