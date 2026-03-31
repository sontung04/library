FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY infrastructure/discovery-server/pom.xml ./infrastructure/discovery-server/pom.xml
RUN mvn -f infrastructure/discovery-server/pom.xml dependency:go-offline -B --no-transfer-progress
COPY infrastructure/discovery-server/src ./infrastructure/discovery-server/src
RUN mvn -f infrastructure/discovery-server/pom.xml package -Dmaven.test.skip=true -B --no-transfer-progress

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/infrastructure/discovery-server/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
