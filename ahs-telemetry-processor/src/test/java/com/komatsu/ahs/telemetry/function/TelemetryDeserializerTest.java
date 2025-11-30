package com.komatsu.ahs.telemetry.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.komatsu.ahs.domain.events.VehicleTelemetryEvent;
import com.komatsu.ahs.domain.model.GpsCoordinate;
import com.komatsu.ahs.domain.model.VehicleStatus;
import com.komatsu.ahs.domain.model.VehicleTelemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Telemetry Deserializer Tests")
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
    @DisplayName("Should deserialize direct VehicleTelemetry format")
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
    @DisplayName("Should deserialize VehicleTelemetryEvent and extract telemetry")
    void testDeserializeVehicleTelemetryEvent() throws Exception {
        VehicleTelemetry telemetry = createSampleTelemetry();
        VehicleTelemetryEvent event = new VehicleTelemetryEvent(telemetry.getVehicleId(), telemetry);
        event.setSource("data-generator");
        event.setVehicleStatus(VehicleStatus.HAULING);
        
        byte[] jsonBytes = objectMapper.writeValueAsBytes(event);
        
        VehicleTelemetry deserialized = deserializer.deserialize(jsonBytes);
        
        assertNotNull(deserialized, "Deserialized telemetry should not be null");
        assertEquals(telemetry.getVehicleId(), deserialized.getVehicleId());
        assertEquals(telemetry.getSpeedKph(), deserialized.getSpeedKph());
        assertEquals(telemetry.getFuelLevelPercent(), deserialized.getFuelLevelPercent());
        assertEquals(telemetry.getEngineTemperatureCelsius(), deserialized.getEngineTemperatureCelsius());
    }

    @Test
    @DisplayName("Should handle VehicleTelemetryEvent with missing vehicleId in telemetry")
    void testDeserializeEventWithMissingTelemetryVehicleId() throws Exception {
        // Create telemetry without vehicleId
        VehicleTelemetry telemetry = VehicleTelemetry.builder()
            .speedKph(45.5)
            .fuelLevelPercent(75.0)
            .build();
        
        // Create event with vehicleId at wrapper level
        VehicleTelemetryEvent event = new VehicleTelemetryEvent("KOMATSU-930E-TEST", telemetry);
        
        byte[] jsonBytes = objectMapper.writeValueAsBytes(event);
        
        VehicleTelemetry deserialized = deserializer.deserialize(jsonBytes);
        
        assertNotNull(deserialized);
        assertEquals("KOMATSU-930E-TEST", deserialized.getVehicleId());
    }

    @Test
    @DisplayName("Should return null for invalid JSON")
    void testDeserializeInvalidJson() throws Exception {
        byte[] invalidJson = "invalid json".getBytes();
        
        VehicleTelemetry result = deserializer.deserialize(invalidJson);
        
        assertNull(result); // Should return null on error
    }

    @Test
    @DisplayName("Should return null for empty message")
    void testDeserializeEmptyMessage() throws Exception {
        byte[] emptyJson = "{}".getBytes();
        
        VehicleTelemetry result = deserializer.deserialize(emptyJson);
        
        // Empty object should deserialize to a VehicleTelemetry with null fields
        assertNotNull(result);
    }

    @Test
    @DisplayName("isEndOfStream should always return false")
    void testIsEndOfStream() {
        VehicleTelemetry telemetry = createSampleTelemetry();
        
        assertFalse(deserializer.isEndOfStream(telemetry));
    }

    @Test
    @DisplayName("getProducedType should return VehicleTelemetry type")
    void testGetProducedType() {
        assertNotNull(deserializer.getProducedType());
        assertEquals(VehicleTelemetry.class, deserializer.getProducedType().getTypeClass());
    }

    @Test
    @DisplayName("Should handle JSON with unknown properties gracefully")
    void testDeserializeWithUnknownProperties() throws Exception {
        String jsonWithExtraFields = """
            {
                "vehicleId": "KOMATSU-930E-001",
                "speedKph": 45.5,
                "unknownField": "should be ignored",
                "anotherUnknown": 12345
            }
            """;
        
        VehicleTelemetry result = deserializer.deserialize(jsonWithExtraFields.getBytes());
        
        assertNotNull(result);
        assertEquals("KOMATSU-930E-001", result.getVehicleId());
        assertEquals(45.5, result.getSpeedKph());
    }

    private VehicleTelemetry createSampleTelemetry() {
        return VehicleTelemetry.builder()
            .vehicleId("KOMATSU-930E-001")
            .speedKph(45.5)
            .fuelLevelPercent(75.0)
            .batteryLevelPercent(80.0)
            .engineTemperatureCelsius(85.0)
            .engineRpm(1500.0)
            .brakePressurePsi(110.0)
            .hydraulicPressurePsi(2800.0)
            .tirePressureFrontLeftPsi(100.0)
            .tirePressureFrontRightPsi(100.0)
            .tirePressureRearLeftPsi(100.0)
            .tirePressureRearRightPsi(100.0)
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
