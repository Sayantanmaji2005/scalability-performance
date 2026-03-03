FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
ENV MAVEN_OPTS="-XX:UseAVX=0"

COPY pom.xml ./
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/worker-service-0.1.0.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
