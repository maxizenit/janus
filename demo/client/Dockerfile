FROM eclipse-temurin:25-jre-alpine
RUN apk add --no-cache curl
ARG JAR_FILE=build/libs/*.jar
ADD ${JAR_FILE} demo-client.jar
ENTRYPOINT ["java", "-jar", "demo-client.jar"]
