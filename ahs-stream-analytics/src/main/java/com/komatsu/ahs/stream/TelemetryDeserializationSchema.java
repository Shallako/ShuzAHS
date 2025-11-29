package com.komatsu.ahs.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.komatsu.ahs.domain.events.VehicleTelemetryEvent;
import com.komatsu.ahs.domain.model.VehicleTelemetry;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.io.IOException;

/**
 * Kafka deserializer for VehicleTelemetryEvent JSON messages
 * Extracts the VehicleTelemetry from the event wrapper
 */
public class TelemetryDeserializationSchema implements DeserializationSchema<VehicleTelemetry> {
    
    private static final long serialVersionUID = 1L;
    private transient ObjectMapper objectMapper;
    
    @Override
    public void open(InitializationContext context) {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    @Override
    public VehicleTelemetry deserialize(byte[] message) throws IOException {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
        // Deserialize as VehicleTelemetryEvent, then extract the telemetry
        VehicleTelemetryEvent event = objectMapper.readValue(message, VehicleTelemetryEvent.class);
        return event.getTelemetry();
    }
    
    @Override
    public boolean isEndOfStream(VehicleTelemetry nextElement) {
        return false;
    }
    
    @Override
    public TypeInformation<VehicleTelemetry> getProducedType() {
        return TypeInformation.of(VehicleTelemetry.class);
    }
}
