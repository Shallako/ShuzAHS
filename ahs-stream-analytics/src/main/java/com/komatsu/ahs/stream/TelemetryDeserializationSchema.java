package com.komatsu.ahs.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.komatsu.ahs.domain.model.VehicleTelemetry;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.io.IOException;

/**
 * Kafka deserializer for VehicleTelemetry JSON messages
 */
public class TelemetryDeserializationSchema implements DeserializationSchema<VehicleTelemetry> {
    
    private static final long serialVersionUID = 1L;
    private transient ObjectMapper objectMapper;
    
    @Override
    public void open(InitializationContext context) {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Override
    public VehicleTelemetry deserialize(byte[] message) throws IOException {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
        }
        return objectMapper.readValue(message, VehicleTelemetry.class);
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
