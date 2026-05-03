import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("java-library")
    id("org.springframework.boot") version libs.versions.springBootPlugin.get()
    id("io.spring.dependency-management") version libs.versions.springDependencyManagementPlugin.get()
    id("io.freefair.lombok") version libs.versions.lombokPlugin.get()
}

group = "org.janus"
version = "1.1.0"
description = "Java SDK Spring Boot Starter"

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
    api(project(":sdk:java:core"))

    implementation(project(":client:api:sidecar-api"))
    implementation(project(":common:grpc"))

    api("org.springframework.boot:spring-boot-autoconfigure")
    api("org.springframework.boot:spring-boot-starter-aspectj")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.grpc:spring-grpc-client-spring-boot-starter")

    implementation("com.google.protobuf:protobuf-java-util:${libs.versions.protobuf.get()}")
    implementation("io.micrometer:micrometer-core")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.grpc:spring-grpc-test")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.grpc:spring-grpc-dependencies:${libs.versions.springGrpc.get()}")
    }
}

tasks.withType<Jar> {
    archiveBaseName.set("janus-spring-boot-starter")
}

tasks.withType<BootJar> {
    enabled = false
}

tasks.withType<Test> {
    useJUnitPlatform()
}