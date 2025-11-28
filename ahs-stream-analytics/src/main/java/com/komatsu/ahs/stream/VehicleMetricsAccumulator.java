package com.komatsu.ahs.stream;

import com.komatsu.ahs.domain.model.VehicleTelemetry;

/**
 * Accumulator for vehicle metrics aggregation
 */
class VehicleMetricsAccumulator {
    String vehicleId;
    int count = 0;
    
    double totalSpeed = 0;
    double maxSpeed = 0;
    double minSpeed = 0;
    
    double totalFuel = 0;
    double totalBattery = 0;
    
    double totalPayload = 0;
    int loadedCount = 0;
    
    double totalEngineTemp = 0;
    double maxEngineTemp = 0;
    
    int warningCount = 0;
    int criticalCount = 0;
    
    VehicleTelemetry firstTelemetry;
    VehicleTelemetry lastTelemetry;
}
