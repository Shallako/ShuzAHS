#!/bin/bash

# Quick start script for AHS Data Generator

echo "=== Komatsu AHS Data Generator ==="
echo ""
echo "Building..."
./gradlew :ahs-data-generator:build -x test

echo ""
echo "Starting generator with default settings (27 vehicles, 5 second interval)..."
echo "Press Ctrl+C to stop"
echo ""

java -jar ahs-data-generator/build/libs/ahs-data-generator.jar \
  --bootstrap-servers localhost:9092 \
  --topic vehicle-telemetry \
  --vehicles 27 \
  --interval 5000 \
  --lidar-interval 15000

# Note: Continuous headless LIDAR scans are enabled by default (every 15s) to ensure
# JMonkeyEngine is exercised during normal runs. Disable with: --lidar-disable
