# AHS Data Generator

Realistic telemetry data generator for testing the Komatsu Autonomous Haulage System (AHS) streaming pipeline.

## Features

- **Realistic Vehicle Simulation**: Simulates autonomous haul truck lifecycle (IDLE → ROUTING → LOADING → HAULING → DUMPING)
- **Configurable Fleet Size**: Simulate 1 to hundreds of vehicles
- **Mixed Fleet**: Automatically creates a realistic mix of Komatsu 930E (67%) and 980E (33%) trucks
- **Kafka Integration**: Publishes telemetry events to Kafka topics
- **CLI Interface**: Easy-to-use command-line interface with sensible defaults
- **LIDAR Point Cloud (JME Ray Casting)**: Headless LIDAR simulation using JMonkeyEngine’s collision system to ray-cast thousands of beams and export a point cloud (CSV)

## Vehicle Lifecycle

Each simulated vehicle follows a realistic haul cycle:

1. **IDLE** (10-60s) - Waiting for assignment
2. **ROUTING** (30-120s) - Traveling to load point
3. **LOADING** (60-180s) - Being loaded by shovel
4. **HAULING** (120-300s) - Traveling loaded to dump point
5. **DUMPING** (40-90s) - Dumping material
6. **ROUTING** (30-120s) - Returning to start
7. Back to **IDLE**

## Generated Telemetry Data

Each telemetry event includes:
- GPS coordinates (latitude, longitude, altitude)
- Vehicle speed (km/h)
- Current load (tons)
- Fuel level (%)
- Engine temperature (°C)
- Oil pressure (PSI)
- Tire pressures (all 4 tires)

## Usage

### Build

```bash
./gradlew :ahs-data-generator:build
```

### Run with Default Settings

Simulates 15 vehicles, publishing telemetry every 5 seconds:

```bash
./gradlew :ahs-data-generator:run
```

### Run with Custom Configuration

```bash
java -jar ahs-data-generator/build/libs/ahs-data-generator.jar \
  --bootstrap-servers localhost:9092 \
  --topic vehicle-telemetry \
  --vehicles 50 \
  --interval 3000 \
  --duration 60
```

### Command-Line Options

| Option | Short | Description | Default |
|--------|-------|-------------|---------|
| `--bootstrap-servers` | `-b` | Kafka bootstrap servers | `localhost:9092` |
| `--topic` | `-t` | Kafka topic to publish to | `vehicle-telemetry` |
| `--vehicles` | `-v` | Number of vehicles to simulate | `15` |
| `--interval` | `-i` | Publish interval (milliseconds) | `5000` |
| `--duration` | `-d` | Duration to run (minutes, 0=infinite) | `0` |
| `--help` | `-h` | Show help message | - |
| `--version` | `-V` | Show version | - |
| `--lidar` |  | Run a one-shot LIDAR scan and exit | - |
| `--lidar-output` |  | Output CSV path for LIDAR points | - |
| `--lidar-h-samples` |  | Horizontal rays count (default 1440) | `1440` |
| `--lidar-v-samples` |  | Vertical rays count (default 1 = planar) | `1` |
| `--lidar-range` |  | Max LIDAR range in meters (default 120) | `120` |
| `--lidar-interval` |  | Continuous LIDAR scan interval in ms during normal run | `15000` |
| `--lidar-disable` |  | Disable continuous LIDAR scanning during normal run | - |
| `--lidar-continuous-h-samples` |  | Horizontal rays for continuous scans | `720` |
| `--lidar-continuous-v-samples` |  | Vertical rays for continuous scans | `1` |

### LIDAR Point Cloud Generation (Headless)

The generator can produce a point cloud by simulating a LIDAR sensor with JMonkeyEngine’s collision system. It ray-casts against a simple built-in scene (in-memory, no graphics required) and writes hit points to CSV.

Example (writes a CSV with `x,y,z`):

```bash
./gradlew :ahs-data-generator:run --args="--lidar \
  --lidar-output build/lidar/scan.csv \
  --lidar-h-samples 2000 \
  --lidar-v-samples 1 \
  --lidar-range 120"
```

Notes:
- This uses JME’s headless collision (no window). Dependency: `jme3-core` only.
- Default scene includes a flat ground and a few boxes; replace with your own scene graph if needed.
- Increase `--lidar-h-samples`/`--lidar-v-samples` for denser scans; performance scales linearly with rays.
- CSV header is `x,y,z`. You can import into tools like CloudCompare, Meshlab, or Python for visualization.

### Continuous LIDAR (Always On)

When the data generator runs normally (not in one-shot `--lidar` mode), it now performs continuous headless LIDAR scans using JME’s collision system at a fixed interval to ensure JME is exercised continuously.

- Default interval: `--lidar-interval 15000` (every 15 seconds)
- Default continuous resolution: `--lidar-continuous-h-samples 720` (0.5°), `--lidar-continuous-v-samples 1`
- Disable continuous scans if needed with `--lidar-disable`.

Docker Compose is configured to include `--lidar-interval 15000` on the `data-generator` service so JME runs continuously when you start the stack.

## Example Output

```
12:34:56.789 [main] INFO  c.k.a.g.DataGeneratorApp - Starting AHS Data Generator
12:34:56.790 [main] INFO  c.k.a.g.DataGeneratorApp - Configuration: vehicles=15, interval=5000ms
12:34:56.801 [main] INFO  c.k.a.g.DataGeneratorApp - Initialized 15 vehicles (10 x 930E, 5 x 980E)
12:34:56.850 [main] INFO  c.k.a.g.KafkaTelemetryProducer - Kafka telemetry producer initialized
12:34:56.851 [main] INFO  c.k.a.g.DataGeneratorApp - Running indefinitely. Press Ctrl+C to stop...
12:35:06.855 [pool-1-thread-2] INFO  c.k.a.g.DataGeneratorApp - === Fleet Status ===
12:35:06.856 [pool-1-thread-2] INFO  c.k.a.g.DataGeneratorApp -   KOMATSU-930E-001 [ROUTING] - Cycle: 0, Remaining: 54s
12:35:06.856 [pool-1-thread-2] INFO  c.k.a.g.DataGeneratorApp -   KOMATSU-930E-002 [LOADING] - Cycle: 0, Remaining: 123s
12:35:06.856 [pool-1-thread-2] INFO  c.k.a.g.DataGeneratorApp -   KOMATSU-930E-003 [IDLE] - Cycle: 0, Remaining: 12s
12:35:06.857 [pool-1-thread-2] INFO  c.k.a.g.DataGeneratorApp - Total cycles completed: 0
```

## Integration with AHS Pipeline

The generated data is published to Kafka and can be consumed by:

1. **ahs-telemetry-processor** - Telemetry Processor (Hazelcast Jet, embedded) for real-time stream processing
2. **ahs-fleet-management** - Fleet management service for tracking vehicle state
3. **ahs-stream-analytics** - Analytics processing (optional)

## Example Scenario: Load Testing

Simulate a large-scale mining operation:

```bash
# Simulate 100 vehicles with high-frequency telemetry (every 2 seconds)
java -jar ahs-data-generator.jar -v 100 -i 2000 -d 120
```

This generates approximately **6,000 telemetry events per minute** for 2 hours.
