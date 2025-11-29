package com.komatsu.ahs.domain.events;

import com.komatsu.ahs.domain.model.GpsCoordinate;
import com.komatsu.ahs.domain.model.VehicleTelemetry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Vehicle Telemetry Event Tests")
class VehicleTelemetryEventTest {

    @Test
    @DisplayName("Should create telemetry event with constructor")
    void testCreateEventWithConstructor() {
        Instant now = Instant.now();
        
        VehicleTelemetry telemetry = VehicleTelemetry.builder()
            .vehicleId("KOMATSU-930E-001")
            .speedKph(35.5)
            .fuelLevelPercent(75.0)
            .location(GpsCoordinate.builder()
                .latitude(-23.4)
                .longitude(-70.35)
                .altitude(2900.0)
                .build())
            .timestamp(now)
            .build();
        
        VehicleTelemetryEvent event = new VehicleTelemetryEvent("KOMATSU-930E-001", telemetry);
        
        assertNotNull(event);
        assertNotNull(event.getEventId());
        assertEquals("KOMATSU-930E-001", event.getVehicleId());
        assertEquals(VehicleTelemetryEvent.EventType.TELEMETRY_UPDATE, event.getEventType());
        assertNotNull(event.getTelemetry());
        assertNotNull(event.getTimestamp());
    }

    @Test
    @DisplayName("Should support all event types")
    void testAllEventTypes() {
        VehicleTelemetryEvent.EventType[] types = {
            VehicleTelemetryEvent.EventType.TELEMETRY_UPDATE,
            VehicleTelemetryEvent.EventType.POSITION_UPDATE,
            VehicleTelemetryEvent.EventType.STATUS_CHANGE,
            VehicleTelemetryEvent.EventType.ALERT
        };
        
        for (VehicleTelemetryEvent.EventType type : types) {
            VehicleTelemetry telemetry = VehicleTelemetry.builder()
                .vehicleId("TEST-001")
                .build();
                
            VehicleTelemetryEvent event = new VehicleTelemetryEvent("TEST-001", telemetry, type);
            
            assertEquals(type, event.getEventType());
        }
    }

    @Test
    @DisplayName("Should create position update event")
    void testPositionUpdateEvent() {
        VehicleTelemetry telemetry = VehicleTelemetry.builder()
            .vehicleId("KOMATSU-930E-001")
            .build();
            
        VehicleTelemetryEvent event = new VehicleTelemetryEvent(
            "KOMATSU-930E-001", 
            telemetry,
            VehicleTelemetryEvent.EventType.POSITION_UPDATE
        );
        
        assertEquals(VehicleTelemetryEvent.EventType.POSITION_UPDATE, event.getEventType());
    }

    @Test
    @DisplayName("Should create status change event")
    void testStatusChangeEvent() {
        VehicleTelemetry telemetry = VehicleTelemetry.builder()
            .vehicleId("KOMATSU-930E-001")
            .build();
            
        VehicleTelemetryEvent event = new VehicleTelemetryEvent(
            "KOMATSU-930E-001",
            telemetry,
            VehicleTelemetryEvent.EventType.STATUS_CHANGE
        );
        
        assertEquals(VehicleTelemetryEvent.EventType.STATUS_CHANGE, event.getEventType());
    }

    @Test
    @DisplayName("Should create alert event")
    void testAlertEvent() {
        VehicleTelemetry telemetry = VehicleTelemetry.builder()
            .vehicleId("KOMATSU-930E-001")
            .build();
            
        VehicleTelemetryEvent event = new VehicleTelemetryEvent(
            "KOMATSU-930E-001",
            telemetry,
            VehicleTelemetryEvent.EventType.ALERT
        );
        event.setSource("flink-processor");
        
        assertEquals(VehicleTelemetryEvent.EventType.ALERT, event.getEventType());
        assertEquals("flink-processor", event.getSource());
    }

    @Test
    @DisplayName("Should support equality based on eventId")
    void testEventEquality() {
        VehicleTelemetry telemetry = VehicleTelemetry.builder()
            .vehicleId("KOMATSU-930E-001")
            .build();
            
        VehicleTelemetryEvent event1 = new VehicleTelemetryEvent("KOMATSU-930E-001", telemetry);
        String eventId = event1.getEventId();
        
        VehicleTelemetryEvent event2 = new VehicleTelemetryEvent("DIFFERENT-VEHICLE", telemetry);
        event2.setEventId(eventId);
        
        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    @DisplayName("Should auto-generate timestamp")
    void testEventTimestamp() {
        Instant before = Instant.now();
        
        VehicleTelemetry telemetry = VehicleTelemetry.builder()
            .vehicleId("KOMATSU-930E-001")
            .build();
            
        VehicleTelemetryEvent event = new VehicleTelemetryEvent("KOMATSU-930E-001", telemetry);
        
        Instant after = Instant.now();
        
        assertNotNull(event.getTimestamp());
        assertTrue(!event.getTimestamp().isBefore(before));
        assertTrue(!event.getTimestamp().isAfter(after));
    }

    @Test
    @DisplayName("Should track event source")
    void testEventSource() {
        VehicleTelemetry telemetry = VehicleTelemetry.builder()
            .vehicleId("KOMATSU-930E-001")
            .build();
            
        VehicleTelemetryEvent event = new VehicleTelemetryEvent("KOMATSU-930E-001", telemetry);
        event.setSource("vehicle-embedded-system");
        
        assertEquals("vehicle-embedded-system", event.getSource());
    }

    @Test
    @DisplayName("Should auto-generate unique event IDs")
    void testUniqueEventIds() {
        VehicleTelemetry telemetry = VehicleTelemetry.builder()
            .vehicleId("KOMATSU-930E-001")
            .build();
            
        VehicleTelemetryEvent event1 = new VehicleTelemetryEvent("KOMATSU-930E-001", telemetry);
        VehicleTelemetryEvent event2 = new VehicleTelemetryEvent("KOMATSU-930E-001", telemetry);
        
        assertNotNull(event1.getEventId());
        assertNotNull(event2.getEventId());
        assertNotEquals(event1.getEventId(), event2.getEventId());
    }

    @Test
    @DisplayName("Should have toString method")
    void testToString() {
        VehicleTelemetry telemetry = VehicleTelemetry.builder()
            .vehicleId("KOMATSU-930E-001")
            .build();
            
        VehicleTelemetryEvent event = new VehicleTelemetryEvent("KOMATSU-930E-001", telemetry);
        
        String str = event.toString();
        assertNotNull(str);
        assertTrue(str.contains("VehicleTelemetryEvent"));
        assertTrue(str.contains(event.getEventId()));
    }
}
