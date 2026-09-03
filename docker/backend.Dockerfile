# Multi-stage build for the clothing-retail Spring Boot backend.
# NOTE: written for later use once Docker is available - not built/run as part of this pass.

# ---- Build stage ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Cache Maven dependencies separately from source changes.
COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline || true

COPY backend/src ./src
RUN ./mvnw -q -DskipTests clean package

# ---- Run stage ----
FROM eclipse-temurin:25-jre AS run
WORKDIR /app

RUN useradd --system --create-home --shell /usr/sbin/nologin appuser
COPY --from=build /workspace/target/backend.jar app.jar
RUN chown appuser:appuser app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
