FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
ARG JAR_FILE=target/rapp-manager-application-*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
