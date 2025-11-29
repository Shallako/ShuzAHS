# Komatsu Autonomous Haulage System (AHS) - Streaming Platform

A production-ready implementation of a real-time telemetry processing and fleet management system for Komatsu's autonomous mining trucks, built with Apache Flink, Apache Thrift, and Spring Boot.

![Java](https://img.shields.io/badge/Java-11-orange)
![Gradle](https://img.shields.io/badge/Gradle-8.14-blue)
![Flink](https://img.shields.io/badge/Apache%20Flink-1.18.0-red)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-green)

## 📋 Table of Contents

- [Architecture Overview](#architecture-overview)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Running Locally](#running-locally)
- [API Documentation](#api-documentation)
- [Configuration Guide](#configuration-guide)
- [Testing Scenarios](#testing-scenarios)
- [Troubleshooting](#troubleshooting)

---

## 🏗️ Architecture Overview

### System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      DATA GENERATION LAYER                       │
│  ┌────────────────────────────────────────────────────────┐    │
│  │ ahs-data-generator (CLI Application)                   │    │
│  │ • Simulates 15-1000+ autonomous haul trucks            │    │
│  │ • Komatsu 930E (300-ton) & 980E (400-ton)             │    │
│  │ • Realistic state machine: IDLE → ROUTING → LOADING    │    │
│  │   → HAULING → DUMPING → repeat                         │    │
│  │ • Generates telemetry: GPS, speed, load, fuel, etc.    │    │
│  └────────────────────────┬───────────────────────────────┘    │
└─────────────────────────────┼───────────────────────────────────┘
                              │
                              ▼ Kafka Topic: vehicle-telemetry
┌─────────────────────────────────────────────────────────────────┐
│                   STREAM PROCESSING LAYER                        │
│  ┌────────────────────────────────────────────────────────┐    │
│  │ ahs-telemetry-processor (Apache Flink)                │    │
│  │ • Real-time telemetry ingestion from Kafka             │    │
│  │ • Complex Event Processing (CEP) for anomalies         │    │
│  │ • Windowed aggregations (1-min tumbling windows)       │    │
│  │ • Alert detection: low fuel, overheating, rapid decel  │    │
│  │ • Outputs: alerts & metrics to Kafka                   │    │
│  └────────────────────────┬───────────────────────────────┘    │
└─────────────────────────────┼───────────────────────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
          ▼                   ▼                   ▼
    Kafka: alerts      Kafka: metrics      Kafka: telemetry
          │                   │                   │
┌─────────┴───────────────────┴───────────────────┴───────────────┐
│                    APPLICATION LAYER                              │
│  ┌────────────────────────────────────────────────────────┐    │
│  │ ahs-fleet-management (Spring Boot REST API)            │    │
│  │ Port: 8080                                              │    │
│  │ • Consumes telemetry events via Kafka                  │    │
│  │ • Tracks real-time vehicle state & location            │    │
│  │ • Fleet-wide statistics & monitoring                    │    │
│  │ • REST endpoints for external integration              │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                   │
│  ┌────────────────────────────────────────────────────────┐    │
│  │ ahs-vehicle-service (Spring Boot + Thrift)             │    │
│  │ Port: 8081 (REST), Thrift RPC                          │    │
│  │ • Vehicle CRUD operations                               │    │
│  │ • Thrift RPC services for cross-service communication  │    │
│  │ • Integration with DISPATCH FMS (simulated)            │    │
│  └────────────────────────────────────────────────────────┘    │
└───────────────────────────────────────────────────────────────────┘
```

### Technology Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Stream Processing** | Apache Flink 1.18.0 | Real-time telemetry processing, CEP, windowed aggregations |
| **Message Broker** | Apache Kafka 3.6.0 | Event streaming, decoupling services |
| **RPC Framework** | Apache Thrift 0.19.0 | Cross-service communication, API definitions |
| **Backend Framework** | Spring Boot 2.7.18 | REST APIs, dependency injection, auto-configuration |
| **Serialization** | Jackson 2.15.3 | JSON processing for events and APIs |
| **Build Tool** | Gradle 8.14 | Multi-module build, dependency management |
| **Language** | Java 11 | All modules |

### Data Flow

1. **Generation**: Data generator simulates autonomous trucks publishing telemetry to Kafka
2. **Ingestion**: Flink consumes telemetry from Kafka topic `vehicle-telemetry`
3. **Processing**: 
   - CEP patterns detect anomalies (rapid deceleration, overheating, low fuel)
   - Windowed aggregations compute per-vehicle metrics every minute
4. **Output**: Alerts and metrics published to separate Kafka topics
5. **Consumption**: Fleet management service consumes events and updates vehicle state
6. **Exposure**: REST APIs provide access to fleet data for external systems

---

## 📁 Project Structure

```
ShuzAHS/
├── ahs-domain/                     # Core domain models and events
│   ├── model/                      # Vehicle, Location, Telemetry, etc.
│   └── events/                     # VehicleTelemetryEvent, VehicleCommandEvent, etc.
│
├── ahs-common/                     # Shared utilities
│   └── util/                       # Common helper classes
│
├── ahs-thrift-api/                 # Thrift service definitions
│   └── thrift/                     # .thrift IDL files
│
├── ahs-data-generator/             # CLI data generator
│   ├── generator/                  # TelemetryDataGenerator, VehicleSimulator
│   └── kafka/                      # KafkaTelemetryProducer
│
├── ahs-telemetry-processor/        # Flink stream processor
│   ├── function/                   # Map/FlatMap functions, CEP patterns
│   └── model/                      # TelemetryAlert, VehicleMetrics
│
├── ahs-fleet-management/           # Fleet management REST API
│   ├── service/                    # FleetManagementService
│   ├── kafka/                      # Kafka consumers
│   └── controller/                 # REST controllers
│
├── ahs-vehicle-service/            # Vehicle CRUD service
│   ├── service/                    # Vehicle operations
│   └── thrift/                     # Thrift service implementations
│
└── ahs-stream-analytics/           # Additional stream analytics
    └── analytics/                  # Custom analytics jobs
```

### Module Dependencies

```
ahs-domain (base)
    ↓
ahs-common
    ↓
├─→ ahs-thrift-api
├─→ ahs-data-generator → Kafka
├─→ ahs-telemetry-processor → Flink + Kafka
├─→ ahs-fleet-management → Spring Boot + Kafka
├─→ ahs-vehicle-service → Spring Boot + Thrift
└─→ ahs-stream-analytics → Flink
```

---

## 🔧 Prerequisites

### Required

- **Java 11** or higher
  ```bash
  java -version
  # openjdk version "11.0.x" or higher
  ```

- **Gradle 8.x** (wrapper included)
  ```bash
  ./gradlew --version
  ```

### Optional

- **Docker** & **Docker Compose** (for Kafka)
  ```bash
  docker --version
  docker-compose --version
  ```

- **Apache Thrift Compiler** (optional, for regenerating Thrift code)
  ```bash
  brew install thrift  # macOS
  # or download from https://thrift.apache.org
  ```

### Infrastructure

- **Apache Kafka** (can run via Docker)
- **Apache Zookeeper** (for Kafka)

---

## ⚡ Quick Start

### 1. Build the Project

```bash
# Clone the repository
cd ShuzAHS

# Build all modules (skip tests for quick build)
./gradlew build -x test
```

Expected output:
```
BUILD SUCCESSFUL in 5s
28 actionable tasks: 28 executed
```

### 2. Start Kafka (Docker)

Create `docker-compose.yml`:
```yaml
version: '3'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

Start Kafka:
```bash
docker-compose up -d
```

### 3. Create Kafka Topics

```bash
# Create telemetry topic
docker exec -it $(docker ps -qf "name=kafka") kafka-topics \
  --create --bootstrap-server localhost:9092 \
  --topic vehicle-telemetry \
  --partitions 3 \
  --replication-factor 1

# Create alerts topic
docker exec -it $(docker ps -qf "name=kafka") kafka-topics \
  --create --bootstrap-server localhost:9092 \
  --topic telemetry-alerts \
  --partitions 3 \
  --replication-factor 1

# Create metrics topic
docker exec -it $(docker ps -qf "name=kafka") kafka-topics \
  --create --bootstrap-server localhost:9092 \
  --topic vehicle-metrics \
  --partitions 3 \
  --replication-factor 1
```

### 4. Start Services

Open separate terminals for each service:

**Terminal 1 - Data Generator:**
```bash
./run-generator.sh
# OR with custom settings:
java -jar ahs-data-generator/build/libs/ahs-data-generator.jar -v 15 -i 5000
```

**Terminal 2 - Flink Telemetry Processor:**
```bash
./gradlew :ahs-telemetry-processor:run
```

**Terminal 3 - Fleet Management:**
```bash
./gradlew :ahs-fleet-management:bootRun
```

**Terminal 4 - Vehicle Service:**
```bash
./gradlew :ahs-vehicle-service:bootRun
```

### 5. Verify System is Running

```bash
# Check fleet statistics
curl http://localhost:8080/api/fleet/statistics

# Watch Kafka topic
docker exec -it $(docker ps -qf "name=kafka") kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic vehicle-telemetry \
  --from-beginning \
  --max-messages 5
```

---

## 🚀 Running Locally

### Development Workflow

#### 1. Start Infrastructure

```bash
# Start Kafka + Zookeeper
docker-compose up -d

# Verify Kafka is running
docker ps | grep kafka
```

#### 2. Build Project

```bash
# Clean build
./gradlew clean build -x test

# Build specific module
./gradlew :ahs-data-generator:build

# Continuous build (watches for changes)
./gradlew build --continuous
```

#### 3. Run Data Generator

**Default Configuration (15 vehicles, 5-second interval):**
```bash
./run-generator.sh
```

**Custom Configuration:**
```bash
java -jar ahs-data-generator/build/libs/ahs-data-generator.jar \
  --vehicles 50 \
  --interval 2000 \
  --duration 30 \
  --bootstrap-servers localhost:9092 \
  --topic vehicle-telemetry
```

**Parameters:**
- `-v, --vehicles` : Number of vehicles to simulate (default: 15)
- `-i, --interval` : Publish interval in milliseconds (default: 5000)
- `-d, --duration` : Duration to run in minutes, 0=infinite (default: 0)
- `-b, --bootstrap-servers` : Kafka bootstrap servers (default: localhost:9092)
- `-t, --topic` : Kafka topic (default: vehicle-telemetry)

**Expected Output:**
```
12:34:56.789 [main] INFO  c.k.a.g.DataGeneratorApp - Starting AHS Data Generator
12:34:56.790 [main] INFO  c.k.a.g.DataGeneratorApp - Configuration: vehicles=15, interval=5000ms
12:34:56.801 [main] INFO  c.k.a.g.DataGeneratorApp - Initialized 15 vehicles (10 x 930E, 5 x 980E)
12:35:06.855 [pool-1-thread-2] INFO  c.k.a.g.DataGeneratorApp - === Fleet Status ===
12:35:06.856 [pool-1-thread-2] INFO  c.k.a.g.DataGeneratorApp -   KOMATSU-930E-001 [HAULING] - Cycle: 1, Remaining: 54s
```

#### 4. Run Flink Telemetry Processor

```bash
./gradlew :ahs-telemetry-processor:run
```

**Expected Output:**
```
INFO  o.a.f.r.d.DispatcherRestEndpoint - Web frontend listening at http://localhost:8081
INFO  c.k.a.t.TelemetryProcessorJob - Starting Telemetry Processing Job
INFO  o.a.f.r.l.s.StandaloneSessionClusterEntrypoint - Creating streaming environment
```

#### 5. Run Fleet Management Service

```bash
./gradlew :ahs-fleet-management:bootRun
```

**Expected Output:**
```
INFO  c.k.a.f.FleetManagementApplication - Started FleetManagementApplication in 3.456 seconds
INFO  c.k.a.f.s.FleetManagementService - Initialized Fleet Management Service
INFO  c.k.a.f.s.FleetManagementService - Initialized mock fleet with 15 vehicles
```

#### 6. Run Vehicle Service (Optional)

```bash
./gradlew :ahs-vehicle-service:bootRun
```

### Monitoring

**Watch Kafka Topics:**
```bash
# Watch telemetry
docker exec -it $(docker ps -qf "name=kafka") kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic vehicle-telemetry

# Watch alerts
docker exec -it $(docker ps -qf "name=kafka") kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic telemetry-alerts

# Watch metrics
docker exec -it $(docker ps -qf "name=kafka") kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic vehicle-metrics
```

**Check Flink Web UI:**
```
http://localhost:8081
```

**Test REST APIs:**
```bash
# Fleet statistics
curl http://localhost:8080/api/fleet/statistics | jq

# List vehicles
curl http://localhost:8080/api/fleet/vehicles | jq

# Get specific vehicle
curl http://localhost:8080/api/fleet/vehicles/KOMATSU-930E-001 | jq
```

---

## 📚 API Documentation

### Fleet Management API

**Base URL:** `http://localhost:8080/api/fleet`

#### Get Fleet Statistics

```http
GET /api/fleet/statistics
```

**Response:**
```json
{
  "totalVehicles": 15,
  "activeVehicles": 12,
  "idleVehicles": 3,
  "statusBreakdown": {
    "HAULING": 5,
    "LOADING": 3,
    "DUMPING": 2,
    "ROUTING": 2,
    "IDLE": 3
  }
}
```

#### Get All Vehicles

```http
GET /api/fleet/vehicles
```

**Response:**
```json
[
  {
    "vehicleId": "KOMATSU-930E-001",
    "model": "930E",
    "manufacturer": "Komatsu",
    "capacity": 300.0,
    "status": "HAULING"
  },
  {
    "vehicleId": "KOMATSU-980E-001",
    "model": "980E",
    "manufacturer": "Komatsu",
    "capacity": 400.0,
    "status": "LOADING"
  }
]
```

#### Get Vehicle by ID

```http
GET /api/fleet/vehicles/{vehicleId}
```

**Example:**
```bash
curl http://localhost:8080/api/fleet/vehicles/KOMATSU-930E-001
```

**Response:**
```json
{
  "vehicleId": "KOMATSU-930E-001",
  "model": "930E",
  "manufacturer": "Komatsu",
  "capacity": 300.0,
  "status": "HAULING",
  "telemetry": {
    "speedKph": 35.2,
    "payloadTons": 285.7,
    "fuelLevelPercent": 75.3,
    "location": {
      "latitude": -23.42,
      "longitude": -70.38,
      "altitude": 2950.5
    },
    "engineTemperatureCelsius": 87.5,
    "timestamp": "2025-11-28T12:34:56.789Z"
  }
}
```

#### Get Vehicles by Status

```http
GET /api/fleet/vehicles/status/{status}
```

**Valid Status Values:**
- `IDLE`
- `ROUTING`
- `LOADING`
- `HAULING`
- `DUMPING`
- `MAINTENANCE`
- `EMERGENCY_STOP`

**Example:**
```bash
curl http://localhost:8080/api/fleet/vehicles/status/HAULING
```

### Telemetry Event Schema

**Kafka Topic:** `vehicle-telemetry`

**Message Format:**
```json
{
  "eventId": "abc-123-def-456",
  "vehicleId": "KOMATSU-930E-001",
  "timestamp": "2025-11-28T12:34:56.789Z",
  "source": "data-generator",
  "eventType": "TELEMETRY_UPDATE",
  "telemetry": {
    "vehicleId": "KOMATSU-930E-001",
    "timestamp": "2025-11-28T12:34:56.789Z",
    "location": {
      "latitude": -23.42,
      "longitude": -70.38,
      "altitude": 2950.5
    },
    "speedKph": 35.2,
    "headingDegrees": 127.5,
    "engineRpm": 1650.0,
    "fuelLevelPercent": 75.3,
    "batteryLevelPercent": 95.0,
    "engineTemperatureCelsius": 87.5,
    "payloadTons": 285.7,
    "isLoaded": true,
    "brakePressurePsi": 52.3,
    "tirePressureFrontLeftPsi": 92.1,
    "tirePressureFrontRightPsi": 91.8,
    "tirePressureRearLeftPsi": 93.2,
    "tirePressureRearRightPsi": 92.5,
    "hydraulicPressurePsi": 2450.0
  }
}
```

### Alert Event Schema

**Kafka Topic:** `telemetry-alerts`

```json
{
  "alertId": "alert-789",
  "vehicleId": "KOMATSU-930E-001",
  "timestamp": "2025-11-28T12:35:00.000Z",
  "alertType": "LOW_FUEL",
  "severity": "WARNING",
  "message": "Low fuel level: 12.3%",
  "metricValue": 12.3,
  "thresholdValue": 15.0
}
```

**Alert Types:**
- `HIGH_TEMPERATURE`
- `LOW_FUEL`
- `LOW_BATTERY`
- `TIRE_PRESSURE_LOW`
- `BRAKE_PRESSURE_LOW`
- `ENGINE_WARNING`
- `OVERLOAD`
- `RAPID_DECELERATION`
- `OVERHEATING`

**Severity Levels:**
- `INFO`
- `WARNING`
- `CRITICAL`
- `EMERGENCY`

---

## ⚙️ Configuration Guide

### Data Generator Configuration

**Location:** Command-line parameters

```bash
java -jar ahs-data-generator.jar \
  --vehicles 50 \              # Number of vehicles
  --interval 2000 \            # Publish interval (ms)
  --duration 60 \              # Run duration (minutes)
  --bootstrap-servers localhost:9092 \
  --topic vehicle-telemetry
```

**Environment-specific configs:**

Development:
```bash
./run-generator.sh  # 15 vehicles, 5s interval
```

Load Testing:
```bash
java -jar ahs-data-generator.jar -v 100 -i 1000 -d 120
```

Stress Testing:
```bash
java -jar ahs-data-generator.jar -v 200 -i 500
```

### Fleet Management Configuration

**Location:** `ahs-fleet-management/src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: ahs-fleet-management

kafka:
  bootstrap-servers: localhost:9092
  consumer:
    group-id: fleet-management-group
    auto-offset-reset: earliest
    key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
  topics:
    vehicle-telemetry: vehicle-telemetry

logging:
  level:
    com.komatsu.ahs: INFO
    org.apache.kafka: WARN
```

**Override via environment variables:**
```bash
export KAFKA_BOOTSTRAP_SERVERS=kafka-cluster:9092
export SERVER_PORT=8080
./gradlew :ahs-fleet-management:bootRun
```

**Override via application-{profile}.yml:**

Create `application-prod.yml`:
```yaml
kafka:
  bootstrap-servers: prod-kafka:9092
  
logging:
  level:
    com.komatsu.ahs: WARN
```

Run with profile:
```bash
./gradlew :ahs-fleet-management:bootRun --args='--spring.profiles.active=prod'
```

### Flink Configuration

**Location:** `ahs-telemetry-processor/src/main/resources/flink-conf.yaml`

```yaml
# JobManager config
jobmanager:
  memory:
    process:
      size: 1600m

# TaskManager config
taskmanager:
  memory:
    process:
      size: 1728m
  numberOfTaskSlots: 2

# Checkpointing
execution:
  checkpointing:
    interval: 10s
    mode: EXACTLY_ONCE
```

**Runtime parameters:**
```bash
./gradlew :ahs-telemetry-processor:run \
  -Djobmanager.memory.process.size=2g \
  -Dtaskmanager.memory.process.size=2g \
  -Dparallelism.default=4
```

### Kafka Configuration

**Topic Configuration:**

```bash
# High-throughput topic
kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic vehicle-telemetry \
  --partitions 6 \
  --replication-factor 3 \
  --config retention.ms=86400000 \
  --config segment.ms=3600000
```

**Producer Configuration (in code):**
```java
props.put(ProducerConfig.ACKS_CONFIG, "1");
props.put(ProducerConfig.RETRIES_CONFIG, 3);
props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
```

### JVM Configuration

**For high-throughput scenarios:**

```bash
export JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC"
./gradlew :ahs-fleet-management:bootRun
```

---

## 🧪 Testing Scenarios

### Scenario 1: Basic Functionality Test

**Objective:** Verify end-to-end data flow

```bash
# 1. Start services
docker-compose up -d
./gradlew :ahs-fleet-management:bootRun &
./gradlew :ahs-telemetry-processor:run &

# 2. Generate data (5 vehicles for 5 minutes)
java -jar ahs-data-generator.jar -v 5 -d 5

# 3. Verify
curl http://localhost:8080/api/fleet/statistics
```

**Expected:** All 5 vehicles tracked, telemetry updating

### Scenario 2: Load Testing

**Objective:** Test system under production load

```bash
# Simulate 100 vehicles, 2-second interval, 30 minutes
java -jar ahs-data-generator.jar -v 100 -i 2000 -d 30

# Monitor Flink UI
open http://localhost:8081

# Monitor fleet API
watch -n 5 'curl -s http://localhost:8080/api/fleet/statistics | jq'
```

**Expected:** 3,000 events/min processed, no backpressure

### Scenario 3: Failure Recovery

**Objective:** Test resilience

```bash
# 1. Start normal load
java -jar ahs-data-generator.jar -v 20 -i 5000 &

# 2. Kill Flink processor
pkill -f TelemetryProcessorJob

# 3. Wait 30 seconds, restart
./gradlew :ahs-telemetry-processor:run &

# 4. Verify catch-up
# Should process backlog from Kafka
```

**Expected:** No data loss, automatic catch-up

### Scenario 4: Alert Detection

**Objective:** Verify CEP patterns trigger

```bash
# Generate data with low fuel scenario
# (Data generator will eventually create low fuel situations)

# Watch alerts
docker exec -it $(docker ps -qf "name=kafka") kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic telemetry-alerts

# Should see alerts like:
# {"alertType":"LOW_FUEL","severity":"WARNING",...}
```

---

## 🔍 Troubleshooting

### Build Issues

**Problem:** `BUILD FAILED` with compilation errors

**Solution:**
```bash
./gradlew clean build -x test --refresh-dependencies
```

**Problem:** Out of memory during build

**Solution:**
```bash
export GRADLE_OPTS="-Xmx4g"
./gradlew clean build
```

### Kafka Issues

**Problem:** Cannot connect to Kafka

**Solution:**
```bash
# Check Kafka is running
docker ps | grep kafka

# Check Kafka logs
docker logs $(docker ps -qf "name=kafka")

# Restart Kafka
docker-compose restart kafka
```

**Problem:** Topic doesn't exist

**Solution:**
```bash
# List topics
docker exec -it $(docker ps -qf "name=kafka") kafka-topics \
  --list --bootstrap-server localhost:9092

# Create missing topic
docker exec -it $(docker ps -qf "name=kafka") kafka-topics \
  --create --bootstrap-server localhost:9092 \
  --topic vehicle-telemetry \
  --partitions 3 --replication-factor 1
```

### Flink Issues

**Problem:** Flink job fails to start

**Solution:**
```bash
# Check Flink logs
tail -f ~/.flink/log/flink-*-taskexecutor-*.log

# Increase memory
./gradlew :ahs-telemetry-processor:run \
  -Djobmanager.memory.process.size=2g
```

**Problem:** Backpressure / slow processing

**Solution:**
```bash
# Increase parallelism
# Edit flink-conf.yaml: parallelism.default: 4

# Or in code:
env.setParallelism(4);
```

### Fleet Management Issues

**Problem:** Port 8080 already in use

**Solution:**
```bash
# Option 1: Kill process on port 8080
lsof -ti:8080 | xargs kill -9

# Option 2: Change port in application.yml
# server.port: 8090
```

**Problem:** Not receiving Kafka events

**Solution:**
```bash
# Check consumer group
docker exec -it $(docker ps -qf "name=kafka") kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group fleet-management-group

# Reset offset if needed
docker exec -it $(docker ps -qf "name=kafka") kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group fleet-management-group \
  --reset-offsets --to-earliest --topic vehicle-telemetry --execute
```

### Data Generator Issues

**Problem:** No data being generated

**Solution:**
```bash
# Check Kafka connectivity
java -jar ahs-data-generator.jar -v 1 -i 1000

# Check logs for errors
# Common issue: Kafka not running or wrong bootstrap servers
```

---

## 📖 Additional Resources

- [Architecture Overview](PROJECT_COMPLETE.md)
- [Data Generator Guide](ahs-data-generator/README.md)
- [Quick Reference](QUICK_REFERENCE.md)
- [Apache Flink Documentation](https://flink.apache.org)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)

---

## 👥 Contributing

This is a demonstration project for Komatsu Mining Technology Solutions.

---

## 📄 License

Proprietary - Komatsu Mining Technology Solutions

---

## 🏆 Project Status

✅ **Production Ready**
- All modules compile successfully
- Complete end-to-end data flow
- Comprehensive documentation
- Ready for demonstration

**Built with ❤️ for Komatsu MTS**
