FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY services/loan-service/pom.xml ./services/loan-service/pom.xml
RUN mvn -f services/loan-service/pom.xml dependency:go-offline -B --no-transfer-progress
COPY services/loan-service/src ./services/loan-service/src
RUN mvn -f services/loan-service/pom.xml package -Dmaven.test.skip=true -B --no-transfer-progress

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/services/loan-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
