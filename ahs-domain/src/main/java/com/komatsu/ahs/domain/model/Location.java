package com.komatsu.ahs.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Location in the mine site
 * 
 * Represents a named location such as loading point, dump point, or waypoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String locationId;
    private String name;
    private LocationType type;
    private GpsCoordinate coordinate;
    private String description;
    
    /**
     * Type of location
     */
    public enum LocationType {
        LOADING_POINT,
        DUMP_POINT,
        WAYPOINT,
        MAINTENANCE_AREA,
        PARKING_AREA,
        FUELING_STATION,
        CHARGING_STATION
    }
}
