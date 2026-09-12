# Multi-stage build for the clothing-retail Spring Boot backend.

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

# A platform-mounted volume (e.g. Railway's, at /app/uploads - see MediaStorageService's
# app.media.upload-dir) is attached fresh at container *start*, owned by root, after this image
# is already built - a build-time `chown` here can't reach it, since the mount doesn't exist
# yet. So the container has to start as root, fix that mount's ownership once it's actually
# there, then drop to the unprivileged appuser before running the app itself - never run the
# JVM itself as root. Using plain `su` (not a separately-installed tool like gosu/su-exec) since
# it already ships on this base image, same as the useradd/chown used above.
RUN printf '%s\n' \
  '#!/bin/sh' \
  'set -e' \
  'mkdir -p /app/uploads' \
  'chown -R appuser:appuser /app/uploads' \
  'exec su -s /bin/sh appuser -c "exec java -jar /app/app.jar"' \
  > /app/entrypoint.sh && chmod +x /app/entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["/app/entrypoint.sh"]
