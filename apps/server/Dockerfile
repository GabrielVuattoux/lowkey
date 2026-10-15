FROM gradle:9.2.1-jdk17 AS build
# Set container working directory to /app
WORKDIR /app
# Copy Gradle configuration files
COPY gradlew /app/
COPY gradle /app/gradle
# Ensure Gradle wrapper is executable
RUN chmod +x ./gradlew
# Copy build script and source code
COPY build.gradle settings.gradle /app/
COPY src /app/src
# Build the server (tests run in CI, see .github/workflows/server.yml)
RUN ./gradlew clean bootJar --no-daemon

# make image smaller by using multi stage build, with a JRE only
FROM eclipse-temurin:17-jre
# Production profile: PostgreSQL configured through DB_URL, DB_USERNAME, DB_PASSWORD
# (see src/main/resources/application-prod.properties and docs/deployment.md)
ENV SPRING_PROFILES_ACTIVE=prod
# get non-root user
RUN groupadd appgroup && \
    useradd -r -g appgroup appuser
USER appuser
# Set container working directory to /app
WORKDIR /app
# copy the executable jar only (the build also produces a "-plain" jar)
COPY --from=build /app/build/libs/soprafs26.jar /app/server.jar
# The server listens on $PORT, 8080 by default
EXPOSE 8080
# Let the JVM size its heap from the container memory limit. The server stores and compares
# local times without an offset (Zurich time), and containers default to UTC: pin the zone.
CMD ["java", "-XX:MaxRAMPercentage=75", "-Duser.timezone=Europe/Zurich", "-jar", "/app/server.jar"]
