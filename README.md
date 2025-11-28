# ShuzAHS - Komatsu Autonomous Haulage System

A comprehensive Java 11 implementation demonstrating Apache Thrift and Apache Flink integration for Komatsu's Mining Technology Solutions (MTS) Autonomous Haulage System.

## Project Overview

This multi-module Gradle project implements a real-time streaming analytics platform for autonomous mining haul trucks, featuring:

- **Apache Thrift** for efficient cross-language RPC between vehicle controllers and central control systems
- **Apache Flink** for real-time telemetry processing and fleet analytics
- **Spring Boot** for microservices architecture
- **Apache Kafka** for distributed event streaming
- **Domain-Driven Design** with clean separation of concerns

## Architecture

```
┌─────────────────────────────────────────┐
│    Autonomous Haul Trucks (400+)        │
│  - GPS, LiDAR, Sensors                  │
│  - Vehicle Controllers                   │
│  - Thrift Clients                        │
└───────────────┬─────────────────────────┘
                │ Private LTE Network
                ▼
┌─────────────────────────────────────────┐
│     Edge Computing Layer                │
│  - Apache Kafka (ingestion)             │
│  - Apache Flink (stream processing)     │
│  - Local state management               │
└───────────────┬─────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────┐
│    Central Control & Backend            │
│  - Thrift RPC Services                  │
│  - DISPATCH Fleet Management            │
│  - Flink Analytics Pipeline             │
│  - Digital Twin & Simulation            │
└─────────────────────────────────────────┘
```

## Modules

### ahs-common
Shared utilities and constants used across all modules.
- Constants for vehicle states, Kafka topics, network configuration
- JSON serialization utilities
- Common data structures

### ahs-domain
Core domain models representing the business entities.
- `Vehicle` - Autonomous haul truck entity
- `VehicleTelemetry` - Sensor and operational data
- `GpsCoordinate` - High-precision location data with Haversine distance calculations

### ahs-thrift-api
Apache Thrift IDL definitions and generated code.
- Thrift service definitions for RPC
- Data transfer objects (DTOs)
- Vehicle command and telemetry structures

### ahs-stream-analytics
Apache Flink streaming jobs for real-time analytics.
- Telemetry stream processing
- Window-based aggregations (1-minute tumbling windows)
- Critical alert detection
- Vehicle metrics computation

### ahs-vehicle-service
Spring Boot microservice for vehicle management.
- RESTful APIs for vehicle operations
- Thrift RPC server implementation
- Kafka integration for event publishing
- Health monitoring and metrics

### ahs-fleet-management
Fleet optimization and route planning.
- DISPATCH integration
- Route assignment algorithms
- Load balancing

### ahs-telemetry-processor
Real-time telemetry ingestion and processing.
- Kafka consumers for telemetry streams
- Data validation and enrichment
- Time-series data storage

## Prerequisites

- **Java 11** or higher
- **Gradle 8.4** (included via wrapper)
- **Apache Kafka** (for local development)
- **Apache Thrift compiler** (for IDL generation)
- **Docker** (optional, for Kafka/Flink)

## Building the Project

```bash
# Clone the repository
cd /Users/shoulicofreeman/Development/ShuzAHS

# Build all modules
./gradlew clean build

# Build specific module
./gradlew :ahs-stream-analytics:build

# Run tests
./gradlew test
```

## Running the Applications

### Start Kafka (Docker)
```bash
docker-compose up -d kafka zookeeper
```

### Run Vehicle Service
```bash
./gradlew :ahs-vehicle-service:bootRun
```

### Run Flink Streaming Jobs
```bash
# Basic telemetry processor
./gradlew :ahs-stream-analytics:run -PmainClass=com.komatsu.ahs.stream.TelemetryStreamingJob

# Advanced metrics processor with windowing
./gradlew :ahs-stream-analytics:run -PmainClass=com.komatsu.ahs.stream.VehicleMetricsStreamingJob
```

## Key Features

### Real-Time Processing
- Process millions of telemetry events per second
- Sub-second latency for safety-critical decisions
- Fault-tolerant stream processing with Flink checkpointing

### Efficient Communication
- Thrift binary protocol for compact serialization
- Cross-language RPC support (Java ↔ C++ vehicle controllers)
- High-throughput, low-latency messaging

### Analytics & Monitoring
- Window-based aggregations (speed, fuel, temperature)
- Critical alert detection (high temperature, low fuel)
- Distance traveled calculations using Haversine formula
- Predictive maintenance metrics

### Safety Features
- Safety envelope collision detection
- Real-time alert generation
- Emergency stop command processing

## Technology Stack

| Category | Technology | Version |
|----------|-----------|---------|
| Language | Java | 11 |
| Build Tool | Gradle | 8.4 |
| Framework | Spring Boot | 2.7.18 |
| Stream Processing | Apache Flink | 1.17.2 |
| RPC | Apache Thrift | 0.18.1 |
| Messaging | Apache Kafka | 3.5.1 |
| Serialization | Jackson | 2.15.3 |

## Development

### IDE Setup (IntelliJ IDEA)
1. Open IntelliJ IDEA
2. File → Open → Select `/Users/shoulicofreeman/Development/ShuzAHS`
3. Wait for Gradle sync to complete
4. Enable annotation processing: Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable

### Generate Thrift Code
```bash
./gradlew :ahs-thrift-api:compileThrift
```

### Running Tests
```bash
./gradlew test
./gradlew :ahs-domain:test
./gradlew integrationTest
```

## Configuration

### Kafka Topics
- `ahs.telemetry` - Vehicle telemetry data
- `ahs.vehicle.status` - Vehicle state changes
- `ahs.alerts` - Critical alerts and warnings
- `ahs.commands` - Vehicle command queue
- `ahs.gps` - GPS coordinate updates
- `ahs.collision.detection` - Collision detection events

### Thrift Services
- **VehicleService** - Vehicle control and monitoring (port 9090)
  - `sendCommand()` - Send commands to vehicles
  - `getVehicleStatus()` - Retrieve current status
  - `getTelemetry()` - Get latest telemetry

## Future Enhancements

- [ ] AWS integration (Kinesis, EMR)
- [ ] Machine learning for predictive maintenance
- [ ] Digital twin simulation
- [ ] Advanced collision avoidance algorithms
- [ ] Fleet optimization with reinforcement learning
- [ ] Real-time route planning
- [ ] Integration with Modular Mining DISPATCH
- [ ] Power BI/Grafana dashboards
- [ ] Battery electric vehicle optimization
- [ ] Edge computing deployment

## References

- [Komatsu FrontRunner AHS](https://www.komatsu.com/en-us/technology/smart-mining/loading-and-haulage/autonomous-haulage-system)
- [Apache Flink Documentation](https://flink.apache.org/docs/stable/)
- [Apache Thrift Documentation](https://thrift.apache.org/docs/)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)

## License

Proprietary - Komatsu Mining Technology Solutions

---

**Created by:** Shoulico Freeman  
**Date:** November 2024  
**Purpose:** Demonstration of Thrift/Flink integration for autonomous mining operations
