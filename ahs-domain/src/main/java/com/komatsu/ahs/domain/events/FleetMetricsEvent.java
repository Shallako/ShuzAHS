package com.komatsu.ahs.domain.events;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event representing aggregated fleet-wide metrics
 * Published periodically for monitoring and analytics
 */
public class FleetMetricsEvent {
    
    private String eventId;
    private Instant timestamp;
    private int totalVehicles;
    private int activeVehicles;
    private int idleVehicles;
    private int routingVehicles;
    private int loadingVehicles;
    private int haulingVehicles;
    private int dumpingVehicles;
    private int emergencyStopVehicles;
    
    // Aggregated telemetry metrics
    private double averageSpeed;
    private double averageFuelLevel;
    private double averageEngineTemperature;
    private double averagePayload;
    
    // Alert metrics
    private int criticalAlertCount;
    private int warningAlertCount;
    private int errorAlertCount;
    
    private Map<String, Object> metadata;
    
    public FleetMetricsEvent() {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.metadata = new HashMap<>();
    }

    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public int getTotalVehicles() { return totalVehicles; }
    public void setTotalVehicles(int totalVehicles) { this.totalVehicles = totalVehicles; }
    
    public int getActiveVehicles() { return activeVehicles; }
    public void setActiveVehicles(int activeVehicles) { this.activeVehicles = activeVehicles; }
    
    public int getIdleVehicles() { return idleVehicles; }
    public void setIdleVehicles(int idleVehicles) { this.idleVehicles = idleVehicles; }
    
    public int getRoutingVehicles() { return routingVehicles; }
    public void setRoutingVehicles(int routingVehicles) { this.routingVehicles = routingVehicles; }
    
    public int getLoadingVehicles() { return loadingVehicles; }
    public void setLoadingVehicles(int loadingVehicles) { this.loadingVehicles = loadingVehicles; }
    
    public int getHaulingVehicles() { return haulingVehicles; }
    public void setHaulingVehicles(int haulingVehicles) { this.haulingVehicles = haulingVehicles; }
    
    public int getDumpingVehicles() { return dumpingVehicles; }
    public void setDumpingVehicles(int dumpingVehicles) { this.dumpingVehicles = dumpingVehicles; }
    
    public int getEmergencyStopVehicles() { return emergencyStopVehicles; }
    public void setEmergencyStopVehicles(int emergencyStopVehicles) { 
        this.emergencyStopVehicles = emergencyStopVehicles; 
    }
    
    public double getAverageSpeed() { return averageSpeed; }
    public void setAverageSpeed(double averageSpeed) { this.averageSpeed = averageSpeed; }
    
    public double getAverageFuelLevel() { return averageFuelLevel; }
    public void setAverageFuelLevel(double averageFuelLevel) { 
        this.averageFuelLevel = averageFuelLevel; 
    }
    
    public double getAverageEngineTemperature() { return averageEngineTemperature; }
    public void setAverageEngineTemperature(double averageEngineTemperature) { 
        this.averageEngineTemperature = averageEngineTemperature; 
    }
    
    public double getAveragePayload() { return averagePayload; }
    public void setAveragePayload(double averagePayload) { this.averagePayload = averagePayload; }
    
    public int getCriticalAlertCount() { return criticalAlertCount; }
    public void setCriticalAlertCount(int criticalAlertCount) { 
        this.criticalAlertCount = criticalAlertCount; 
    }
    
    public int getWarningAlertCount() { return warningAlertCount; }
    public void setWarningAlertCount(int warningAlertCount) { 
        this.warningAlertCount = warningAlertCount; 
    }
    
    public int getErrorAlertCount() { return errorAlertCount; }
    public void setErrorAlertCount(int errorAlertCount) { this.errorAlertCount = errorAlertCount; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    @Override
    public String toString() {
        return "FleetMetricsEvent{" +
                "eventId='" + eventId + '\'' +
                ", timestamp=" + timestamp +
                ", totalVehicles=" + totalVehicles +
                ", activeVehicles=" + activeVehicles +
                ", idleVehicles=" + idleVehicles +
                ", averageSpeed=" + averageSpeed +
                ", averageFuelLevel=" + averageFuelLevel +
                '}';
    }
}
