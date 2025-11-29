# UI Access Guide - Quick Fix

**Date**: November 29, 2025  
**Status**: ✅ UIs NOW ACCESSIBLE

---

## ✅ WORKING UIs (Ready Now!)

### Kafka UI - http://localhost:8080
**Status**: ✅ Running  
**What you'll see**: Kafka topics, messages, brokers

### Flink Dashboard - http://localhost:8081  
**Status**: ✅ Running  
**What you'll see**: Task managers, job monitoring

---

## ❌ Application Issues Found

### Problem: Spring Boot Apps Crashing

**Services Affected**:
- data-generator (Port 8082) - Restarting
- fleet-management (Port 8083) - Restarting  
- vehicle-service (Port 8084) - Unhealthy

**Error**: 
```
java.lang.NoSuchMethodError: 
'java.lang.ClassLoader ch.qos.logback.core.util.Loader.systemClassloaderIfNull'
```

**Root Cause**: Logback version incompatibility with Java 17

---

## Quick Solution - Use Working Infrastructure

Since the applications have build issues, you can still:

### ✅ 1. View Kafka UI (Working!)
```bash
# Open in browser
open http://localhost:8080
```

**What you can do**:
- View Kafka brokers (should show 1 broker)
- See topics (will be empty until apps run)
- Check consumer groups
- Monitor cluster health

### ✅ 2. View Flink Dashboard (Working!)
```bash
# Open in browser  
open http://localhost:8081
```

**What you can do**:
- See Task Managers (2 running)
- View available task slots (8 total)
- Check JobManager status
- Monitor cluster metrics

### ✅ 3. Interact with Kafka Directly
```bash
# Create a test topic
docker-compose exec kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic test-topic \
  --partitions 3 \
  --replication-factor 1

# Send test message
docker-compose exec kafka bash -c \
  "echo 'Hello Kafka' | kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic test-topic"

# View in Kafka UI - refresh browser
# You should see the test-topic now!
```

### ✅ 4. Check Database
```bash
# Access PostgreSQL
docker-compose exec postgres psql -U ahs_user -d ahs_database

# Run SQL
\dt  # List tables
\q   # Quit
```

### ✅ 5. Check Redis
```bash
# Access Redis CLI
docker-compose exec redis redis-cli

# Test commands
PING              # Should return PONG
SET test "value"  # Set a key
GET test          # Get the value
KEYS *            # List all keys
exit              # Quit
```

---

## Fix the Application Issues

The Spring Boot applications need dependency fixes. Here's the quick fix:

### Option 1: Run Apps Locally (Recommended for Now)

Instead of Docker, run the apps directly with Gradle:

```bash
cd /Users/shoulicofreeman/Development/ShuzAHS

# Terminal 1 - Data Generator
./gradlew :ahs-data-generator:bootRun

# Terminal 2 - Fleet Management  
./gradlew :ahs-fleet-management:bootRun

# Terminal 3 - Vehicle Service
./gradlew :ahs-vehicle-service:bootRun
```

This avoids the Docker build issue and lets you develop faster!

### Option 2: Fix build.gradle Dependencies

The issue is likely a logback version mismatch. To fix in the future:

1. Check `build.gradle` in each module
2. Ensure logback version is compatible with Spring Boot 3.2.0
3. Add explicit dependency management:

```gradle
dependencies {
    implementation 'ch.qos.logback:logback-classic:1.4.14'
    implementation 'ch.qos.logback:logback-core:1.4.14'
}
```

---

## What's Currently Working

| Service | Status | Port | Access |
|---------|--------|------|--------|
| Zookeeper | ✅ Running | 2181 | Internal |
| Kafka | ✅ Healthy | 9092 | Internal |
| Kafka UI | ✅ Running | 8080 | http://localhost:8080 |
| PostgreSQL | ✅ Healthy | 5432 | docker-compose exec |
| Redis | ✅ Healthy | 6379 | docker-compose exec |
| Flink JobManager | ✅ Running | 8081 | http://localhost:8081 |
| Flink TaskManager | ✅ Running | - | View in Flink UI |
| Data Generator | ❌ Crashing | 8082 | - |
| Fleet Management | ❌ Crashing | 8083 | - |
| Vehicle Service | ❌ Unhealthy | 8084 | - |

---

## Immediate Next Steps

### For UI Viewing (Ready Now!)

1. ✅ **Open Kafka UI**: http://localhost:8080
   - Click **Brokers** in sidebar
   - See broker health and config
   
2. ✅ **Open Flink Dashboard**: http://localhost:8081  
   - Click **Task Managers** tab
   - See 2 TaskManagers with 4 slots each

3. ✅ **Create test data in Kafka**:
   ```bash
   # Create topic
   docker-compose exec kafka kafka-topics --create \
     --bootstrap-server localhost:9092 \
     --topic vehicle-telemetry \
     --partitions 3 \
     --replication-factor 1
   
   # Send message
   docker-compose exec kafka bash -c \
     'echo "{\"vehicleId\":\"TEST-001\",\"speed\":45}" | \
     kafka-console-producer \
     --bootstrap-server localhost:9092 \
     --topic vehicle-telemetry'
   
   # View in Kafka UI
   # Refresh browser → Topics → vehicle-telemetry → Messages
   ```

### For Application Development

Run apps locally instead of Docker:

```bash
# Use Gradle bootRun (faster for development)
./gradlew :ahs-data-generator:bootRun

# This connects to Docker Kafka/Postgres/Redis
# But runs the app on your host machine
```

---

## Screenshots of What You'll See

### Kafka UI (http://localhost:8080)

**Brokers Tab**:
```
Broker ID: 1
Status: Online
Host: kafka:29092
Topics: 0 (initially)
```

**Topics Tab** (after creating test topic):
```
vehicle-telemetry
├── Partitions: 3
├── Replicas: 1
└── Messages: 1
```

### Flink Dashboard (http://localhost:8081)

**Overview**:
```
TaskManagers: 2
Available Task Slots: 8
Running Jobs: 0
```

**Task Managers Tab**:
```
TaskManager 1: 4 slots
TaskManager 2: 4 slots
Memory: 1728 MB each
```

---

## Why This Happened

1. **UIs not started**: docker-compose.yml has them defined, but they weren't started
2. **Applications failing**: Logback library version incompatibility
3. **Grafana not created**: Not started in docker-compose

---

## Simple Commands to Remember

```bash
# Check what's running
docker-compose ps

# Start UI services
docker-compose up -d kafka-ui flink-jobmanager flink-taskmanager

# View logs
docker-compose logs -f kafka-ui

# Access Kafka UI
open http://localhost:8080

# Access Flink Dashboard  
open http://localhost:8081
```

---

## Summary

✅ **Kafka UI**: http://localhost:8080 - WORKING NOW!  
✅ **Flink Dashboard**: http://localhost:8081 - WORKING NOW!  
✅ **Infrastructure**: Kafka, PostgreSQL, Redis all healthy  
❌ **Spring Boot Apps**: Need dependency fix (run locally for now)  
❌ **Grafana**: Not started (optional monitoring)  

**You can now view the UIs and interact with the infrastructure!** 🎉

The data generator and other apps can be run locally with `./gradlew bootRun` 
which is actually faster for development anyway!
