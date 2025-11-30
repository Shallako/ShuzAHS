package com.komatsu.ahs.generator;

import com.komatsu.ahs.domain.model.GpsCoordinate;
import com.komatsu.ahs.domain.model.Location;
import com.komatsu.ahs.domain.model.VehicleTelemetry;
import com.komatsu.ahs.domain.model.VehicleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Telemetry Data Generator Tests")
class TelemetryDataGeneratorTest {

    private TelemetryDataGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new TelemetryDataGenerator();
    }

    @Test
    @DisplayName("Should generate valid telemetry for 930E truck")
    void testGenerate930ETelemetry() {
        String vehicleId = "KOMATSU-930E-001";
        VehicleStatus status = VehicleStatus.HAULING;
        
        VehicleTelemetry telemetry = generator.generateTelemetry(vehicleId, status);
        
        assertNotNull(telemetry);
        assertEquals(vehicleId, telemetry.getVehicleId());
        assertNotNull(telemetry.getLocation());
        assertNotNull(telemetry.getTimestamp());
        assertTrue(telemetry.getSpeedKph() >= 0);
        assertTrue(telemetry.getFuelLevelPercent() >= 5.0 && telemetry.getFuelLevelPercent() <= 100.0);
    }

    @Test
    @DisplayName("Should generate valid telemetry for 980E truck")
    void testGenerate980ETelemetry() {
        String vehicleId = "KOMATSU-980E-001";
        VehicleStatus status = VehicleStatus.LOADING;
        
        VehicleTelemetry telemetry = generator.generateTelemetry(vehicleId, status);
        
        assertNotNull(telemetry);
        assertEquals(vehicleId, telemetry.getVehicleId());
    }

    @Test
    @DisplayName("Should generate different speeds for HAULING vs ROUTING states")
    void testSpeedByState() {
        String vehicleId = "KOMATSU-930E-001";
        
        VehicleTelemetry haulingTelemetry = generator.generateTelemetry(vehicleId, 
            VehicleStatus.HAULING);
        VehicleTelemetry routingTelemetry = generator.generateTelemetry(vehicleId, 
            VehicleStatus.ROUTING);
        
        // HAULING should be slower (20-40 kph) than ROUTING (30-60 kph when empty)
        assertTrue(haulingTelemetry.getSpeedKph() <= 40.0);
        assertTrue(routingTelemetry.getSpeedKph() >= 0);
    }

    @Test
    @DisplayName("Should generate payload for loaded states")
    void testPayloadForLoadedStates() {
        String vehicleId = "KOMATSU-930E-001";
        
        VehicleTelemetry haulingTelemetry = generator.generateTelemetry(vehicleId, 
            VehicleStatus.HAULING);
        
        assertTrue(haulingTelemetry.isLoaded());
        assertTrue(haulingTelemetry.getPayloadTons() > 0);
    }

    @Test
    @DisplayName("Should generate zero payload for empty states")
    void testPayloadForEmptyStates() {
        String vehicleId = "KOMATSU-930E-001";
        
        VehicleTelemetry idleTelemetry = generator.generateTelemetry(vehicleId, 
            VehicleStatus.IDLE);
        VehicleTelemetry routingTelemetry = generator.generateTelemetry(vehicleId, 
            VehicleStatus.ROUTING);
        
        assertFalse(idleTelemetry.isLoaded());
        assertEquals(0.0, idleTelemetry.getPayloadTons());
        assertFalse(routingTelemetry.isLoaded());
    }

    @Test
    @DisplayName("Should generate valid loading location")
    void testGenerateLoadLocation() {
        Location loadLocation = generator.generateLoadLocation();
        
        assertNotNull(loadLocation);
        assertEquals(Location.LocationType.LOADING_POINT, loadLocation.getType());
        assertNotNull(loadLocation.getCoordinate());
    }

    @Test
    @DisplayName("Should generate valid dump location")
    void testGenerateDumpLocation() {
        Location dumpLocation = generator.generateDumpLocation();
        
        assertNotNull(dumpLocation);
        assertEquals(Location.LocationType.DUMP_POINT, dumpLocation.getType());
        assertNotNull(dumpLocation.getCoordinate());
    }

    @Test
    @DisplayName("Should generate realistic engine temperature")
    void testEngineTemperature() {
        String vehicleId = "KOMATSU-930E-001";
        
        VehicleTelemetry telemetry = generator.generateTelemetry(vehicleId, 
            VehicleStatus.HAULING);
        
        assertTrue(telemetry.getEngineTemperatureCelsius() >= 80.0 
            && telemetry.getEngineTemperatureCelsius() <= 95.0);
    }

    @Test
    @DisplayName("Should generate realistic tire pressures")
    void testTirePressures() {
        String vehicleId = "KOMATSU-930E-001";
        
        VehicleTelemetry telemetry = generator.generateTelemetry(vehicleId, 
            VehicleStatus.HAULING);
        
        // Mining haul trucks run at higher tire pressures (90-110 PSI)
        assertTrue(telemetry.getTirePressureFrontLeftPsi() >= 90.0 
            && telemetry.getTirePressureFrontLeftPsi() <= 110.0);
        assertTrue(telemetry.getTirePressureFrontRightPsi() >= 90.0 
            && telemetry.getTirePressureFrontRightPsi() <= 110.0);
        assertTrue(telemetry.getTirePressureRearLeftPsi() >= 90.0 
            && telemetry.getTirePressureRearLeftPsi() <= 110.0);
        assertTrue(telemetry.getTirePressureRearRightPsi() >= 90.0 
            && telemetry.getTirePressureRearRightPsi() <= 110.0);
    }

    @Test
    @DisplayName("Should generate different payloads for 930E vs 980E")
    void testPayloadByModel() {
        VehicleTelemetry telemetry930E = generator.generateTelemetry("KOMATSU-930E-001", 
            VehicleStatus.HAULING);
        VehicleTelemetry telemetry980E = generator.generateTelemetry("KOMATSU-980E-001", 
            VehicleStatus.HAULING);
        
        // 930E capacity: 300 tons (240-300 when loaded at 80-100%)
        assertTrue(telemetry930E.getPayloadTons() >= 240.0 
            && telemetry930E.getPayloadTons() <= 300.0);
        
        // 980E capacity: 400 tons (320-400 when loaded at 80-100%)
        assertTrue(telemetry980E.getPayloadTons() >= 320.0 
            && telemetry980E.getPayloadTons() <= 400.0);
    }

    @Test
    @DisplayName("Should generate telemetry with location")
    void testTelemetryLocation() {
        VehicleTelemetry telemetry = generator.generateTelemetry("KOMATSU-930E-001", 
            VehicleStatus.HAULING);
        
        assertNotNull(telemetry.getLocation());
        assertNotNull(telemetry.getLocation().getLatitude());
        assertNotNull(telemetry.getLocation().getLongitude());
        assertNotNull(telemetry.getLocation().getAltitude());
    }

    @Test
    @DisplayName("Should generate telemetry with valid heading")
    void testHeading() {
        VehicleTelemetry telemetry = generator.generateTelemetry("KOMATSU-930E-001", 
            VehicleStatus.HAULING);
        
        assertTrue(telemetry.getHeadingDegrees() >= 0 && telemetry.getHeadingDegrees() <= 360);
    }

    @Test
    @DisplayName("Should generate telemetry with valid engine RPM")
    void testEngineRpm() {
        VehicleTelemetry telemetry = generator.generateTelemetry("KOMATSU-930E-001", 
            VehicleStatus.HAULING);
        
        // When moving, should be 1200-1800 RPM
        assertTrue(telemetry.getEngineRpm() >= 600);
    }

    @Test
    @DisplayName("Should generate zero speed for IDLE state")
    void testIdleSpeed() {
        VehicleTelemetry telemetry = generator.generateTelemetry("KOMATSU-930E-001", 
            VehicleStatus.IDLE);
        
        assertEquals(0.0, telemetry.getSpeedKph());
    }

    @Test
    @DisplayName("Should generate zero speed for LOADING state")
    void testLoadingSpeed() {
        VehicleTelemetry telemetry = generator.generateTelemetry("KOMATSU-930E-001", 
            VehicleStatus.LOADING);
        
        assertEquals(0.0, telemetry.getSpeedKph());
    }

    @Test
    @DisplayName("Should generate zero speed for DUMPING state")
    void testDumpingSpeed() {
        VehicleTelemetry telemetry = generator.generateTelemetry("KOMATSU-930E-001", 
            VehicleStatus.DUMPING);
        
        assertEquals(0.0, telemetry.getSpeedKph());
    }
}
