package com.komatsu.ahs.fleet.integration;

import com.komatsu.ahs.domain.model.Vehicle;
import com.komatsu.ahs.domain.model.VehicleStatus;
import com.komatsu.ahs.domain.model.VehicleTelemetry;
import com.komatsu.ahs.domain.model.GpsCoordinate;
import com.komatsu.ahs.fleet.kafka.producer.AlertProducer;
import com.komatsu.ahs.fleet.kafka.producer.MetricsProducer;
import com.komatsu.ahs.fleet.metrics.FleetMetricsExporter;
import com.komatsu.ahs.fleet.service.FleetManagementService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Fleet Metrics Pipeline
 * 
 * Tests the complete flow from fleet state changes through
 * FleetMetricsExporter to Prometheus metrics.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Fleet Metrics Integration Tests")
class FleetMetricsIntegrationTest {

    @Mock
    private AlertProducer alertProducer;
    
    @Mock
    private MetricsProducer metricsProducer;

    private MeterRegistry meterRegistry;
    private FleetMetricsExporter metricsExporter;
    private FleetManagementService fleetService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsExporter = new FleetMetricsExporter(meterRegistry);
        metricsExporter.initMetrics();
        
        fleetService = new FleetManagementService(alertProducer, metricsProducer, metricsExporter);
        fleetService.initialize();
    }

    @Test
    @DisplayName("Should expose vehicle count gauges")
    void testVehicleCountGauges() {
        // Metric names use underscores (Prometheus convention)
        assertNotNull(meterRegistry.find("ahs_fleet_vehicles_total").gauge());
        assertNotNull(meterRegistry.find("ahs_fleet_vehicles_active").gauge());
        assertNotNull(meterRegistry.find("ahs_fleet_vehicles_idle").gauge());
        assertNotNull(meterRegistry.find("ahs_fleet_vehicles_hauling").gauge());
        assertNotNull(meterRegistry.find("ahs_fleet_vehicles_loading").gauge());
        assertNotNull(meterRegistry.find("ahs_fleet_vehicles_dumping").gauge());
        assertNotNull(meterRegistry.find("ahs_fleet_vehicles_routing").gauge());
        assertNotNull(meterRegistry.find("ahs_fleet_vehicles_emergency").gauge());
    }

    @Test
    @DisplayName("Should update vehicle count metrics when status changes")
    void testVehicleCountMetricsUpdate() {
        // Update some vehicles to specific statuses
        fleetService.updateVehicleStatus("KOMATSU-930E-001", VehicleStatus.HAULING);
        fleetService.updateVehicleStatus("KOMATSU-930E-002", VehicleStatus.LOADING);
        fleetService.updateVehicleStatus("KOMATSU-930E-003", VehicleStatus.IDLE);
        fleetService.updateVehicleStatus("KOMATSU-930E-004", VehicleStatus.IDLE);
        
        // Update metrics (simulating scheduled update)
        updateFleetMetrics();
        
        // Verify total reflects dynamically registered vehicles (4 above)
        assertEquals(4.0, meterRegistry.find("ahs_fleet_vehicles_total").gauge().value());
    }

    @Test
    @DisplayName("Should track emergency stop vehicles")
    void testEmergencyStopMetrics() {
        // Register some vehicles first, then trigger emergency stop for all
        fleetService.updateVehicleStatus("KOMATSU-930E-001", VehicleStatus.IDLE);
        fleetService.updateVehicleStatus("KOMATSU-930E-002", VehicleStatus.IDLE);
        fleetService.updateVehicleStatus("KOMATSU-980E-001", VehicleStatus.IDLE);
        
        // Trigger emergency stop for all vehicles
        fleetService.emergencyStopAll();
        
        // Update metrics
        updateFleetMetrics();
        
        // All registered vehicles (3) should be in emergency stop
        assertEquals(3.0, meterRegistry.find("ahs_fleet_vehicles_emergency").gauge().value());
    }

    @Test
    @DisplayName("Should expose alert counters")
    void testAlertCountersExposed() {
        // Counter names use underscores and have _total suffix
        assertNotNull(meterRegistry.find("ahs_fleet_alerts_critical_total").counter());
        assertNotNull(meterRegistry.find("ahs_fleet_alerts_warning_total").counter());
        assertNotNull(meterRegistry.find("ahs_fleet_alerts_error_total").counter());
    }

    @Test
    @DisplayName("Should expose CEP alert counters")
    void testCepAlertCountersExposed() {
        assertNotNull(meterRegistry.find("ahs_cep_alerts_rapid_deceleration_total").counter());
        assertNotNull(meterRegistry.find("ahs_cep_alerts_low_fuel_total").counter());
        assertNotNull(meterRegistry.find("ahs_cep_alerts_overheating_total").counter());
        assertNotNull(meterRegistry.find("ahs_cep_alerts_high_temperature_total").counter());
        assertNotNull(meterRegistry.find("ahs_cep_alerts_tire_pressure_total").counter());
    }

    @Test
    @DisplayName("Should track vehicle status distribution")
    void testVehicleStatusDistribution() {
        // Set various vehicle statuses
        fleetService.updateVehicleStatus("KOMATSU-930E-001", VehicleStatus.HAULING);
        fleetService.updateVehicleStatus("KOMATSU-930E-002", VehicleStatus.HAULING);
        fleetService.updateVehicleStatus("KOMATSU-930E-003", VehicleStatus.LOADING);
        fleetService.updateVehicleStatus("KOMATSU-930E-004", VehicleStatus.DUMPING);
        fleetService.updateVehicleStatus("KOMATSU-930E-005", VehicleStatus.ROUTING);
        
        // Get fleet statistics
        var stats = fleetService.getFleetStatistics();
        
        assertNotNull(stats.getStatusBreakdown());
        assertTrue(stats.getStatusBreakdown().containsKey(VehicleStatus.HAULING));
        assertEquals(2L, stats.getStatusBreakdown().get(VehicleStatus.HAULING));
    }

    @Test
    @DisplayName("Should handle telemetry updates with location data")
    void testTelemetryWithLocation() {
        String vehicleId = "KOMATSU-930E-001";
        
        VehicleTelemetry telemetry = VehicleTelemetry.builder()
            .vehicleId(vehicleId)
            .timestamp(Instant.now())
            .speedKph(35.5)
            .fuelLevelPercent(75.0)
            .engineTemperatureCelsius(88.0)
            .location(GpsCoordinate.builder()
                .latitude(-23.5)
                .longitude(-70.4)
                .altitude(2900.0)
                .build())
            .build();
        
        fleetService.updateVehicleTelemetry(vehicleId, telemetry);
        
        var retrieved = fleetService.getVehicleTelemetry(vehicleId);
        assertTrue(retrieved.isPresent());
        assertEquals(-23.5, retrieved.get().getLocation().getLatitude());
    }

    @Test
    @DisplayName("Should process fleet with mixed vehicle models")
    void testMixedVehicleModels() {
        // Register a mix of vehicle models
        fleetService.registerVehicle(Vehicle.builder()
            .vehicleId("KOMATSU-930E-101")
            .model("930E")
            .manufacturer("Komatsu")
            .capacity(300.0)
            .build());

        fleetService.registerVehicle(Vehicle.builder()
            .vehicleId("KOMATSU-980E-201")
            .model("980E")
            .manufacturer("Komatsu")
            .capacity(360.0)
            .build());

        List<Vehicle> vehicles = fleetService.getAllActiveVehicles();

        // Should have mix of 930E and 980E (1 each)
        long count930E = vehicles.stream()
            .filter(v -> v.getModel().equals("930E"))
            .count();
        long count980E = vehicles.stream()
            .filter(v -> v.getModel().equals("980E"))
            .count();
        
        assertEquals(1, count930E);
        assertEquals(1, count980E);
    }

    @Test
    @DisplayName("Should register new vehicle and include in metrics")
    void testNewVehicleRegistration() {
        Vehicle newVehicle = Vehicle.builder()
            .vehicleId("KOMATSU-930E-TEST")
            .model("930E")
            .manufacturer("Komatsu")
            .capacity(300.0)
            .build();
        
        int initialCount = fleetService.getAllActiveVehicles().size();
        
        fleetService.registerVehicle(newVehicle);
        
        assertEquals(initialCount + 1, fleetService.getAllActiveVehicles().size());
    }

    @Test
    @DisplayName("Should increment CEP alert counters by type")
    void testCepAlertTypeIncrement() {
        metricsExporter.incrementCepAlertByType("RAPID_DECELERATION");
        metricsExporter.incrementCepAlertByType("LOW_FUEL");
        metricsExporter.incrementCepAlertByType("OVERHEATING");
        
        assertEquals(1.0, meterRegistry.find("ahs_cep_alerts_rapid_deceleration_total").counter().count());
        assertEquals(1.0, meterRegistry.find("ahs_cep_alerts_low_fuel_total").counter().count());
        assertEquals(1.0, meterRegistry.find("ahs_cep_alerts_overheating_total").counter().count());
    }

    // Helper method to update metrics
    private void updateFleetMetrics() {
        var stats = fleetService.getFleetStatistics();
        metricsExporter.updateVehicleCounts(
            stats.getTotalVehicles(),
            stats.getActiveVehicles(),
            stats.getIdleVehicles(),
            stats.getStatusBreakdown().getOrDefault(VehicleStatus.ROUTING, 0L).intValue(),
            stats.getStatusBreakdown().getOrDefault(VehicleStatus.LOADING, 0L).intValue(),
            stats.getStatusBreakdown().getOrDefault(VehicleStatus.HAULING, 0L).intValue(),
            stats.getStatusBreakdown().getOrDefault(VehicleStatus.DUMPING, 0L).intValue(),
            stats.getStatusBreakdown().getOrDefault(VehicleStatus.EMERGENCY_STOP, 0L).intValue()
        );
    }
}
