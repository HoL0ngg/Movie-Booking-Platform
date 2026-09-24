FROM eclipse-temurin:21-jre

ARG JAR_FILE=target/*.jar
WORKDIR /app
COPY ${JAR_FILE} application.jar

USER 10001:10001

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/application.jar"]
