package com.komatsu.ahs.fleet.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.komatsu.ahs.domain.model.VehicleTelemetry;
import com.komatsu.ahs.domain.model.GpsCoordinate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Telemetry Data Simulator
 * 
 * Generates realistic telemetry data for autonomous vehicles
 * for testing and demonstration purposes
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TelemetrySimulator {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.vehicle-telemetry:ahs.telemetry.raw}")
    private String telemetryTopic;
    
    @Value("${simulator.enabled:false}")
    private boolean simulatorEnabled;
    
    @Value("${simulator.vehicle-count:5}")
    private int vehicleCount;
    
    private final List<String> vehicleIds = new ArrayList<>();
    private final Random random = new Random();
    
    @PostConstruct
    public void initialize() {
        if (simulatorEnabled) {
            for (int i = 1; i <= vehicleCount; i++) {
                vehicleIds.add("KOMATSU-930E-" + String.format("%03d", i));
            }
            log.info("Telemetry simulator initialized for {} vehicles", vehicleCount);
        }
    }
    
    /**
     * Generate and publish telemetry data every 5 seconds
     */
    @Scheduled(fixedRate = 5000)
    public void generateTelemetry() {
        if (!simulatorEnabled) {
            return;
        }
        
        for (String vehicleId : vehicleIds) {
            try {
                VehicleTelemetry telemetry = generateRealisticTelemetry(vehicleId);
                String message = objectMapper.writeValueAsString(telemetry);
                kafkaTemplate.send(telemetryTopic, vehicleId, message);
                
                log.debug("Published telemetry for vehicle: {}", vehicleId);
            } catch (Exception e) {
                log.error("Failed to publish telemetry for vehicle: {}", vehicleId, e);
            }
        }
    }
    
    /**
     * Generate realistic telemetry data with variations
     */
    private VehicleTelemetry generateRealisticTelemetry(String vehicleId) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        
        VehicleTelemetry telemetry = new VehicleTelemetry();
        telemetry.setVehicleId(vehicleId);
        telemetry.setTimestamp(Instant.now());
        
        // Simulate various operational states
        double baseSpeed = rand.nextDouble(0, 60); // 0-60 kph
        telemetry.setSpeedKph(baseSpeed);
        telemetry.setHeadingDegrees(rand.nextDouble(0, 360));
        
        // GPS Coordinates (simulated mine area in Arizona)
        GpsCoordinate location = new GpsCoordinate();
        location.setLatitude(32.2217 + rand.nextDouble(-0.01, 0.01));
        location.setLongitude(-110.9265 + rand.nextDouble(-0.01, 0.01));
        location.setAltitude(750.0 + rand.nextDouble(-50, 50));
        telemetry.setLocation(location);
        
        // Engine metrics
        telemetry.setEngineRpm(baseSpeed > 0 ? rand.nextDouble(800, 2200) : rand.nextDouble(600, 900));
        telemetry.setEngineTemperatureCelsius(rand.nextDouble(75, 95)); // 75-95°C normal range
        
        // Fuel/Battery (simulating gradual consumption)
        telemetry.setFuelLevelPercent(rand.nextDouble(20, 100)); // Percentage
        telemetry.setBatteryLevelPercent(rand.nextDouble(80, 100));
        
        // Payload (300 ton capacity)
        telemetry.setPayloadTons(rand.nextDouble(0, 300));
        telemetry.setLoaded(telemetry.getPayloadTons() > 50);
        
        // Tire metrics (individual tire pressures)
        double basePressure = rand.nextDouble(95, 105);
        telemetry.setTirePressureFrontLeftPsi(basePressure + rand.nextDouble(-2, 2));
        telemetry.setTirePressureFrontRightPsi(basePressure + rand.nextDouble(-2, 2));
        telemetry.setTirePressureRearLeftPsi(basePressure + rand.nextDouble(-2, 2));
        telemetry.setTirePressureRearRightPsi(basePressure + rand.nextDouble(-2, 2));
        telemetry.setTireTemperatureAvgCelsius(rand.nextDouble(30, 50));
        
        // Brake metrics
        telemetry.setBrakePressurePsi(rand.nextDouble(0, 100));
        
        // Hydraulics
        telemetry.setHydraulicPressurePsi(rand.nextDouble(2500, 3500));
        telemetry.setHydraulicFluidTempCelsius(rand.nextDouble(40, 80));
        
        // Diagnostics
        telemetry.setDiagnosticCodeCount(0);
        telemetry.setWarningLight(false);
        telemetry.setCheckEngineLight(false);
        
        // Operational metrics
        telemetry.setTotalDistanceMeters(rand.nextLong(1000000, 10000000));
        telemetry.setOperatingHours(rand.nextLong(1000, 50000));
        telemetry.setTripCount(rand.nextInt(100, 5000));
        
        // Occasionally simulate anomalies (5% chance)
        if (rand.nextDouble() < 0.05) {
            simulateAnomaly(telemetry, rand);
        }
        
        return telemetry;
    }

    /**
     * Simulate various anomalies for testing alert detection
     */
    private void simulateAnomaly(VehicleTelemetry telemetry, ThreadLocalRandom rand) {
        int anomalyType = rand.nextInt(4);
        
        switch (anomalyType) {
            case 0: // Low fuel
                telemetry.setFuelLevelPercent(rand.nextDouble(3, 10));
                log.debug("Simulating low fuel for vehicle: {}", telemetry.getVehicleId());
                break;
            case 1: // Overheating
                telemetry.setEngineTemperatureCelsius(rand.nextDouble(96, 105));
                telemetry.setWarningLight(true);
                log.debug("Simulating overheating for vehicle: {}", telemetry.getVehicleId());
                break;
            case 2: // Low tire pressure
                telemetry.setTirePressureFrontLeftPsi(rand.nextDouble(70, 85));
                telemetry.setWarningLight(true);
                log.debug("Simulating low tire pressure for vehicle: {}", telemetry.getVehicleId());
                break;
            case 3: // High speed (for testing rapid deceleration pattern)
                telemetry.setSpeedKph(rand.nextDouble(55, 70));
                log.debug("Simulating high speed for vehicle: {}", telemetry.getVehicleId());
                break;
        }
    }
}
