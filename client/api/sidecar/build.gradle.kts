import com.google.protobuf.gradle.id

plugins {
    java
    id("com.google.protobuf") version libs.versions.protobufPlugin.get()
}

group = "org.janus"
version = "0.0.1-SNAPSHOT"
description = "Sidecar API"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
    }
}

repositories {
    mavenCentral()
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java"
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
