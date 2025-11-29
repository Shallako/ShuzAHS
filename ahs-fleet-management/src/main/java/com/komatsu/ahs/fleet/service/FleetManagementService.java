package com.komatsu.ahs.fleet.service;

import com.komatsu.ahs.domain.events.FleetMetricsEvent;
import com.komatsu.ahs.domain.events.VehicleAlertEvent;
import com.komatsu.ahs.domain.model.Vehicle;
import com.komatsu.ahs.domain.model.VehicleStatus;
import com.komatsu.ahs.domain.model.VehicleTelemetry;
import com.komatsu.ahs.domain.model.Location;
import com.komatsu.ahs.fleet.kafka.producer.AlertProducer;
import com.komatsu.ahs.fleet.kafka.producer.MetricsProducer;
import com.komatsu.ahs.fleet.metrics.FleetMetricsExporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Fleet Management Service
 * 
 * Core service for managing the autonomous vehicle fleet.
 * Maintains real-time state of all vehicles and provides
 * fleet-wide operations and queries.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FleetManagementService {

    private final AlertProducer alertProducer;
    private final MetricsProducer metricsProducer;
    private final FleetMetricsExporter metricsExporter;
    
    private final Map<String, Vehicle> activeFleet = new ConcurrentHashMap<>();
    private final Map<String, VehicleStatus> vehicleStatuses = new ConcurrentHashMap<>();
    private final Map<String, VehicleTelemetry> vehicleTelemetry = new ConcurrentHashMap<>();
    
    // Alert thresholds
    private static final double LOW_FUEL_THRESHOLD = 10.0; // percentage
    private static final double ENGINE_OVERHEAT_THRESHOLD = 95.0; // degrees Celsius
    private static final double LOW_TIRE_PRESSURE_THRESHOLD = 85.0; // PSI
    private static final double EXCESSIVE_SPEED_THRESHOLD = 65.0; // km/h
    private static final double BRAKE_PRESSURE_LOW_THRESHOLD = 80.0; // PSI
    private static final double HYDRAULIC_PRESSURE_LOW_THRESHOLD = 2000.0; // PSI
    
    @PostConstruct
    public void initialize() {
        log.info("Initializing Fleet Management Service");
        // Initialize with some mock vehicles for demo
        initializeMockFleet();
    }
    
    /**
     * Register a vehicle in the fleet
     */
    public void registerVehicle(Vehicle vehicle) {
        activeFleet.put(vehicle.getVehicleId(), vehicle);
        vehicleStatuses.put(vehicle.getVehicleId(), VehicleStatus.IDLE);
        log.info("Registered vehicle: {}", vehicle.getVehicleId());
    }
    
    /**
     * Update vehicle status
     */
    public void updateVehicleStatus(String vehicleId, VehicleStatus status) {
        if (!activeFleet.containsKey(vehicleId)) {
            log.warn("Attempt to update unknown vehicle: {}", vehicleId);
            return;
        }
        vehicleStatuses.put(vehicleId, status);
        log.debug("Updated vehicle {} status to {}", vehicleId, status);
    }
    
    /**
     * Update vehicle telemetry and perform anomaly detection
     */
    public void updateVehicleTelemetry(String vehicleId, VehicleTelemetry telemetry) {
        if (!activeFleet.containsKey(vehicleId)) {
            log.warn("Attempt to update telemetry for unknown vehicle: {}", vehicleId);
            return;
        }
        vehicleTelemetry.put(vehicleId, telemetry);
        log.debug("Updated telemetry for vehicle {}", vehicleId);
        
        // Perform anomaly detection
        detectAnomalies(vehicleId, telemetry);
    }
    
    /**
     * Detect anomalies in vehicle telemetry and publish alerts
     */
    private void detectAnomalies(String vehicleId, VehicleTelemetry telemetry) {
        // Check fuel level
        if (telemetry.getFuelLevelPercent() < LOW_FUEL_THRESHOLD) {
            publishAlert(vehicleId, VehicleAlertEvent.AlertType.LOW_FUEL,
                VehicleAlertEvent.Severity.WARNING,
                String.format("Low fuel detected: %.1f%%", telemetry.getFuelLevelPercent()),
                Map.of("fuelLevel", telemetry.getFuelLevelPercent()));
        }
        
        // Check engine temperature
        if (telemetry.getEngineTemperatureCelsius() > ENGINE_OVERHEAT_THRESHOLD) {
            publishAlert(vehicleId, VehicleAlertEvent.AlertType.ENGINE_OVERHEATING,
                VehicleAlertEvent.Severity.CRITICAL,
                String.format("Engine overheating: %.1f°C", telemetry.getEngineTemperatureCelsius()),
                Map.of("temperature", telemetry.getEngineTemperatureCelsius()));
        }
        
        // Check tire pressure (average of all four tires)
        double avgTirePressure = (telemetry.getTirePressureFrontLeftPsi() + 
                                  telemetry.getTirePressureFrontRightPsi() +
                                  telemetry.getTirePressureRearLeftPsi() + 
                                  telemetry.getTirePressureRearRightPsi()) / 4.0;
        if (avgTirePressure < LOW_TIRE_PRESSURE_THRESHOLD) {
            publishAlert(vehicleId, VehicleAlertEvent.AlertType.LOW_TIRE_PRESSURE,
                VehicleAlertEvent.Severity.WARNING,
                String.format("Low tire pressure: %.1f PSI (avg)", avgTirePressure),
                Map.of("avgTirePressure", avgTirePressure,
                       "frontLeft", telemetry.getTirePressureFrontLeftPsi(),
                       "frontRight", telemetry.getTirePressureFrontRightPsi(),
                       "rearLeft", telemetry.getTirePressureRearLeftPsi(),
                       "rearRight", telemetry.getTirePressureRearRightPsi()));
        }
        
        // Check speed
        if (telemetry.getSpeedKph() > EXCESSIVE_SPEED_THRESHOLD) {
            publishAlert(vehicleId, VehicleAlertEvent.AlertType.EXCESSIVE_SPEED,
                VehicleAlertEvent.Severity.WARNING,
                String.format("Excessive speed detected: %.1f km/h", telemetry.getSpeedKph()),
                Map.of("speed", telemetry.getSpeedKph()));
        }
        
        // Check brake pressure
        if (telemetry.getBrakePressurePsi() < BRAKE_PRESSURE_LOW_THRESHOLD) {
            publishAlert(vehicleId, VehicleAlertEvent.AlertType.BRAKE_PRESSURE_LOW,
                VehicleAlertEvent.Severity.ERROR,
                String.format("Low brake pressure: %.1f PSI", telemetry.getBrakePressurePsi()),
                Map.of("brakePressure", telemetry.getBrakePressurePsi()));
        }
        
        // Check hydraulic pressure
        if (telemetry.getHydraulicPressurePsi() < HYDRAULIC_PRESSURE_LOW_THRESHOLD) {
            publishAlert(vehicleId, VehicleAlertEvent.AlertType.HYDRAULIC_PRESSURE_LOW,
                VehicleAlertEvent.Severity.WARNING,
                String.format("Low hydraulic pressure: %.1f PSI", telemetry.getHydraulicPressurePsi()),
                Map.of("hydraulicPressure", telemetry.getHydraulicPressurePsi()));
        }
    }
    
    /**
     * Publish alert with metadata
     */
    private void publishAlert(String vehicleId, VehicleAlertEvent.AlertType alertType,
                             VehicleAlertEvent.Severity severity, String message,
                             Map<String, Object> metadata) {
        VehicleAlertEvent alert = new VehicleAlertEvent(vehicleId, alertType, severity, message);
        metadata.forEach(alert::addMetadata);
        alertProducer.publishAlert(alert);
        
        // Track in Prometheus
        switch (severity) {
            case CRITICAL -> metricsExporter.incrementCriticalAlert();
            case WARNING -> metricsExporter.incrementWarningAlert();
            case ERROR -> metricsExporter.incrementErrorAlert();
        }
    }
    
    /**
     * Get vehicle telemetry
     */
    public Optional<VehicleTelemetry> getVehicleTelemetry(String vehicleId) {
        return Optional.ofNullable(vehicleTelemetry.get(vehicleId));
    }
    
    /**
     * Get all active vehicles
     */
    @Cacheable("activeVehicles")
    public List<Vehicle> getAllActiveVehicles() {
        return new ArrayList<>(activeFleet.values());
    }
    
    /**
     * Get vehicles by status
     */
    public List<Vehicle> getVehiclesByStatus(VehicleStatus status) {
        return vehicleStatuses.entrySet().stream()
            .filter(entry -> entry.getValue() == status)
            .map(entry -> activeFleet.get(entry.getKey()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * Get vehicle by ID
     */
    public Optional<Vehicle> getVehicle(String vehicleId) {
        return Optional.ofNullable(activeFleet.get(vehicleId));
    }
    
    /**
     * Get fleet statistics
     */
    public FleetStatistics getFleetStatistics() {
        int total = activeFleet.size();
        Map<VehicleStatus, Long> statusCounts = vehicleStatuses.values().stream()
            .collect(Collectors.groupingBy(status -> status, Collectors.counting()));
            
        return FleetStatistics.builder()
            .totalVehicles(total)
            .activeVehicles(statusCounts.getOrDefault(VehicleStatus.HAULING, 0L).intValue() +
                           statusCounts.getOrDefault(VehicleStatus.LOADING, 0L).intValue() +
                           statusCounts.getOrDefault(VehicleStatus.DUMPING, 0L).intValue())
            .idleVehicles(statusCounts.getOrDefault(VehicleStatus.IDLE, 0L).intValue())
            .statusBreakdown(statusCounts)
            .build();
    }
    
    /**
     * Assign route to vehicle
     */
    public boolean assignRoute(String vehicleId, String routeId, 
                               Location loadPoint, Location dumpPoint) {
        Vehicle vehicle = activeFleet.get(vehicleId);
        if (vehicle == null) {
            log.error("Cannot assign route to unknown vehicle: {}", vehicleId);
            return false;
        }
        
        if (vehicleStatuses.get(vehicleId) != VehicleStatus.IDLE) {
            log.warn("Vehicle {} is not idle, current status: {}", 
                    vehicleId, vehicleStatuses.get(vehicleId));
            return false;
        }
        
        // Update vehicle with route assignment
        updateVehicleStatus(vehicleId, VehicleStatus.ROUTING);
        log.info("Assigned route {} to vehicle {}", routeId, vehicleId);
        return true;
    }
    
    /**
     * Emergency stop all vehicles
     */
    public void emergencyStopAll() {
        log.warn("EMERGENCY STOP INITIATED FOR ALL VEHICLES");
        activeFleet.keySet().forEach(vehicleId -> 
            updateVehicleStatus(vehicleId, VehicleStatus.EMERGENCY_STOP));
    }
    
    /**
     * Initialize mock fleet for demonstration
     */
    private void initializeMockFleet() {
        // Create sample Komatsu 930E trucks
        for (int i = 1; i <= 10; i++) {
            Vehicle vehicle = Vehicle.builder()
                .vehicleId("KOMATSU-930E-" + String.format("%03d", i))
                .model("930E")
                .manufacturer("Komatsu")
                .capacity(300.0) // 300 ton capacity
                .build();
            registerVehicle(vehicle);
        }
        
        // Create sample Komatsu 980E trucks
        for (int i = 1; i <= 5; i++) {
            Vehicle vehicle = Vehicle.builder()
                .vehicleId("KOMATSU-980E-" + String.format("%03d", i))
                .model("980E")
                .manufacturer("Komatsu")
                .capacity(400.0) // 400 ton capacity
                .build();
            registerVehicle(vehicle);
        }
        
        log.info("Initialized mock fleet with {} vehicles", activeFleet.size());
    }
    
    /**
     * Aggregate and publish fleet metrics every 30 seconds
     */
    @Scheduled(fixedRate = 30000)
    public void publishFleetMetrics() {
        FleetMetricsEvent metrics = aggregateFleetMetrics();
        metricsProducer.publishMetrics(metrics);
    }
    
    /**
     * Aggregate current fleet metrics
     */
    private FleetMetricsEvent aggregateFleetMetrics() {
        FleetMetricsEvent metrics = new FleetMetricsEvent();
        
        // Count vehicles by status
        Map<VehicleStatus, Long> statusCounts = vehicleStatuses.values().stream()
            .collect(Collectors.groupingBy(status -> status, Collectors.counting()));
        
        int total = activeFleet.size();
        int idle = statusCounts.getOrDefault(VehicleStatus.IDLE, 0L).intValue();
        int routing = statusCounts.getOrDefault(VehicleStatus.ROUTING, 0L).intValue();
        int loading = statusCounts.getOrDefault(VehicleStatus.LOADING, 0L).intValue();
        int hauling = statusCounts.getOrDefault(VehicleStatus.HAULING, 0L).intValue();
        int dumping = statusCounts.getOrDefault(VehicleStatus.DUMPING, 0L).intValue();
        int emergency = statusCounts.getOrDefault(VehicleStatus.EMERGENCY_STOP, 0L).intValue();
        int activeCount = routing + loading + hauling + dumping;
        
        metrics.setTotalVehicles(total);
        metrics.setIdleVehicles(idle);
        metrics.setRoutingVehicles(routing);
        metrics.setLoadingVehicles(loading);
        metrics.setHaulingVehicles(hauling);
        metrics.setDumpingVehicles(dumping);
        metrics.setEmergencyStopVehicles(emergency);
        metrics.setActiveVehicles(activeCount);
        
        // Update Prometheus metrics
        metricsExporter.updateVehicleCounts(total, activeCount, idle, routing, 
                                           loading, hauling, dumping, emergency);
        
        // Aggregate telemetry data
        if (!vehicleTelemetry.isEmpty()) {
            double totalSpeed = 0;
            double totalFuel = 0;
            double totalTemp = 0;
            double totalPayload = 0;
            int count = vehicleTelemetry.size();
            
            for (VehicleTelemetry telemetry : vehicleTelemetry.values()) {
                totalSpeed += telemetry.getSpeedKph();
                totalFuel += telemetry.getFuelLevelPercent();
                totalTemp += telemetry.getEngineTemperatureCelsius();
                totalPayload += telemetry.getPayloadTons();
            }
            
            metrics.setAverageSpeed(totalSpeed / count);
            metrics.setAverageFuelLevel(totalFuel / count);
            metrics.setAverageEngineTemperature(totalTemp / count);
            metrics.setAveragePayload(totalPayload / count);
        }
        
        // Add metadata
        metrics.addMetadata("fleetUtilization", 
            total == 0 ? 0.0 : (double) activeCount / total * 100);
        
        return metrics;
    }
    
    /**
     * Fleet Statistics Inner Class
     */
    @lombok.Data
    @lombok.Builder
    public static class FleetStatistics {
        private int totalVehicles;
        private int activeVehicles;
        private int idleVehicles;
        private Map<VehicleStatus, Long> statusBreakdown;
    }
}
