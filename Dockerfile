# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache dependencies first
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline

# Build
COPY src ./src
RUN mvn -q -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
# Run as non-root
RUN useradd -u 10001 appuser
USER appuser

WORKDIR /app
# Copy the fat JAR (adjust artifactId if needed)
COPY --from=build /workspace/target/cmx-uploader-*.jar /app/app.jar

# Port your app listens on
EXPOSE 8085

# Sensible container JVM defaults
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+UseZGC"
# Activate profile "docker" by default; override with SPRING_PROFILES_ACTIVE if needed
ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Dserver.port=8084 -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -jar /app/app.jar"]
