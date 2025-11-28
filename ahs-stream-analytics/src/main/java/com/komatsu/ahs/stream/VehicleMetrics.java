package com.komatsu.ahs.stream;

import com.komatsu.ahs.domain.model.VehicleTelemetry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Aggregated vehicle metrics over a time window
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleMetrics implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String vehicleId;
    private long windowStart;
    private long windowEnd;
    
    // Speed metrics
    private double avgSpeedKph;
    private double maxSpeedKph;
    private double minSpeedKph;
    
    // Distance traveled
    private double distanceTraveledMeters;
    
    // Fuel/Battery consumption
    private double avgFuelLevelPercent;
    private double avgBatteryLevelPercent;
    private double fuelConsumedPercent;
    
    // Operational metrics
    private int dataPointCount;
    private double avgPayloadTons;
    private long loadedTimeMs;
    
    // Temperature metrics
    private double avgEngineTemperature;
    private double maxEngineTemperature;
    
    // Alert counts
    private int warningCount;
    private int criticalAlertCount;
}
