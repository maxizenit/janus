FROM eclipse-temurin:25-jre-alpine
RUN apk add --no-cache curl
ARG JAR_FILE=build/libs/*.jar
ADD ${JAR_FILE} demo-server.jar
ENTRYPOINT ["java", "-jar", "demo-server.jar"]
