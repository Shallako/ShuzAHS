package com.komatsu.ahs.fleet.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.komatsu.ahs.fleet.metrics.FleetMetricsExporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for AlertConsumer
 * 
 * Tests the consumption of Flink CEP alerts and their
 * proper routing to Prometheus metrics.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Alert Consumer Tests")
class AlertConsumerTest {

    @Mock
    private FleetMetricsExporter metricsExporter;
    
    private ObjectMapper objectMapper;
    private AlertConsumer alertConsumer;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        alertConsumer = new AlertConsumer(metricsExporter, objectMapper);
    }

    @Test
    @DisplayName("Should increment critical alert counter for CRITICAL severity")
    void testCriticalSeverityAlert() {
        String alertJson = """
            {
                "vehicleId": "KOMATSU-930E-001",
                "alertType": "OVERHEATING",
                "severity": "CRITICAL",
                "message": "Engine overheating: 98.5°C sustained for 2 minutes"
            }
            """;
        
        alertConsumer.consumeAlert(alertJson);
        
        verify(metricsExporter, times(1)).incrementCriticalAlert();
        verify(metricsExporter, never()).incrementWarningAlert();
        verify(metricsExporter, never()).incrementErrorAlert();
    }
    
    @Test
    @DisplayName("Should increment critical alert counter for EMERGENCY severity")
    void testEmergencySeverityAlert() {
        String alertJson = """
            {
                "vehicleId": "KOMATSU-930E-002",
                "alertType": "COLLISION_WARNING",
                "severity": "EMERGENCY",
                "message": "Potential collision detected"
            }
            """;
        
        alertConsumer.consumeAlert(alertJson);
        
        verify(metricsExporter, times(1)).incrementCriticalAlert();
        verify(metricsExporter, never()).incrementWarningAlert();
        verify(metricsExporter, never()).incrementErrorAlert();
    }
    
    @Test
    @DisplayName("Should increment warning alert counter for WARNING severity")
    void testWarningSeverityAlert() {
        String alertJson = """
            {
                "vehicleId": "KOMATSU-930E-003",
                "alertType": "RAPID_DECELERATION",
                "severity": "WARNING",
                "message": "Rapid deceleration detected: 55.0 kph to 8.0 kph in 5 seconds"
            }
            """;
        
        alertConsumer.consumeAlert(alertJson);
        
        verify(metricsExporter, times(1)).incrementWarningAlert();
        verify(metricsExporter, never()).incrementCriticalAlert();
        verify(metricsExporter, never()).incrementErrorAlert();
    }
    
    @Test
    @DisplayName("Should increment error alert counter for ERROR severity")
    void testErrorSeverityAlert() {
        String alertJson = """
            {
                "vehicleId": "KOMATSU-980E-001",
                "alertType": "SENSOR_MALFUNCTION",
                "severity": "ERROR",
                "message": "GPS sensor not responding"
            }
            """;
        
        alertConsumer.consumeAlert(alertJson);
        
        verify(metricsExporter, times(1)).incrementErrorAlert();
        verify(metricsExporter, never()).incrementCriticalAlert();
        verify(metricsExporter, never()).incrementWarningAlert();
    }

    @Test
    @DisplayName("Should handle LOW_FUEL alert type")
    void testLowFuelAlert() {
        String alertJson = """
            {
                "vehicleId": "KOMATSU-930E-004",
                "alertType": "LOW_FUEL",
                "severity": "WARNING",
                "message": "Low fuel level: 12.5%"
            }
            """;
        
        alertConsumer.consumeAlert(alertJson);
        
        verify(metricsExporter, times(1)).incrementWarningAlert();
    }
    
    @Test
    @DisplayName("Should handle critical LOW_FUEL alert")
    void testCriticalLowFuelAlert() {
        String alertJson = """
            {
                "vehicleId": "KOMATSU-930E-005",
                "alertType": "LOW_FUEL",
                "severity": "CRITICAL",
                "message": "Low fuel level: 3.2%"
            }
            """;
        
        alertConsumer.consumeAlert(alertJson);
        
        verify(metricsExporter, times(1)).incrementCriticalAlert();
    }
    
    @Test
    @DisplayName("Should handle RAPID_DECELERATION CEP pattern alert")
    void testRapidDecelerationCepAlert() {
        String alertJson = """
            {
                "vehicleId": "KOMATSU-980E-002",
                "alertType": "RAPID_DECELERATION",
                "severity": "WARNING",
                "message": "Rapid deceleration detected: 52.3 kph to 5.1 kph in 5 seconds"
            }
            """;
        
        alertConsumer.consumeAlert(alertJson);
        
        verify(metricsExporter, times(1)).incrementWarningAlert();
    }
    
    @Test
    @DisplayName("Should handle OVERHEATING CEP pattern alert")
    void testOverheatingCepAlert() {
        String alertJson = """
            {
                "vehicleId": "KOMATSU-930E-006",
                "alertType": "OVERHEATING",
                "severity": "CRITICAL",
                "message": "Engine overheating: 97.2°C sustained for 2 minutes"
            }
            """;
        
        alertConsumer.consumeAlert(alertJson);
        
        verify(metricsExporter, times(1)).incrementCriticalAlert();
    }

    @Test
    @DisplayName("Should handle lowercase severity")
    void testLowercaseSeverity() {
        String alertJson = """
            {
                "vehicleId": "KOMATSU-930E-007",
                "alertType": "TIRE_PRESSURE_LOW",
                "severity": "warning",
                "message": "Tire pressure low: front-left at 85 PSI"
            }
            """;
        
        alertConsumer.consumeAlert(alertJson);
        
        verify(metricsExporter, times(1)).incrementWarningAlert();
    }
    
    @Test
    @DisplayName("Should handle unknown severity gracefully")
    void testUnknownSeverity() {
        String alertJson = """
            {
                "vehicleId": "KOMATSU-930E-008",
                "alertType": "UNKNOWN_TYPE",
                "severity": "INFO",
                "message": "Informational message"
            }
            """;
        
        alertConsumer.consumeAlert(alertJson);
        
        // Should not increment any counter for unknown severity
        verify(metricsExporter, never()).incrementCriticalAlert();
        verify(metricsExporter, never()).incrementWarningAlert();
        verify(metricsExporter, never()).incrementErrorAlert();
    }
    
    @Test
    @DisplayName("Should handle malformed JSON gracefully")
    void testMalformedJson() {
        String malformedJson = "{ invalid json }";
        
        // Should not throw exception
        alertConsumer.consumeAlert(malformedJson);
        
        // Should not increment any counter
        verify(metricsExporter, never()).incrementCriticalAlert();
        verify(metricsExporter, never()).incrementWarningAlert();
        verify(metricsExporter, never()).incrementErrorAlert();
    }
    
    @Test
    @DisplayName("Should handle missing fields gracefully")
    void testMissingFields() {
        String alertJson = """
            {
                "vehicleId": "KOMATSU-930E-009"
            }
            """;
        
        // Should not throw exception
        alertConsumer.consumeAlert(alertJson);
        
        // Missing severity means no counter incremented
        verify(metricsExporter, never()).incrementCriticalAlert();
        verify(metricsExporter, never()).incrementWarningAlert();
        verify(metricsExporter, never()).incrementErrorAlert();
    }
    
    @Test
    @DisplayName("Should process multiple alerts sequentially")
    void testMultipleAlerts() {
        String criticalAlert = """
            {
                "vehicleId": "KOMATSU-930E-010",
                "alertType": "OVERHEATING",
                "severity": "CRITICAL",
                "message": "Critical overheating"
            }
            """;
        
        String warningAlert = """
            {
                "vehicleId": "KOMATSU-930E-011",
                "alertType": "LOW_FUEL",
                "severity": "WARNING",
                "message": "Low fuel warning"
            }
            """;
        
        alertConsumer.consumeAlert(criticalAlert);
        alertConsumer.consumeAlert(warningAlert);
        alertConsumer.consumeAlert(criticalAlert);
        
        verify(metricsExporter, times(2)).incrementCriticalAlert();
        verify(metricsExporter, times(1)).incrementWarningAlert();
    }
}
