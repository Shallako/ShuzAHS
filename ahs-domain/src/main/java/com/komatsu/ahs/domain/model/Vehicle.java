package com.komatsu.ahs.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Represents an autonomous haul truck in the fleet
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String vehicleId;
    private String model;         // e.g., "930E", "980E"
    private String manufacturer;  // e.g., "Komatsu"
    private Double capacity;      // Payload capacity in tons
    private VehicleStatus status; // Current operational status
    private VehicleTelemetry currentTelemetry;
    
    private String assignedRouteId;
    private String currentLocationId;  // e.g., loading point, dump point
    private String destinationId;
    
    private Instant lastUpdateTime;
    private Instant lastMaintenanceTime;
    private Instant nextScheduledMaintenanceTime;
    
    private boolean autonomousModeEnabled;
    private boolean operationalStatus;
    
    // Capabilities
    private int maxPayloadTons;
    private double maxSpeedKph;
    private String powerType;  // "DIESEL", "ELECTRIC", "BATTERY_ELECTRIC"
    
    // Safety envelope dimensions (meters)
    private double safetyEnvelopeFront;
    private double safetyEnvelopeRear;
    private double safetyEnvelopeSides;
}
