FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY modules ./modules
RUN mvn -B -pl modules/import-worker-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/modules/import-worker-service/target/import-worker-service-0.1.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
