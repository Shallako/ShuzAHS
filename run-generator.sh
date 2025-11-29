#!/bin/bash

# Quick start script for AHS Data Generator

echo "=== Komatsu AHS Data Generator ==="
echo ""
echo "Building..."
./gradlew :ahs-data-generator:build -x test

echo ""
echo "Starting generator with default settings (15 vehicles, 5 second interval)..."
echo "Press Ctrl+C to stop"
echo ""

java -jar ahs-data-generator/build/libs/ahs-data-generator.jar \
  --bootstrap-servers localhost:9092 \
  --topic vehicle-telemetry \
  --vehicles 15 \
  --interval 5000
