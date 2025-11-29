package com.komatsu.ahs.telemetry.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Vehicle Metrics Tests")
class VehicleMetricsTest {

    @Test
    @DisplayName("Should create vehicle metrics with all fields")
    void testCreateCompleteMetrics() {
        Instant now = Instant.now();
        
        VehicleMetrics metrics = VehicleMetrics.builder()
            .vehicleId("KOMATSU-930E-001")
            .windowStart(now.minusSeconds(60))
            .windowEnd(now)
            .avgSpeedKph(35.5)
            .maxSpeedKph(42.0)
            .minSpeedKph(28.0)
            .avgFuelLevelPercent(75.0)
            .avgEngineTempCelsius(85.0)
            .maxEngineTempCelsius(92.0)
            .recordCount(120L)
            .build();
        
        assertNotNull(metrics);
        assertEquals("KOMATSU-930E-001", metrics.getVehicleId());
        assertEquals(35.5, metrics.getAvgSpeedKph());
        assertEquals(42.0, metrics.getMaxSpeedKph());
        assertEquals(28.0, metrics.getMinSpeedKph());
        assertEquals(120L, metrics.getRecordCount());
    }

    @Test
    @DisplayName("Should validate speed ranges")
    void testSpeedRanges() {
        VehicleMetrics metrics = VehicleMetrics.builder()
            .vehicleId("KOMATSU-930E-001")
            .avgSpeedKph(35.0)
            .maxSpeedKph(45.0)
            .minSpeedKph(25.0)
            .build();
        
        assertTrue(metrics.getMinSpeedKph() <= metrics.getAvgSpeedKph());
        assertTrue(metrics.getAvgSpeedKph() <= metrics.getMaxSpeedKph());
    }

    @Test
    @DisplayName("Should track temperature metrics")
    void testTemperatureMetrics() {
        VehicleMetrics metrics = VehicleMetrics.builder()
            .vehicleId("KOMATSU-930E-001")
            .avgEngineTempCelsius(85.0)
            .maxEngineTempCelsius(95.0)
            .build();
        
        assertEquals(85.0, metrics.getAvgEngineTempCelsius());
        assertEquals(95.0, metrics.getMaxEngineTempCelsius());
        assertTrue(metrics.getAvgEngineTempCelsius() <= metrics.getMaxEngineTempCelsius());
    }

    @Test
    @DisplayName("Should track fuel level metrics")
    void testFuelMetrics() {
        VehicleMetrics metrics = VehicleMetrics.builder()
            .vehicleId("KOMATSU-930E-001")
            .avgFuelLevelPercent(75.0)
            .build();
        
        assertEquals(75.0, metrics.getAvgFuelLevelPercent());
        assertTrue(metrics.getAvgFuelLevelPercent() >= 0 
            && metrics.getAvgFuelLevelPercent() <= 100);
    }

    @Test
    @DisplayName("Should include window time range")
    void testWindowTimeRange() {
        Instant start = Instant.now().minusSeconds(60);
        Instant end = Instant.now();
        
        VehicleMetrics metrics = VehicleMetrics.builder()
            .vehicleId("KOMATSU-930E-001")
            .windowStart(start)
            .windowEnd(end)
            .build();
        
        assertEquals(start, metrics.getWindowStart());
        assertEquals(end, metrics.getWindowEnd());
        assertTrue(metrics.getWindowStart().isBefore(metrics.getWindowEnd()));
    }

    @Test
    @DisplayName("Should count total events")
    void testEventCounting() {
        VehicleMetrics metrics = VehicleMetrics.builder()
            .vehicleId("KOMATSU-930E-001")
            .recordCount(150L)
            .build();
        
        assertEquals(150L, metrics.getRecordCount());
        assertTrue(metrics.getRecordCount() > 0);
    }

    @Test
    @DisplayName("Should handle high event counts")
    void testHighEventCounts() {
        VehicleMetrics metrics = VehicleMetrics.builder()
            .vehicleId("KOMATSU-930E-001")
            .recordCount(10000L)
            .build();
        
        assertEquals(10000L, metrics.getRecordCount());
    }

    @Test
    @DisplayName("Should create metrics for 930E truck")
    void test930EMetrics() {
        VehicleMetrics metrics = VehicleMetrics.builder()
            .vehicleId("KOMATSU-930E-001")
            .avgSpeedKph(32.0)
            .maxSpeedKph(40.0)  // Loaded speed limit
            .build();
        
        assertEquals("KOMATSU-930E-001", metrics.getVehicleId());
        assertTrue(metrics.getMaxSpeedKph() <= 40.0);  // 930E loaded max
    }

    @Test
    @DisplayName("Should create metrics for 980E truck")
    void test980EMetrics() {
        VehicleMetrics metrics = VehicleMetrics.builder()
            .vehicleId("KOMATSU-980E-001")
            .avgSpeedKph(30.0)
            .maxSpeedKph(38.0)  // Loaded speed limit
            .build();
        
        assertEquals("KOMATSU-980E-001", metrics.getVehicleId());
        assertTrue(metrics.getMaxSpeedKph() <= 40.0);  // 980E loaded max
    }

    @Test
    @DisplayName("Should track payload metrics")
    void testPayloadMetrics() {
        VehicleMetrics metrics = VehicleMetrics.builder()
            .vehicleId("KOMATSU-930E-001")
            .avgPayloadTons(285.0)
            .maxPayloadTons(300.0)
            .loadedPercentage(0.75)
            .build();
        
        assertEquals(285.0, metrics.getAvgPayloadTons());
        assertEquals(300.0, metrics.getMaxPayloadTons());
        assertEquals(0.75, metrics.getLoadedPercentage());
    }

    @Test
    @DisplayName("Should track battery metrics")
    void testBatteryMetrics() {
        VehicleMetrics metrics = VehicleMetrics.builder()
            .vehicleId("KOMATSU-930E-001")
            .avgBatteryLevelPercent(85.0)
            .build();
        
        assertEquals(85.0, metrics.getAvgBatteryLevelPercent());
        assertTrue(metrics.getAvgBatteryLevelPercent() >= 0 
            && metrics.getAvgBatteryLevelPercent() <= 100);
    }

    @Test
    @DisplayName("Should convert to JSON")
    void testToJson() {
        VehicleMetrics metrics = VehicleMetrics.builder()
            .vehicleId("KOMATSU-930E-001")
            .avgSpeedKph(35.5)
            .recordCount(100L)
            .build();
        
        String json = metrics.toJson();
        
        assertNotNull(json);
        assertTrue(json.contains("KOMATSU-930E-001"));
        assertTrue(json.contains("avg_speed_kph"));
    }

    @Test
    @DisplayName("Should use convenience setters")
    void testConvenienceSetters() {
        VehicleMetrics metrics = new VehicleMetrics();
        
        metrics.setDataPointCount(100L);
        metrics.setAverageSpeed(35.5);
        metrics.setMinSpeed(25.0);
        metrics.setMaxSpeed(45.0);
        metrics.setAverageFuelLevel(75.0);
        
        assertEquals(100L, metrics.getRecordCount());
        assertEquals(35.5, metrics.getAvgSpeedKph());
        assertEquals(25.0, metrics.getMinSpeedKph());
        assertEquals(45.0, metrics.getMaxSpeedKph());
        assertEquals(75.0, metrics.getAvgFuelLevelPercent());
    }
}
