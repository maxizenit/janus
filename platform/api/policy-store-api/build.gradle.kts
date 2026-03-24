import com.google.protobuf.gradle.id

plugins {
    id("java-library")
    id("com.google.protobuf") version libs.versions.protobufPlugin.get()
}

group = "org.janus"
version = "0.0.1-SNAPSHOT"
description = "Policy Store API"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.google.protobuf:protobuf-java:${libs.versions.protobuf.get()}")
    api("io.grpc:grpc-stub:${libs.versions.grpc.get()}")
    api("io.grpc:grpc-protobuf:${libs.versions.grpc.get()}")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc") {
                    option("@generated=omit")
                }
            }
        }
    }
}

tasks.withType<Jar> {
    archiveBaseName.set("janus-policy-store-api")
}