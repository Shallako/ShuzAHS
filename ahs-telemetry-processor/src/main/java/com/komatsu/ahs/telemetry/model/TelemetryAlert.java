package com.komatsu.ahs.telemetry.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Telemetry Alert
 * 
 * Represents an alert generated from telemetry anomaly detection
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryAlert implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule());
    
    @JsonProperty("alert_id")
    private String alertId;
    
    @JsonProperty("vehicle_id")
    private String vehicleId;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("alert_type")
    private AlertType alertType;
    
    @JsonProperty("severity")
    private AlertSeverity severity;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("metric_value")
    private double metricValue;
    
    @JsonProperty("threshold_value")
    private double thresholdValue;
    
    /**
     * Alert Type Enumeration
     */
    public enum AlertType {
        HIGH_TEMPERATURE,
        LOW_FUEL,
        LOW_BATTERY,
        TIRE_PRESSURE_LOW,
        TIRE_TEMPERATURE_HIGH,
        BRAKE_PRESSURE_LOW,
        ENGINE_WARNING,
        OVERLOAD,
        DIAGNOSTIC_ERROR,
        SPEED_VIOLATION,
        RAPID_DECELERATION,
        OVERHEATING
    }
    
    /**
     * Alert Severity Levels
     */
    public enum AlertSeverity {
        INFO,
        WARNING,
        CRITICAL,
        EMERGENCY
    }
    
    /**
     * Convert to JSON string
     */
    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize TelemetryAlert to JSON", e);
        }
    }
}
