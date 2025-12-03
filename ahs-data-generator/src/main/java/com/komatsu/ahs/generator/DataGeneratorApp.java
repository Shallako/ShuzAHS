package com.komatsu.ahs.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.komatsu.ahs.generator.lidar.LidarConfig;
import com.komatsu.ahs.generator.lidar.LidarEnvironmentBuilder;
import com.komatsu.ahs.generator.lidar.LidarPointCloudWriter;
import com.komatsu.ahs.generator.lidar.LidarSimulator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.File;

@Command(name = "ahs-data-generator",
        mixinStandardHelpOptions = true,
        version = "1.0",
        description = "Generate telemetry data for Komatsu AHS testing")
public class DataGeneratorApp implements Callable<Integer> {

    private static final Logger LOG = LoggerFactory.getLogger(DataGeneratorApp.class);

    @Option(names = {"-b", "--bootstrap-servers"},
            description = "Kafka bootstrap servers",
            defaultValue = "localhost:9092")
    private String bootstrapServers;

    @Option(names = {"-t", "--topic"},
            description = "Kafka topic to publish to",
            defaultValue = "vehicle-telemetry")
    private String topic;

    @Option(names = {"-v", "--vehicles"},
            description = "Number of vehicles to simulate. Overrides application.yml.")
    private Integer vehicleCount;

    @Option(names = {"-i", "--interval"},
            description = "Telemetry publish interval in milliseconds. Overrides application.yml.")
    private Long publishInterval;

    @Option(names = {"-d", "--duration"},
            description = "Duration to run in minutes (0 = infinite)",
            defaultValue = "0")
    private int durationMinutes;

    private List<VehicleSimulator> vehicles;
    private KafkaTelemetryProducer producer;
    private ScheduledExecutorService scheduler;
    private GeneratorConfig config;

    // LIDAR options
    @Option(names = {"--lidar"}, description = "Run a LIDAR scan and export a point cloud (CSV)")
    private boolean lidarEnabled = false;

    @Option(names = {"--lidar-output"}, description = "Output CSV path for LIDAR point cloud")
    private File lidarOutputFile;

    @Option(names = {"--lidar-h-samples"}, description = "LIDAR horizontal samples (default: 1440)")
    private Integer lidarHorizontalSamples;

    @Option(names = {"--lidar-v-samples"}, description = "LIDAR vertical samples (default: 1)")
    private Integer lidarVerticalSamples;

    @Option(names = {"--lidar-range"}, description = "LIDAR max range meters (default: 120)")
    private Float lidarRange;

    // Continuous LIDAR options (always-on scanning during normal run)
    @Option(names = {"--lidar-interval"}, description = "Continuous LIDAR scan interval in ms (default: 15000)")
    private Long lidarIntervalMs;

    @Option(names = {"--lidar-disable"}, description = "Disable continuous LIDAR scanning during normal run")
    private boolean lidarDisable = false;

    @Option(names = {"--lidar-continuous-h-samples"}, description = "Continuous LIDAR horizontal samples (default: 720)")
    private Integer lidarContinuousHSamples;

    @Option(names = {"--lidar-continuous-v-samples"}, description = "Continuous LIDAR vertical samples (default: 1)")
    private Integer lidarContinuousVSamples;

    @Override
    public Integer call() throws Exception {
        config = new GeneratorConfig();
        // If CLI options are not provided, use values from config
        if (vehicleCount == null) {
            vehicleCount = config.getVehicles();
        }
        if (publishInterval == null) {
            publishInterval = config.getInterval();
        }

        LOG.info("Starting AHS Data Generator");
        LOG.info("Configuration: vehicles={}, interval={}ms, topic={}, bootstrap={}",
                vehicleCount, publishInterval, topic, bootstrapServers);

        // Optional LIDAR one-shot mode
        if (lidarEnabled) {
            if (lidarOutputFile == null) {
                LOG.error("--lidar requires --lidar-output to specify a CSV file path");
                return 2;
            }
            return runLidarOnce();
        }

        // Initialize
        initializeVehicles();
        producer = new KafkaTelemetryProducer(bootstrapServers);
        // Telemetry, state updates, status print, and continuous LIDAR
        scheduler = Executors.newScheduledThreadPool(4);

        // Schedule telemetry publishing
        scheduler.scheduleAtFixedRate(
            this::publishTelemetry,
            0,
            publishInterval,
            TimeUnit.MILLISECONDS
        );

        // Schedule state updates (more frequent than telemetry)
        scheduler.scheduleAtFixedRate(
            this::updateVehicleStates,
            0,
            1000,
            TimeUnit.MILLISECONDS
        );

        // Print status updates
        scheduler.scheduleAtFixedRate(
            this::printStatus,
            10,
            10,
            TimeUnit.SECONDS
        );

        // Continuous, headless LIDAR scanning to ensure JME is exercised during normal run
        if (!lidarDisable) {
            scheduleContinuousLidar();
        } else {
            LOG.info("Continuous LIDAR scanning disabled via --lidar-disable");
        }

        // Run for specified duration or until interrupted
        if (durationMinutes > 0) {
            LOG.info("Running for {} minutes...", durationMinutes);
            Thread.sleep(TimeUnit.MINUTES.toMillis(durationMinutes));
            shutdown();
        } else {
            LOG.info("Running indefinitely. Press Ctrl+C to stop...");
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            Thread.currentThread().join();
        }

        return 0;
    }

