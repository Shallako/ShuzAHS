# Dockerfile for AHS Telemetry Processor (Hazelcast Jet, embedded)
# Builds a fat JAR and runs it directly inside the container.

FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy all module build files
COPY ahs-common/build.gradle ahs-common/
COPY ahs-domain/build.gradle ahs-domain/
COPY ahs-thrift-api/build.gradle ahs-thrift-api/
COPY ahs-proto/build.gradle ahs-proto/
COPY ahs-telemetry-processor/build.gradle ahs-telemetry-processor/

# Copy source code
COPY ahs-common/src ahs-common/src
COPY ahs-domain/src ahs-domain/src
COPY ahs-thrift-api/src ahs-thrift-api/src
COPY ahs-proto/src ahs-proto/src
COPY ahs-telemetry-processor/src ahs-telemetry-processor/src

# Build the shadow JAR
RUN chmod +x gradlew && \
    ./gradlew :ahs-telemetry-processor:shadowJar -x test --no-daemon

# Runtime stage - run the Jet job directly
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the fat JAR from builder
COPY --from=builder /app/ahs-telemetry-processor/build/libs/*-all.jar /app/telemetry-processor.jar

# Environment variables (overridable at runtime)
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:29092 \
    INPUT_TOPIC=ahs.telemetry.processed \
    OUTPUT_ALERTS_TOPIC=ahs.vehicle.alerts \
    OUTPUT_METRICS_TOPIC=ahs.fleet.metrics \
    CONSUMER_GROUP=telemetry-processor-group \
    HZ_JET_ENABLED=true

# Run the embedded Hazelcast Jet job
ENTRYPOINT ["java","-jar","/app/telemetry-processor.jar"]
