package com.komatsu.ahs.domain.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Event representing a vehicle command issued by the fleet management system.
 */
public class VehicleCommandEvent {
    private String eventId;
    private String vehicleId;
    private CommandType commandType;
    private String commandPayload;
    private Instant timestamp;
    private String issuedBy;
    private Priority priority;

    public enum CommandType {
        ROUTE_ASSIGNMENT,
        SPEED_ADJUSTMENT,
        EMERGENCY_STOP,
        RESUME_OPERATION,
        DUMP_LOAD,
        RETURN_TO_BASE,
        MAINTENANCE_MODE
    }

    public enum Priority {
        LOW, NORMAL, HIGH, CRITICAL
    }

    public VehicleCommandEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.priority = Priority.NORMAL;
    }

    public VehicleCommandEvent(String vehicleId, CommandType commandType, String commandPayload) {
        this();
        this.vehicleId = vehicleId;
        this.commandType = commandType;
        this.commandPayload = commandPayload;
    }

    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
    public CommandType getCommandType() { return commandType; }
    public void setCommandType(CommandType commandType) { this.commandType = commandType; }
    public String getCommandPayload() { return commandPayload; }
    public void setCommandPayload(String commandPayload) { this.commandPayload = commandPayload; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getIssuedBy() { return issuedBy; }
    public void setIssuedBy(String issuedBy) { this.issuedBy = issuedBy; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VehicleCommandEvent that = (VehicleCommandEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "VehicleCommandEvent{" +
                "eventId='" + eventId + '\'' +
                ", vehicleId='" + vehicleId + '\'' +
                ", commandType=" + commandType +
                ", priority=" + priority +
                ", timestamp=" + timestamp +
                '}';
    }
}
