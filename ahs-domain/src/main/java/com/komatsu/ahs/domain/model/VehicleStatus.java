package com.komatsu.ahs.domain.model;

/**
 * Vehicle Status Enumeration
 * 
 * Represents the operational status of an autonomous vehicle
 */
public enum VehicleStatus {
    /**
     * Vehicle is idle and available for assignment
     */
    IDLE,
    
    /**
     * Vehicle is routing to a loading point
     */
    ROUTING,
    
    /**
     * Vehicle is being loaded with material
     */
    LOADING,
    
    /**
     * Vehicle is hauling material to dump location
     */
    HAULING,
    
    /**
     * Vehicle is dumping its load
     */
    DUMPING,
    
    /**
     * Vehicle is returning empty to loading area
     */
    RETURNING,
    
    /**
     * Vehicle is undergoing maintenance
     */
    MAINTENANCE,
    
    /**
     * Vehicle is offline/not operational
     */
    OFFLINE,
    
    /**
     * Emergency stop has been triggered
     */
    EMERGENCY_STOP,
    
    /**
     * Vehicle has encountered an error
     */
    ERROR
}
