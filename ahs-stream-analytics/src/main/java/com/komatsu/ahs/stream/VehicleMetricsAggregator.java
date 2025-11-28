package com.komatsu.ahs.stream;

import com.komatsu.ahs.domain.model.VehicleTelemetry;
import org.apache.flink.api.common.functions.AggregateFunction;

/**
 * Aggregate function to compute vehicle metrics over a time window
 */
public class VehicleMetricsAggregator 
        implements AggregateFunction<VehicleTelemetry, VehicleMetricsAccumulator, VehicleMetrics> {
    
    @Override
    public VehicleMetricsAccumulator createAccumulator() {
        return new VehicleMetricsAccumulator();
    }
    
    @Override
    public VehicleMetricsAccumulator add(VehicleTelemetry telemetry, VehicleMetricsAccumulator acc) {
        acc.vehicleId = telemetry.getVehicleId();
        acc.count++;
        
        // Speed metrics
        acc.totalSpeed += telemetry.getSpeedKph();
        acc.maxSpeed = Math.max(acc.maxSpeed, telemetry.getSpeedKph());
        acc.minSpeed = acc.minSpeed == 0 ? telemetry.getSpeedKph() : 
                       Math.min(acc.minSpeed, telemetry.getSpeedKph());
        
        // Fuel/Battery
        acc.totalFuel += telemetry.getFuelLevelPercent();
        acc.totalBattery += telemetry.getBatteryLevelPercent();
        
        // Payload
        acc.totalPayload += telemetry.getPayloadTons();
        if (telemetry.isLoaded()) {
            acc.loadedCount++;
        }
        
        // Temperature
        acc.totalEngineTemp += telemetry.getEngineTemperatureCelsius();
        acc.maxEngineTemp = Math.max(acc.maxEngineTemp, telemetry.getEngineTemperatureCelsius());
        
        // Warnings
        if (telemetry.isWarningLight()) {
            acc.warningCount++;
        }
        if (telemetry.isCheckEngineLight()) {
            acc.criticalCount++;
        }
        
        // Track first and last telemetry for distance calculation
        if (acc.firstTelemetry == null) {
            acc.firstTelemetry = telemetry;
        }
        acc.lastTelemetry = telemetry;
        
        return acc;
    }
    
    @Override
    public VehicleMetricsAccumulator merge(VehicleMetricsAccumulator a, VehicleMetricsAccumulator b) {
        a.count += b.count;
        a.totalSpeed += b.totalSpeed;
        a.maxSpeed = Math.max(a.maxSpeed, b.maxSpeed);
        a.minSpeed = Math.min(a.minSpeed, b.minSpeed);
        a.totalFuel += b.totalFuel;
        a.totalBattery += b.totalBattery;
        a.totalPayload += b.totalPayload;
        a.loadedCount += b.loadedCount;
        a.totalEngineTemp += b.totalEngineTemp;
        a.maxEngineTemp = Math.max(a.maxEngineTemp, b.maxEngineTemp);
        a.warningCount += b.warningCount;
        a.criticalCount += b.criticalCount;
        return a;
    }
    
    @Override
    public VehicleMetrics getResult(VehicleMetricsAccumulator acc) {
        VehicleMetrics metrics = new VehicleMetrics();
        metrics.setVehicleId(acc.vehicleId);
        metrics.setDataPointCount(acc.count);
        
        if (acc.count > 0) {
            metrics.setAvgSpeedKph(acc.totalSpeed / acc.count);
            metrics.setMaxSpeedKph(acc.maxSpeed);
            metrics.setMinSpeedKph(acc.minSpeed);
            metrics.setAvgFuelLevelPercent(acc.totalFuel / acc.count);
            metrics.setAvgBatteryLevelPercent(acc.totalBattery / acc.count);
            metrics.setAvgPayloadTons(acc.totalPayload / acc.count);
            metrics.setAvgEngineTemperature(acc.totalEngineTemp / acc.count);
            metrics.setMaxEngineTemperature(acc.maxEngineTemp);
        }
        
        // Calculate distance if we have GPS data
        if (acc.firstTelemetry != null && acc.lastTelemetry != null) {
            double distance = acc.firstTelemetry.getLocation()
                    .distanceTo(acc.lastTelemetry.getLocation());
            metrics.setDistanceTraveledMeters(distance);
        }
        
        metrics.setWarningCount(acc.warningCount);
        metrics.setCriticalAlertCount(acc.criticalCount);
        
        return metrics;
    }
}
