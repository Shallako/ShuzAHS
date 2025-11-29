package com.komatsu.ahs.telemetry.function;

import com.komatsu.ahs.domain.model.VehicleTelemetry;
import com.komatsu.ahs.telemetry.model.VehicleMetrics;
import org.apache.flink.api.common.functions.AggregateFunction;

import java.time.Instant;

/**
 * Vehicle Metrics Aggregator
 * 
 * Aggregates telemetry data over time windows to compute vehicle metrics
 */
public class VehicleMetricsAggregator 
        implements AggregateFunction<VehicleTelemetry, VehicleMetricsAggregator.Accumulator, VehicleMetrics> {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Accumulator for aggregating metrics
     */
    public static class Accumulator {
        String vehicleId;
        Instant windowStart;
        Instant windowEnd;
        long count = 0;
        
        // Speed
        double totalSpeed = 0.0;
        double maxSpeed = Double.MIN_VALUE;
        double minSpeed = Double.MAX_VALUE;
        
        // Fuel/Battery
        double totalFuel = 0.0;
        double totalBattery = 0.0;
        
        // Engine
        double totalEngineTemp = 0.0;
        double maxEngineTemp = Double.MIN_VALUE;
        double totalEngineRpm = 0.0;
        
        // Payload
        double totalPayload = 0.0;
        double maxPayload = Double.MIN_VALUE;
        long loadedCount = 0;
        
        // Distance and hours
        long lastDistance = 0;
        long lastHours = 0;
        
        // Tires
        double totalTirePressure = 0.0;
        double totalTireTemp = 0.0;
        
        // Brakes
        double totalBrakePressure = 0.0;
        
        // Diagnostics
        long warningCount = 0;
        long diagnosticCodes = 0;
    }
    
    @Override
    public Accumulator createAccumulator() {
        return new Accumulator();
    }
    
    @Override
    public Accumulator add(VehicleTelemetry telemetry, Accumulator acc) {
        // Initialize on first record
        if (acc.count == 0) {
            acc.vehicleId = telemetry.getVehicleId();
            acc.windowStart = telemetry.getTimestamp();
        }
        acc.windowEnd = telemetry.getTimestamp();
        acc.count++;
        
        // Speed
        double speed = telemetry.getSpeedKph();
        acc.totalSpeed += speed;
        acc.maxSpeed = Math.max(acc.maxSpeed, speed);
        acc.minSpeed = Math.min(acc.minSpeed, speed);
        
        // Fuel/Battery
        acc.totalFuel += telemetry.getFuelLevelPercent();
        acc.totalBattery += telemetry.getBatteryLevelPercent();
        
        // Engine
        double engineTemp = telemetry.getEngineTemperatureCelsius();
        acc.totalEngineTemp += engineTemp;
        acc.maxEngineTemp = Math.max(acc.maxEngineTemp, engineTemp);
        acc.totalEngineRpm += telemetry.getEngineRpm();
        
        // Payload
        double payload = telemetry.getPayloadTons();
        acc.totalPayload += payload;
        acc.maxPayload = Math.max(acc.maxPayload, payload);
        if (telemetry.isLoaded()) {
            acc.loadedCount++;
        }
        
        // Distance and hours (take latest values)
        acc.lastDistance = telemetry.getTotalDistanceMeters();
        acc.lastHours = telemetry.getOperatingHours();
        
        // Tires (average of all four)
        double avgTirePressure = (telemetry.getTirePressureFrontLeftPsi() +
                                  telemetry.getTirePressureFrontRightPsi() +
                                  telemetry.getTirePressureRearLeftPsi() +
                                  telemetry.getTirePressureRearRightPsi()) / 4.0;
        acc.totalTirePressure += avgTirePressure;
        acc.totalTireTemp += telemetry.getTireTemperatureAvgCelsius();
        
        // Brakes
        acc.totalBrakePressure += telemetry.getBrakePressurePsi();
        
        // Diagnostics
        if (telemetry.isWarningLight()) {
            acc.warningCount++;
        }
        acc.diagnosticCodes += telemetry.getDiagnosticCodeCount();
        
        return acc;
    }
    
    @Override
    public VehicleMetrics getResult(Accumulator acc) {
        if (acc.count == 0) {
            return null;
        }
        
        return VehicleMetrics.builder()
            .vehicleId(acc.vehicleId)
            .windowStart(acc.windowStart)
            .windowEnd(acc.windowEnd)
            .recordCount(acc.count)
            .avgSpeedKph(acc.totalSpeed / acc.count)
            .maxSpeedKph(acc.maxSpeed)
            .minSpeedKph(acc.minSpeed)
            .avgFuelLevelPercent(acc.totalFuel / acc.count)
            .avgBatteryLevelPercent(acc.totalBattery / acc.count)
            .avgEngineTempCelsius(acc.totalEngineTemp / acc.count)
            .maxEngineTempCelsius(acc.maxEngineTemp)
            .avgEngineRpm(acc.totalEngineRpm / acc.count)
            .avgPayloadTons(acc.totalPayload / acc.count)
            .maxPayloadTons(acc.maxPayload)
            .loadedPercentage((acc.loadedCount * 100.0) / acc.count)
            .totalDistanceMeters(acc.lastDistance)
            .operatingHours(acc.lastHours)
            .avgTirePressurePsi(acc.totalTirePressure / acc.count)
            .avgTireTempCelsius(acc.totalTireTemp / acc.count)
            .avgBrakePressurePsi(acc.totalBrakePressure / acc.count)
            .warningCount(acc.warningCount)
            .diagnosticCodes(acc.diagnosticCodes)
            .build();
    }
    
    @Override
    public Accumulator merge(Accumulator a, Accumulator b) {
        a.count += b.count;
        a.totalSpeed += b.totalSpeed;
        a.maxSpeed = Math.max(a.maxSpeed, b.maxSpeed);
        a.minSpeed = Math.min(a.minSpeed, b.minSpeed);
        a.totalFuel += b.totalFuel;
        a.totalBattery += b.totalBattery;
        a.totalEngineTemp += b.totalEngineTemp;
        a.maxEngineTemp = Math.max(a.maxEngineTemp, b.maxEngineTemp);
        a.totalEngineRpm += b.totalEngineRpm;
        a.totalPayload += b.totalPayload;
        a.maxPayload = Math.max(a.maxPayload, b.maxPayload);
        a.loadedCount += b.loadedCount;
        a.lastDistance = Math.max(a.lastDistance, b.lastDistance);
        a.lastHours = Math.max(a.lastHours, b.lastHours);
        a.totalTirePressure += b.totalTirePressure;
        a.totalTireTemp += b.totalTireTemp;
        a.totalBrakePressure += b.totalBrakePressure;
        a.warningCount += b.warningCount;
        a.diagnosticCodes += b.diagnosticCodes;
        a.windowEnd = b.windowEnd;
        
        return a;
    }
}
