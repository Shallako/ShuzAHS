package com.komatsu.ahs.domain.events;

import com.komatsu.ahs.domain.model.VehicleStatus;
import com.komatsu.ahs.domain.model.VehicleTelemetry;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Event representing vehicle telemetry data for streaming processing.
 * This event is published to Kafka when telemetry data is received from autonomous vehicles.
 */
public class VehicleTelemetryEvent {
    private String eventId;
    private String vehicleId;
    private VehicleTelemetry telemetry;
    private VehicleStatus vehicleStatus;
    private Instant timestamp;
    private String source;
    private EventType eventType;

    public enum EventType {
        TELEMETRY_UPDATE,
        POSITION_UPDATE,
        STATUS_CHANGE,
        ALERT
    }

    public VehicleTelemetryEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.eventType = EventType.TELEMETRY_UPDATE;
    }

    public VehicleTelemetryEvent(String vehicleId, VehicleTelemetry telemetry) {
        this();
        this.vehicleId = vehicleId;
        this.telemetry = telemetry;
    }

    public VehicleTelemetryEvent(String vehicleId, VehicleTelemetry telemetry, EventType eventType) {
        this(vehicleId, telemetry);
        this.eventType = eventType;
    }

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public VehicleTelemetry getTelemetry() {
        return telemetry;
    }

    public void setTelemetry(VehicleTelemetry telemetry) {
        this.telemetry = telemetry;
    }

    public VehicleStatus getVehicleStatus() {
        return vehicleStatus;
    }

    public void setVehicleStatus(VehicleStatus vehicleStatus) {
        this.vehicleStatus = vehicleStatus;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VehicleTelemetryEvent that = (VehicleTelemetryEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "VehicleTelemetryEvent{" +
                "eventId='" + eventId + '\'' +
                ", vehicleId='" + vehicleId + '\'' +
                ", timestamp=" + timestamp +
                ", eventType=" + eventType +
                ", source='" + source + '\'' +
                '}';
    }
}
