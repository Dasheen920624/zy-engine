# syntax=docker/dockerfile:1.7
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace/medkernel-backend
COPY medkernel-backend/pom.xml ./
COPY medkernel-backend/src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /workspace/medkernel-backend/target/medkernel-backend-*.jar /app/app.jar

EXPOSE 18080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
