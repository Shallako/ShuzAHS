package com.komatsu.ahs.telemetry.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Telemetry Alert Tests")
class TelemetryAlertTest {

    @Test
    @DisplayName("Should create high temperature alert")
    void testHighTemperatureAlert() {
        TelemetryAlert alert = TelemetryAlert.builder()
            .alertId("ALERT-001")
            .vehicleId("KOMATSU-930E-001")
            .timestamp(Instant.now())
            .alertType(TelemetryAlert.AlertType.HIGH_TEMPERATURE)
            .severity(TelemetryAlert.AlertSeverity.WARNING)
            .message("Engine temperature high: 95°C")
            .metricValue(95.0)
            .thresholdValue(90.0)
            .build();
        
        assertNotNull(alert);
        assertEquals(TelemetryAlert.AlertType.HIGH_TEMPERATURE, alert.getAlertType());
        assertEquals(TelemetryAlert.AlertSeverity.WARNING, alert.getSeverity());
        assertEquals(95.0, alert.getMetricValue());
        assertEquals(90.0, alert.getThresholdValue());
    }

    @Test
    @DisplayName("Should create low fuel alert")
    void testLowFuelAlert() {
        TelemetryAlert alert = TelemetryAlert.builder()
            .alertId("ALERT-002")
            .vehicleId("KOMATSU-930E-001")
            .timestamp(Instant.now())
            .alertType(TelemetryAlert.AlertType.LOW_FUEL)
            .severity(TelemetryAlert.AlertSeverity.CRITICAL)
            .message("Critical fuel level: 3%")
            .metricValue(3.0)
            .thresholdValue(5.0)
            .build();
        
        assertEquals(TelemetryAlert.AlertType.LOW_FUEL, alert.getAlertType());
        assertEquals(TelemetryAlert.AlertSeverity.CRITICAL, alert.getSeverity());
        assertTrue(alert.getMetricValue() < alert.getThresholdValue());
    }

    @Test
    @DisplayName("Should support all alert types")
    void testAllAlertTypes() {
        TelemetryAlert.AlertType[] types = {
            TelemetryAlert.AlertType.HIGH_TEMPERATURE,
            TelemetryAlert.AlertType.LOW_FUEL,
            TelemetryAlert.AlertType.LOW_BATTERY,
            TelemetryAlert.AlertType.TIRE_PRESSURE_LOW,
            TelemetryAlert.AlertType.BRAKE_PRESSURE_LOW,
            TelemetryAlert.AlertType.ENGINE_WARNING,
            TelemetryAlert.AlertType.OVERLOAD,
            TelemetryAlert.AlertType.RAPID_DECELERATION,
            TelemetryAlert.AlertType.OVERHEATING
        };
        
        for (TelemetryAlert.AlertType type : types) {
            TelemetryAlert alert = TelemetryAlert.builder()
                .alertId("TEST-" + type.name())
                .vehicleId("TEST-001")
                .alertType(type)
                .severity(TelemetryAlert.AlertSeverity.WARNING)
                .build();
            
            assertEquals(type, alert.getAlertType());
        }
    }

    @Test
    @DisplayName("Should support all severity levels")
    void testAllSeverityLevels() {
        TelemetryAlert.AlertSeverity[] severities = {
            TelemetryAlert.AlertSeverity.INFO,
            TelemetryAlert.AlertSeverity.WARNING,
            TelemetryAlert.AlertSeverity.CRITICAL,
            TelemetryAlert.AlertSeverity.EMERGENCY
        };
        
        for (TelemetryAlert.AlertSeverity severity : severities) {
            TelemetryAlert alert = TelemetryAlert.builder()
                .alertId("TEST-" + severity.name())
                .vehicleId("TEST-001")
                .alertType(TelemetryAlert.AlertType.ENGINE_WARNING)
                .severity(severity)
                .build();
            
            assertEquals(severity, alert.getSeverity());
        }
    }

    @Test
    @DisplayName("Should create rapid deceleration alert")
    void testRapidDecelerationAlert() {
        TelemetryAlert alert = TelemetryAlert.builder()
            .alertId("ALERT-003")
            .vehicleId("KOMATSU-930E-001")
            .timestamp(Instant.now())
            .alertType(TelemetryAlert.AlertType.RAPID_DECELERATION)
            .severity(TelemetryAlert.AlertSeverity.WARNING)
            .message("Rapid deceleration detected: 35 kph drop in 5 seconds")
            .metricValue(35.0)
            .build();
        
        assertEquals(TelemetryAlert.AlertType.RAPID_DECELERATION, alert.getAlertType());
        assertTrue(alert.getMessage().contains("deceleration"));
    }

    @Test
    @DisplayName("Should create overheating alert")
    void testOverheatingAlert() {
        TelemetryAlert alert = TelemetryAlert.builder()
            .alertId("ALERT-004")
            .vehicleId("KOMATSU-930E-001")
            .timestamp(Instant.now())
            .alertType(TelemetryAlert.AlertType.OVERHEATING)
            .severity(TelemetryAlert.AlertSeverity.CRITICAL)
            .message("Engine overheating: sustained temperature above 100°C")
            .metricValue(105.0)
            .thresholdValue(100.0)
            .build();
        
        assertEquals(TelemetryAlert.AlertType.OVERHEATING, alert.getAlertType());
        assertEquals(TelemetryAlert.AlertSeverity.CRITICAL, alert.getSeverity());
        assertTrue(alert.getMetricValue() > alert.getThresholdValue());
    }

    @Test
    @DisplayName("Should include timestamp")
    void testAlertTimestamp() {
        Instant now = Instant.now();
        
        TelemetryAlert alert = TelemetryAlert.builder()
            .alertId("ALERT-005")
            .vehicleId("KOMATSU-930E-001")
            .timestamp(now)
            .alertType(TelemetryAlert.AlertType.LOW_FUEL)
            .severity(TelemetryAlert.AlertSeverity.WARNING)
            .build();
        
        assertNotNull(alert.getTimestamp());
        assertEquals(now, alert.getTimestamp());
    }

    @Test
    @DisplayName("Should create brake pressure alert")
    void testBrakePressureAlert() {
        TelemetryAlert alert = TelemetryAlert.builder()
            .alertId("ALERT-006")
            .vehicleId("KOMATSU-930E-001")
            .alertType(TelemetryAlert.AlertType.BRAKE_PRESSURE_LOW)
            .severity(TelemetryAlert.AlertSeverity.CRITICAL)
            .message("Brake pressure critically low")
            .metricValue(15.0)
            .thresholdValue(30.0)
            .build();
        
        assertEquals(TelemetryAlert.AlertType.BRAKE_PRESSURE_LOW, alert.getAlertType());
        assertTrue(alert.getMetricValue() < alert.getThresholdValue());
    }
}
