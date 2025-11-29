#!/bin/bash

# Komatsu AHS Platform Startup Script

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

# Start Docker Compose services
echo "🚀 Starting services..."
docker-compose up -d

echo ""
echo "⏳ Waiting for services to be ready..."
sleep 10

# Check service health
echo ""
echo "🔍 Service Status:"
echo "===================="

# Check Kafka
if docker-compose ps | grep kafka | grep Up > /dev/null; then
    echo "✅ Kafka: Running on localhost:9092"
else
    echo "❌ Kafka: Not running"
fi

# Check Zookeeper
if docker-compose ps | grep zookeeper | grep Up > /dev/null; then
    echo "✅ Zookeeper: Running on localhost:2181"
else
    echo "❌ Zookeeper: Not running"
fi

# Check Fleet Management
if docker-compose ps | grep fleet-management | grep Up > /dev/null; then
    echo "✅ Fleet Management: Running on http://localhost:8080"
else
    echo "❌ Fleet Management: Not running"
fi

# Check Flink
if docker-compose ps | grep flink-jobmanager | grep Up > /dev/null; then
    echo "✅ Flink Dashboard: Running on http://localhost:8081"
else
    echo "❌ Flink: Not running"
fi

echo ""
echo "========================================="
echo " Services Started Successfully!"
echo "========================================="
echo ""
echo "Access Points:"
echo "  • Fleet Management API: http://localhost:8080"
echo "  • Flink Dashboard: http://localhost:8081"
echo "  • Health Check: http://localhost:8080/actuator/health"
echo ""
echo "View Logs:"
echo "  docker-compose logs -f"
echo ""
echo "Stop Services:"
echo "  docker-compose down"
echo ""
