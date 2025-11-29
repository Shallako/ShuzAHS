# Multi-stage build for AHS Data Generator
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copy gradle files
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Copy all module sources
COPY ahs-common ./ahs-common
COPY ahs-domain ./ahs-domain
COPY ahs-thrift-api ./ahs-thrift-api
COPY ahs-data-generator ./ahs-data-generator

# Build the application (skip thrift compilation issues)
RUN gradle :ahs-data-generator:bootJar -x :ahs-thrift-api:compileJava --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Install wget for health checks
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

# Copy the built JAR
COPY --from=builder /app/ahs-data-generator/build/libs/*.jar app.jar

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
