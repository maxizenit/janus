plugins {
    java
    id("org.springframework.boot") version libs.versions.springBootPlugin.get()
    id("io.spring.dependency-management") version libs.versions.springDependencyManagementPlugin.get()
    id("io.freefair.lombok") version libs.versions.lombokPlugin.get()
}

group = "org.janus"
version = "1.1.0"
description = "Policy Store"

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

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-json")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.grpc:spring-grpc-server-spring-boot-starter")
    implementation("org.springframework.grpc:spring-grpc-client-spring-boot-starter")

    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    implementation("io.grpc:grpc-services")
    implementation("com.google.protobuf:protobuf-java-util:${libs.versions.protobuf.get()}")

    implementation("org.mapstruct:mapstruct:${libs.versions.mapstruct.get()}")
    annotationProcessor("org.mapstruct:mapstruct-processor:${libs.versions.mapstruct.get()}")

    runtimeOnly("com.h2database:h2")
    implementation("org.springframework.boot:spring-boot-h2console")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.grpc:spring-grpc-test")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.grpc:spring-grpc-dependencies:${libs.versions.springGrpc.get()}")
    }
}

tasks.withType<Jar> {
    archiveBaseName.set("janus-policy-store")
}

tasks.withType<Test> {
    useJUnitPlatform()
}