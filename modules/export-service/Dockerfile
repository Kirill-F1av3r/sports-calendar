FROM maven:3.9.10-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml pom.xml
COPY modules modules

RUN mvn -B -pl modules/export-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /app/modules/export-service/target/export-service-*.jar /app/app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
