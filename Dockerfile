# =========================
# Build stage
# =========================
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# cache deps
COPY pom.xml .
RUN mvn -q -B -e dependency:go-offline

# build
COPY src ./src
RUN mvn -q -B -e clean package -DskipTests

# =========================
# Runtime stage (JRE only)
# =========================
FROM eclipse-temurin:17-jre-jammy

# --- security: run as non-root
ARG APP_USER=appuser
RUN useradd -ms /bin/bash ${APP_USER}

WORKDIR /app

# copy fat jar
COPY --from=build /app/target/*.jar /app/app.jar

# envs (tweak as needed)
ENV JAVA_OPTS=""
ENV TZ=Asia/Singapore
# If you use Google Cloud Storage with a service account key:
# ENV GOOGLE_APPLICATION_CREDENTIALS=/secrets/gcs-sa.json

# ephemeral tmp for uploads/processing
VOLUME ["/tmp"]

# expose service port (change if your app uses a different one)
EXPOSE 8084

# optional healthcheck (requires spring-boot-starter-actuator)
HEALTHCHECK --interval=30s --timeout=3s --retries=5 \
  CMD wget -qO- http://localhost:8084/actuator/health | grep -qi '"status":"UP"' || exit 1

USER ${APP_USER}

# use exec form so signals are forwarded
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
