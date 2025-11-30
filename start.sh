#!/bin/bash

# Komatsu AHS Platform Startup Script
# Starts all 13 containers (12 services, flink-taskmanager has 2 replicas)

set -e

echo "========================================="
echo " Komatsu AHS Streaming Platform"
echo "========================================="
echo ""

# Check Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker Desktop."
    exit 1
fi

echo "✅ Docker is running"
echo ""

# Build the project
echo "📦 Building project..."
./gradlew build -x test --no-daemon
echo "✅ Build complete"
echo ""

# Start Docker Compose services (all 13 containers)
echo "🚀 Starting all 13 containers..."
docker-compose up -d

echo ""
echo "⏳ Waiting for services to be ready..."
sleep 15

# Check service health - All 13 containers
echo ""
echo "🔍 Container Status (13 Total):"
echo "================================"
echo ""

# Infrastructure Services
echo "📦 Infrastructure:"

# 1. Zookeeper
if docker ps --format '{{.Names}}' | grep -q "ahs-zookeeper"; then
    echo "  ✅ [1/13] Zookeeper: Running on localhost:2181"
else
    echo "  ❌ [1/13] Zookeeper: Not running"
fi

# 2. Kafka
if docker ps --format '{{.Names}}' | grep -q "ahs-kafka$"; then
    echo "  ✅ [2/13] Kafka: Running on localhost:9092"
else
    echo "  ❌ [2/13] Kafka: Not running"
fi

# 3. PostgreSQL
if docker ps --format '{{.Names}}' | grep -q "ahs-postgres"; then
    echo "  ✅ [3/13] PostgreSQL: Running on localhost:5432"
else
    echo "  ❌ [3/13] PostgreSQL: Not running"
fi

# 4. Redis
if docker ps --format '{{.Names}}' | grep -q "ahs-redis"; then
    echo "  ✅ [4/13] Redis: Running on localhost:6379"
else
    echo "  ❌ [4/13] Redis: Not running"
fi

echo ""
echo "🖥️  Web UIs:"

# 5. Kafka UI
if docker ps --format '{{.Names}}' | grep -q "ahs-kafka-ui"; then
    echo "  ✅ [5/13] Kafka UI: http://localhost:8080"
else
    echo "  ❌ [5/13] Kafka UI: Not running"
fi

# 6. Flink JobManager (UI)
if docker ps --format '{{.Names}}' | grep -q "ahs-flink-ui"; then
    echo "  ✅ [6/13] Flink UI: http://localhost:8081"
else
    echo "  ❌ [6/13] Flink UI: Not running"
fi

# 7-8. Flink TaskManagers (2 replicas)
TASKMANAGER_COUNT=$(docker ps --format '{{.Names}}' | grep -c "flink-taskmanager" || echo "0")
if [ "$TASKMANAGER_COUNT" -eq 2 ]; then
    echo "  ✅ [7-8/13] Flink TaskManagers: 2 workers running"
else
    echo "  ❌ [7-8/13] Flink TaskManagers: $TASKMANAGER_COUNT/2 running"
fi

# 9. Prometheus UI
if docker ps --format '{{.Names}}' | grep -q "ahs-prometheus-ui"; then
    echo "  ✅ [9/13] Prometheus UI: http://localhost:9090"
else
    echo "  ❌ [9/13] Prometheus UI: Not running"
fi

# 10. Grafana UI
if docker ps --format '{{.Names}}' | grep -q "ahs-grafana-ui"; then
    echo "  ✅ [10/13] Grafana UI: http://localhost:3000"
else
    echo "  ❌ [10/13] Grafana UI: Not running"
fi

echo ""
echo "⚙️  Application Services:"

# 11. Data Generator
if docker ps --format '{{.Names}}' | grep -q "ahs-data-generator"; then
    echo "  ✅ [11/13] Data Generator: Running on localhost:8082"
else
    echo "  ❌ [11/13] Data Generator: Not running"
fi

# 12. Fleet Management
if docker ps --format '{{.Names}}' | grep -q "ahs-fleet-management"; then
    echo "  ✅ [12/13] Fleet Management API: http://localhost:8083"
else
    echo "  ❌ [12/13] Fleet Management: Not running"
fi

# 13. Vehicle Service
if docker ps --format '{{.Names}}' | grep -q "ahs-vehicle-service"; then
    echo "  ✅ [13/13] Vehicle Service API: http://localhost:8084"
else
    echo "  ❌ [13/13] Vehicle Service: Not running"
fi


# Count running containers
RUNNING=$(docker ps --format '{{.Names}}' | grep -E "ahs-|flink-taskmanager" | wc -l | tr -d ' ')

echo ""
echo "========================================="
if [ "$RUNNING" -eq 13 ]; then
    echo " ✅ All 13 Containers Running!"
else
    echo " ⚠️  $RUNNING/13 Containers Running"
fi
echo "========================================="
echo ""
echo "🌐 Access Points:"
echo "  • Kafka UI:           http://localhost:8080"
echo "  • Flink Dashboard:    http://localhost:8081"
echo "  • Data Generator:     http://localhost:8082"
echo "  • Fleet Management:   http://localhost:8083"
echo "  • Vehicle Service:    http://localhost:8084"
echo "  • Prometheus:         http://localhost:9090"
echo "  • Grafana:            http://localhost:3000 (admin/admin)"
echo ""
echo "📋 Commands:"
echo "  View logs:      docker-compose logs -f"
echo "  View status:    docker-compose ps"
echo "  Stop services:  docker-compose down"
echo ""
