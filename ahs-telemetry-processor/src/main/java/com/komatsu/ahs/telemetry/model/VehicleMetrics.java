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
 * Vehicle Metrics - Aggregated telemetry metrics
 * 
 * Represents aggregated vehicle performance metrics
 * calculated over time windows (e.g., 1 minute, 5 minutes)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleMetrics implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule());
    
    @JsonProperty("vehicle_id")
    private String vehicleId;
    
    @JsonProperty("window_start")
    private Instant windowStart;
    
    @JsonProperty("window_end")
    private Instant windowEnd;
    
    @JsonProperty("record_count")
    private long recordCount;
    
    // Speed metrics
    @JsonProperty("avg_speed_kph")
    private double avgSpeedKph;
    
    @JsonProperty("max_speed_kph")
    private double maxSpeedKph;
    
    @JsonProperty("min_speed_kph")
    private double minSpeedKph;
    
    // Fuel/Battery metrics
    @JsonProperty("avg_fuel_level_percent")
    private double avgFuelLevelPercent;
    
    @JsonProperty("avg_battery_level_percent")
    private double avgBatteryLevelPercent;
    
    // Engine metrics
    @JsonProperty("avg_engine_temp_celsius")
    private double avgEngineTempCelsius;
    
    @JsonProperty("max_engine_temp_celsius")
    private double maxEngineTempCelsius;
    
    @JsonProperty("avg_engine_rpm")
    private double avgEngineRpm;
    
    // Payload metrics
    @JsonProperty("avg_payload_tons")
    private double avgPayloadTons;
    
    @JsonProperty("max_payload_tons")
    private double maxPayloadTons;
    
    @JsonProperty("loaded_percentage")
    private double loadedPercentage;
    
    // Distance and operational metrics
    @JsonProperty("total_distance_meters")
    private long totalDistanceMeters;
    
    @JsonProperty("operating_hours")
    private long operatingHours;
    
    // Tire metrics
    @JsonProperty("avg_tire_pressure_psi")
    private double avgTirePressurePsi;
    
    @JsonProperty("avg_tire_temp_celsius")
    private double avgTireTempCelsius;
    
    // Brake metrics
    @JsonProperty("avg_brake_pressure_psi")
    private double avgBrakePressurePsi;
    
    // Diagnostics
    @JsonProperty("warning_count")
    private long warningCount;
    
    @JsonProperty("diagnostic_codes")
    private long diagnosticCodes;
    
    /**
     * Convert to JSON string
     */
    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize VehicleMetrics to JSON", e);
        }
    }

    /**
     * Accumulator for aggregating telemetry data
     */
    public static class Accumulator implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public long count = 0;
        public double totalSpeed = 0;
        public double totalFuelLevel = 0;
        public Double minSpeed = null;
        public Double maxSpeed = null;
        public double totalDistance = 0;
        public com.komatsu.ahs.domain.model.VehicleTelemetry lastTelemetry = null;
    }
    
    // Additional convenience setters for aggregator
    public void setDataPointCount(long count) {
        this.recordCount = count;
    }
    
    public void setAverageSpeed(double speed) {
        this.avgSpeedKph = speed;
    }
    
    public void setMinSpeed(Double speed) {
        this.minSpeedKph = speed != null ? speed : 0.0;
    }
    
    public void setMaxSpeed(Double speed) {
        this.maxSpeedKph = speed != null ? speed : 0.0;
    }
    
    public void setTotalDistance(double distance) {
        this.totalDistanceMeters = (long)(distance * 1000); // km to meters
    }
    
    public void setAverageFuelLevel(double fuelLevel) {
        this.avgFuelLevelPercent = fuelLevel;
    }
}
