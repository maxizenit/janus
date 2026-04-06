FROM eclipse-temurin:25-jre-alpine
RUN apk add --no-cache curl
ARG JAR_FILE=build/libs/*.jar
ADD ${JAR_FILE} janus-admin-ui.jar
ENTRYPOINT ["java", "-jar", "janus-admin-ui.jar"]
