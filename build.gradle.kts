import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage

plugins {
    id("com.bmuschko.docker-remote-api") version libs.versions.dockerRemoteApiPlugin.get() apply false
}

subprojects {
    afterEvaluate {
        if (plugins.hasPlugin("org.springframework.boot") && file("Dockerfile").exists()) {
            apply(plugin = "com.bmuschko.docker-remote-api")

            tasks.register<DockerBuildImage>("buildDockerImageLocal") {
                group = "deploy"
                dependsOn("bootJar")
                inputDir.set(layout.projectDirectory.asFile)
                dockerFile.set(layout.projectDirectory.file("Dockerfile"))
                images.add("${project.name}:local")
            }
        }
    }
}
