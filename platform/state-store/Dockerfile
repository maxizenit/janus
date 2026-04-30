FROM eclipse-temurin:25-jre-alpine
RUN apk add --no-cache curl
ARG JAR_FILE=build/libs/*.jar
ADD ${JAR_FILE} janus-state-store.jar
ENTRYPOINT ["java", "-jar", "janus-state-store.jar"]
