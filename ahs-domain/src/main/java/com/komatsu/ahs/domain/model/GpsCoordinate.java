package com.komatsu.ahs.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * GPS Coordinate data with high precision for autonomous navigation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GpsCoordinate implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private double latitude;
    private double longitude;
    private double altitude;  // meters above sea level
    private double accuracy;  // horizontal accuracy in meters
    private double heading;   // degrees from true north (0-359)
    private double speed;     // meters per second
    private Instant timestamp;
    
    /**
     * Calculate distance to another coordinate using Haversine formula
     * @param other the other GPS coordinate
     * @return distance in meters
     */
    public double distanceTo(GpsCoordinate other) {
        final double EARTH_RADIUS = 6371000; // meters
        
        double lat1Rad = Math.toRadians(this.latitude);
        double lat2Rad = Math.toRadians(other.latitude);
        double deltaLat = Math.toRadians(other.latitude - this.latitude);
        double deltaLon = Math.toRadians(other.longitude - this.longitude);
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS * c;
    }
    
    /**
     * Calculate bearing to another coordinate
     * @param other the destination coordinate
     * @return bearing in degrees (0-359)
     */
    public double bearingTo(GpsCoordinate other) {
        double lat1Rad = Math.toRadians(this.latitude);
        double lat2Rad = Math.toRadians(other.latitude);
        double deltaLon = Math.toRadians(other.longitude - this.longitude);
        
        double y = Math.sin(deltaLon) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                   Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLon);
        
        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360) % 360;
    }
}
