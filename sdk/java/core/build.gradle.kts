plugins {
    id("java-library")
    id("io.freefair.lombok") version libs.versions.lombokPlugin.get()
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

    testImplementation("org.junit.jupiter:junit-jupiter:${libs.versions.junit.get()}")
    testImplementation("org.assertj:assertj-core:${libs.versions.assertj.get()}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<Jar> {
    archiveBaseName.set("janus-sdk-core")
}