    private int runLidarOnce() {
        try {
            // Build simple default scene (ground + obstacles)
            var root = LidarEnvironmentBuilder.buildDefaultScene();

            // Configure LIDAR
            LidarConfig lc = new LidarConfig();
            if (lidarHorizontalSamples != null) lc.setHorizontalSamples(lidarHorizontalSamples);
            if (lidarVerticalSamples != null) lc.setVerticalSamples(lidarVerticalSamples);
            if (lidarRange != null) lc.setMaxRange(lidarRange);

            // Place vehicle at origin with slight height and identity rotation
            Vector3f vehiclePos = new Vector3f(0, 2.0f, 0);
            Quaternion vehicleRot = new Quaternion();

            LidarSimulator simulator = new LidarSimulator(root, lc);
            var points = simulator.scan(vehiclePos, vehicleRot);

            LidarPointCloudWriter.writeCsv(points, lidarOutputFile);
            LOG.info("LIDAR scan complete: {} points written to {}", points.size(), lidarOutputFile.getAbsolutePath());
            return 0;
        } catch (Exception e) {
            LOG.error("LIDAR scan failed", e);
            return 1;
        }
    }

    private void scheduleContinuousLidar() {
        // Defaults tuned for low overhead but regular JME usage
        long interval = (lidarIntervalMs != null && lidarIntervalMs > 0) ? lidarIntervalMs : 15000L; // 15s

        // Build simple default scene once (ground + obstacles)
        var root = LidarEnvironmentBuilder.buildDefaultScene();

        // Configure continuous LIDAR
        LidarConfig lc = new LidarConfig();
        // Use lighter defaults for continuous scans
        if (lidarContinuousHSamples != null) {
            lc.setHorizontalSamples(lidarContinuousHSamples);
        } else {
            lc.setHorizontalSamples(720); // 0.5° resolution by default
        }
        if (lidarContinuousVSamples != null) {
            lc.setVerticalSamples(lidarContinuousVSamples);
        } else {
            lc.setVerticalSamples(1);
        }
        if (lidarRange != null) {
            lc.setMaxRange(lidarRange);
        }

        final LidarSimulator simulator = new LidarSimulator(root, lc);

        // Static vehicle pose for periodic scans
        final Vector3f vehiclePos = new Vector3f(0, 2.0f, 0);
        final Quaternion vehicleRot = new Quaternion();

        LOG.info("Scheduling continuous LIDAR scans: interval={}ms, hSamples={}, vSamples={}, range={}m",
                interval, lc.getHorizontalSamples(), lc.getVerticalSamples(), lc.getMaxRange());

        scheduler.scheduleAtFixedRate(() -> {
            try {
                var points = simulator.scan(vehiclePos, vehicleRot);
                LOG.debug("LIDAR scan produced {} points", points.size());
            } catch (Exception e) {
                LOG.warn("Continuous LIDAR scan failed: {}", e.getMessage());
            }
        }, 0, interval, TimeUnit.MILLISECONDS);
    }

    private void initializeVehicles() {
        vehicles = new ArrayList<>();

        // Create mix of 930E and 980E trucks
        int num930E = (int) (vehicleCount * 0.67);  // 67% are 930E
        int num980E = vehicleCount - num930E;        // 33% are 980E

        for (int i = 1; i <= num930E; i++) {
            String vehicleId = "KOMATSU-930E-" + String.format("%03d", i);
            vehicles.add(new VehicleSimulator(vehicleId));
        }

        for (int i = 1; i <= num980E; i++) {
            String vehicleId = "KOMATSU-980E-" + String.format("%03d", i);
            vehicles.add(new VehicleSimulator(vehicleId));
        }

        LOG.info("Initialized {} vehicles ({} x 930E, {} x 980E)",
                vehicleCount, num930E, num980E);
    }

    private void updateVehicleStates() {
        for (VehicleSimulator vehicle : vehicles) {
            vehicle.updateState();
        }
    }

    private void publishTelemetry() {
        try {
            for (VehicleSimulator vehicle : vehicles) {
                producer.publishTelemetry(
                    topic,
                    vehicle.getVehicleId(),
                    vehicle.getCurrentStatus()
                );
            }
            producer.flush();
        } catch (Exception e) {
            LOG.error("Error publishing telemetry batch", e);
        }
    }

    private void printStatus() {
        LOG.info("=== Fleet Status ===");
        vehicles.forEach(v -> LOG.info("  {}", v.getStateInfo()));

        long totalCycles = vehicles.stream()
            .mapToInt(VehicleSimulator::getCycleCount)
            .sum();
        LOG.info("Total cycles completed: {}", totalCycles);
    }

    private void shutdown() {
        LOG.info("Shutting down...");

        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }

        if (producer != null) {
            producer.close();
        }

        LOG.info("Shutdown complete");
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new DataGeneratorApp()).execute(args);
        System.exit(exitCode);
    }
}
