package com.komatsu.ahs.telemetry.function;

import com.komatsu.ahs.domain.model.VehicleTelemetry;
import com.komatsu.ahs.telemetry.model.VehicleMetrics;
import org.apache.flink.api.common.functions.AggregateFunction;

/**
 * Telemetry Windowing Aggregator
 * 
 * Aggregates telemetry data within time windows to compute metrics
 * like average speed, total distance, fuel consumption, etc.
 */
public class TelemetryWindowAggregator 
        implements AggregateFunction<VehicleTelemetry, VehicleMetrics.Accumulator, VehicleMetrics> {
    
    @Override
    public VehicleMetrics.Accumulator createAccumulator() {
        return new VehicleMetrics.Accumulator();
    }
    
    @Override
    public VehicleMetrics.Accumulator add(VehicleTelemetry telemetry, 
                                          VehicleMetrics.Accumulator accumulator) {
        accumulator.count++;
        accumulator.totalSpeed += telemetry.getSpeedKph();
        accumulator.totalFuelLevel += telemetry.getFuelLevelPercent();
        
        if (accumulator.minSpeed == null || telemetry.getSpeedKph() < accumulator.minSpeed) {
            accumulator.minSpeed = telemetry.getSpeedKph();
        }
        if (accumulator.maxSpeed == null || telemetry.getSpeedKph() > accumulator.maxSpeed) {
            accumulator.maxSpeed = telemetry.getSpeedKph();
        }
        
        if (accumulator.lastTelemetry != null && telemetry.getLocation() != null && 
            accumulator.lastTelemetry.getLocation() != null) {
            // Calculate distance traveled using simple approximation
            double distanceKm = calculateDistance(
                accumulator.lastTelemetry.getLocation().getLatitude(),
                accumulator.lastTelemetry.getLocation().getLongitude(),
                telemetry.getLocation().getLatitude(),
                telemetry.getLocation().getLongitude()
            );
            accumulator.totalDistance += distanceKm;
        }
        
        accumulator.lastTelemetry = telemetry;
        return accumulator;
    }
    
    @Override
    public VehicleMetrics getResult(VehicleMetrics.Accumulator accumulator) {
        if (accumulator.count == 0) {
            return null;
        }
        
        VehicleMetrics metrics = new VehicleMetrics();
        metrics.setVehicleId(accumulator.lastTelemetry.getVehicleId());
        metrics.setAverageSpeed(accumulator.totalSpeed / accumulator.count);
        metrics.setMinSpeed(accumulator.minSpeed);
        metrics.setMaxSpeed(accumulator.maxSpeed);
        metrics.setTotalDistance(accumulator.totalDistance);
        metrics.setAverageFuelLevel(accumulator.totalFuelLevel / accumulator.count);
        metrics.setDataPointCount(accumulator.count);
        
        return metrics;
    }
    
    @Override
    public VehicleMetrics.Accumulator merge(VehicleMetrics.Accumulator a, 
                                           VehicleMetrics.Accumulator b) {
        a.count += b.count;
        a.totalSpeed += b.totalSpeed;
        a.totalFuelLevel += b.totalFuelLevel;
        a.totalDistance += b.totalDistance;
        
        if (b.minSpeed != null && (a.minSpeed == null || b.minSpeed < a.minSpeed)) {
            a.minSpeed = b.minSpeed;
        }
        if (b.maxSpeed != null && (a.maxSpeed == null || b.maxSpeed > a.maxSpeed)) {
            a.maxSpeed = b.maxSpeed;
        }
        
        return a;
    }

    /**
     * Calculate distance between two GPS coordinates using Haversine formula
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }
}
