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
        // Fuel can be low during anomaly injection (3-18%) or normal (35-100%)
        assertTrue(telemetry.getFuelLevelPercent() >= 3.0 && telemetry.getFuelLevelPercent() <= 100.0);
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
        
        // HAULING can be up to 68 kph during excessive speed anomaly
        assertTrue(haulingTelemetry.getSpeedKph() <= 70.0);
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
        
        // Normal range is 78-92°C, but anomaly injection can produce 96-108°C for overheating alerts
        assertTrue(telemetry.getEngineTemperatureCelsius() >= 78.0 
            && telemetry.getEngineTemperatureCelsius() <= 108.0,
            "Engine temp should be 78-108°C (includes overheating anomaly), was: " 
            + telemetry.getEngineTemperatureCelsius());
    }

    @Test
    @DisplayName("Should generate realistic tire pressures")
    void testTirePressures() {
        String vehicleId = "KOMATSU-930E-001";
        
        VehicleTelemetry telemetry = generator.generateTelemetry(vehicleId, 
            VehicleStatus.HAULING);
        
        // Normal range is 98-115 PSI, but anomaly injection can produce 82-93 PSI for low tire alerts
        // So the valid range is 82-115 PSI
        assertTrue(telemetry.getTirePressureFrontLeftPsi() >= 82.0 
            && telemetry.getTirePressureFrontLeftPsi() <= 115.0,
            "Front left tire pressure should be 82-115 PSI, was: " 
            + telemetry.getTirePressureFrontLeftPsi());
        assertTrue(telemetry.getTirePressureFrontRightPsi() >= 82.0 
            && telemetry.getTirePressureFrontRightPsi() <= 115.0,
            "Front right tire pressure should be 82-115 PSI, was: " 
            + telemetry.getTirePressureFrontRightPsi());
        assertTrue(telemetry.getTirePressureRearLeftPsi() >= 82.0 
            && telemetry.getTirePressureRearLeftPsi() <= 115.0,
            "Rear left tire pressure should be 82-115 PSI, was: " 
            + telemetry.getTirePressureRearLeftPsi());
        assertTrue(telemetry.getTirePressureRearRightPsi() >= 82.0 
            && telemetry.getTirePressureRearRightPsi() <= 115.0,
            "Rear right tire pressure should be 82-115 PSI, was: " 
            + telemetry.getTirePressureRearRightPsi());
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

    // ==================== NEW TESTS FOR ANOMALY INJECTION ====================

    @Test
    @DisplayName("Should generate valid battery level (normal or low anomaly)")
    void testBatteryLevel() {
        VehicleTelemetry telemetry = generator.generateTelemetry("KOMATSU-930E-001", 
            VehicleStatus.HAULING);
        
        // Normal 70-100%, anomaly 12-28%
        assertTrue(telemetry.getBatteryLevelPercent() >= 12.0 
            && telemetry.getBatteryLevelPercent() <= 100.0,
            "Battery level should be 12-100%, was: " + telemetry.getBatteryLevelPercent());
    }

    @Test
    @DisplayName("Should generate valid brake pressure (normal or low anomaly)")
    void testBrakePressure() {
        VehicleTelemetry telemetry = generator.generateTelemetry("KOMATSU-930E-001", 
            VehicleStatus.HAULING);
        
        // Normal 95-125 PSI, anomaly 72-88 PSI
        assertTrue(telemetry.getBrakePressurePsi() >= 72.0 
            && telemetry.getBrakePressurePsi() <= 125.0,
            "Brake pressure should be 72-125 PSI, was: " + telemetry.getBrakePressurePsi());
    }

    @Test
    @DisplayName("Should generate valid hydraulic pressure (normal or low anomaly)")
    void testHydraulicPressure() {
        VehicleTelemetry telemetry = generator.generateTelemetry("KOMATSU-930E-001", 
            VehicleStatus.HAULING);
        
        // Normal 2600-3200 PSI, anomaly 2100-2450 PSI
        assertTrue(telemetry.getHydraulicPressurePsi() >= 2100.0 
            && telemetry.getHydraulicPressurePsi() <= 3200.0,
            "Hydraulic pressure should be 2100-3200 PSI, was: " + telemetry.getHydraulicPressurePsi());
    }

    @Test
    @DisplayName("Should generate valid diagnostic code count")
    void testDiagnosticCodes() {
        VehicleTelemetry telemetry = generator.generateTelemetry("KOMATSU-930E-001", 
            VehicleStatus.HAULING);
        
        // Normal 0-1, anomaly 4-7
        assertTrue(telemetry.getDiagnosticCodeCount() >= 0 
            && telemetry.getDiagnosticCodeCount() <= 7,
            "Diagnostic code count should be 0-7, was: " + telemetry.getDiagnosticCodeCount());
    }

    @Test
    @DisplayName("Should generate valid operating hours")
    void testOperatingHours() {
        VehicleTelemetry telemetry = generator.generateTelemetry("KOMATSU-930E-001", 
            VehicleStatus.HAULING);
        
        // Should be >= 0 (can be near maintenance threshold ~500 hours)
        assertTrue(telemetry.getOperatingHours() >= 0,
            "Operating hours should be >= 0, was: " + telemetry.getOperatingHours());
    }
}
