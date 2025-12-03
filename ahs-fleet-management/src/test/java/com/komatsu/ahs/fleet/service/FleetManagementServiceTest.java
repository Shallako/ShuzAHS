package com.komatsu.ahs.fleet.service;

import com.komatsu.ahs.domain.model.GpsCoordinate;
import com.komatsu.ahs.domain.model.Vehicle;
import com.komatsu.ahs.domain.model.VehicleStatus;
import com.komatsu.ahs.domain.model.VehicleTelemetry;
import com.komatsu.ahs.fleet.kafka.producer.AlertProducer;
import com.komatsu.ahs.fleet.kafka.producer.MetricsProducer;
import com.komatsu.ahs.fleet.metrics.FleetMetricsExporter;
import com.komatsu.ahs.fleet.service.FleetManagementService.FleetStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fleet Management Service Tests")
class FleetManagementServiceTest {

    @Mock
    private AlertProducer alertProducer;
    
    @Mock
    private MetricsProducer metricsProducer;
    
    @Mock
    private FleetMetricsExporter metricsExporter;

    private FleetManagementService service;

    @BeforeEach
    void setUp() {
        service = new FleetManagementService(alertProducer, metricsProducer, metricsExporter);
        // Manually call initialize since @PostConstruct is not invoked in plain JUnit tests
        service.initialize();
    }

    @Test
    @DisplayName("Should initialize with empty dynamic fleet")
    void testInitialization() {
        List<Vehicle> vehicles = service.getAllActiveVehicles();
        
        assertNotNull(vehicles);
        assertEquals(0, vehicles.size());
    }

    @Test
    @DisplayName("Should get vehicle by ID after registration")
    void testGetVehicleById() {
        Vehicle newVehicle = new Vehicle();
        newVehicle.setVehicleId("KOMATSU-930E-001");
        newVehicle.setModel("930E");
        newVehicle.setManufacturer("Komatsu");
        newVehicle.setCapacity(300.0);
        service.registerVehicle(newVehicle);

        Optional<Vehicle> vehicle = service.getVehicle("KOMATSU-930E-001");
        assertTrue(vehicle.isPresent());
        assertEquals("KOMATSU-930E-001", vehicle.get().getVehicleId());
        assertEquals("930E", vehicle.get().getModel());
    }

    @Test
    @DisplayName("Should return empty for non-existent vehicle")
    void testGetNonExistentVehicle() {
        Optional<Vehicle> vehicle = service.getVehicle("NON-EXISTENT-ID");
        
        assertFalse(vehicle.isPresent());
    }

    @Test
    @DisplayName("Should get all active vehicles after registration")
    void testGetAllActiveVehicles() {
        // Dynamically register a couple of vehicles
        service.updateVehicleStatus("KOMATSU-930E-001", VehicleStatus.IDLE);
        service.updateVehicleStatus("KOMATSU-980E-001", VehicleStatus.IDLE);

        List<Vehicle> vehicles = service.getAllActiveVehicles();
        assertNotNull(vehicles);
        assertEquals(2, vehicles.size());
    }

    @Test
    @DisplayName("Should get vehicles by status")
    void testGetVehiclesByStatus() {
        // Update some vehicles to specific status (auto-registers vehicles)
        service.updateVehicleStatus("KOMATSU-930E-001", VehicleStatus.ROUTING);
        service.updateVehicleStatus("KOMATSU-930E-002", VehicleStatus.ROUTING);
        
        List<Vehicle> routingVehicles = service.getVehiclesByStatus(VehicleStatus.ROUTING);
        
        assertTrue(routingVehicles.size() >= 2);
    }

    @Test
    @DisplayName("Should update vehicle status")
    void testUpdateVehicleStatus() {
        String vehicleId = "KOMATSU-930E-001";
        // Auto-registers and sets status
        service.updateVehicleStatus(vehicleId, VehicleStatus.MAINTENANCE);
        
        List<Vehicle> maintenanceVehicles = service.getVehiclesByStatus(VehicleStatus.MAINTENANCE);
        assertTrue(maintenanceVehicles.stream()
            .anyMatch(v -> v.getVehicleId().equals(vehicleId)));
    }

    @Test
    @DisplayName("Should update vehicle telemetry")
    void testUpdateVehicleTelemetry() {
        String vehicleId = "KOMATSU-930E-001";
        
        VehicleTelemetry telemetry = new VehicleTelemetry();
        telemetry.setVehicleId(vehicleId);
        telemetry.setSpeedKph(35.5);
        telemetry.setFuelLevelPercent(75.0);
        GpsCoordinate coord = new GpsCoordinate();
        coord.setLatitude(-23.4);
        coord.setLongitude(-70.35);
        coord.setAltitude(2900.0);
        telemetry.setLocation(coord);
        telemetry.setTimestamp(Instant.now());
        
        service.updateVehicleTelemetry(vehicleId, telemetry);
        
        Optional<VehicleTelemetry> retrieved = service.getVehicleTelemetry(vehicleId);
        assertTrue(retrieved.isPresent());
        assertEquals(35.5, retrieved.get().getSpeedKph());
        assertEquals(75.0, retrieved.get().getFuelLevelPercent());
    }

