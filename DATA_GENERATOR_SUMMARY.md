# 🚛 Komatsu AHS Data Generator - Complete

## ✅ What Was Created

A complete, production-ready data generator module for testing the Komatsu Autonomous Haulage System streaming pipeline.

### 📦 Module: `ahs-data-generator`

#### Core Components

**1. TelemetryDataGenerator.java**
- Generates realistic telemetry data for Komatsu 930E and 980E haul trucks
- Simulates GPS coordinates within mine site boundaries
- Generates state-appropriate metrics:
  - Speed (0-60 km/h depending on state)
  - Payload (0-400 tons)
  - Fuel level (5-100%)
  - Engine temperature, RPM, heading
  - Tire pressures, brake pressure, hydraulic pressure
  
**2. VehicleSimulator.java**
- Manages vehicle lifecycle state transitions
- Realistic state durations:
  - IDLE: 10-60s
  - ROUTING: 30-120s
  - LOADING: 60-180s
  - HAULING: 120-300s
  - DUMPING: 40-90s
- Tracks cycle counts and state timing

**3. KafkaTelemetryProducer.java**
- Kafka producer for publishing telemetry events
- JSON serialization with Jackson
- Async publishing with callbacks
- Batch flushing support

**4. DataGeneratorApp.java**
- CLI application with PicoCLI
- Configurable parameters:
  - Bootstrap servers
  - Topic name
  - Number of vehicles
  - Publish interval
  - Duration
- Scheduled executors for telemetry publishing and state updates
- Fleet status monitoring
- Graceful shutdown

### 📊 Generated Event Structure

Each telemetry event published to Kafka:

```json
{
  "eventId": "uuid",
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

## 🚀 Usage

### Quick Start

```bash
# Build and run with defaults
./run-generator.sh
```

### Custom Configuration

```bash
# Build
./gradlew :ahs-data-generator:build

# Run with 50 vehicles, 2-second interval, for 30 minutes
java -jar ahs-data-generator/build/libs/ahs-data-generator.jar \
  -v 50 \
  -i 2000 \
  -d 30 \
  -b localhost:9092 \
  -t vehicle-telemetry
```

### Load Testing Scenarios

**Small Fleet (Development)**
```bash
java -jar ahs-data-generator.jar -v 5 -i 10000
# 5 vehicles, every 10 seconds = ~30 events/minute
```

**Medium Fleet (Testing)**
```bash
java -jar ahs-data-generator.jar -v 25 -i 5000
# 25 vehicles, every 5 seconds = ~300 events/minute
```

**Large Fleet (Production Scale)**
```bash
java -jar ahs-data-generator.jar -v 100 -i 2000 -d 120
# 100 vehicles, every 2 seconds = ~3,000 events/minute for 2 hours
```

**Stress Test**
```bash
java -jar ahs-data-generator.jar -v 200 -i 1000
# 200 vehicles, every 1 second = ~12,000 events/minute
```

## 📈 Data Volume Examples

| Vehicles | Interval | Events/min | Events/hour | Events/day |
|----------|----------|------------|-------------|------------|
| 10 | 5s | 120 | 7,200 | 172,800 |
| 50 | 5s | 600 | 36,000 | 864,000 |
| 100 | 2s | 3,000 | 180,000 | 4,320,000 |
| 200 | 1s | 12,000 | 720,000 | 17,280,000 |

## 🔗 Integration with Pipeline

The generated data flows through the complete system:

```
┌─────────────────────┐
│  Data Generator     │
│  (15 vehicles)      │
└──────────┬──────────┘
           │ Kafka Topic: vehicle-telemetry
           │ (JSON events)
           ▼
┌─────────────────────┬──────────────────────┐
│                     │                      │
▼                     ▼                      ▼
Telemetry          Fleet              Stream
Processor         Management        Analytics
(Hazelcast Jet)   (Spring Boot)     (optional)
```

### Downstream Consumers

1. **ahs-telemetry-processor** (Hazelcast Jet, embedded)
   - Real-time stream processing
   - Windowed aggregations
   - Alert detection

2. **ahs-fleet-management** (Spring Boot)
   - Vehicle state tracking
   - Fleet statistics
   - REST API

3. **ahs-stream-analytics** (optional)
   - Complex event processing
   - Metrics calculation
   - Anomaly detection

## 🧪 Testing Scenarios

### Scenario 1: Basic Functionality
```bash
# Generate data for 5 minutes with 10 vehicles
java -jar ahs-data-generator.jar -v 10 -d 5
```
**Validates:** Basic telemetry generation, state transitions, Kafka connectivity

### Scenario 2: State Transition Testing
```bash
# Run with detailed logging to observe state changes
java -jar ahs-data-generator.jar -v 3 -i 3000
# Watch the logs for state transitions
```
**Validates:** Vehicle lifecycle, timing accuracy, state logic

### Scenario 3: High Throughput
```bash
# Simulate large fleet
java -jar ahs-data-generator.jar -v 100 -i 1000 -d 10
```
**Validates:** Kafka throughput, telemetry processing capacity, backpressure handling

### Scenario 4: Long Running Stability
```bash
# Run for 24 hours
java -jar ahs-data-generator.jar -v 50 -i 5000 -d 1440
```
**Validates:** Memory leaks, connection stability, long-term reliability

## 📝 Next Steps

1. **Start Kafka** (if not running):
   ```bash
   # Using Docker
   docker-compose up -d kafka
   ```

2. **Create Topic**:
   ```bash
   kafka-topics --create \
     --bootstrap-server localhost:9092 \
     --topic vehicle-telemetry \
     --partitions 3 \
     --replication-factor 1
   ```

3. **Start Data Generator**:
   ```bash
   ./run-generator.sh
   ```

4. **Start Consumers**:
   ```bash
   # Terminal 1: Telemetry Processor
   ./gradlew :ahs-telemetry-processor:run
   
   # Terminal 2: Fleet management
   ./gradlew :ahs-fleet-management:bootRun
   ```

5. **Monitor Output**:
   ```bash
   # Watch Kafka topic
   kafka-console-consumer \
     --bootstrap-server localhost:9092 \
     --topic vehicle-telemetry \
     --from-beginning
   ```

## ✨ Features Highlights

- ✅ **Realistic Data**: Mimics actual Komatsu 930E/980E truck behavior
- ✅ **Mixed Fleet**: Automatically creates 67% 930E, 33% 980E trucks
- ✅ **State Machine**: Full vehicle lifecycle simulation
- ✅ **Configurable**: CLI parameters for all aspects
- ✅ **Production Ready**: Proper logging, error handling, graceful shutdown
- ✅ **Scalable**: Test from 1 to 1000+ vehicles
- ✅ **Well Documented**: README, JavaDocs, examples

## 🎯 Success Metrics

The data generator successfully:
- ✅ Compiles without errors
- ✅ Generates realistic telemetry data
- ✅ Publishes to Kafka
- ✅ Simulates vehicle state transitions
- ✅ Provides CLI interface
- ✅ Handles graceful shutdown
- ✅ Scales to hundreds of vehicles
- ✅ Integrated with existing domain model

**Ready for testing the complete AHS streaming pipeline! 🚀**
