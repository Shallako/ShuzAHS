package com.komatsu.ahs.domain.events;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Event representing safety or operational alerts from autonomous vehicles.
 */
public class VehicleAlertEvent {
    private String eventId;
    private String vehicleId;
    private AlertType alertType;
    private Severity severity;
    private String message;
    private Instant timestamp;
    private Map<String, Object> metadata;
    private boolean acknowledged;

    public enum AlertType {
        // Safety Alerts
        COLLISION_WARNING,
        OBSTACLE_DETECTED,
        SAFETY_ENVELOPE_BREACH,
        
        // Operational Alerts
        LOW_FUEL,
        ENGINE_OVERHEATING,
        LOW_TIRE_PRESSURE,
        EXCESSIVE_SPEED,
        BRAKE_PRESSURE_LOW,
        HYDRAULIC_PRESSURE_LOW,
        
        // System Alerts
        SYSTEM_FAULT,
        LOW_BATTERY,
        MAINTENANCE_REQUIRED,
        COMMUNICATION_LOSS,
        
        // Navigation Alerts
        ROUTE_DEVIATION,
        STUCK_DETECTION,
        POSITIONING_ERROR
    }

    public enum Severity {
        INFO, WARNING, ERROR, CRITICAL
    }

    public VehicleAlertEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.metadata = new HashMap<>();
        this.acknowledged = false;
    }

    public VehicleAlertEvent(String vehicleId, AlertType alertType, Severity severity, String message) {
        this();
        this.vehicleId = vehicleId;
        this.alertType = alertType;
        this.severity = severity;
        this.message = message;
    }

    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
    public AlertType getAlertType() { return alertType; }
    public void setAlertType(AlertType alertType) { this.alertType = alertType; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public boolean isAcknowledged() { return acknowledged; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VehicleAlertEvent that = (VehicleAlertEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "VehicleAlertEvent{" +
                "eventId='" + eventId + '\'' +
                ", vehicleId='" + vehicleId + '\'' +
                ", alertType=" + alertType +
                ", severity=" + severity +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", acknowledged=" + acknowledged +
                '}';
    }
}
