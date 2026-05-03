plugins {
    id("java-library")
    id("io.freefair.lombok") version libs.versions.lombokPlugin.get()
}

group = "org.janus"
version = "1.1.0"
description = "Common gRPC client utilities"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    api("io.grpc:grpc-api:${libs.versions.grpc.get()}")
    api("org.jspecify:jspecify:${libs.versions.jspecify.get()}")

    testImplementation("org.junit.jupiter:junit-jupiter:${libs.versions.junit.get()}")
    testImplementation("org.assertj:assertj-core:${libs.versions.assertj.get()}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<Jar> {
    archiveBaseName.set("janus-common-grpc")
}
