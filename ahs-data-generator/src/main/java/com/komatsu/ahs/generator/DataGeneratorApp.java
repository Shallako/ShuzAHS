package com.komatsu.ahs.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Data Generator Application
 * 
 * CLI application for generating realistic telemetry data for testing
 * the Komatsu AHS streaming pipeline.
 * 
 * Usage:
 *   java -jar ahs-data-generator.jar --vehicles 15 --interval 5000
 */
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
            description = "Number of vehicles to simulate",
            defaultValue = "15")
    private int vehicleCount;
    
    @Option(names = {"-i", "--interval"}, 
            description = "Telemetry publish interval in milliseconds",
            defaultValue = "5000")
    private long publishInterval;
    
    @Option(names = {"-d", "--duration"}, 
            description = "Duration to run in minutes (0 = infinite)",
            defaultValue = "0")
    private int durationMinutes;
    
    private List<VehicleSimulator> vehicles;
    private KafkaTelemetryProducer producer;
    private ScheduledExecutorService scheduler;
    
    @Override
    public Integer call() throws Exception {
        LOG.info("Starting AHS Data Generator");
        LOG.info("Configuration: vehicles={}, interval={}ms, topic={}, bootstrap={}", 
                vehicleCount, publishInterval, topic, bootstrapServers);
        
        // Initialize
        initializeVehicles();
        producer = new KafkaTelemetryProducer(bootstrapServers);
        scheduler = Executors.newScheduledThreadPool(2);
        
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
