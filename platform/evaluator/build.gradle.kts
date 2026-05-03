plugins {
    java
    id("org.springframework.boot") version libs.versions.springBootPlugin.get()
    id("io.spring.dependency-management") version libs.versions.springDependencyManagementPlugin.get()
    id("io.freefair.lombok") version libs.versions.lombokPlugin.get()
}

group = "org.janus"
version = "1.2.0-SNAPSHOT"
description = "Evaluator"

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
    implementation("org.springframework.boot:spring-boot-starter-webclient")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.grpc:spring-grpc-client-spring-boot-starter")

    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    implementation("io.grpc:grpc-services")
    implementation("com.google.protobuf:protobuf-java-util:${libs.versions.protobuf.get()}")

    implementation("org.mapstruct:mapstruct:${libs.versions.mapstruct.get()}")
    annotationProcessor("org.mapstruct:mapstruct-processor:${libs.versions.mapstruct.get()}")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webclient-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
    testImplementation("org.springframework.grpc:spring-grpc-test")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.grpc:spring-grpc-dependencies:${libs.versions.springGrpc.get()}")
    }
}

tasks.withType<Jar> {
    archiveBaseName.set("janus-evaluator")
}

tasks.withType<Test> {
    useJUnitPlatform()
}