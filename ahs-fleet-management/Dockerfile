# Multi-stage build for Fleet Management Service
FROM gradle:8.5-jdk11 AS builder

WORKDIR /app

# Copy gradle files
COPY settings.gradle .
COPY build.gradle .
COPY gradle.properties .

# Copy all modules
COPY ahs-common ./ahs-common
COPY ahs-domain ./ahs-domain
COPY ahs-thrift-api ./ahs-thrift-api
COPY ahs-fleet-management ./ahs-fleet-management

# Build the application
RUN gradle :ahs-fleet-management:bootJar -x test --no-daemon

# Runtime stage
FROM openjdk:11-jre-slim

WORKDIR /app

# Copy the built jar
COPY --from=builder /app/ahs-fleet-management/build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
