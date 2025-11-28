package com.komatsu.ahs.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Vehicle telemetry data from sensors and controllers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleTelemetry implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String vehicleId;
    private Instant timestamp;
    
    // Location and navigation
    private GpsCoordinate location;
    private double speedKph;
    private double headingDegrees;
    
    // Powertrain
    private double engineRpm;
    private double fuelLevelPercent;
    private double batteryLevelPercent;
    private double engineTemperatureCelsius;
    
    // Payload and load
    private double payloadTons;
    private boolean isLoaded;
    
    // Brakes and tires
    private double brakePressurePsi;
    private double tirePressureFrontLeftPsi;
    private double tirePressureFrontRightPsi;
    private double tirePressureRearLeftPsi;
    private double tirePressureRearRightPsi;
    private double tireTemperatureAvgCelsius;
    
    // Hydraulics
    private double hydraulicPressurePsi;
    private double hydraulicFluidTempCelsius;
    
    // Diagnostics
    private int diagnosticCodeCount;
    private boolean warningLight;
    private boolean checkEngineLight;
    
    // Operational metrics
    private long totalDistanceMeters;
    private long operatingHours;
    private int tripCount;
}
