package com.komatsu.ahs.telemetry.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.komatsu.ahs.domain.model.GpsCoordinate;
import com.komatsu.ahs.domain.model.VehicleTelemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryDeserializerTest {

    private TelemetryDeserializer deserializer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        deserializer = new TelemetryDeserializer();
        deserializer.open(null);
        
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testDeserializeValidTelemetry() throws Exception {
        VehicleTelemetry original = createSampleTelemetry();
        byte[] jsonBytes = objectMapper.writeValueAsBytes(original);
        
        VehicleTelemetry deserialized = deserializer.deserialize(jsonBytes);
        
        assertNotNull(deserialized);
        assertEquals(original.getVehicleId(), deserialized.getVehicleId());
        assertEquals(original.getSpeedKph(), deserialized.getSpeedKph());
        assertEquals(original.getFuelLevelPercent(), deserialized.getFuelLevelPercent());
    }

    @Test
    void testDeserializeInvalidJson() throws Exception {
        byte[] invalidJson = "invalid json".getBytes();
        
        VehicleTelemetry result = deserializer.deserialize(invalidJson);
        
        assertNull(result); // Should return null on error
    }

    @Test
    void testIsEndOfStream() {
        VehicleTelemetry telemetry = createSampleTelemetry();
        
        assertFalse(deserializer.isEndOfStream(telemetry));
    }

    @Test
    void testGetProducedType() {
        assertNotNull(deserializer.getProducedType());
        assertEquals(VehicleTelemetry.class, deserializer.getProducedType().getTypeClass());
    }

    private VehicleTelemetry createSampleTelemetry() {
        return VehicleTelemetry.builder()
            .vehicleId("KOMATSU-930E-001")
            .speedKph(45.5)
            .fuelLevelPercent(75.0)
            .batteryLevelPercent(80.0)
            .location(GpsCoordinate.builder()
                .latitude(-23.5505)
                .longitude(-46.6333)
                .altitude(800.0)
                .timestamp(Instant.now())
                .build())
            .timestamp(Instant.now())
            .build();
    }
}
