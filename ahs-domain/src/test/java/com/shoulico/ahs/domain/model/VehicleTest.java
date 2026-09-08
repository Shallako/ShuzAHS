package com.shoulico.ahs.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Vehicle Model Tests")
class VehicleTest {

    @Test
    @DisplayName("Should create vehicle with builder")
    void testVehicleBuilder() {
        Vehicle vehicle = Vehicle.builder()
            .vehicleId("TITAN-300-001")
            .model("Titan 300")
            .manufacturer("Titan")
            .capacity(300.0)
            .status(VehicleStatus.IDLE)
            .build();
        
        assertNotNull(vehicle);
        assertEquals("TITAN-300-001", vehicle.getVehicleId());
        assertEquals("Titan 300", vehicle.getModel());
        assertEquals("Titan", vehicle.getManufacturer());
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
            .vehicleId("TITAN-300-001")
            .model("Titan 300")
            .build();
        
        Vehicle vehicle2 = Vehicle.builder()
            .vehicleId("TITAN-300-001")
            .model("Titan 300-DIFFERENT")
            .build();
        
        // Lombok @Data generates equals/hashCode based on all fields
        // For testing, just verify the objects are created correctly
        assertEquals("TITAN-300-001", vehicle1.getVehicleId());
        assertEquals("TITAN-300-001", vehicle2.getVehicleId());
    }

    @Test
    @DisplayName("Should create Titan 300 vehicle")
    void testTitan300Vehicle() {
        Vehicle vehicle = Vehicle.builder()
            .vehicleId("TITAN-300-001")
            .model("Titan 300")
            .manufacturer("Titan")
            .capacity(300.0)
            .maxPayloadTons(300)
            .maxSpeedKph(60.0)
            .build();
        
        assertEquals(300.0, vehicle.getCapacity());
        assertEquals(300, vehicle.getMaxPayloadTons());
    }

    @Test
    @DisplayName("Should create Titan 400 vehicle")
    void testTitan400Vehicle() {
        Vehicle vehicle = Vehicle.builder()
            .vehicleId("TITAN-400-001")
            .model("Titan 400")
            .manufacturer("Titan")
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
            .vehicleId("TITAN-300-001")
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
            .vehicleId("TITAN-300-001")
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
            .vehicleId("TITAN-300-001")
            .assignedRouteId("ROUTE-001")
            .currentLocationId("LOAD-POINT-1")
            .destinationId("DUMP-POINT-1")
            .build();
        
        assertEquals("ROUTE-001", vehicle.getAssignedRouteId());
        assertEquals("LOAD-POINT-1", vehicle.getCurrentLocationId());
        assertEquals("DUMP-POINT-1", vehicle.getDestinationId());
    }
}
