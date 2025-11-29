package com.komatsu.ahs.fleet.controller;

import com.komatsu.ahs.domain.model.Vehicle;
import com.komatsu.ahs.domain.model.VehicleStatus;
import com.komatsu.ahs.domain.model.VehicleTelemetry;
import com.komatsu.ahs.fleet.service.FleetManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Fleet Management REST Controller
 * 
 * Provides REST API endpoints for fleet management operations
 */
@RestController
@RequestMapping("/api/v1/fleet")
@RequiredArgsConstructor
@Slf4j
public class FleetController {

    private final FleetManagementService fleetService;

    /**
     * Get all active vehicles
     */
    @GetMapping("/vehicles")
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        log.info("Fetching all active vehicles");
        return ResponseEntity.ok(fleetService.getAllActiveVehicles());
    }
    
    /**
     * Get vehicle by ID
     */
    @GetMapping("/vehicles/{vehicleId}")
    public ResponseEntity<Vehicle> getVehicle(@PathVariable String vehicleId) {
        log.info("Fetching vehicle: {}", vehicleId);
        return fleetService.getVehicle(vehicleId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get vehicles by status
     */
    @GetMapping("/vehicles/status/{status}")
    public ResponseEntity<List<Vehicle>> getVehiclesByStatus(@PathVariable VehicleStatus status) {
        log.info("Fetching vehicles with status: {}", status);
        return ResponseEntity.ok(fleetService.getVehiclesByStatus(status));
    }

    /**
     * Get vehicle telemetry
     */
    @GetMapping("/vehicles/{vehicleId}/telemetry")
    public ResponseEntity<VehicleTelemetry> getVehicleTelemetry(@PathVariable String vehicleId) {
        log.info("Fetching telemetry for vehicle: {}", vehicleId);
        return fleetService.getVehicleTelemetry(vehicleId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get fleet statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<FleetManagementService.FleetStatistics> getFleetStatistics() {
        log.info("Fetching fleet statistics");
        return ResponseEntity.ok(fleetService.getFleetStatistics());
    }
    
    /**
     * Register new vehicle
     */
    @PostMapping("/vehicles")
    public ResponseEntity<Void> registerVehicle(@RequestBody Vehicle vehicle) {
        log.info("Registering vehicle: {}", vehicle.getVehicleId());
        fleetService.registerVehicle(vehicle);
        return ResponseEntity.ok().build();
    }

    /**
     * Update vehicle status
     */
    @PutMapping("/vehicles/{vehicleId}/status")
    public ResponseEntity<Void> updateVehicleStatus(
            @PathVariable String vehicleId,
            @RequestParam VehicleStatus status) {
        log.info("Updating vehicle {} status to {}", vehicleId, status);
        fleetService.updateVehicleStatus(vehicleId, status);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Emergency stop all vehicles
     */
    @PostMapping("/emergency-stop")
    public ResponseEntity<Void> emergencyStopAll() {
        log.warn("Emergency stop requested for all vehicles");
        fleetService.emergencyStopAll();
        return ResponseEntity.ok().build();
    }
}
