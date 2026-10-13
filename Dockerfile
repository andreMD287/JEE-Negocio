# Etapa 1: compilar el WAR con Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# Etapa 2: desplegar en Payara Server 6 (Jakarta EE 10, JDK 17)
FROM payara/server-full:6.2024.6-jdk17
COPY --from=build /app/target/recursos.war $DEPLOY_DIR/recursos.war
