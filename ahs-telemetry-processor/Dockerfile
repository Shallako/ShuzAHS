# Dockerfile for AHS Telemetry Processor (Flink Job)
# Submits the Flink job to the cluster via REST API

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
COPY ahs-telemetry-processor/build.gradle ahs-telemetry-processor/

# Copy source code
COPY ahs-common/src ahs-common/src
COPY ahs-domain/src ahs-domain/src
COPY ahs-thrift-api/src ahs-thrift-api/src
COPY ahs-telemetry-processor/src ahs-telemetry-processor/src

# Build the shadow JAR
RUN chmod +x gradlew && \
    ./gradlew :ahs-telemetry-processor:shadowJar -x test --no-daemon

# Runtime stage - uses curl to submit job to Flink
FROM eclipse-temurin:17-jre

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the fat JAR from builder
COPY --from=builder /app/ahs-telemetry-processor/build/libs/*-all.jar /app/telemetry-processor.jar

# Copy the submission script
COPY ahs-telemetry-processor/submit-job.sh /app/submit-job.sh
RUN chmod +x /app/submit-job.sh

# Environment variables
ENV FLINK_JOBMANAGER_HOST=flink-jobmanager
ENV FLINK_JOBMANAGER_PORT=8081
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:29092

ENTRYPOINT ["/app/submit-job.sh"]
