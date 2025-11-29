package com.komatsu.ahs.domain.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VehicleCommandEventTest {

    @Test
    void testDefaultConstructor() {
        VehicleCommandEvent event = new VehicleCommandEvent();
        
        assertNotNull(event.getEventId());
        assertNotNull(event.getTimestamp());
        assertEquals(VehicleCommandEvent.Priority.NORMAL, event.getPriority());
    }

    @Test
    void testConstructorWithParameters() {
        VehicleCommandEvent event = new VehicleCommandEvent(
            "KOMATSU-930E-001",
            VehicleCommandEvent.CommandType.ROUTE_ASSIGNMENT,
            "{\"routeId\": \"ROUTE-001\"}"
        );
        
        assertEquals("KOMATSU-930E-001", event.getVehicleId());
        assertEquals(VehicleCommandEvent.CommandType.ROUTE_ASSIGNMENT, event.getCommandType());
        assertEquals("{\"routeId\": \"ROUTE-001\"}", event.getCommandPayload());
    }

    @Test
    void testPriorityLevels() {
        VehicleCommandEvent event = new VehicleCommandEvent();
        
        event.setPriority(VehicleCommandEvent.Priority.CRITICAL);
        assertEquals(VehicleCommandEvent.Priority.CRITICAL, event.getPriority());
        
        event.setPriority(VehicleCommandEvent.Priority.LOW);
        assertEquals(VehicleCommandEvent.Priority.LOW, event.getPriority());
    }

    @Test
    void testCommandTypes() {
        VehicleCommandEvent event = new VehicleCommandEvent();
        
        event.setCommandType(VehicleCommandEvent.CommandType.EMERGENCY_STOP);
        assertEquals(VehicleCommandEvent.CommandType.EMERGENCY_STOP, event.getCommandType());
        
        event.setCommandType(VehicleCommandEvent.CommandType.DUMP_LOAD);
        assertEquals(VehicleCommandEvent.CommandType.DUMP_LOAD, event.getCommandType());
    }

    @Test
    void testIssuedBy() {
        VehicleCommandEvent event = new VehicleCommandEvent();
        event.setIssuedBy("operator-001");
        
        assertEquals("operator-001", event.getIssuedBy());
    }

    @Test
    void testEqualsAndHashCode() {
        VehicleCommandEvent event1 = new VehicleCommandEvent();
        VehicleCommandEvent event2 = new VehicleCommandEvent();
        
        event2.setEventId(event1.getEventId());
        
        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    void testToString() {
        VehicleCommandEvent event = new VehicleCommandEvent(
            "KOMATSU-930E-001",
            VehicleCommandEvent.CommandType.EMERGENCY_STOP,
            ""
        );
        event.setPriority(VehicleCommandEvent.Priority.CRITICAL);
        
        String toString = event.toString();
        assertTrue(toString.contains("VehicleCommandEvent"));
        assertTrue(toString.contains("KOMATSU-930E-001"));
        assertTrue(toString.contains("EMERGENCY_STOP"));
        assertTrue(toString.contains("CRITICAL"));
    }
}
