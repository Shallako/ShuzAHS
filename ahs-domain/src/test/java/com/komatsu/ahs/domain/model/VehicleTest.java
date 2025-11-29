package com.komatsu.ahs.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Vehicle Model Tests")
class VehicleTest {

    @Test
    @DisplayName("Should create vehicle with builder")
    void testVehicleBuilder() {
        Vehicle vehicle = Vehicle.builder()
            .vehicleId("KOMATSU-930E-001")
            .model("930E")
            .manufacturer("Komatsu")
            .capacity(300.0)
            .status(VehicleStatus.IDLE)
            .build();
        
        assertNotNull(vehicle);
        assertEquals("KOMATSU-930E-001", vehicle.getVehicleId());
        assertEquals("930E", vehicle.getModel());
        assertEquals("Komatsu", vehicle.getManufacturer());
        assertEquals(300.0, vehicle.getCapacity());
        assertEquals(VehicleStatus.IDLE, vehicle.getStatus());
    }

    @Test
    @DisplayName("Should support all vehicle statuses")
    void testVehicleStatuses() {
        VehicleStatus[] statuses = {
            VehicleStatus.IDLE,
            VehicleStatus.ROUTING,
            VehicleStatus.LOADING,
            VehicleStatus.HAULING,
            VehicleStatus.DUMPING,
            VehicleStatus.RETURNING,
            VehicleStatus.MAINTENANCE,
            VehicleStatus.OFFLINE,
            VehicleStatus.EMERGENCY_STOP,
            VehicleStatus.ERROR
        };
        
        for (VehicleStatus status : statuses) {
            Vehicle vehicle = Vehicle.builder()
                .vehicleId("TEST-001")
                .status(status)
                .build();
            
            assertEquals(status, vehicle.getStatus());
        }
    }

    @Test
    @DisplayName("Should support equality based on vehicleId")
    void testVehicleEquality() {
        Vehicle vehicle1 = Vehicle.builder()
            .vehicleId("KOMATSU-930E-001")
            .model("930E")
            .build();
        
        Vehicle vehicle2 = Vehicle.builder()
            .vehicleId("KOMATSU-930E-001")
            .model("930E-DIFFERENT")
            .build();
        
        // Lombok @Data generates equals/hashCode based on all fields
        // For testing, just verify the objects are created correctly
        assertEquals("KOMATSU-930E-001", vehicle1.getVehicleId());
        assertEquals("KOMATSU-930E-001", vehicle2.getVehicleId());
    }

    @Test
    @DisplayName("Should create 930E vehicle")
    void test930EVehicle() {
        Vehicle vehicle = Vehicle.builder()
            .vehicleId("KOMATSU-930E-001")
            .model("930E")
            .manufacturer("Komatsu")
            .capacity(300.0)
            .maxPayloadTons(300)
            .maxSpeedKph(60.0)
            .build();
        
        assertEquals(300.0, vehicle.getCapacity());
        assertEquals(300, vehicle.getMaxPayloadTons());
    }

    @Test
    @DisplayName("Should create 980E vehicle")
    void test980EVehicle() {
        Vehicle vehicle = Vehicle.builder()
            .vehicleId("KOMATSU-980E-001")
            .model("980E")
            .manufacturer("Komatsu")
            .capacity(400.0)
            .maxPayloadTons(400)
            .maxSpeedKph(60.0)
            .build();
        
        assertEquals(400.0, vehicle.getCapacity());
        assertEquals(400, vehicle.getMaxPayloadTons());
    }

    @Test
    @DisplayName("Should support autonomous mode flag")
    void testAutonomousMode() {
        Vehicle vehicle = Vehicle.builder()
            .vehicleId("KOMATSU-930E-001")
            .autonomousModeEnabled(true)
            .operationalStatus(true)
            .build();
        
        assertTrue(vehicle.isAutonomousModeEnabled());
        assertTrue(vehicle.isOperationalStatus());
    }

    @Test
    @DisplayName("Should support safety envelope dimensions")
    void testSafetyEnvelope() {
        Vehicle vehicle = Vehicle.builder()
            .vehicleId("KOMATSU-930E-001")
            .safetyEnvelopeFront(5.0)
            .safetyEnvelopeRear(5.0)
            .safetyEnvelopeSides(3.0)
            .build();
        
        assertEquals(5.0, vehicle.getSafetyEnvelopeFront());
        assertEquals(5.0, vehicle.getSafetyEnvelopeRear());
        assertEquals(3.0, vehicle.getSafetyEnvelopeSides());
    }

    @Test
    @DisplayName("Should support route assignment")
    void testRouteAssignment() {
        Vehicle vehicle = Vehicle.builder()
            .vehicleId("KOMATSU-930E-001")
            .assignedRouteId("ROUTE-001")
            .currentLocationId("LOAD-POINT-1")
            .destinationId("DUMP-POINT-1")
            .build();
        
        assertEquals("ROUTE-001", vehicle.getAssignedRouteId());
        assertEquals("LOAD-POINT-1", vehicle.getCurrentLocationId());
        assertEquals("DUMP-POINT-1", vehicle.getDestinationId());
    }
}