    @Test
    @DisplayName("Should get fleet statistics for dynamic fleet")
    void testGetFleetStatistics() {
        // Set up some vehicles in different states (auto-registers vehicles)
        service.updateVehicleStatus("KOMATSU-930E-001", VehicleStatus.HAULING);
        service.updateVehicleStatus("KOMATSU-930E-002", VehicleStatus.LOADING);
        service.updateVehicleStatus("KOMATSU-930E-003", VehicleStatus.IDLE);
        service.updateVehicleStatus("KOMATSU-930E-004", VehicleStatus.MAINTENANCE);
        
        FleetStatistics stats = service.getFleetStatistics();
        
        assertNotNull(stats);
        assertTrue(stats.getTotalVehicles() >= 4);
        assertTrue(stats.getActiveVehicles() >= 0);
        assertTrue(stats.getIdleVehicles() >= 0);
    }

    @Test
    @DisplayName("Should handle status breakdown in fleet statistics for dynamic fleet")
    void testFleetStatisticsBreakdown() {
        // Ensure we have some vehicles and statuses
        service.updateVehicleStatus("KOMATSU-930E-001", VehicleStatus.HAULING);
        service.updateVehicleStatus("KOMATSU-930E-002", VehicleStatus.IDLE);
        FleetStatistics stats = service.getFleetStatistics();
        
        assertNotNull(stats);
        assertNotNull(stats.getStatusBreakdown());
        assertTrue(stats.getStatusBreakdown().size() > 0);
    }

    @Test
    @DisplayName("Should count active vehicles correctly")
    void testActiveVehicleCount() {
        service.updateVehicleStatus("KOMATSU-930E-001", VehicleStatus.HAULING);
        service.updateVehicleStatus("KOMATSU-930E-002", VehicleStatus.LOADING);
        service.updateVehicleStatus("KOMATSU-930E-003", VehicleStatus.IDLE);
        
        FleetStatistics stats = service.getFleetStatistics();
        
        int activeCount = stats.getActiveVehicles();
        assertTrue(activeCount >= 2); // At least HAULING + LOADING
    }

    @Test
    @DisplayName("Should handle telemetry for non-existent vehicle")
    void testTelemetryForNonExistentVehicle() {
        Optional<VehicleTelemetry> telemetry = service.getVehicleTelemetry("NON-EXISTENT");
        
        assertFalse(telemetry.isPresent());
    }

    @Test
    @DisplayName("Should register new vehicle explicitly")
    void testRegisterVehicle() {
        Vehicle newVehicle = new Vehicle();
        newVehicle.setVehicleId("TEST-VEHICLE-001");
        newVehicle.setModel("930E");
        newVehicle.setManufacturer("Komatsu");
        newVehicle.setCapacity(300.0);
        
        service.registerVehicle(newVehicle);
        
        Optional<Vehicle> retrieved = service.getVehicle("TEST-VEHICLE-001");
        assertTrue(retrieved.isPresent());
        assertEquals("TEST-VEHICLE-001", retrieved.get().getVehicleId());
    }

    @Test
    @DisplayName("Should trigger emergency stop for all vehicles in dynamic fleet")
    void testEmergencyStopAll() {
        // Register a few vehicles via status update
        service.updateVehicleStatus("KOMATSU-930E-001", VehicleStatus.IDLE);
        service.updateVehicleStatus("KOMATSU-930E-002", VehicleStatus.IDLE);
        service.updateVehicleStatus("KOMATSU-930E-003", VehicleStatus.IDLE);

        service.emergencyStopAll();

        List<Vehicle> emergencyVehicles = service.getVehiclesByStatus(VehicleStatus.EMERGENCY_STOP);
        assertEquals(3, emergencyVehicles.size());
    }

    @Test
    @DisplayName("Should track idle vehicles")
    void testIdleVehicles() {
        service.updateVehicleStatus("KOMATSU-930E-001", VehicleStatus.IDLE);
        service.updateVehicleStatus("KOMATSU-930E-002", VehicleStatus.IDLE);
        service.updateVehicleStatus("KOMATSU-930E-003", VehicleStatus.IDLE);
        
        FleetStatistics stats = service.getFleetStatistics();
        assertTrue(stats.getIdleVehicles() >= 3);
    }

    @Test
    @DisplayName("Should auto-register when updating status for unknown vehicle")
    void testUpdateNonExistentVehicleStatus() {
        // Should auto-register and not throw exception
        assertDoesNotThrow(() ->
            service.updateVehicleStatus("NON-EXISTENT", VehicleStatus.HAULING)
        );
        List<Vehicle> haulingVehicles = service.getVehiclesByStatus(VehicleStatus.HAULING);
        assertTrue(haulingVehicles.stream().anyMatch(v -> v.getVehicleId().equals("NON-EXISTENT")));
    }

    @Test
    @DisplayName("Should auto-register when updating telemetry for unknown vehicle")
    void testUpdateNonExistentVehicleTelemetry() {
        VehicleTelemetry telemetry = new VehicleTelemetry();
        telemetry.setVehicleId("NON-EXISTENT");
            
        // Should auto-register and not throw exception
        assertDoesNotThrow(() ->
            service.updateVehicleTelemetry("NON-EXISTENT", telemetry)
        );
        Optional<VehicleTelemetry> retrieved = service.getVehicleTelemetry("NON-EXISTENT");
        assertTrue(retrieved.isPresent());
    }
}
