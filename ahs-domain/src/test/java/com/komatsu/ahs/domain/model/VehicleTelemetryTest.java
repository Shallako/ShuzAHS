package com.komatsu.ahs.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Vehicle Telemetry Tests")
class VehicleTelemetryTest {

    @Test
    @DisplayName("Should create telemetry with all fields")
    void testCompleteTelemetry() {
        Instant now = Instant.now();
        GpsCoordinate coordinate = GpsCoordinate.builder()
            .latitude(-23.5)
            .longitude(-70.4)
            .altitude(2900.0)
            .timestamp(now)
            .build();
        
        VehicleTelemetry telemetry = VehicleTelemetry.builder()
            .vehicleId("KOMATSU-930E-001")
            .timestamp(now)
            .location(coordinate)
            .speedKph(35.5)
            .headingDegrees(127.0)
            .engineRpm(1650.0)
            .fuelLevelPercent(75.0)
            .batteryLevelPercent(95.0)
            .engineTemperatureCelsius(85.0)
            .payloadTons(285.0)
            .isLoaded(true)
            .brakePressurePsi(50.0)
            .tirePressureFrontLeftPsi(90.0)
            .tirePressureFrontRightPsi(91.0)
            .tirePressureRearLeftPsi(92.0)
            .tirePressureRearRightPsi(93.0)
            .hydraulicPressurePsi(2400.0)
            .build();
        
        assertEquals("KOMATSU-930E-001", telemetry.getVehicleId());
        assertEquals(35.5, telemetry.getSpeedKph());
        assertEquals(285.0, telemetry.getPayloadTons());
        assertTrue(telemetry.isLoaded());
        assertNotNull(telemetry.getLocation());
    }

    @Test
    @DisplayName("Should create telemetry for loaded vehicle")
    void testLoadedVehicleTelemetry() {
        VehicleTelemetry telemetry = VehicleTelemetry.builder()
            .vehicleId("KOMATSU-930E-001")
            .payloadTons(285.0)
            .isLoaded(true)
            .build();
        
        assertTrue(telemetry.isLoaded());
        assertEquals(285.0, telemetry.getPayloadTons());
    }

    @Test
    @DisplayName("Should create telemetry for empty vehicle")
    void testEmptyVehicleTelemetry() {
        VehicleTelemetry telemetry = VehicleTelemetry.builder()
            .vehicleId("KOMATSU-930E-001")
            .payloadTons(0.0)
            .isLoaded(false)
            .build();
        
        assertFalse(telemetry.isLoaded());
        assertEquals(0.0, telemetry.getPayloadTons());
    }

    @Test
    @DisplayName("Should validate realistic speed ranges")
    void testSpeedRanges() {
        VehicleTelemetry slowSpeed = VehicleTelemetry.builder()
            .vehicleId("TEST-001")
            .speedKph(5.0)
            .build();
        
        VehicleTelemetry maxSpeed = VehicleTelemetry.builder()
            .vehicleId("TEST-002")
            .speedKph(60.0)
            .build();
        
        assertTrue(slowSpeed.getSpeedKph() >= 0);
        assertTrue(maxSpeed.getSpeedKph() <= 60.0);
    }

    @Test
    @DisplayName("Should validate fuel level percentage")
    void testFuelLevel() {
        VehicleTelemetry lowFuel = VehicleTelemetry.builder()
            .vehicleId("TEST-001")
            .fuelLevelPercent(10.0)
            .build();
        
        VehicleTelemetry fullFuel = VehicleTelemetry.builder()
            .vehicleId("TEST-002")
            .fuelLevelPercent(100.0)
            .build();
        
        assertTrue(lowFuel.getFuelLevelPercent() >= 0 && lowFuel.getFuelLevelPercent() <= 100);
        assertTrue(fullFuel.getFuelLevelPercent() >= 0 && fullFuel.getFuelLevelPercent() <= 100);
    }

    @Test
    @DisplayName("Should include GPS location")
    void testGpsLocation() {
        GpsCoordinate coordinate = GpsCoordinate.builder()
            .latitude(-23.4)
            .longitude(-70.35)
            .altitude(2950.0)
            .build();
        
        VehicleTelemetry telemetry = VehicleTelemetry.builder()
            .vehicleId("KOMATSU-930E-001")
            .location(coordinate)
            .build();
        
        assertNotNull(telemetry.getLocation());
        assertEquals(-23.4, telemetry.getLocation().getLatitude());
        assertEquals(-70.35, telemetry.getLocation().getLongitude());
    }
}
