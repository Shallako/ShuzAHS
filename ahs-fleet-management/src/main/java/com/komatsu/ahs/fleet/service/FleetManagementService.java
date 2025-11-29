package com.komatsu.ahs.fleet.service;

import com.komatsu.ahs.domain.model.Vehicle;
import com.komatsu.ahs.domain.model.VehicleStatus;
import com.komatsu.ahs.domain.model.VehicleTelemetry;
import com.komatsu.ahs.domain.model.Location;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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
public class FleetManagementService {

    private final Map<String, Vehicle> activeFleet = new ConcurrentHashMap<>();
    private final Map<String, VehicleStatus> vehicleStatuses = new ConcurrentHashMap<>();
    private final Map<String, VehicleTelemetry> vehicleTelemetry = new ConcurrentHashMap<>();
    
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
     * Update vehicle telemetry
     */
    public void updateVehicleTelemetry(String vehicleId, VehicleTelemetry telemetry) {
        if (!activeFleet.containsKey(vehicleId)) {
            log.warn("Attempt to update telemetry for unknown vehicle: {}", vehicleId);
            return;
        }
        vehicleTelemetry.put(vehicleId, telemetry);
        log.debug("Updated telemetry for vehicle {}", vehicleId);
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
