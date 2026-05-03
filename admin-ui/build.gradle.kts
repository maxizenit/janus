plugins {
    java
    id("org.springframework.boot") version libs.versions.springBootPlugin.get()
    id("io.spring.dependency-management") version libs.versions.springDependencyManagementPlugin.get()
    id("io.freefair.lombok") version libs.versions.lombokPlugin.get()
    id("com.vaadin") version libs.versions.vaadin.get()
}

group = "org.janus"
version = "1.1.0"
description = "Admin UI"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common:grpc"))
    implementation(project(":platform:api:policy-store-api"))
    implementation(project(":platform:api:state-store-api"))

    implementation("com.vaadin:vaadin-spring-boot-starter")
    developmentOnly("com.vaadin:vaadin-dev")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.grpc:spring-grpc-client-spring-boot-starter")
    implementation("com.google.protobuf:protobuf-java-util:${libs.versions.protobuf.get()}")

    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("com.vaadin:vaadin-bom:${libs.versions.vaadin.get()}")
        mavenBom("org.springframework.grpc:spring-grpc-dependencies:${libs.versions.springGrpc.get()}")
    }
}

tasks.withType<Jar> {
    archiveBaseName.set("janus-admin-ui")
}

tasks.withType<Test> {
    useJUnitPlatform()
}