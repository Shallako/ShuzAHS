#!/bin/bash

# Quick start script for AHS Data Generator

echo "=== Titan AHS Data Generator ==="
echo ""
echo "Building..."
./gradlew :ahs-data-generator:build -x test

echo ""
echo "Starting generator with default settings (27 vehicles, 5 second interval)..."
echo "Press Ctrl+C to stop"
echo ""

# Read resolved Kafka port from .env if present
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "${SCRIPT_DIR}/.env" ]; then
    while IFS='=' read -r key value || [ -n "$key" ]; do
        [[ "$key" =~ ^[[:space:]]*# ]] && continue
        [[ -z "$key" ]] && continue
        key=$(echo "$key" | xargs)
        value=$(echo "$value" | xargs)
        if [ "$key" = "KAFKA_PORT" ] && [ -n "$value" ] && [ -z "$KAFKA_PORT" ]; then
            KAFKA_PORT="$value"
        fi
    done < "${SCRIPT_DIR}/.env"
fi

BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS:-localhost:${KAFKA_PORT:-9092}}"

java -jar ahs-data-generator/build/libs/ahs-data-generator.jar \
  --bootstrap-servers "$BOOTSTRAP_SERVERS" \
  --topic vehicle-telemetry \
  --vehicles 27 \
  --interval 5000 \
  --lidar-interval 15000

# Note: Continuous headless LIDAR scans are enabled by default (every 15s) to ensure
# JMonkeyEngine is exercised during normal runs. Disable with: --lidar-disable
