# Multi-stage build for AHS Fleet Management Service
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copy gradle files
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Copy all module sources
COPY ahs-common ./ahs-common
COPY ahs-domain ./ahs-domain
COPY ahs-fleet-management ./ahs-fleet-management

# Build the application
RUN gradle :ahs-fleet-management:bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Install wget for health checks
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

# Copy the built JAR
COPY --from=builder /app/ahs-fleet-management/build/libs/*.jar app.jar

# Expose port
EXPOSE 8083

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
