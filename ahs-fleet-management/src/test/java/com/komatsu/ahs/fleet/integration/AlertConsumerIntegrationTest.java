package com.komatsu.ahs.fleet.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.komatsu.ahs.fleet.kafka.AlertConsumer;
import com.komatsu.ahs.fleet.metrics.FleetMetricsExporter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Alert Consumer and Metrics Pipeline
 * 
 * Tests the complete flow from Flink CEP alerts through the
 * AlertConsumer to Prometheus metrics via FleetMetricsExporter.
 */
@DisplayName("Alert Consumer Integration Tests")
class AlertConsumerIntegrationTest {

    private MeterRegistry meterRegistry;
    private FleetMetricsExporter metricsExporter;
    private AlertConsumer alertConsumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Use SimpleMeterRegistry for testing without Spring context
        meterRegistry = new SimpleMeterRegistry();
        metricsExporter = new FleetMetricsExporter(meterRegistry);
        metricsExporter.initMetrics();
        
        objectMapper = new ObjectMapper();
        alertConsumer = new AlertConsumer(metricsExporter, objectMapper);
    }

    @Test
    @DisplayName("Should increment critical alert counter and verify in registry")
    void testCriticalAlertMetricsIntegration() {
        String criticalAlert = """
            {
                "vehicleId": "KOMATSU-930E-001",
                "alertType": "OVERHEATING",
                "severity": "CRITICAL",
                "message": "Engine overheating: 98.5°C sustained for 2 minutes",
                "timestamp": "2025-11-30T02:00:00Z"
            }
            """;
        
        // Process the alert
        alertConsumer.consumeAlert(criticalAlert);
        
        // Verify the metric was recorded (metric name uses underscores and _total suffix)
        double criticalCount = meterRegistry.find("ahs_fleet_alerts_critical_total").counter().count();
        assertEquals(1.0, criticalCount);
    }

    @Test
    @DisplayName("Should track multiple alert severities independently")
    void testMultipleSeverityMetrics() {
        // Send critical alert
        alertConsumer.consumeAlert(createAlertJson("KOMATSU-930E-001", "OVERHEATING", "CRITICAL"));
        alertConsumer.consumeAlert(createAlertJson("KOMATSU-930E-002", "COLLISION", "CRITICAL"));
        
        // Send warning alerts
        alertConsumer.consumeAlert(createAlertJson("KOMATSU-930E-003", "LOW_FUEL", "WARNING"));
        alertConsumer.consumeAlert(createAlertJson("KOMATSU-930E-004", "RAPID_DECELERATION", "WARNING"));
        alertConsumer.consumeAlert(createAlertJson("KOMATSU-930E-005", "TIRE_PRESSURE_LOW", "WARNING"));
        
        // Send error alert
        alertConsumer.consumeAlert(createAlertJson("KOMATSU-980E-001", "SENSOR_FAILURE", "ERROR"));
        
        // Verify all counters (using correct metric names)
        assertEquals(2.0, meterRegistry.find("ahs_fleet_alerts_critical_total").counter().count());
        assertEquals(3.0, meterRegistry.find("ahs_fleet_alerts_warning_total").counter().count());
        assertEquals(1.0, meterRegistry.find("ahs_fleet_alerts_error_total").counter().count());
    }

    private String createAlertJson(String vehicleId, String alertType, String severity) {
        return String.format("""
            {
                "vehicleId": "%s",
                "alertType": "%s",
                "severity": "%s",
                "message": "Test alert",
                "timestamp": "2025-11-30T02:00:00Z"
            }
            """, vehicleId, alertType, severity);
    }
}
