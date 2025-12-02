# 🚀 Komatsu AHS Quick Reference

## Build & Run Commands

### Build Everything
```bash
./gradlew build -x test
```

### Run Data Generator
```bash
# Default: 15 vehicles, 5 second interval
./run-generator.sh

# Custom: 50 vehicles, 2 second interval, 30 minutes
java -jar ahs-data-generator/build/libs/ahs-data-generator.jar -v 50 -i 2000 -d 30
```

### Run Flink Telemetry Processor
```bash
./gradlew :ahs-telemetry-processor:run
```

### Run Fleet Management Service
```bash
./gradlew :ahs-fleet-management:bootRun
```

### Run Vehicle Service
```bash
./gradlew :ahs-vehicle-service:bootRun
```

## Kafka Commands

### Create Topic
```bash
kafka-topics --create --bootstrap-server localhost:9092 \
  --topic vehicle-telemetry --partitions 3 --replication-factor 1
```

### Watch Topic
```bash
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic vehicle-telemetry --from-beginning
```

## API Endpoints

### Fleet Management (Port 8080, context-path: /)
```bash
# Base path: /api/v1/fleet

# Get fleet statistics
curl http://localhost:8080/api/v1/fleet/statistics

# Get all vehicles
curl http://localhost:8080/api/v1/fleet/vehicles

# Get vehicle by ID
curl http://localhost:8080/api/v1/fleet/vehicles/KOMATSU-930E-001
```

## Project Modules

| Module | Purpose | Technology |
|--------|---------|------------|
| ahs-domain | Core models | POJOs, Lombok |
| ahs-data-generator | Test data | Java 17, Kafka |
| ahs-telemetry-processor | Stream processing | Apache Flink |
| ahs-fleet-management | Fleet API | Spring Boot |
| ahs-vehicle-service | Vehicle API | Spring Boot, Thrift |
| ahs-stream-analytics | Analytics | Apache Flink |

## Key Files

- `PROJECT_COMPLETE.md` - Complete documentation
- `DATA_GENERATOR_SUMMARY.md` - Generator details
- `run-generator.sh` - Quick start script
- `settings.gradle` - Module configuration
- `build.gradle` - Root build config

## Troubleshooting

### Build fails
```bash
./gradlew clean build -x test
```

### Flink Kafka connector resolution
- For Flink ≥ 1.15, connectors are versioned independently from core Flink.
- With Flink 1.18.x use `org.apache.flink:flink-connector-kafka:3.x.y-1.18`.
- Example (Gradle):
```gradle
implementation "org.apache.flink:flink-connector-kafka:${flinkKafkaConnectorVersion}" // e.g., 3.2.0-1.18
```
Do not force a separate `org.apache.kafka:kafka-clients` version; the connector brings the correct client transitively.

### Kafka not running
```bash
docker-compose up -d kafka zookeeper
```

### Port already in use
```bash
# Fleet Management: Change in application.yml (default: 8080, context-path: /)
# Vehicle Service: Change in application.yml (default: 8080)
```

## Success Indicators

✅ BUILD SUCCESSFUL
✅ All 8 modules compile
✅ Data flows: Generator → Kafka → Flink → Fleet Management
✅ REST APIs respond
✅ Realistic telemetry data generated

**Ready for demonstration! 🎯**
