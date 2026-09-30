FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM mcr.microsoft.com/playwright/java:v1.55.0-noble
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV PLAYWRIGHT_HEADLESS=true
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
CMD ["sh","-c","java $JAVA_TOOL_OPTIONS -jar app.jar"]
